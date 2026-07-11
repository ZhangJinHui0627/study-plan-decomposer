import Foundation

final class TimerEngine {
    var state = TimerState()
    var onUpdate: ((TimerState) -> Void)?
    private var timer: Timer?

    func select(_ task: Task?) {
        timer?.invalidate()
        timer = nil
        state.isRunning = false
        state.isPaused = false
        state.isCountdown = true
        state.allowOverflow = false
        state.elapsedSeconds = 0
        state.startedAt = nil
        state.pausedAt = nil
        state.activeTaskID = task?.id
        state.totalSeconds = task.map { max(1, $0.duration) * 60 } ?? 1500
        publish()
    }

    func start(minutes: Int, isCountdown: Bool, allowOverflow: Bool) {
        timer?.invalidate()
        state.isRunning = true
        state.isPaused = false
        state.isCountdown = isCountdown
        state.allowOverflow = allowOverflow
        state.totalSeconds = max(1, minutes) * 60
        state.elapsedSeconds = 0
        state.startedAt = .now
        state.pausedAt = nil
        scheduleTimer()
        publish()
    }

    func toggle() {
        guard state.isRunning else { return }
        if state.isPaused {
            state.isPaused = false
            state.startedAt = .now.addingTimeInterval(-TimeInterval(state.elapsedSeconds))
            state.pausedAt = nil
            scheduleTimer()
        } else {
            state.isPaused = true
            state.pausedAt = .now
            timer?.invalidate()
        }
        publish()
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        state.isRunning = false
        state.isPaused = false
        state.startedAt = nil
        state.pausedAt = nil
        state.elapsedSeconds = 0
        publish()
    }

    func refresh() {
        guard state.isRunning, !state.isPaused else { return }
        scheduleTimer()
        tick()
    }

    private func scheduleTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in self?.tick() }
    }

    private func tick() {
        guard state.isRunning, !state.isPaused, let startedAt = state.startedAt else { return }
        state.elapsedSeconds = max(0, Int(Date.now.timeIntervalSince(startedAt)))
        if state.elapsedSeconds >= state.totalSeconds && (state.isCountdown || !state.allowOverflow) {
            state.elapsedSeconds = state.totalSeconds
            state.isRunning = false
            timer?.invalidate()
            timer = nil
        }
        publish()
    }

    private func publish() { onUpdate?(state) }
}
