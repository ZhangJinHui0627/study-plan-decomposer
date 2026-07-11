import Foundation

struct Task: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var date: Date = .now
    var timeRange: String = ""
    var subject: String = ""
    var content: String = ""
    var duration: Int = 0
    var pages: Int = 0
    var priority: Int = 0
    var status: Int = 0
    var specificTime: String = ""
    var sortIndex: Int = 0
    var calendarEventID: String?
}

struct TimerState: Codable, Equatable {
    var isRunning = false
    var isPaused = false
    var isCountdown = true
    var allowOverflow = false
    var totalSeconds = 1500
    var elapsedSeconds = 0
    var startedAt: Date?
    var pausedAt: Date?
    var activeTaskID: UUID?
}
