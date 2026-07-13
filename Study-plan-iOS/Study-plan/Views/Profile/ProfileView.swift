import SwiftUI
import PhotosUI

struct ProfileView: View {
    @EnvironmentObject private var store: StudyPlanStore
    @State private var showingSettings = false
    @State private var showingAbout = false
    @State private var selectedPhoto: PhotosPickerItem?

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                VStack(spacing: 8) {
                    PhotosPicker(selection: $selectedPhoto, matching: .images) {
                        AvatarFrameView(frameColorName: store.avatarFrameColor, imageData: store.avatarImageData)
                            .frame(width: 96, height: 96)
                    }
                    .onChange(of: selectedPhoto) { _, item in
                        Swift.Task { if let data = try? await item?.loadTransferable(type: Data.self) { store.avatarImageData = data } }
                    }
                    Text(store.nickname).font(.system(size: 18, weight: .bold)).foregroundStyle(StudyPlanTheme.textPrimary)
                    Text(store.signature).font(.system(size: 13)).foregroundStyle(StudyPlanTheme.textSecondary)
                }
                .frame(maxWidth: .infinity)
                .padding(20)
                .glassCard()

                VStack(spacing: 0) {
                    ToggleRow(title: "允许手动打卡", icon: "checkmark", isOn: $store.manualCompletionEnabled)
                    Divider().padding(.leading, 54)
                    Button { showingAbout = true } label: { NavigationRow(title: "关于学习计划拆解", icon: "info.circle") }
                    Divider().padding(.leading, 54)
                    Button { showingSettings = true } label: { NavigationRow(title: "设置", icon: "gearshape") }
                }
                .padding(6)
                .glassCard()
            }
            .padding(16)
        }
        .sheet(isPresented: $showingSettings) { SettingsView() }
        .sheet(isPresented: $showingAbout) { AboutView() }
        .onChange(of: store.manualCompletionEnabled) { _, _ in
            store.saveProfile()
        }
        .onDisappear { store.saveProfile() }
    }
}

private struct ToggleRow: View {
    let title: String
    let icon: String
    @Binding var isOn: Bool
    var body: some View { HStack { Image(systemName: icon).frame(width: 24); Text(title).frame(maxWidth: .infinity, alignment: .leading); Toggle("", isOn: $isOn).labelsHidden() }.foregroundStyle(StudyPlanTheme.textPrimary).padding(.horizontal, 12).frame(height: 52) }
}

private struct NavigationRow: View {
    let title: String
    let icon: String
    var body: some View { HStack { Image(systemName: icon).frame(width: 24); Text(title).frame(maxWidth: .infinity, alignment: .leading); Image(systemName: "chevron.right").font(.caption).foregroundStyle(StudyPlanTheme.textSecondary) }.foregroundStyle(StudyPlanTheme.textPrimary).padding(.horizontal, 12).frame(height: 52) }
}
