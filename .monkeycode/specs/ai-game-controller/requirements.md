# AI Game Controller - Requirements Document

## Introduction

AI Game Controller 是一款Android应用，通过本地部署的AI模型，实现对手机游戏的自动化控制。该应用主要针对火炬之光无限等ARPG游戏，利用无障碍服务获取屏幕内容并执行触控操作，通过实时图像识别理解游戏状态，自动执行游戏操作。

## Glossary

- **AccessibilityService**: Android无障碍服务，用于获取屏幕内容和模拟用户操作
- **PaliGemma**: Google开源的视觉语言模型，具有强大的图像理解能力
- **Game State**: 游戏当前状态，包括角色位置、技能冷却、血量蓝量、敌人位置等
- **Action Sequence**: 连续的游戏操作序列，如移动、释放技能、使用物品等
- **Knowledge Base**: AI的知识存储，包含技能效果、装备属性、游戏策略等
- **Stream Output**: 流式输出，AI持续输出连续动作而非单次决策
- **Memory Module**: 记忆模块，用于存储和调用游戏相关知识
- **ARPG**: 动作角色扮演游戏，如火炬之光无限

## Requirements

### REQ-001: 无障碍服务权限管理

**User Story:** AS 用户, I want 应用能够正确获取和使用无障碍权限, so that AI能够控制手机执行游戏操作

#### Acceptance Criteria

1. WHEN 用户首次启动应用, THE 系统 SHALL 显示无障碍服务权限请求界面并提供引导说明
2. WHEN 用户未授予无障碍权限, THE 系统 SHALL 在主界面显示权限缺失提示并提供跳转设置入口
3. WHEN 无障碍服务被系统禁用, THE 系统 SHALL 在3秒内检测到并通知用户重新启用
4. WHILE 无障碍服务运行中, THE 系统 SHALL 保持服务前台状态防止系统回收
5. WHEN 应用请求无障碍权限, THE 系统 SHALL 显示权限用途说明和使用风险提示

### REQ-002: 屏幕截图与图像采集

**User Story:** AS AI系统, I want 能够持续获取屏幕图像, so that 实时理解游戏内容

#### Acceptance Criteria

1. WHILE 游戏运行中, THE 系统 SHALL 以每秒15-30帧的频率捕获屏幕图像
2. WHEN 捕获屏幕图像, THE 系统 SHALL 支持降低分辨率至原始分辨率的25%-50%以提高处理速度
3. WHEN 图像捕获失败, THE 系统 SHALL 在100毫秒内重试，最多重试3次
4. WHILE 连续捕获屏幕, THE 系统 SHALL 保持截图模块内存占用不超过300MB
5. WHEN 设备支持MediaProjection API, THE 系统 SHALL 优先使用该API获取更高性能的截图

### REQ-003: 本地AI模型部署 (PaliGemma)

**User Story:** AS 用户, I want AI模型完全在本地运行且具备强大理解能力, so that 无需网络连接且能准确理解游戏内容

#### Acceptance Criteria

1. WHEN 应用首次启动, THE 系统 SHALL 从内置资源加载PaliGemma模型文件
2. WHEN 应用启动, THE 系统 SHALL 在10秒内完成AI模型初始化
3. WHILE AI模型运行, THE 系统 SHALL 总内存占用不超过3GB（含应用基础内存）
4. WHEN 执行图像理解, THE 系统 SHALL 在200毫秒内输出识别结果
5. WHEN 设备支持GPU/NPU加速, THE 系统 SHALL 自动启用硬件加速
6. IF 设备内存不足, THE 系统 SHALL 显示警告并提供降级模式选项
7. WHEN AI模型推理完成, THE 系统 SHALL 输出结构化的游戏状态描述

### REQ-004: 游戏界面识别

**User Story:** AS AI系统, I want 能够识别火炬之光无限的游戏界面元素, so that 正确理解当前游戏状态

#### Acceptance Criteria

