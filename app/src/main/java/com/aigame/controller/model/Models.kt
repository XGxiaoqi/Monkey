package com.aigame.controller.model

import android.graphics.Point

/**
 * 应用配置
 */
data class AppConfig(
    val frameRate: Int = 20,            // 截图帧率 10-30 fps
    val resolutionScale: Float = 0.5f,  // 分辨率缩放 0.25-1.0
    val actionDelay: Int = 50,          // 操作延迟 30-200 ms
    val strategy: StrategyType = StrategyType.BALANCED,
    val autoPickup: Boolean = true,     // 自动拾取
    val autoPotionThreshold: Int = 30,  // 自动吃药阈值 0-100%
    val targetPackageName: String = ""  // 目标游戏包名
) {
    /**
     * 根据策略类型获取血量安全线
     */
    fun getHealthThreshold(): Int = when (strategy) {
        StrategyType.AGGRESSIVE -> 30
        StrategyType.BALANCED -> 50
        StrategyType.CONSERVATIVE -> 70
    }
}

/**
 * 游戏状态
 */
data class GameState(
    val screenType: ScreenType = ScreenType.UNKNOWN,
    val playerState: PlayerState? = null,
    val enemies: List<EnemyInfo> = emptyList(),
    val skills: List<SkillState> = emptyList(),
    val items: List<ItemState> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 玩家状态
 */
data class PlayerState(
    val healthPercent: Int = 100,
    val manaPercent: Int = 100,
    val position: Point = Point(0, 0),
    val isMoving: Boolean = false
)

/**
 * 敌人信息
 */
data class EnemyInfo(
    val position: Point,
    val distance: Float,
    val type: String? = null,
    val isAggressive: Boolean = true
)

/**
 * 技能状态
 */
data class SkillState(
    val index: Int,
    val name: String? = null,
    val isReady: Boolean = true,
    val cooldownRemaining: Int = 0,
    val position: Point? = null
)

/**
 * 物品状态
 */
data class ItemState(
    val index: Int,
    val name: String? = null,
    val count: Int = 0,
    val position: Point? = null
)

/**
 * 游戏动作密封类
 */
sealed class GameAction {
    /**
     * 移动动作
     * @param direction 方向角度 (0-360度, 0为右, 90为上)
     * @param distance 移动距离 (0-1归一化)
     */
    data class Move(
        val direction: Float,
        val distance: Float = 1f
    ) : GameAction()

    /**
     * 使用技能
     * @param skillIndex 技能索引
     * @param targetX 目标X坐标
     * @param targetY 目标Y坐标
     */
    data class UseSkill(
        val skillIndex: Int,
        val targetX: Int? = null,
        val targetY: Int? = null
    ) : GameAction()

    /**
     * 使用物品
     * @param itemIndex 物品索引
     */
    data class UseItem(
        val itemIndex: Int
    ) : GameAction()

    /**
     * 点击动作
     * @param x X坐标
     * @param y Y坐标
     * @param duration 持续时间(ms)
     */
    data class Click(
        val x: Int,
        val y: Int,
        val duration: Long = 100
    ) : GameAction()

    /**
     * 滑动动作
     */
    data class Swipe(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int,
        val duration: Long = 300
    ) : GameAction()

    /**
     * 等待动作
     * @param durationMs 等待时间(毫秒)
     */
    data class Wait(
        val durationMs: Long
    ) : GameAction()

    /**
     * 组合动作
     * @param actions 动作列表
     */
    data class CompositeAction(
        val actions: List<GameAction>
    ) : GameAction()
}

/**
 * 技能知识
 */
data class SkillKnowledge(
    val id: String,
    val name: String,
    val iconFeature: FloatArray,
    val description: String,
    val cooldown: Int?,
    val effectType: SkillEffectType,
    val effectValue: Int?,
    val position: Point?,      // 在屏幕上的位置
    val learnTime: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SkillKnowledge
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * 物品知识
 */
data class ItemKnowledge(
    val id: String,
    val name: String,
    val iconFeature: FloatArray,
    val attributes: Map<String, Int>,
    val position: Point?,
    val learnTime: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ItemKnowledge
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
