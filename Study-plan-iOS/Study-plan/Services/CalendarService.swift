import EventKit

final class CalendarService {
    private let store = EKEventStore()

    func requestAccess() {
        if #available(iOS 17.0, *) { store.requestFullAccessToEvents { _, _ in } }
        else { store.requestAccess(to: .event) { _, _ in } }
    }

    func addOrUpdate(_ task: Task) -> String? {
        let status = EKEventStore.authorizationStatus(for: .event)
        if #available(iOS 17.0, *) {
            guard status == .fullAccess else { return nil }
        } else {
            guard status == .authorized else { return nil }
        }
        guard let calendar = store.defaultCalendarForNewEvents else { return nil }
        let event = task.calendarEventID.flatMap { store.event(withIdentifier: $0) } ?? EKEvent(eventStore: store)
        event.calendar = calendar
        event.title = "\(task.status == 1 ? "[已完成] " : "")[\(task.subject)] \(task.content)"
        event.notes = "来自学习计划拆解"
        event.startDate = startDate(for: task)
        event.endDate = event.startDate.addingTimeInterval(TimeInterval(max(60, task.duration * 60)))
        try? store.save(event, span: .thisEvent)
        return event.eventIdentifier
    }

    func delete(_ task: Task) {
        guard let id = task.calendarEventID, let event = store.event(withIdentifier: id) else { return }
        try? store.remove(event, span: .thisEvent)
    }

    private func startDate(for task: Task) -> Date {
        let parts = task.specificTime.split(separator: ":")
        guard parts.count == 2, let hour = Int(parts[0]), let minute = Int(parts[1]) else { return task.date }
        return Calendar.current.date(bySettingHour: hour, minute: minute, second: 0, of: task.date) ?? task.date
    }
}
