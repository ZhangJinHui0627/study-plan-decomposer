import SwiftUI

enum StudyPlanTheme {
    static let primary = Color(red: 0.10, green: 0.45, blue: 0.91)
    static let textPrimary = Color(red: 0.12, green: 0.12, blue: 0.12)
    static let textSecondary = Color(red: 0.37, green: 0.39, blue: 0.41)
    static let statGreen = Color(red: 0.16, green: 0.60, blue: 0.32)
    static let statOrange = Color(red: 0.93, green: 0.49, blue: 0.12)
    static let background = LinearGradient(colors: [Color(red: 0.95, green: 0.98, blue: 1), Color(red: 0.91, green: 0.95, blue: 1)], startPoint: .topLeading, endPoint: .bottomTrailing)
}

extension View {
    func glassCard(cornerRadius: CGFloat = 18) -> some View {
        background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: cornerRadius))
            .overlay(RoundedRectangle(cornerRadius: cornerRadius).stroke(.white.opacity(0.55), lineWidth: 1))
            .shadow(color: .black.opacity(0.06), radius: 8, y: 3)
    }
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, ((int >> 8) & 0xf) * 17, ((int >> 4) & 0xf) * 17, (int & 0xf) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, (int >> 16) & 0xff, (int >> 8) & 0xff, int & 0xff)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = ((int >> 24) & 0xff, (int >> 16) & 0xff, (int >> 8) & 0xff, int & 0xff)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

