import SwiftUI
import UIKit

struct AvatarFrameView: View {
    let frameColorName: String
    var imageData: Data? = nil
    
    var body: some View {
        TimelineView(.animation) { context in
            let time = context.date.timeIntervalSinceReferenceDate
            let progress = (time.truncatingRemainder(dividingBy: 6.5) / 6.5)
            let rotation = progress * 360
            
            // 脉冲周期与旋转周期一致
            let pulse = 0.5 + 0.5 * sin(progress * .pi * 2)
            
            let colors = getColors(for: frameColorName)
            
            GeometryReader { geometry in
                let size = min(geometry.size.width, geometry.size.height)
                let radius = size / 2 - 3
                
                ZStack {
                    // 主旋转渐变环
                    Circle()
                        .stroke(
                            AngularGradient(colors: colors, center: .center),
                            style: StrokeStyle(lineWidth: 6)
                        )
                        .rotationEffect(.degrees(rotation))
                        .frame(width: radius * 2, height: radius * 2)
                    
                    // 白色呼吸发光环
                    Circle()
                        .stroke(
                            Color.white.opacity(0.176 + 0.334 * pulse),
                            style: StrokeStyle(lineWidth: CGFloat(2.0 + 2.0 * pulse))
                        )
                        .frame(width: (radius - 4) * 2, height: (radius - 4) * 2)
                    
                    // 头像部分
                    Group {
                        if let imageData, let image = UIImage(data: imageData) {
                            Image(uiImage: image)
                                .resizable()
                                .scaledToFill()
                        } else {
                            Circle()
                                .fill(Color.white)
                                .overlay(
                                    Text("添加图片")
                                        .font(.system(size: max(11, size * 0.14), weight: .medium))
                                        .foregroundStyle(StudyPlanTheme.textSecondary)
                                )
                        }
                    }
                    .clipShape(Circle())
                    .frame(width: (radius - 6) * 2, height: (radius - 6) * 2)
                }
                .frame(width: size, height: size)
            }
        }
    }
    
    private func getColors(for _: String) -> [Color] {
        [
            Color(hex: "#FF5F6D"), Color(hex: "#FFC371"), Color(hex: "#FFE66D"), Color(hex: "#7BE495"),
            Color(hex: "#56CCF2"), Color(hex: "#8E7CFF"), Color(hex: "#FF6FD8"), Color(hex: "#FF5F6D")
        ]
    }
}
