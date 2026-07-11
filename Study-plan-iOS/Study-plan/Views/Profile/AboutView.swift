import SwiftUI

struct AboutView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @Environment(\.dismiss) private var dismiss
    @State private var isHistoryExpanded = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // 头像同步显示，对齐 Android
                    AvatarFrameView(
                        text: store.avatarText,
                        frameColorName: store.avatarFrameColor,
                        imageData: store.avatarImageData
                    )
                    .frame(width: 96, height: 96)
                    .padding(.top, 20)
                    
                    VStack(spacing: 8) {
                        Text("学习计划拆解")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundStyle(StudyPlanTheme.textPrimary)
                        
                        Text("当前版本：alpha-2")
                            .font(.system(size: 14))
                            .foregroundStyle(StudyPlanTheme.textSecondary)
                    }
                    
                    Text("把目标拆成今天就能完成的小任务，让学习更有条理。")
                        .multilineTextAlignment(.center)
                        .font(.system(size: 14))
                        .foregroundStyle(StudyPlanTheme.textSecondary)
                        .padding(.horizontal, 30)
                    
                    // 版本历史折叠展开控制，对齐 Android
                    VStack(alignment: .leading, spacing: 0) {
                        DisclosureGroup(isExpanded: $isHistoryExpanded) {
                            VStack(alignment: .leading, spacing: 16) {
                                Divider().padding(.vertical, 8)
                                
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("alpha-2 更新")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundStyle(StudyPlanTheme.textPrimary)
                                    
                                    Group {
                                        Text("• 任务卡片支持搜索、展开详情、拖拽排序、左右滑动操作和批量删除。")
                                        Text("• 专注计时支持倒计时、正计时、溢出计时、独立计时和任务绑定计时。")
                                        Text("• 未填写任务时长时，开始计时会提示输入例如“25分钟”，避免任务立即结束。")
                                        Text("• 独立计时同样显示屏幕边框跑马灯，且边框圆角适配不同设备。")
                                        Text("• 统计页新增学习小结、今日专注建议和任务状态概览。")
                                        Text("• 统一弹窗、胶囊按钮和任务卡片视觉样式，并优化学科词库管理布局。")
                                    }
                                    .font(.system(size: 13))
                                    .foregroundStyle(StudyPlanTheme.textSecondary)
                                    .padding(.leading, 4)
                                }
                                
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("alpha-1 更新")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundStyle(StudyPlanTheme.textPrimary)
                                    
                                    Text("• 完成首个主要功能版本，包含任务拆解、专注计时、提醒、日历同步、统计和个人设置。")
                                        .font(.system(size: 13))
                                        .foregroundStyle(StudyPlanTheme.textSecondary)
                                        .padding(.leading, 4)
                                }
                            }
                            .padding(.top, 4)
                        } label: {
                            HStack {
                                Image(systemName: "clock.arrow.circlepath")
                                    .foregroundStyle(StudyPlanTheme.primary)
                                    .frame(width: 24)
                                Text("版本历史")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundStyle(StudyPlanTheme.textPrimary)
                                Spacer()
                            }
                        }
                    }
                    .padding(16)
                    .glassCard()
                    .padding(.horizontal, 16)
                    
                    Spacer()
                }
            }
            .background(StudyPlanTheme.background)
            .navigationTitle("关于")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("完成") {
                        dismiss()
                    }
                }
            }
        }
    }
}
