import Foundation
import Combine
import SwiftUI

final class StudyPlanStore: ObservableObject {
    @Published var tasks: [Task] = []
    @Published var timer = TimerState()
    let timerEngine: TimerEngine
    @Published var nickname = "我的学习"
    @Published var signature = "更高效的学习方式"
    @Published var avatarText = "我"
    @Published var avatarImageData: Data?
    @Published var avatarFrameColor = "blue"
    @Published var remindersEnabled = true
    @Published var vibrateEnabled = true
    @Published var defaultTimerMinutes = 25
    @Published var manualCompletionEnabled = true
    @Published var isShowingAddTask = false
    @Published var isShowingSearch = false
    @Published var editingTask: Task?
    @Published var searchText = ""
    @Published var isBatchDeleting = false
    @Published var selectedTaskIDs: Set<UUID> = []
    @Published var subjects = ["数学", "英语", "语文", "物理", "化学", "编程"]
    
    // 双端一致性新增状态
    @Published var isManualOrder = false
    @Published var tasksOrder: [String] = []
    @Published var todayStudySeconds: Int = 0
    @Published var reminderTime = "08:00"
    private var lastElapsedSeconds: Int = 0

    private let notificationScheduler = NotificationScheduler()
    private let calendarService = CalendarService()

    private let tasksKey = "study-plan.tasks"
    private let profileKey = "study-plan.profile"
    private let timerKey = "study-plan.timer"

    init() {
        timerEngine = TimerEngine()
        load()
        loadTodayStudySeconds()
        timerEngine.onUpdate = { [weak self] state in
            guard let self = self else { return }
            self.timer = state
            
            // 实时学习时间累加
            if state.isRunning && !state.isPaused {
                let delta = state.elapsedSeconds - self.lastElapsedSeconds
                if delta > 0 {
                    self.todayStudySeconds += delta
                    self.lastElapsedSeconds = state.elapsedSeconds
                    self.saveTodayStudySeconds()
                }
            } else {
                self.lastElapsedSeconds = 0
            }
            
            if state.isRunning {
                self.isManualOrder = false
            }
            
            self.sortTasks()
            self.save()
            
            if let data = try? JSONEncoder().encode(state) {
                UserDefaults.standard.set(data, forKey: self.timerKey)
            }
        }
        notificationScheduler.requestPermission()
        calendarService.requestAccess()
    }

    func sortTasks() {
        if isManualOrder {
            tasks.sort { (a, b) -> Bool in
                let idxA = tasksOrder.firstIndex(of: a.id.uuidString) ?? Int.max
                let idxB = tasksOrder.firstIndex(of: b.id.uuidString) ?? Int.max
                return idxA < idxB
            }
        } else {
            let timingID = timer.activeTaskID
            tasks.sort { (a, b) -> Bool in
                let isATiming = (a.id == timingID && timer.isRunning)
                let isBTiming = (b.id == timingID && timer.isRunning)
                if isATiming && !isBTiming { return true }
                if !isATiming && isBTiming { return false }
                
                if a.status != b.status {
                    return a.status < b.status // 0 (未完成) < 1 (已完成)
                }
                
                if a.priority != b.priority {
                    return a.priority > b.priority // 优先级降序
                }
                
                if a.date != b.date {
                    return a.date < b.date
                }
                
                return a.specificTime < b.specificTime
            }
        }
    }

