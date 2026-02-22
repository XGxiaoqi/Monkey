# AI Game Controller - Android应用

## 项目概述

AI Game Controller 是一款基于本地AI的Android游戏自动化控制应用，主要针对火炬之光无限等ARPG游戏。该应用通过无障碍服务获取屏幕内容，使用PaliGemma视觉语言模型实时理解游戏画面，自动生成并执行游戏操作。

## 核心功能

1. **本地AI推理** - 使用PaliGemma视觉语言模型进行屏幕理解
2. **无障碍服务控制** - 通过AccessibilityService执行触控操作
3. **知识学习系统** - 自动学习技能/装备信息，支持预设知识库
4. **策略配置** - 激进/平衡/保守三种AI策略
5. **流式动作输出** - 连续不间断的游戏操作

## 技术架构

```
AIGameController/
├── app/
│   ├── src/main/
│   │   ├── java/com/aigame/controller/
│   │   │   ├── ui/                    # UI层
│   │   │   │   ├── MainActivity       # 主界面
│   │   │   │   ├── SettingsActivity   # 设置界面
│   │   │   │   ├── KnowledgeActivity  # 知识库界面
│   │   │   │   ├── LearnActivity      # 预学习界面
│   │   │   │   └── LogActivity        # 日志界面
│   │   │   ├── service/               # 服务层
│   │   │   │   ├── GameAccessibilityService  # 无障碍服务
│   │   │   │   ├── ScreenCaptureService      # 屏幕截图服务
│   │   │   │   └── GameControlService        # 控制服务
│   │   │   ├── ai/                    # AI引擎层
│   │   │   │   ├── AIEngine           # AI模型管理
│   │   │   │   ├── GameStateParser    # 游戏状态解析
│   │   │   │   └── ActionGenerator    # 动作生成器
│   │   │   ├── data/                  # 数据层
│   │   │   │   ├── entity/            # 数据实体
│   │   │   │   ├── KnowledgeDatabase  # Room数据库
│   │   │   │   └── TorchlightInfiniteData  # 预设数据
│   │   │   └── utils/                 # 工具类
│   │   ├── res/                       # 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## 环境要求

### 开发环境
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.2

### 运行设备
- Android 8.0 (API 26) 或更高版本
- 推荐：红米Turbo3 (骁龙8s Gen3, 8GB RAM)
- 屏幕分辨率：2712x1220 或类似

## 构建步骤

### 1. 克隆项目
```bash
cd /workspace
```

### 2. 使用Android Studio打开项目
1. 打开 Android Studio
2. 选择 "Open an Existing Project"
3. 选择 `AIGameController` 目录

### 3. 同步Gradle
首次打开项目时，Android Studio会自动同步Gradle依赖。

### 4. 添加AI模型文件（可选）
将PaliGemma的TensorFlow Lite模型文件放置到：
```
app/src/main/assets/paligemma.tflite
```

如果没有模型文件，应用会以模拟模式运行，使用简化的图像处理逻辑。

### 5. 构建APK

**Debug版本：**
```bash
./gradlew assembleDebug
```

**Release版本：**
```bash
./gradlew assembleRelease
```

生成的APK位于：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 安装和使用

### 1. 安装APK
```bash
adb install app-debug.apk
```

### 2. 授予权限
首次运行需要授予以下权限：
1. **无障碍服务权限**
   - 进入系统设置 -> 无障碍
   - 找到 "AI Game Controller"
   - 开启服务

2. **悬浮窗权限**（可选，用于显示控制面板）
   - 进入系统设置 -> 应用 -> AI Game Controller
   - 授予"显示在其他应用上层"权限

### 3. 预学习（推荐）
在开始控制前，建议先进行预学习：
1. 打开火炬之光无限游戏
2. 回到AI Game Controller应用
3. 点击"预学习"
4. 点击"开始学习"
5. 让AI自动识别技能和装备
6. 确认并保存学习结果

### 4. 开始控制
1. 打开目标游戏
2. 回到AI Game Controller应用
3. 点击"开始"按钮
4. 授予屏幕截图权限
5. AI将开始自动控制游戏

### 5. 调整设置
- **帧率**：调整截图频率 (10-30 FPS)
- **分辨率**：降低分辨率可提高速度
- **策略**：选择激进/平衡/保守策略
- **自动吃药**：设置自动使用药水的血量阈值

## 性能优化建议

1. **降低分辨率**：将分辨率缩放设置为50%可显著提高响应速度
2. **调整帧率**：15-20 FPS通常足够流畅
3. **关闭不必要的动画**：在开发者选项中关闭窗口动画
4. **清理后台应用**：释放更多内存给AI模型

## 注意事项

1. **法律声明**：本应用仅供学习研究使用，请勿用于违反游戏规则的行为
2. **使用风险**：使用本应用产生的任何后果由用户自行承担
3. **数据安全**：所有数据均在本地处理，不会上传到服务器
4. **兼容性**：主要针对火炬之光无限优化，其他游戏可能需要调整

## 已知问题

1. 某些设备可能需要额外授予权限
2. AI模型文件较大（约300MB），首次加载需要时间
3. 在低端设备上可能需要降低分辨率和帧率

## 版本历史

### v1.0.0 (2026-02-22)
- 初始版本发布
- 支持火炬之光无限基本自动化
- 内置预设技能知识库
- 支持三种AI策略

## 开发者信息

本项目使用以下开源技术：
- TensorFlow Lite 2.14
- Room Database 2.6
- Kotlin Coroutines
- Android Jetpack Components

## 许可证

本项目仅供个人学习研究使用。
