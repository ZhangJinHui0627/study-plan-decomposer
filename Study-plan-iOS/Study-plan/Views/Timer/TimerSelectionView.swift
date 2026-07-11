import SwiftUI

struct TimerSelectionView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Button("不绑定任务（自定义）") { store.selectTimerTask(nil); dismiss() }
                ForEach(store.tasks.filter { $0.status == 0 && $0.duration > 0 }) { task in
                    Button { store.selectTimerTask(task); dismiss() } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(task.content).foregroundStyle(StudyPlanTheme.textPrimary)
                            Text(task.subject).font(.caption).foregroundStyle(StudyPlanTheme.textSecondary)
                        }
                    }
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
