package com.aigame.controller.model

/**
 * 运行状态枚举
 */
enum class RunStatus {
    IDLE,           // 空闲
    INITIALIZING,   // 初始化中
    RUNNING,        // 运行中
    PAUSED,         // 暂停
    ERROR           // 错误
}

/**
 * AI策略类型
 */
enum class StrategyType {
    AGGRESSIVE,     // 激进型 - 优先攻击，血量安全线30%
    BALANCED,       // 平衡型 - 均衡攻防，血量安全线50%
    CONSERVATIVE    // 保守型 - 优先保命，血量安全线70%
}

/**
 * 屏幕类型枚举
 */
enum class ScreenType {
    MAIN_MENU,      // 主菜单
    BATTLE,         // 战斗中
    INVENTORY,      // 背包
    SHOP,           // 商店
    DIALOG,         // 对话
    LOADING,        // 加载中
    UNKNOWN         // 未知
}

/**
 * 技能效果类型
 */
enum class SkillEffectType {
    DAMAGE,         // 伤害
    HEAL,           // 治疗
    BUFF,           // 增益
    DEBUFF,         // 减益
    CONTROL,        // 控制
    MOBILITY        // 位移
}
