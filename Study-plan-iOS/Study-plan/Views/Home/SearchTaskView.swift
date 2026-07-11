import SwiftUI

struct SearchTaskView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List(store.filteredTasks) { task in
                TaskCardView(task: task)
                    .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(StudyPlanTheme.background)
            .searchable(text: $store.searchText, prompt: "搜索科目或任务")
            .navigationTitle("搜索任务")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("关闭") { store.searchText = ""; dismiss() } } }
        }
    }
}
