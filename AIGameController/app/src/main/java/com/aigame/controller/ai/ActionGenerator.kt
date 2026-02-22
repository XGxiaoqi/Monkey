package com.aigame.controller.ai

import com.aigame.controller.data.KnowledgeDatabase
import com.aigame.controller.model.*
import com.aigame.controller.utils.LogManager
import kotlinx.coroutines.runBlocking
import kotlin.math.*

/**
 * 动作生成器
 * 根据游戏状态生成动作序列
 */
class ActionGenerator(
    private var config: AppConfig,
    private val knowledgeDatabase: KnowledgeDatabase
) {

    private val logManager = LogManager.Companion.instance

    // 策略参数
    private var healthThreshold = config.getHealthThreshold()
    private var autoPickup = config.autoPickup
    private var autoPotionThreshold = config.autoPotionThreshold

    // 技能优先级（可通过知识库学习）
    private var skillPriority = listOf(0, 1, 2, 3, 4)

    // 当前动作序列
    private var currentActionSequence: MutableList<GameAction> = mutableListOf()
    private var sequenceIndex = 0

    /**
     * 生成动作序列
     */
    fun generateActions(state: GameState): List<GameAction> {
        val actions = mutableListOf<GameAction>()

        when (state.screenType) {
            ScreenType.BATTLE -> {
                actions.addAll(generateBattleActions(state))
            }
            ScreenType.MAIN_MENU -> {
                actions.addAll(generateMenuActions(state))
            }
            ScreenType.INVENTORY -> {
                actions.addAll(generateInventoryActions(state))
            }
            ScreenType.SHOP -> {
                actions.addAll(generateShopActions(state))
            }
            else -> {
                // 未知界面，等待
                actions.add(GameAction.Wait(500))
            }
        }

        return actions
    }

    /**
     * 生成战斗动作
     */
    private fun generateBattleActions(state: GameState): List<GameAction> {
        val actions = mutableListOf<GameAction>()
        val playerState = state.playerState ?: return listOf(GameAction.Wait(100))

        // 1. 检查紧急情况（血量过低）
        if (playerState.healthPercent < healthThreshold) {
            val emergencyAction = generateEmergencyAction(state)
            if (emergencyAction != null) {
                actions.add(emergencyAction)
                return actions
            }
        }

        // 2. 检查是否需要使用药水
        if (playerState.healthPercent < autoPotionThreshold) {
            actions.add(GameAction.UseItem(0))  // 假设药水在物品栏第一位
        }

        // 3. 寻找目标并攻击
        val nearestEnemy = state.enemies.minByOrNull { it.distance }
        if (nearestEnemy != null && nearestEnemy.isAggressive) {
            // 移动接近敌人
            if (nearestEnemy.distance > 0.3f) {
                val moveDirection = calculateDirection(playerState.position, nearestEnemy.position)
                actions.add(GameAction.Move(moveDirection, 0.5f))
            }

            // 使用技能攻击
            val availableSkills = state.skills.filter { it.isReady }
            if (availableSkills.isNotEmpty()) {
                // 按优先级选择技能
                for (skillIndex in skillPriority) {
                    val skill = availableSkills.find { it.index == skillIndex }
                    if (skill != null) {
                        actions.add(GameAction.UseSkill(
                            skillIndex = skill.index,
                            targetX = nearestEnemy.position.x,
                            targetY = nearestEnemy.position.y
                        ))
                        break
                    }
                }
            }
        } else {
            // 没有敌人，探索移动
            actions.add(GameAction.Move((Math.random() * 360).toFloat(), 0.3f))
        }

        // 4. 自动拾取
        if (autoPickup && actions.isEmpty()) {
            // 检测地面物品并拾取（简化实现）
            // actions.add(...)
        }

        return actions.ifEmpty { listOf(GameAction.Wait(50)) }
    }

    /**
     * 生成紧急回避动作
     */
    private fun generateEmergencyAction(state: GameState): GameAction? {
        val playerState = state.playerState ?: return null

        // 找到最近的敌人
        val nearestEnemy = state.enemies.minByOrNull { it.distance } ?: return null

        // 计算远离敌人的方向
        val awayDirection = calculateDirection(nearestEnemy.position, playerState.position)

        // 快速移动远离
        return GameAction.Move(awayDirection, 1f)
    }

    /**
     * 生成菜单动作
     */
    private fun generateMenuActions(state: GameState): List<GameAction> {
        // 在主菜单界面，尝试进入游戏
        return listOf(
            GameAction.Click(540, 1200, 100),  // 点击开始游戏按钮（假设位置）
            GameAction.Wait(500)
        )
    }

    /**
     * 生成背包动作
     */
    private fun generateInventoryActions(state: GameState): List<GameAction> {
        // 简单处理：关闭背包返回游戏
        return listOf(
            GameAction.Click(50, 50, 100),  // 点击关闭按钮
            GameAction.Wait(200)
        )
    }

    /**
     * 生成商店动作
     */
    private fun generateShopActions(state: GameState): List<GameAction> {
        // 简单处理：关闭商店
        return listOf(
            GameAction.Click(50, 50, 100),
            GameAction.Wait(200)
        )
    }

    /**
     * 流式生成动作
     */
    fun generateActionsStream(state: GameState, callback: (GameAction) -> Unit) {
        val actions = generateActions(state)
        for (action in actions) {
            callback(action)
        }
    }

    /**
     * 计算移动方向
     */
    private fun calculateDirection(from: android.graphics.Point, to: android.graphics.Point): Float {
        val dx = to.x - from.x
        val dy = to.y - from.y
        // 转换为角度（0度为右，90度为上）
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    /**
     * 计算两点距离
     */
    private fun calculateDistance(p1: android.graphics.Point, p2: android.graphics.Point): Float {
        val dx = (p2.x - p1.x).toFloat()
        val dy = (p2.y - p1.y).toFloat()
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * 更新配置
     */
    fun updateConfig(newConfig: AppConfig) {
        config = newConfig
        healthThreshold = config.getHealthThreshold()
        autoPickup = config.autoPickup
        autoPotionThreshold = config.autoPotionThreshold
    }

    /**
     * 设置技能优先级
     */
    fun setSkillPriority(priority: List<Int>) {
        skillPriority = priority
    }

    /**
     * 从知识库加载技能信息
     */
    fun loadSkillKnowledge() {
        runBlocking {
            try {
                val skills = knowledgeDatabase.skillDao().getAll()
                if (skills.isNotEmpty()) {
                    // 根据技能效果类型重新排序优先级
                    val priorityMap = mapOf(
                        SkillEffectType.CONTROL to 0,
                        SkillEffectType.DAMAGE to 1,
                        SkillEffectType.BUFF to 2,
                        SkillEffectType.HEAL to 3,
                        SkillEffectType.MOBILITY to 4,
                        SkillEffectType.DEBUFF to 5
                    )

                    val sortedSkills = skills.sortedBy { skill ->
                        priorityMap[SkillEffectType.valueOf(skill.effectType)] ?: 99
                    }

                    skillPriority = sortedSkills.map { it.id.toInt() }
                    logManager.info(TAG, "Loaded skill priority from knowledge base: $skillPriority")
                }
            } catch (e: Exception) {
                logManager.error(TAG, "Failed to load skill knowledge: ${e.message}")
            }
        }
    }

    /**
     * 生成连招序列
     */
    fun generateCombo(comboId: String, state: GameState): List<GameAction> {
        // 预定义的连招模板
        return when (comboId) {
            "basic_attack" -> listOf(
                GameAction.UseSkill(1),  // 技能1
                GameAction.Wait(100),
                GameAction.UseSkill(2),  // 技能2
                GameAction.Wait(100),
                GameAction.UseSkill(0)   // 普攻
            )
            "burst" -> listOf(
                GameAction.UseSkill(4),  // 大招
                GameAction.Wait(200),
                GameAction.UseSkill(3),  // 技能3
                GameAction.Wait(100),
                GameAction.UseSkill(2),  // 技能2
                GameAction.Wait(100),
                GameAction.UseSkill(1)   // 技能1
            )
            else -> emptyList()
        }
    }

    companion object {
        private const val TAG = "ActionGenerator"
    }
}