1. WHEN 屏幕显示主界面/大厅, THE 系统 SHALL 在500毫秒内识别当前界面类型
2. WHEN 屏幕显示战斗界面, THE 系统 SHALL 识别并定位技能按钮、移动摇杆、血量蓝量条、小地图、技能冷却指示器
3. WHEN 屏幕显示背包/装备界面, THE 系统 SHALL 识别装备物品和属性信息
4. WHEN 屏幕显示商店界面, THE 系统 SHALL 识别可购买的物品和价格
5. WHILE 战斗进行中, THE 系统 SHALL 实时识别敌人位置、数量、类型
6. WHILE 战斗进行中, THE 系统 SHALL 识别技能冷却状态、血量百分比、蓝量百分比
7. WHEN 屏幕显示技能说明, THE 系统 SHALL 识别技能效果描述用于学习

### REQ-005: 游戏操作执行

**User Story:** AS AI系统, I want 能够执行各种游戏操作, so that 实现自动化游戏控制

#### Acceptance Criteria

1. WHEN AI决策需要移动, THE 系统 SHALL 通过模拟触摸虚拟摇杆执行移动操作
2. WHEN AI决策需要释放技能, THE 系统 SHALL 在正确位置点击技能按钮
3. WHEN AI决策需要使用物品/药水, THE 系统 SHALL 点击对应物品栏位置
4. WHEN 执行连续操作, THE 系统 SHALL 支持流式输出，操作间隔可配置为30-200毫秒
5. WHEN 执行技能连招, THE 系统 SHALL 按照预设或学习到的连招顺序执行
6. IF 操作执行失败, THE 系统 SHALL 记录失败原因并在下一帧重新决策
7. WHILE 执行移动操作, THE 系统 SHALL 支持八方向和自由方向移动

### REQ-006: AI知识自动学习系统

**User Story:** AS 用户, I want AI能够自动学习游戏知识, so that 快速适应游戏无需手动配置

#### Acceptance Criteria

1. WHEN 用户启动预学习模式, THE 系统 SHALL 自动开始扫描当前游戏界面
2. WHILE 预学习模式运行, THE 系统 SHALL 使用AI识别界面中的技能图标和说明文字
3. WHEN AI识别到新技能, THE 系统 SHALL 提取技能名称、图标特征、效果描述并生成知识条目
4. WHEN AI识别到装备/物品, THE 系统 SHALL 提取物品名称、图标特征、属性效果
5. WHEN 知识条目生成完成, THE 系统 SHALL 显示识别结果供用户确认或修改
6. WHEN 用户确认知识条目, THE 系统 SHALL 将知识保存至本地SQLite数据库
7. WHEN AI在游戏中遇到已学习的技能/物品, THE 系统 SHALL 直接调用存储的知识辅助决策
8. WHEN 用户查看知识库, THE 系统 SHALL 分类显示已保存的技能、装备、策略信息

### REQ-007: 流式动作输出

**User Story:** AS AI系统, I want 能够输出连贯的动作序列, so that 游戏操作流畅自然

#### Acceptance Criteria

1. WHILE 战斗进行中, THE 系统 SHALL 持续生成并输出动作序列
2. WHEN 生成动作序列, THE 系统 SHALL 根据实时游戏状态动态调整后续动作
3. WHEN 检测到危险情况（血量低于阈值）, THE 系统 SHALL 立即中断当前动作并执行回避/治疗操作
4. WHILE 输出动作, THE 系统 SHALL 保持单次决策到操作的延迟在50毫秒以内
5. WHEN 当前动作序列执行完毕, THE 系统 SHALL 无缝生成下一组动作序列

### REQ-008: AI策略配置

**User Story:** AS 用户, I want 能够配置AI的战斗策略, so that 根据不同场景调整AI行为

#### Acceptance Criteria

1. WHEN 用户打开设置界面, THE 系统 SHALL 显示AI策略配置选项
2. THE 系统 SHALL 提供以下策略预设选项：激进型、平衡型、保守型
3. WHEN 用户选择激进型策略, THE 系统 SHALL 配置AI优先攻击敌人，血量安全线设为30%
4. WHEN 用户选择平衡型策略, THE 系统 SHALL 配置AI均衡攻击和躲避，血量安全线设为50%
5. WHEN 用户选择保守型策略, THE 系统 SHALL 配置AI优先保命，血量安全线设为70%
6. THE 系统 SHALL 提供高级配置选项：技能释放优先级、自动吃药阈值、自动拾取开关
7. WHEN 用户修改策略配置, THE 系统 SHALL 实时保存并在当前游戏中生效

