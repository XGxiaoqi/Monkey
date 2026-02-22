# AI Game Controller

基于本地AI的Android游戏自动化控制应用，主要针对火炬之光无限等ARPG游戏。

## 功能特性

- **本地AI推理** - 使用PaliGemma视觉语言模型进行屏幕理解
- **无障碍服务控制** - 通过AccessibilityService执行触控操作
- **知识学习系统** - 自动学习技能/装备信息，支持预设知识库
- **策略配置** - 激进/平衡/保守三种AI策略
- **流式动作输出** - 连续不间断的游戏操作

## 下载APK

前往 [Releases](https://github.com/XGxiaoqi/Monkey/releases) 页面下载最新版本的APK。

## 使用方法

1. 安装APK到Android设备
2. 授予无障碍服务权限
3. （可选）进行预学习识别技能
4. 打开火炬之光无限游戏
5. 回到应用点击"开始"

## 环境要求

- Android 8.0+ (API 26)
- 推荐：红米Turbo3或同等性能设备
- 屏幕分辨率：2712x1220或类似

## 免责声明

本应用仅供学习研究使用，请勿用于违反游戏规则的行为。使用本应用产生的任何后果由用户自行承担。

## 技术文档

详细的需求文档和技术设计请查看 `.monkeycode/specs/ai-game-controller/` 目录。
