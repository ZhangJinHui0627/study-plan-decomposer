import Foundation
import UserNotifications

final class NotificationScheduler {
    func requestPermission() { UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in } }

    func schedule(task: Task) {
        guard !task.specificTime.isEmpty, let date = reminderDate(for: task) else { return }
        let content = UNMutableNotificationContent()
        content.title = task.subject.isEmpty ? "学习计划提醒" : task.subject
        content.body = task.content
        content.sound = .default
        let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: date)
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: task.id.uuidString, content: content, trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: false)))
    }

    func cancel(taskID: UUID) { UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [taskID.uuidString]) }

    func scheduleTimerEnd(after seconds: Int) {
        let content = UNMutableNotificationContent()
        content.title = "专注计时完成"
        content.body = "本次专注已结束，休息一下吧。"
        content.sound = .default
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: "study-plan.timer", content: content, trigger: UNTimeIntervalNotificationTrigger(timeInterval: TimeInterval(max(1, seconds)), repeats: false)))
    }

    func cancelTimerEnd() { UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["study-plan.timer"]) }

    private func reminderDate(for task: Task) -> Date? {
        let parts = task.specificTime.split(separator: ":")
        guard parts.count == 2, let hour = Int(parts[0]), let minute = Int(parts[1]), hour < 24, minute < 60 else { return nil }
        return Calendar.current.date(bySettingHour: hour, minute: minute, second: 0, of: task.date)
    }
}
