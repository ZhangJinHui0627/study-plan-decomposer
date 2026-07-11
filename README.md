# StudyPlanApp (双端学习计划分解助手)

StudyPlanApp 是一款旨在帮助用户高效分解和执行学习计划的跨平台应用。通过智能拆解学习目标、沉浸式专注计时以及多维度的进度统计，帮助用户建立良好的学习惯性，双端在功能设计、数据协议、业务逻辑以及视觉体验上均达到了完全一致。

---

## 🚀 核心功能

*   **智能任务管理 (Task Management)**：
    *   智能解析目标文本，自动提取并分解为具体任务。
    *   支持任务优先级划分、日期时间设定及独立的闹钟提醒。
    *   支持展开详情查看、拖拽手动排序、左右滑动操作以及长按批量勾选删除。
*   **沉浸式专注计时 (Timer)**：
    *   支持倒计时与正计时，针对特定任务或自定义独立计时。
    *   倒计时结束触发高频振动及弹窗状态同步；正计时支持“超时溢出模式”，方便统计超额专注时间。
    *   防秒退拦截：对设定时长为 0 分钟的任务，自动拦截并提醒输入有效时长。
*   **多维度数据统计 (Stats)**：
    *   可视化呈现“任务完成率”、“已完成任务的专注时间”及“已完成任务的计划阅读页数”。
    *   提供“学习小结”及“今日专注建议”，建立科学的学习启动惯性。
*   **个人中心与个性化 (Profile)**：
    *   允许自由修改昵称、个性签名，支持自定义系统头像或相册更换头像。
    *   支持动态七彩霓虹边框或特定单色的发光旋转边框。
    *   学科词库管理：支持用户自定义专有学科名词规则，强化智能文本匹配库。

---

## 🎨 视觉与交互规范 (跨端对齐)

1.  **全屏流光跑马灯 (ScreenGlow)**：当任意计时器正在运行时，全屏/全窗口边缘会循环显示蓝绿交替的跑马灯，具备不透明度呼吸（`0.3`~`0.85`）与外发光阴影抖动（`8`~`14`）特效，营造沉浸专注场域。
2.  **动态头像框 (AvatarFrame)**：个人头像外围由 6.5s 周期旋转的 SweepGradient 环绕，在默认七彩霓虹或单色渐变的基础上，内部白光线宽与不透明度随正弦脉冲振荡起伏。
3.  **视觉统一卡片**：对 `[加急]` 与 `[优先]` 任务予以红/橙加粗色块区分，已完成卡片呈半透明磨砂质感并加中划线，逾期卡片背景保持默认，仅日期显示红色高亮。

---

## 🛠 技术栈与目录结构

### 1. Android 端
*   **技术栈**：Java 17 / Min SDK 26 / SQLite / Activity & Fragment 传统布局
*   **目录结构** (`Study-plan- Android/app/src/main/java/com/example/studyplan/`)：
    *   包含主要 Activity、Fragment 容器以及自定义绘制的 `AvatarFrameView.java`、`ScreenGlowFrameView.java` 等组件。

### 2. iOS 端
*   **技术栈**：Swift 5.10+ / iOS 17.0+ / SwiftUI / EventKit (日历同步) / UserNotifications (本地提醒)
*   **重构优化后的目录结构** (`Study-plan-iOS/Study-plan/`)：
    *   `Models/`：`Task.swift` (数据实体与计时器状态模型)
    *   `Services/`：`StudyPlanStore.swift` (状态管理)、`CalendarService.swift` (系统日历同步)、`NotificationScheduler.swift` (本地通知)、`TimerEngine.swift` (计时引擎)
    *   `Helpers/`：`TaskParser.swift` (智能提取解析)、`TextPreprocessor.swift` (正则处理器)、`StudyPlanTheme.swift` (毛玻璃与色彩系统)
    *   `Views/`：`ContentView.swift` (主标签栏导航及流光跑马灯)
        *   `Home/`：主页待办列表、任务卡片、任务编辑器及搜索页面
        *   `Timer/`：专注倒计时钟、专注选项配置及待办选择绑定页面
        *   `Stats/`：完成率统计、学习建议及状态概览卡片
        *   `Profile/`：个人名片管理、设置管理、关于页面及自定义学科词库页面

---

## 📦 编译与运行

### Android 端
1.  使用 Android Studio 打开 `Study-plan- Android` 目录。
2.  同步 Gradle，运行 `:app:assembleDebug` 生成调试版 APK。
3.  运行单元测试命令：`./gradlew :app:testDebugUnitTest`。

### iOS 端
1.  在 macOS 下，使用 Xcode 16+ 打开 `Study-plan-iOS/Study-plan.xcodeproj`。
2.  通过命令行直接构建：
    ```bash
    xcodebuild -project Study-plan.xcodeproj -scheme Study-plan -destination 'generic/platform=iOS Simulator'
    ```
3.  在 Xcode 模拟器或真机上点击 Run (Command + R) 即可运行。
