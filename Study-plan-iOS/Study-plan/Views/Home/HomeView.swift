import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @State private var showBatchDeleteConfirm = false

    var body: some View {
        VStack(spacing: 0) {
            if store.tasks.isEmpty {
                Text("任务清单空空如也\n快去开启高效的一天吧")
                    .multilineTextAlignment(.center)
                    .lineSpacing(8)
                    .font(.system(size: 15))
                    .foregroundStyle(StudyPlanTheme.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 150)
                    .frame(maxHeight: .infinity, alignment: .top)
            } else {
                List {
                    ForEach(store.tasks) { task in
                        TaskCardView(task: task)
                            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 4, trailing: 16))
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                            .contentShape(Rectangle())
                    }
                    .onDelete { offsets in
                        let deleted = offsets.map { store.tasks[$0] }
                        deleted.forEach { store.delete($0) }
                    }
                    // 在多选状态下置空 onMove，拦截禁用拖拽，对齐 Android
                    .onMove(perform: store.isBatchDeleting ? nil : { store.move(from: $0, to: $1) })
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
            }

            if store.isBatchDeleting {
                HStack {
                    Button("取消") {
                        store.isBatchDeleting = false
                        store.selectedTaskIDs.removeAll()
                    }
                    Spacer()
                    Text("已选择 \(store.selectedTaskIDs.count) 项")
                        .font(.system(size: 14))
                        .foregroundStyle(StudyPlanTheme.textPrimary)
                    Spacer()
                    Button("删除") {
                        showBatchDeleteConfirm = true
                    }
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
        .toolbar {
            ToolbarItem {
                EditButton()
            }
        }
        .contextMenu {
            Button("批量删除", role: .destructive) {
                store.isBatchDeleting = true
            }
        }
        // 批量删除二次确认弹窗对齐 Android
        .confirmationDialog(
            "确定要删除选中的 \(store.selectedTaskIDs.count) 个任务吗？",
            isPresented: $showBatchDeleteConfirm,
            titleVisibility: .visible
        ) {
            Button("删除", role: .destructive) {
                store.deleteSelected()
            }
            Button("取消", role: .cancel) {}
        }
    }
}
