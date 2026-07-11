import SwiftUI

struct TaskEditorView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @Environment(\.dismiss) private var dismiss
    let task: Task?
    @State private var subject: String
    @State private var content: String
    @State private var duration: String
    @State private var pages: String
    @State private var priority: Int
    @State private var date: Date
    @State private var specificTime: String
    @State private var quickInput = ""

    init(task: Task?) {
        self.task = task
        _subject = State(initialValue: task?.subject ?? "")
        _content = State(initialValue: task?.content ?? "")
        _duration = State(initialValue: task.map { $0.duration == 0 ? "" : String($0.duration) } ?? "")
        _pages = State(initialValue: task.map { $0.pages == 0 ? "" : String($0.pages) } ?? "")
        _priority = State(initialValue: task?.priority ?? 0)
        _date = State(initialValue: task?.date ?? .now)
        _specificTime = State(initialValue: task?.specificTime ?? "08:00")
    }

    var body: some View {
        NavigationStack {
            Form {
                if task == nil {
                    Section("快速拆解") {
                        TextEditor(text: $quickInput)
                            .frame(minHeight: 90)
                            .overlay(alignment: .topLeading) { if quickInput.isEmpty { Text("例如：明天晚上 背英语单词 30分钟 第3章").foregroundStyle(.secondary).padding(.top, 8) } }
                        Button("解析并添加") { parseAndAdd() }.disabled(quickInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                }
                Section("计划内容") {
                    TextField("科目", text: $subject)
                    TextField("要完成什么？", text: $content, axis: .vertical)
                        .lineLimit(2...5)
                }
                Section("计划信息") {
                    DatePicker("日期", selection: $date, displayedComponents: .date)
                    TextField("具体时间（如 19:30）", text: $specificTime)
                    TextField("时长（分钟）", text: $duration).keyboardType(.numberPad)
                    TextField("页数", text: $pages).keyboardType(.numberPad)
                    Picker("优先级", selection: $priority) {
                        Text("低").tag(0)
                        Text("普通").tag(1)
                        Text("高").tag(2)
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(StudyPlanTheme.background)
            .navigationTitle(task == nil ? "添加学习计划" : "编辑学习计划")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("保存") { save() }.disabled(content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) }
            }
        }
    }

    private func save() {
        var value = task ?? Task()
        value.subject = subject
        value.content = content.trimmingCharacters(in: .whitespacesAndNewlines)
        value.date = date
        value.duration = Int(duration) ?? 0
        value.pages = Int(pages) ?? 0
        value.priority = priority
        value.specificTime = specificTime
        if task == nil { store.add(value) } else { store.update(value) }
        dismiss()
    }

    private func parseAndAdd() {
        TaskParser.parse(quickInput, subjects: store.subjects).forEach { store.add($0) }
        dismiss()
    }
}
