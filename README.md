# Study-plan

Study-plan 是一款学习计划分解与专注管理应用，支持 Android 与 iOS 两个独立客户端。用户可以输入自然语言学习目标，将其拆分为任务，并通过提醒、日历同步、专注计时和统计功能跟踪学习进度。

## 当前状态

- Android 应用显示名：`Study-plan`
- iOS 应用显示名：`Study-plan`
- Android：Java 17、Android 原生布局、SQLite，最低支持 Android 8.0（API 26）
- iOS：Swift、SwiftUI、EventKit、UserNotifications，面向 iOS 18 真机与模拟器构建
- Android 与 iOS 共享产品目标，但各端保留符合平台习惯的交互方式

## 主要功能

- 自然语言学习计划解析与任务拆分
- 任务日期、具体时间、优先级、时长和页数管理
- 任务展开、排序、完成状态、编辑、搜索和删除
- 本地学习提醒与系统日历同步
- 倒计时、正计时和溢出计时
- 任务完成率、专注时长和阅读页数统计
- 个人资料、头像、学科词库和任务数据导出
- 计时运行时的屏幕流光边框与完成提醒

## 客户端说明

### Android

Android 客户端位于 `Study-plan- Android/`，使用传统 Activity/Fragment 与 XML 布局实现。

构建 APK：

```bash
cd "Study-plan- Android"
./gradlew :app:assembleDebug
```

运行单元测试：

```bash
./gradlew :app:testDebugUnitTest
```

应用显示名由 `app/src/main/res/values/strings.xml` 中的 `app_name` 定义。

### iOS

iOS 客户端位于 `Study-plan-iOS/`，使用 SwiftUI 实现，删除、搜索、表单、导航、分享和系统选择器优先采用 iOS 原生交互。

使用 Xcode 打开：

```text
Study-plan-iOS/Study-plan.xcodeproj
```

连接已信任的 iPhone 后，在 Xcode 中选择真机并运行即可。命令行真机构建示例：

```bash
cd Study-plan-iOS
xcodebuild -project Study-plan.xcodeproj \
  -scheme Study-plan \
  -destination 'generic/platform=iOS' \
  build
```

## 目录结构

```text
Study-Plan/
├── Study-plan- Android/       Android 客户端
├── Study-plan-iOS/            iOS 客户端
├── Documents/                 项目文档归档目录
└── README.md                  项目说明
```

iOS 主要模块：

- `Models/`：任务与计时状态模型
- `Services/`：状态存储、计时引擎、日历和本地通知
- `Helpers/`：主题、文本预处理和任务解析
- `Views/Home/`：任务主页、编辑和搜索
- `Views/Timer/`：计时、任务选择和计时选项
- `Views/Stats/`：学习统计
- `Views/Profile/`：个人中心、设置、关于和学科词库

## 开发约定

- 新功能应先明确属于 Android、iOS 或双端范围。
- Android 与 iOS 可以共享业务目标，但删除、导航、表单、系统分享等基础交互优先遵循各平台原生规范。
- 修改前保留用户已有的 Xcode 工作区状态文件，不将其作为业务代码提交。
- 新增文本文件使用 UTF-8 编码和 LF 换行。
