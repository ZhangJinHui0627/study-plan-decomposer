import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @State private var selectedTab = AppTab.home

    var body: some View {
        ZStack {
            StudyPlanTheme.background.ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    Text(selectedTab.title)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(StudyPlanTheme.textPrimary)
                    Spacer()
                    if selectedTab == .home {
                        Button { store.isShowingAddTask = true } label: { Image(systemName: "plus").frame(width: 40, height: 40) }
                        Button { store.isShowingSearch = true } label: { Image(systemName: "magnifyingglass").frame(width: 40, height: 40) }
                    }
                }
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(StudyPlanTheme.textPrimary)
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .padding(.bottom, 8)

                Group {
                    switch selectedTab {
                    case .home: HomeView()
                    case .stats: StatsView()
                    case .timer: TimerView()
                    case .profile: ProfileView()
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                HStack(spacing: 0) {
                    ForEach(AppTab.allCases) { tab in
                        Button { selectedTab = tab } label: {
                            VStack(spacing: 4) {
                                Image(systemName: tab.icon).font(.system(size: 17, weight: .semibold))
                                Text(tab.label).font(.system(size: 11, weight: .medium))
                            }
                            .foregroundStyle(selectedTab == tab ? StudyPlanTheme.primary : StudyPlanTheme.textSecondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                        }
                    }
                }
                .glassCard(cornerRadius: 24)
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }
        }
        .sheet(isPresented: $store.isShowingAddTask) { TaskEditorView(task: nil) }
        .sheet(item: $store.editingTask) { TaskEditorView(task: $0) }
        .sheet(isPresented: $store.isShowingSearch) { SearchTaskView() }
        .overlay { ScreenGlowOverlay(isRunning: store.timer.isRunning) }
    }
}

enum AppTab: String, CaseIterable, Identifiable {
    case home, stats, timer, profile
    var id: String { rawValue }
    var title: String { switch self { case .home: "学习计划"; case .stats: "学习统计"; case .timer: "专注计时"; case .profile: "个人中心" } }
    var label: String { switch self { case .home: "计划"; case .stats: "统计"; case .timer: "计时"; case .profile: "我的" } }
    var icon: String { switch self { case .home: "house"; case .stats: "chart.bar"; case .timer: "timer"; case .profile: "person" } }
}

private struct ScreenGlowOverlay: View {
    let isRunning: Bool
    var body: some View {
        if isRunning {
            TimelineView(.animation) { context in
                let time = context.date.timeIntervalSinceReferenceDate
                let angleRad = (time.truncatingRemainder(dividingBy: 16) / 16) * 2 * .pi
                let angleDeg = angleRad * 180 / .pi
                
                let breathAlpha = 0.575 + 0.275 * sin(angleRad * 2.5)
                let alpha = max(0.3, min(0.85, breathAlpha))
                
                let blur = 11.0 + 3.0 * sin(angleRad * 3.0)
                let blurRadius = max(8.0, min(14.0, blur))
                
                RoundedRectangle(cornerRadius: 44)
                    .stroke(
                        AngularGradient(
                            colors: [Color(hex: "#1A73E8"), Color(hex: "#00D2FF"), Color(hex: "#1A73E8")],
                            center: .center
                        ),
                        lineWidth: 3
                    )
                    .rotationEffect(.degrees(angleDeg))
                    .shadow(color: Color(hex: "#1A73E8").opacity(alpha * 0.7), radius: blurRadius)
                    .opacity(alpha)
                    .padding(3)
                    .ignoresSafeArea()
                    .allowsHitTesting(false)
            }
        }
    }
}
