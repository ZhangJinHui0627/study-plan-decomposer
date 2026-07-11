import SwiftUI

struct TimerSelectionView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Button {
                    store.selectTimerTask(nil)
                    dismiss()
                } label: {
                    Text("不绑定任务（自定义 25 分钟）")
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)
                .padding(14)
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))

                ForEach(store.tasks.filter { $0.status == 0 && $0.duration > 0 }) { task in
                    Button {
                        store.selectTimerTask(task)
                        dismiss()
                    } label: {
                        Text("[\(task.subject)] \(task.content)（\(task.duration) 分钟）")
                            .foregroundStyle(StudyPlanTheme.textPrimary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(.plain)
                    .padding(14)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
                }
            }
            .scrollContentBackground(.hidden)
            .background(StudyPlanTheme.background)
            .navigationTitle("选择学习计划")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("关闭") { dismiss() } } }
        }
    }
}
