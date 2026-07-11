import SwiftUI

struct StatsView: View {
    @EnvironmentObject private var store: StudyPlanStore

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                HStack(spacing: 0) {
                    StatValue(value: "\(store.tasks.count)", title: "全部任务", color: StudyPlanTheme.primary)
                    Divider().frame(height: 34)
                    StatValue(value: "\(store.completedTasks)", title: "已完成", color: StudyPlanTheme.statGreen)
                    Divider().frame(height: 34)
                    StatValue(value: "\(store.completionRate)%", title: "完成率", color: StudyPlanTheme.statOrange)
                }
                .padding(16)
                .glassCard()

                InsightCard(title: "学习小结", text: store.tasks.isEmpty ? "开始你的第一个学习计划吧" : store.completedTasks == store.tasks.count ? "全部任务已完成，今天的节奏很棒！" : "还有 \(store.tasks.count - store.completedTasks) 项待完成，先专注 25 分钟吧")

                HStack(spacing: 12) {
                    StatBox(title: "专注时长", value: "\(store.totalStudyMinutes) 分钟", color: StudyPlanTheme.primary)
                    StatBox(title: "总页数", value: "\(store.totalPages) 页", color: StudyPlanTheme.statGreen)
                }

                InsightCard(title: "今日专注建议", text: store.tasks.isEmpty ? "添加一个小目标，开始今天的专注" : store.completedTasks == store.tasks.count ? "今日计划已清零，可以安心休息了" : store.completedTasks == 0 ? "先完成最简单的一项，建立启动惯性" : "已完成 \(store.completionRate)%，再坚持一小步就更接近目标")

                InsightCard(
                    title: "任务状态概览",
                    text: store.tasks.isEmpty ? "暂无任务数据" : "全部 \(store.tasks.count) 项 · 已完成 \(store.completedTasks) 项 · 待完成 \(store.tasks.count - store.completedTasks) 项"
                )

                VStack(alignment: .leading, spacing: 12) {
                    Text("学习进度")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(StudyPlanTheme.textPrimary)
                    ProgressView(value: Double(store.completedTasks), total: Double(max(1, store.tasks.count)))
                        .tint(StudyPlanTheme.statGreen)
                    Text(store.tasks.isEmpty ? "还没有任务，快去添加吧" : "已完成 \(store.completedTasks) / \(store.tasks.count) 项任务")
                        .font(.system(size: 13))
                        .foregroundStyle(StudyPlanTheme.textSecondary)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .glassCard()
            }
            .padding(16)
        }
    }
}

private struct StatValue: View {
    let value: String
    let title: String
    let color: Color
    var body: some View { VStack(spacing: 4) { Text(value).font(.system(size: 22, weight: .bold)).foregroundStyle(color); Text(title).font(.system(size: 12)).foregroundStyle(StudyPlanTheme.textSecondary) }.frame(maxWidth: .infinity) }
}

private struct StatBox: View {
    let title: String
    let value: String
    let color: Color
    var body: some View { VStack(spacing: 6) { Text(title).font(.system(size: 13)).foregroundStyle(StudyPlanTheme.textSecondary); Text(value).font(.system(size: 16, weight: .bold)).foregroundStyle(color) }.frame(maxWidth: .infinity).padding(16).glassCard() }
}

private struct InsightCard: View {
    let title: String
    let text: String
    var body: some View { VStack(alignment: .leading, spacing: 8) { Text(title).font(.system(size: 15, weight: .bold)).foregroundStyle(StudyPlanTheme.textPrimary); Text(text).font(.system(size: 13)).foregroundStyle(StudyPlanTheme.textSecondary) }.frame(maxWidth: .infinity, alignment: .leading).padding(16).glassCard() }
}
