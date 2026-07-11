import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var store: StudyPlanStore

    var body: some View {
        VStack(spacing: 0) {
            if store.tasks.isEmpty {
                Text("任务清单空空如也\n快去开启高效的一天吧")
                    .multilineTextAlignment(.center)
                    .lineSpacing(8)
                    .font(.system(size: 15))
                    .foregroundStyle(StudyPlanTheme.textSecondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List {
                    ForEach(store.tasks) { task in
                        TaskCardView(task: task)
                            .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 16))
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                            .contentShape(Rectangle())
                    }
                    .onMove(perform: store.move)
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
            }

            if store.isBatchDeleting {
                HStack {
                    Button("取消") { store.isBatchDeleting = false; store.selectedTaskIDs.removeAll() }
                    Spacer()
                    Text("已选择 (store.selectedTaskIDs.count) 项")
                    Spacer()
                    Button("删除") { store.deleteSelected() }
                        .foregroundStyle(.red)
                        .disabled(store.selectedTaskIDs.isEmpty)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .glassCard(cornerRadius: 18)
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }
        }
        .toolbar { EditButton() }
        .contextMenu {
            Button("批量删除", role: .destructive) { store.isBatchDeleting = true }
        }
    }
}