    private var todayKey: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return "pref_today_study_seconds_" + formatter.string(from: Date())
    }
    
    func loadTodayStudySeconds() {
        todayStudySeconds = UserDefaults.standard.integer(forKey: todayKey)
    }
    
    func saveTodayStudySeconds() {
        UserDefaults.standard.set(todayStudySeconds, forKey: todayKey)
    }

    func autoLoadNextTask() {
        let pending = tasks.filter { $0.status == 0 && $0.duration > 0 }
        if let next = pending.first {
            selectTimerTask(next)
        } else {
            selectTimerTask(nil)
        }
    }

    func selectTimerTask(_ task: Task?) {
        lastElapsedSeconds = 0
        timerEngine.select(task)
        timer = timerEngine.state
        isManualOrder = false
        sortTasks()
        save()
    }

    func startTimer(minutes: Int, isCountdown: Bool, allowOverflow: Bool) {
        lastElapsedSeconds = 0
        timerEngine.start(minutes: minutes, isCountdown: isCountdown, allowOverflow: allowOverflow)
        timer = timerEngine.state
        isManualOrder = false
        sortTasks()
        save()
        if isCountdown { notificationScheduler.scheduleTimerEnd(after: max(1, minutes) * 60) }
    }

    func startTimerForTask(_ task: Task, isCountdown: Bool = true, allowOverflow: Bool = false) {
        selectTimerTask(task)
        startTimer(minutes: task.duration > 0 ? task.duration : defaultTimerMinutes, isCountdown: isCountdown, allowOverflow: allowOverflow)
    }

    func toggleTimer() {
        lastElapsedSeconds = 0
        timerEngine.toggle()
        timer = timerEngine.state
        isManualOrder = false
        sortTasks()
        save()
    }

    func stopTimer() {
        lastElapsedSeconds = 0
        timerEngine.stop()
        timer = timerEngine.state
        isManualOrder = false
        sortTasks()
        save()
        notificationScheduler.cancelTimerEnd()
    }

    func refreshTimer() {
        timerEngine.refresh()
        timer = timerEngine.state
    }

    func setRemindersEnabled(_ enabled: Bool) {
        remindersEnabled = enabled
        tasks.forEach { notificationScheduler.cancel(taskID: $0.id); if enabled { notificationScheduler.schedule(task: $0) } }
        save()
    }

    func addSubject(_ subject: String) {
        if !subject.isEmpty && !subjects.contains(subject) {
            subjects.append(subject)
            save()
        }
    }
    
    func deleteSubject(_ subject: String) {
        subjects.removeAll { $0 == subject }
        save()
    }
    
    func saveProfile() {
        save()
    }
    
    var exportJSON: String {
        (try? String(data: JSONEncoder().encode(tasks), encoding: .utf8)) ?? "[]"
    }

    func add(_ task: Task) {
        var value = task
        value.calendarEventID = calendarService.addOrUpdate(value)
        tasks.append(value)
        isManualOrder = false
        sortTasks()
        if remindersEnabled { notificationScheduler.schedule(task: value) }
        save()
    }

    func update(_ task: Task) {
        guard let index = tasks.firstIndex(where: { $0.id == task.id }) else { return }
        var value = task
        value.calendarEventID = calendarService.addOrUpdate(value)
        tasks[index] = value
        notificationScheduler.cancel(taskID: value.id)
        if remindersEnabled { notificationScheduler.schedule(task: value) }
        isManualOrder = false
        sortTasks()
        save()
    }

    func delete(_ task: Task) {
        tasks.removeAll { $0.id == task.id }
        selectedTaskIDs.remove(task.id)
        notificationScheduler.cancel(taskID: task.id)
        calendarService.delete(task)
        if isManualOrder {
            tasksOrder.removeAll { $0 == task.id.uuidString }
        }
        sortTasks()
        save()
    }

    func deleteSelected() {
        let deleted = tasks.filter { selectedTaskIDs.contains($0.id) }
        tasks.removeAll { selectedTaskIDs.contains($0.id) }
        deleted.forEach { notificationScheduler.cancel(taskID: $0.id); calendarService.delete($0) }
        if isManualOrder {
            let idsToDelete = Set(deleted.map { $0.id.uuidString })
            tasksOrder.removeAll { idsToDelete.contains($0) }
        }
        selectedTaskIDs.removeAll()
        isBatchDeleting = false
        sortTasks()
        save()
    }

    func clearAllTasks() {
        tasks.forEach {
            notificationScheduler.cancel(taskID: $0.id)
            calendarService.delete($0)
        }
        tasks.removeAll()
        save()
    }

    var completedTasks: Int { tasks.filter { $0.status == 1 }.count }
    var completionRate: Int { tasks.isEmpty ? 0 : completedTasks * 100 / tasks.count }
    var totalPages: Int { tasks.filter { $0.status == 1 }.reduce(0) { $0 + $1.pages } }
    var totalStudyMinutes: Int { tasks.filter { $0.status == 1 }.reduce(0) { $0 + $1.duration } }

    func toggle(_ task: Task) {
        guard let index = tasks.firstIndex(where: { $0.id == task.id }) else { return }
        tasks[index].status = tasks[index].status == 1 ? 0 : 1
        isManualOrder = false
        sortTasks()
        save()
    }

    func move(from source: IndexSet, to destination: Int) {
        tasks.move(fromOffsets: source, toOffset: destination)
        isManualOrder = true
        tasksOrder = tasks.map { $0.id.uuidString }
        save()
    }

    var filteredTasks: [Task] {
        guard !searchText.isEmpty else { return [] }
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return tasks.filter {
            $0.content.localizedCaseInsensitiveContains(searchText) ||
            $0.subject.localizedCaseInsensitiveContains(searchText) ||
            formatter.string(from: $0.date).contains(searchText)
        }
    }

    private func save() {
        if let data = try? JSONEncoder().encode(tasks) { UserDefaults.standard.set(data, forKey: tasksKey) }
        UserDefaults.standard.set([nickname, signature, avatarText, avatarImageData ?? Data(), avatarFrameColor, remindersEnabled, vibrateEnabled, defaultTimerMinutes, manualCompletionEnabled, subjects], forKey: profileKey)
        UserDefaults.standard.set(isManualOrder, forKey: "study-plan.isManualOrder")
        UserDefaults.standard.set(tasksOrder, forKey: "study-plan.tasksOrder")
        UserDefaults.standard.set(reminderTime, forKey: "study-plan.reminderTime")
    }

    private func load() {
        if let data = UserDefaults.standard.data(forKey: tasksKey), let saved = try? JSONDecoder().decode([Task].self, from: data) { tasks = saved }
        if let data = UserDefaults.standard.data(forKey: timerKey), let saved = try? JSONDecoder().decode(TimerState.self, from: data) { timer = saved; timerEngine.state = saved }
        if let profile = UserDefaults.standard.array(forKey: profileKey) {
            if let value = profile.first as? String { nickname = value }
            if let value = profile.dropFirst().first as? String { signature = value }
            if let value = profile.dropFirst(2).first as? String { avatarText = value }
            if let value = profile.dropFirst(3).first as? Data, !value.isEmpty { avatarImageData = value }
            if let value = profile.dropFirst(4).first as? String { avatarFrameColor = value }
            if let value = profile.dropFirst(5).first as? Bool { remindersEnabled = value }
            if let value = profile.dropFirst(6).first as? Bool { vibrateEnabled = value }
            if let value = profile.dropFirst(7).first as? Int { defaultTimerMinutes = value }
            if let value = profile.dropFirst(8).first as? Bool { manualCompletionEnabled = value }
            if let value = profile.dropFirst(9).first as? [String] { subjects = value }
        }
        isManualOrder = UserDefaults.standard.bool(forKey: "study-plan.isManualOrder")
        tasksOrder = UserDefaults.standard.stringArray(forKey: "study-plan.tasksOrder") ?? []
        reminderTime = UserDefaults.standard.string(forKey: "study-plan.reminderTime") ?? "08:00"
        sortTasks()
        if timer.activeTaskID == nil && !timer.isRunning {
            autoLoadNextTask()
        }
    }
}
