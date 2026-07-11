import SwiftUI

struct AboutView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var isHistoryExpanded = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    VStack(spacing: 4) {
                        Text("学习计划拆解")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundStyle(StudyPlanTheme.textPrimary)

                        Text("v1.0")
                            .font(.system(size: 15))
                            .foregroundStyle(StudyPlanTheme.textSecondary)
                    }
                    .padding(.top, 34)
                    .padding(.bottom, 18)

                    aboutCard
                    versionHistoryCard
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 32)
            }
            .background(StudyPlanTheme.background)
            .navigationTitle("关于")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "chevron.backward")
                    }
                    .accessibilityLabel("返回")
                }
            }
        }
    }

    private var aboutCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("关于应用")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(StudyPlanTheme.textPrimary)
                .padding(.bottom, 8)

            Text("学习计划智能拆解与提醒 App\n\n帮助大学生将自然语言学习计划拆解为结构化任务，支持提醒与进度管理。")
                .font(.system(size: 14))
                .foregroundStyle(StudyPlanTheme.textSecondary)
                .lineSpacing(4)

            Divider()
                .padding(.vertical, 16)

            VStack(alignment: .leading, spacing: 4) {
                Text("开发者：张锦慧")
                Text("协作者：吴思彤")
                Text("版本：v1.0")
                    .foregroundStyle(StudyPlanTheme.textSecondary)
                Text("技术栈：SwiftUI + EventKit + UserNotifications")
                    .foregroundStyle(StudyPlanTheme.textSecondary)
            }
            .font(.system(size: 14))
            .foregroundStyle(StudyPlanTheme.textPrimary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .glassCard()
    }

    private var versionHistoryCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    isHistoryExpanded.toggle()
                }
            } label: {
                HStack(spacing: 16) {
                    Image(systemName: "info.circle.fill")
                        .foregroundStyle(StudyPlanTheme.textPrimary)
                        .frame(width: 22)

                    Text("版本历史")
                        .font(.system(size: 15))
                        .foregroundStyle(StudyPlanTheme.textPrimary)

                    Spacer()

                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(StudyPlanTheme.textSecondary)
                        .rotationEffect(.degrees(isHistoryExpanded ? 90 : 0))
                }
                .padding(.horizontal, 16)
                .frame(height: 52)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if isHistoryExpanded {
                Divider()
                    .padding(.horizontal, 16)

                VStack(alignment: .leading, spacing: 12) {
                    Text("alpha-1：首个功能与界面重构升级版本，包含任务拆解、专注计时、提醒、日历同步、统计和个人设置。")
                    Text("alpha-2：统一任务卡片、计时与弹窗交互样式；新增统计页状态概览与专注建议，并优化学科词库管理。")
                    Text("v1.0：正式发布版本，统一双端产品信息并修复已知问题。")
                }
                .font(.system(size: 14))
                .foregroundStyle(StudyPlanTheme.textSecondary)
                .lineSpacing(4)
                .padding(.horizontal, 54)
                .padding(.vertical, 16)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.vertical, 6)
        .glassCard()
    }
}
