import SwiftUI

struct TaskCardView: View {
    @EnvironmentObject private var store: StudyPlanStore
    let task: Task
    @State private var expanded = false
    @State private var showZeroDurationAlert = false

    private var isTiming: Bool { store.timer.activeTaskID == task.id && store.timer.isRunning }
    
    private var isOverdue: Bool {
        guard task.status == 0 else { return false }
        
        let calendar = Calendar.current
        let todayStart = calendar.startOfDay(for: .now)
        let taskStart = calendar.startOfDay(for: task.date)
        
        if taskStart < todayStart {
            return true
        }
        
        if calendar.isDateInToday(task.date) {
            guard !task.specificTime.isEmpty else { return false }
            let currentStr = Self.timeFormatter.string(from: .now)
            if task.specificTime < currentStr && !isTiming {
                return true
            }
        }
        
        return false
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                // 批量选择勾选框
                if store.isBatchDeleting {
                    Image(systemName: store.selectedTaskIDs.contains(task.id) ? "checkmark.circle.fill" : "circle")
                        .foregroundStyle(store.selectedTaskIDs.contains(task.id) ? StudyPlanTheme.primary : StudyPlanTheme.textSecondary)
                        .font(.system(size: 20))
                        .transition(.move(edge: .leading).combined(with: .opacity))
                }
                
                // 学科首字头像
                Circle()
                    .fill(avatarColor.gradient)
                    .frame(width: 48, height: 48)
                    .overlay(
                        Text(task.subject.first.map(String.init) ?? "学")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundStyle(.white)
                    )
                
                // 标题和内容
                VStack(alignment: .leading, spacing: 3) {
                    titleText
                        .font(.system(size: 15))
                        .foregroundStyle(task.status == 1 ? Color(hex: "#9E9E9E") : StudyPlanTheme.textPrimary)
                        .strikethrough(task.status == 1)
                        .lineLimit(1)
                    
                    Text(task.content)
                        .font(.system(size: 12))
                        .foregroundStyle(task.status == 1 ? Color(hex: "#9E9E9E") : StudyPlanTheme.textSecondary)
                        .strikethrough(task.status == 1)
                        .lineLimit(1)
                }
                
                Spacer()
                
                // 右侧日期
                Text(Self.dateFormatter.string(from: task.date))
                    .font(.system(size: 11))
                    .foregroundStyle(dateColor)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .contentShape(Rectangle())
            .onTapGesture {
                if store.isBatchDeleting {
                    if store.selectedTaskIDs.contains(task.id) {
                        store.selectedTaskIDs.remove(task.id)
                    } else {
                        store.selectedTaskIDs.insert(task.id)
                    }
                } else {
                    withAnimation(.easeInOut(duration: 0.2)) {
                        expanded.toggle()
                    }
                }
            }

            // 展开的详情部分
            if expanded && !store.isBatchDeleting {
                Divider().padding(.leading, store.isBatchDeleting ? 106 : 74)
                HStack {
                    Text("计划时长: \(task.duration) 分钟" + (task.pages > 0 ? " | 计划阅读: \(task.pages) 页" : ""))
                        .font(.system(size: 12))
                        .foregroundStyle(StudyPlanTheme.textSecondary)
                    
                    Spacer()
                    
                    // 操作按钮：根据打卡首选项联动
                    if store.manualCompletionEnabled {
                        Button(task.status == 1 ? "重做任务" : "标记完成") {
                            toggleTaskStatus()
                        }
                        .buttonStyle(.plain)
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(StudyPlanTheme.primary)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 5)
                        .background(StudyPlanTheme.primary.opacity(0.12), in: Capsule())
                    } else {
                        if task.status == 1 {
                            Button("重做任务") {
                                toggleTaskStatus()
                            }
                            .buttonStyle(.plain)
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(StudyPlanTheme.primary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 5)
                            .background(StudyPlanTheme.primary.opacity(0.12), in: Capsule())
                        } else {
                            Button("开始专注") {
                                startFocusing()
                            }
                            .buttonStyle(.plain)
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(StudyPlanTheme.primary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 5)
                            .background(StudyPlanTheme.primary.opacity(0.12), in: Capsule())
                        }
                    }
                }
                .padding(.leading, store.isBatchDeleting ? 106 : 74)
                .padding(.trailing, 16)
                .padding(.bottom, 12)
            }
        }
        .opacity(task.status == 1 ? 0.4 : 1.0)
        .background(isTiming ? StudyPlanTheme.primary.opacity(0.12) : Color.clear)
        .glassCard(cornerRadius: 18)
        .overlay(alignment: .center) {
            // 快捷打卡胶囊（仅在允许打卡、未完成、非计时中、且非折叠、且非多选时显示）
            if store.manualCompletionEnabled && task.status == 0 && !isTiming && !store.isBatchDeleting {
                Button("点击完成") {
                    toggleTaskStatus()
                }
                .buttonStyle(.plain)
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(StudyPlanTheme.primary)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(StudyPlanTheme.primary.opacity(0.12), in: Capsule())
                .opacity(expanded ? 0 : 1)
            }
        }
        .swipeActions(edge: .leading, allowsFullSwipe: false) {
            if store.manualCompletionEnabled {
                Button {
                    toggleTaskStatus()
                } label: {
                    Label(task.status == 1 ? "未完成" : "完成", systemImage: "checkmark")
                }
                .tint(StudyPlanTheme.statGreen)
            } else {
                Button {
                    store.isBatchDeleting = true
                    store.selectedTaskIDs.insert(task.id)
                } label: {
                    Label("选择", systemImage: "checklist")
                }
                .tint(StudyPlanTheme.primary)
            }
        }
        .contextMenu {
            Button("编辑") { store.editingTask = task }
            Button("删除", role: .destructive) { store.delete(task) }
        }
        .alert("提示", isPresented: $showZeroDurationAlert) {
            Button("确定", role: .cancel) {}
        } message: {
            Text("请先输入计划时长，例如“25分钟”")
        }
    }

    private var titleText: Text {
        var prefix = Text("")
        if task.status != 1 {
            if task.priority >= 2 {
                prefix = Text("[加急] ").foregroundColor(.red).bold()
            } else if task.priority == 1 {
                prefix = Text("[优先] ").foregroundColor(StudyPlanTheme.statOrange).bold()
            }
        }
        
        let label = [task.subject, task.timeRange].filter { !$0.isEmpty }.joined(separator: " · ")
        let titleStr = label.isEmpty ? "学习计划" : label
        
        return prefix + Text(titleStr)
    }

    private var dateColor: Color {
        if task.status == 1 {
            return Color(hex: "#9E9E9E")
        }
        if isOverdue {
            return Color(hex: "#C62828")
        }
        if task.priority >= 2 {
            return .red
        }
        if task.priority == 1 {
            return StudyPlanTheme.statOrange
        }
        return StudyPlanTheme.textSecondary
    }

    private var avatarColor: Color {
        let colors = [StudyPlanTheme.primary, .green, .orange, .purple, .teal]
        return colors[abs(task.subject.hashValue) % colors.count]
    }

    private func toggleTaskStatus() {
        if task.status == 0 {
            store.toggle(task)
            if store.timer.activeTaskID == task.id {
                store.stopTimer()
                store.selectTimerTask(nil)
            }
        } else {
            store.toggle(task)
        }
    }

    private func startFocusing() {
        if task.duration <= 0 {
            showZeroDurationAlert = true
            return
        }
        store.startTimerForTask(task)
    }

    private static let dateFormatter: DateFormatter = {
        let value = DateFormatter()
        value.dateFormat = "MM-dd"
        return value
    }()

    private static let timeFormatter: DateFormatter = {
        let value = DateFormatter()
        value.dateFormat = "HH:mm"
        return value
    }()
}
