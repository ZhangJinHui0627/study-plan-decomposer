import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @Environment(\.dismiss) private var dismiss
    @State private var reminderDate = Date()

    var body: some View {
        NavigationStack {
            Form {
                Section("账号") {
                    TextField("昵称", text: $store.nickname)
                    TextField("个性签名", text: $store.signature)
                    TextField("头像文字", text: $store.avatarText)
                }
                
                Section("本地规则库") {
                    NavigationLink("学科词库管理") {
                        SubjectRulesView()
                    }
                }

                Section("通知") {
                    Toggle("学习提醒", isOn: Binding(
                        get: { store.remindersEnabled },
                        set: { store.setRemindersEnabled($0) }
                    ))
                    
                    // 当开启提醒时展示提醒时间 Picker，有滑动过渡效果
                    if store.remindersEnabled {
                        DatePicker("提醒时间", selection: $reminderDate, displayedComponents: .hourAndMinute)
                            .transition(.opacity.combined(with: .slide))
                            .onChange(of: reminderDate) { _, newDate in
                                store.setReminderTime(newDate)
                            }
                    }
                    
                    Toggle("震动提醒", isOn: $store.vibrateEnabled)
                    Stepper("默认计时：\(store.defaultTimerMinutes) 分钟", value: $store.defaultTimerMinutes, in: 1...180)
                }
                
                Section("学习计划记录管理") {
                    ShareLink(item: store.exportJSON, subject: Text("学习计划数据")) {
                        Label("导出任务数据", systemImage: "square.and.arrow.up")
                    }
                    Button("清空全部任务", role: .destructive) {
                        store.clearAllTasks()
                    }
                    HStack {
                        Label("数据库大小", systemImage: "externaldrive")
                        Spacer()
                        Text("\(store.exportJSON.utf8.count) B")
                            .foregroundStyle(StudyPlanTheme.textSecondary)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(StudyPlanTheme.background)
            .onAppear {
                // 初始化提醒时间 Picker
                let parts = store.reminderTime.split(separator: ":")
                if parts.count == 2, let h = Int(parts[0]), let m = Int(parts[1]) {
                    reminderDate = Calendar.current.date(bySettingHour: h, minute: m, second: 0, of: Date()) ?? Date()
                }
            }
            .navigationTitle("设置")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("完成") {
                        dismiss()
                    }
                }
            }
            .onDisappear {
                store.saveProfile()
            }
        }
    }
}
