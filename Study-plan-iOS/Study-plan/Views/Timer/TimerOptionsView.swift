import SwiftUI

struct TimerOptionsView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @Environment(\.dismiss) private var dismiss
    let task: Task?
    @State private var minutes: String = "25"
    @State private var isCountdown = true
    @State private var allowOverflow = false

    init(task: Task? = nil) {
        self.task = task
    }

    private var activeTask: Task? {
        task ?? store.tasks.first { $0.id == store.timer.activeTaskID }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("计时模式") {
                    Picker("模式", selection: $isCountdown) {
                        Text("倒计时").tag(true)
                        Text("正计时").tag(false)
                    }
                    .pickerStyle(.segmented)
                    
                    if activeTask == nil {
                        HStack {
                            Text("专注时长")
                            Spacer()
                            TextField("分钟", text: $minutes)
                                .keyboardType(.numberPad)
                                .multilineTextAlignment(.trailing)
                                .frame(width: 80)
                            Text("分钟")
                        }
                    } else {
                        HStack {
                            Text("任务设定时长")
                            Spacer()
                            Text("\(activeTask?.duration ?? 0) 分钟")
                                .foregroundStyle(StudyPlanTheme.textSecondary)
                        }
                    }
                    
                    // 仅正计时模式下显示溢出计时开关，对齐 Android
                    if !isCountdown {
                        Toggle("允许超时继续计时 (溢出)", isOn: $allowOverflow)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(StudyPlanTheme.background)
            .onAppear {
                if let task = activeTask {
                    minutes = String(task.duration)
                } else {
                    minutes = String(store.defaultTimerMinutes)
                }
                
                // 默认从 Store 恢复设置
                isCountdown = store.timer.isCountdown
                if !isCountdown {
                    allowOverflow = store.timer.allowOverflow
                } else {
                    allowOverflow = false
                }
            }
            .navigationTitle("选择计时方式")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("开始") {
                        let finalMins = max(1, Int(minutes) ?? store.defaultTimerMinutes)
                        // 倒计时下强制不允许溢出
                        let finalOverflow = isCountdown ? false : allowOverflow
                        
                        if let task = activeTask {
                            store.startTimerForTask(task, isCountdown: isCountdown, allowOverflow: finalOverflow)
                        } else {
                            store.startTimer(minutes: finalMins, isCountdown: isCountdown, allowOverflow: finalOverflow)
                        }
                        dismiss()
                    }
                }
            }
        }
    }
}
