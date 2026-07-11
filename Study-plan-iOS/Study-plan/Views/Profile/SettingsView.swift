import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @Environment(\.dismiss) private var dismiss
    @State private var showClearConfirmation = false
    @State private var reminderDate = Date()

    var body: some View {
        NavigationStack {
            Form {
                Section("个人资料") {
                    TextField("昵称", text: $store.nickname)
                    TextField("个性签名", text: $store.signature)
                    TextField("头像文字", text: $store.avatarText)
                    
                    Picker("头像边框", selection: $store.avatarFrameColor) {
                        Text("七彩霓虹").tag("default")
                        Text("蓝色").tag("blue")
                        Text("紫色").tag("purple")
                        Text("绿色").tag("green")
                    }
                }
                
                Section("提醒与计时") {
                    Toggle("开启提醒", isOn: Binding(
                        get: { store.remindersEnabled },
                        set: { store.setRemindersEnabled($0) }
                    ))
                    
                    // 当开启提醒时展示提醒时间 Picker，有滑动过渡效果
                    if store.remindersEnabled {
                        DatePicker("提醒时间", selection: $reminderDate, displayedComponents: .hourAndMinute)
                            .transition(.opacity.combined(with: .slide))
                            .onChange(of: reminderDate) { _, newDate in
                                let formatter = DateFormatter()
                                formatter.dateFormat = "HH:mm"
                                store.reminderTime = formatter.string(from: newDate)
                            }
                    }
                    
                    Toggle("完成时振动", isOn: $store.vibrateEnabled)
                    Stepper("默认计时：\(store.defaultTimerMinutes) 分钟", value: $store.defaultTimerMinutes, in: 1...180)
                }
                
                Section("数据与词库") {
                    NavigationLink("学科词库") {
                        SubjectRulesView()
                    }
                    ShareLink(item: store.exportJSON, subject: Text("学习计划数据")) {
                        Label("导出任务数据", systemImage: "square.and.arrow.up")
                    }
                }
                
                Section {
                    Button("清空全部任务", role: .destructive) {
                        showClearConfirmation = true
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
            .confirmationDialog(
                "确定清空全部任务？",
                isPresented: $showClearConfirmation,
                titleVisibility: .visible
            ) {
                Button("清空", role: .destructive) {
                    store.clearAllTasks()
                }
                Button("取消", role: .cancel) {}
            }
            .onDisappear {
                store.saveProfile()
            }
        }
    }
}