### REQ-009: 应用基础设置

**User Story:** AS 用户, I want 能够配置应用的基础参数, so that 根据设备性能调整运行状态

#### Acceptance Criteria

1. WHEN 用户打开设置界面, THE 系统 SHALL 显示基础配置选项
2. THE 系统 SHALL 提供以下配置项：截图帧率(10-30fps)、分辨率缩放比例(25%-100%)、操作延迟(30-200ms)
3. THE 系统 SHALL 显示当前内存占用和预估性能影响
4. WHEN 用户修改配置, THE 系统 SHALL 实时保存配置并在下次启动时自动加载
5. WHEN 用户点击重置配置, THE 系统 SHALL 恢复所有设置到默认值

### REQ-010: 日志与调试

**User Story:** AS 用户, I want 查看AI运行日志, so that 了解AI决策过程和排查问题

#### Acceptance Criteria

1. WHILE AI运行中, THE 系统 SHALL 记录屏幕识别结果、AI决策、执行操作等关键日志
2. WHEN 用户打开日志界面, THE 系统 SHALL 分页显示日志记录，每页50条
3. WHEN 日志存储超过100MB, THE 系统 SHALL 自动清理7天前的日志
4. WHEN 用户需要反馈问题, THE 系统 SHALL 支持导出最近1小时的日志文件
5. THE 系统 SHALL 支持按日志级别筛选：DEBUG、INFO、WARN、ERROR

### REQ-011: 运行状态管理

**User Story:** AS 用户, I want 能够方便地控制AI的启停, so that 灵活管理自动化过程

#### Acceptance Criteria

1. WHEN 用户点击开始按钮, THE 系统 SHALL 启动屏幕捕获和AI推理循环
2. WHEN 用户点击暂停按钮, THE 系统 SHALL 停止操作执行但保持屏幕捕获
3. WHEN 用户点击停止按钮, THE 系统 SHALL 完全停止AI运行并释放资源
4. WHILE AI运行中, THE 系统 SHALL 在状态栏显示持续通知，指示当前状态
5. WHEN 用户切换到其他应用, THE 系统 SHALL 保持后台运行并继续控制游戏

### REQ-012: 稳定性与异常处理

**User Story:** AS 用户, I want 应用稳定运行不崩溃, so that 游戏自动化过程不被中断

#### Acceptance Criteria

1. IF 目标游戏进程异常退出, THE 系统 SHALL 检测到并自动暂停AI操作
2. IF 内存占用超过4GB阈值, THE 系统 SHALL 清理图像缓存并释放内存
3. WHEN 应用被系统强制后台, THE 系统 SHALL 尝试恢复前台服务状态
4. IF AI模型推理异常, THE 系统 SHALL 捕获异常并在3秒内重新初始化
5. IF 连续异常超过5次, THE 系统 SHALL 停止运行并提示用户检查设备状态

## Non-Functional Requirements

### NFR-001: 性能要求
- 屏幕捕获帧率：15-30 FPS（可配置）
- 图像理解延迟：< 200ms
- 操作执行延迟：< 50ms
- 总内存占用：< 3GB（含AI模型）

### NFR-002: 兼容性要求
- Android版本：Android 8.0+
- 目标设备：红米Turbo3 (骁龙8s Gen3, 8GB RAM + 扩展内存)
- 屏幕分辨率：支持2712x1220及常见分辨率自适应

### NFR-003: 可用性要求
- 首次启动提供完整的权限配置向导
- 主界面显示清晰的状态指示（运行中/暂停/停止/错误）
- 支持悬浮窗快捷控制（开始/暂停/停止）
- 提供新手引导教程

## Constraints

1. **AI模型约束**：使用PaliGemma视觉语言模型，需内置约300MB模型文件
2. **设备约束**：需适配红米Turbo3的硬件性能，依赖扩展内存运行大模型
3. **法律约束**：应用仅供个人学习研究使用，用户需自行承担使用风险
4. **安全约束**：不得获取用户敏感信息，所有数据本地处理，不联网传输
5. **游戏约束**：主要针对火炬之光无限优化，可扩展支持其他ARPG游戏
