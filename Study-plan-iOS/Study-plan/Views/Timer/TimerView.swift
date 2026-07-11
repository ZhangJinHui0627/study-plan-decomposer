import SwiftUI
import AudioToolbox

struct TimerView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @State private var isShowingSelection = false
    @State private var isShowingOptions = false
    @State private var showPauseActionDialog = false
    @State private var showChangeTaskDialog = false
    @State private var showFinishDialog = false
    @State private var showZeroDurationAlert = false
    @State private var hasTriggeredOverflowToast = false

    private var elapsed: Int { store.timer.elapsedSeconds }
    private var remaining: Int { max(0, store.timer.totalSeconds - elapsed) }
    private var displaySeconds: Int {
        if store.timer.isCountdown {
            return remaining
        } else {
            if store.timer.allowOverflow && elapsed > store.timer.totalSeconds {
                // 溢出计时显示已走过的时间，如果超过了也可以显示
                return elapsed
            }
            return min(store.timer.totalSeconds, elapsed)
        }
    }
    
    private var display: String {
        let isNegative = store.timer.isCountdown && elapsed > store.timer.totalSeconds
        let seconds = abs(displaySeconds)
        let h = seconds / 3600
        let m = (seconds % 3600) / 60
        let s = seconds % 60
        return String(format: "%@%02d:%02d:%02d", isNegative ? "-" : "", h, m, s)
    }
    
    private var progress: Double {
        guard store.timer.totalSeconds > 0 else { return 0 }
        return min(1.0, Double(elapsed) / Double(store.timer.totalSeconds))
    }
    
    private var activeTask: Task? {
        store.tasks.first { $0.id == store.timer.activeTaskID }
    }
    
    private var isFinished: Bool {
        let t = store.timer
        guard t.totalSeconds > 0 else { return false }
        if t.isCountdown {
            return t.elapsedSeconds >= t.totalSeconds && !t.isRunning
        } else {
            return !t.allowOverflow && t.elapsedSeconds >= t.totalSeconds && !t.isRunning
        }
    }

    private var isOverflowing: Bool {
        let t = store.timer
        return t.isRunning && !t.isCountdown && t.allowOverflow && t.elapsedSeconds >= t.totalSeconds
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                VStack(spacing: 24) {
                    ZStack {
                        Circle()
                            .stroke(StudyPlanTheme.primary.opacity(0.12), lineWidth: 10)
                        
                        Circle()
                            .trim(from: 0, to: store.timer.isCountdown ? max(0.02, 1.0 - progress) : progress)
                            .stroke(
                                isOverflowing ? Color.red.gradient : StudyPlanTheme.primary.gradient,
                                style: StrokeStyle(lineWidth: 10, lineCap: .round)
                            )
                            .rotationEffect(.degrees(-90))
                        
                        Text(display)
                            .font(.system(size: 28, weight: .bold, design: .monospaced))
                            .foregroundStyle(isOverflowing ? .red : StudyPlanTheme.primary)
                    }
                    .frame(width: 220, height: 220)
                    
                    // 开始 / 暂停 / 继续 按钮
                    Button(action: handleStartPauseTap) {
                        Text(store.timer.isRunning ? (store.timer.isPaused ? "继续" : "暂停") : "开始")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(store.timer.isRunning && !store.timer.isPaused ? StudyPlanTheme.statOrange : StudyPlanTheme.statGreen)
                            .frame(width: 160, height: 50)
                            .glassCard(cornerRadius: 25)
                    }
                    .buttonStyle(.plain)
                }
                .padding(32)
                .frame(maxWidth: .infinity)
                .glassCard()

                // 绑定任务与今日学习统计卡片
                Button {
                    if store.timer.isRunning {
                        showChangeTaskDialog = true
                    } else {
                        isShowingSelection = true
                    }
                } label: {
                    VStack(spacing: 12) {
                        if isOverflowing {
                            Text("已超时，正在记录溢出时间...")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundStyle(.red)
                        } else {
                            Text(activeTask.map { "当前任务: [\($0.subject)] \($0.content) (\($0.duration)分钟)" } ?? "不绑定任务（自定义 25分钟）")
                                .font(.system(size: 14))
                                .foregroundStyle(StudyPlanTheme.textSecondary)
                                .multilineTextAlignment(.center)
                        }
                        
                        Divider()
                        
                        Text("今日累计学习：\(todayMinutes) 分钟")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(StudyPlanTheme.textPrimary)
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity)
                    .glassCard()
                }
                .buttonStyle(.plain)
            }
            .padding(16)
        }
        .sheet(isPresented: $isShowingSelection) { TimerSelectionView() }
        .sheet(isPresented: $isShowingOptions) { TimerOptionsView() }
        
        // 运行中点击暂停绑定任务的弹窗对齐 Android
        .confirmationDialog(
            "暂停/完成",
            isPresented: $showPauseActionDialog,
            titleVisibility: .visible
        ) {
            Button("是的，已完成") {
                if let task = activeTask {
                    store.toggle(task)
                }
                store.stopTimer()
                store.autoLoadNextTask()
            }
            Button("仅暂停/稍后") {
                store.toggleTimer()
            }
            Button("取消", role: .cancel) {}
        } message: {
            Text("是否将当前计划「\(activeTask?.content ?? "")」标记为已完成？")
        }

        .confirmationDialog(
            "提示",
            isPresented: $showChangeTaskDialog,
            titleVisibility: .visible
        ) {
            Button("确定切换", role: .destructive) {
                store.stopTimer()
                store.selectTimerTask(nil)
                isShowingSelection = true
            }
            Button("取消", role: .cancel) {}
        } message: {
            Text("当前正在计时，切换任务将终止并重置当前计时，确定要切换吗？")
        }
        
        // 计时结束弹窗对齐 Android
        .alert(
            activeTask != nil ? "学习时间到" : "时间到",
            isPresented: $showFinishDialog
        ) {
            if let task = activeTask {
                Button("是的，已完成") {
                    store.toggle(task)
                    store.stopTimer()
                    store.autoLoadNextTask()
                }
                Button("稍后处理") {
                    store.stopTimer()
                }
            } else {
                Button("确定") {
                    store.stopTimer()
                }
            }
        } message: {
            if let task = activeTask {
                Text("恭喜您完成了学习计划「\(task.content)」！\n是否将其标记为已完成？")
            } else {
                Text("专注时间已结束，休息一下吧！")
            }
        }
        
        // 溢出计时警示 Alert 对齐 Android
        .alert("提示", isPresented: $hasTriggeredOverflowToast) {
            Button("确定", role: .cancel) {}
        } message: {
            Text("计划已到时，正在记录溢出时间...")
        }

        .alert("提示", isPresented: $showZeroDurationAlert) {
            Button("确定", role: .cancel) {}
        } message: {
            Text("请先输入计划时长，例如“25分钟”")
        }
        
        // 监听计时结束
        .onChange(of: isFinished) { _, finished in
            if finished {
                triggerVibration()
                showFinishDialog = true
            }
        }
        
        // 监听溢出触发提示
        .onChange(of: isOverflowing) { _, overflowing in
            if overflowing && !hasTriggeredOverflowToast {
                triggerVibration()
                hasTriggeredOverflowToast = true
            }
        }
        .onAppear {
            if !store.timer.isRunning {
                hasTriggeredOverflowToast = false
            }
        }
    }

    private var todayMinutes: Int {
        store.todayStudySeconds / 60
    }

    private func handleStartPauseTap() {
        if !store.timer.isRunning {
            // 开始计时：若未绑定任务，直接开始自定义 25 分钟；若绑定了任务且时长为 0，弹出拦截
            if let task = activeTask, task.duration <= 0 {
                showZeroDurationAlert = true
            } else {
                hasTriggeredOverflowToast = false
                isShowingOptions = true
            }
        } else {
            // 正在运行中：若有绑定任务，触发 Android 对齐暂停交互；若无，直接暂停
            if activeTask != nil {
                if store.timer.isPaused {
                    store.toggleTimer()
                } else {
                    showPauseActionDialog = true
                }
            } else {
                store.toggleTimer()
            }
        }
    }

    private func triggerVibration() {
        if store.vibrateEnabled {
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        }
    }
}
