package com.aigame.controller.ai

import android.graphics.Point
import android.graphics.Rect
import com.aigame.controller.data.KnowledgeDatabase
import com.aigame.controller.model.*
import com.aigame.controller.utils.LogManager
import kotlin.math.*

/**
 * 游戏状态解析器
 * 将AI模型输出转换为结构化的游戏状态
 */
class GameStateParser {

    private val logManager = LogManager.Companion.instance

    /**
     * 从模型输出解析游戏状态
     */
    fun parseFromModelOutput(output: FloatArray, screenWidth: Int, screenHeight: Int): GameState {
        // 这是一个简化的解析逻辑
        // 实际实现需要根据PaliGemma的输出格式进行调整

        // 假设输出格式：
        // [0-3]: 屏幕类型 (one-hot encoding)
        // [4-7]: 玩家血量百分比
        // [8-11]: 玩家蓝量百分比
        // [12-15]: 玩家X位置 (归一化)
        // [16-19]: 玩家Y位置 (归一化)
        // [20-23]: 敌人数量
        // ... 更多字段

        val screenType = parseScreenType(output, 0)
        val playerState = parsePlayerState(output, screenWidth, screenHeight)
        val enemies = parseEnemies(output, screenWidth, screenHeight)
        val skills = parseSkills(output, screenWidth, screenHeight)

        return GameState(
            screenType = screenType,
            playerState = playerState,
            enemies = enemies,
            skills = skills,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * 解析屏幕类型
     */
    private fun parseScreenType(output: FloatArray, offset: Int): ScreenType {
        if (output.size < offset + 4) return ScreenType.UNKNOWN

        val maxIndex = (offset until offset + 4).maxByOrNull { output[it] } ?: offset
        return when (maxIndex - offset) {
            0 -> ScreenType.MAIN_MENU
            1 -> ScreenType.BATTLE
            2 -> ScreenType.INVENTORY
            3 -> ScreenType.SHOP
            else -> ScreenType.UNKNOWN
        }
    }

    /**
     * 解析玩家状态
     */
    private fun parsePlayerState(output: FloatArray, screenWidth: Int, screenHeight: Int): PlayerState {
        // 简化的解析逻辑
        val healthPercent = if (output.size > 7) {
            (output[4] * 100).toInt().coerceIn(0, 100)
        } else 100

        val manaPercent = if (output.size > 11) {
            (output[8] * 100).toInt().coerceIn(0, 100)
        } else 100

        val posX = if (output.size > 15) {
            (output[12] * screenWidth).toInt()
        } else screenWidth / 2

        val posY = if (output.size > 19) {
            (output[16] * screenHeight).toInt()
        } else screenHeight / 2

        return PlayerState(
            healthPercent = healthPercent,
            manaPercent = manaPercent,
            position = Point(posX, posY),
            isMoving = false
        )
    }

    /**
     * 解析敌人信息
     */
    private fun parseEnemies(output: FloatArray, screenWidth: Int, screenHeight: Int): List<EnemyInfo> {
        // 简化的解析逻辑，假设有3个敌人槽位
        val enemies = mutableListOf<EnemyInfo>()

        // 每个敌人需要6个值: x, y, distance, type, isAggressive, confidence
        val enemyOffset = 24
        val enemyStride = 6
        val maxEnemies = 5

        for (i in 0 until maxEnemies) {
            val baseIndex = enemyOffset + i * enemyStride
            if (output.size < baseIndex + enemyStride) break

            val confidence = output[baseIndex + 5]
            if (confidence < 0.5f) continue  // 跳过低置信度的检测

            val x = (output[baseIndex] * screenWidth).toInt()
            val y = (output[baseIndex + 1] * screenHeight).toInt()
            val distance = output[baseIndex + 2]

            enemies.add(EnemyInfo(
                position = Point(x, y),
                distance = distance,
                type = null,
                isAggressive = output[baseIndex + 4] > 0.5f
            ))
        }

        return enemies
    }

    /**
     * 解析技能状态
     */
    private fun parseSkills(output: FloatArray, screenWidth: Int, screenHeight: Int): List<SkillState> {
        // 简化的解析逻辑，假设有5个技能
        val skills = mutableListOf<SkillState>()

        // 每个技能需要4个值: x, y, isReady, cooldownPercent
        val skillOffset = 60
        val skillStride = 4
        val maxSkills = 5

        // 常见技能位置（右下角）
        val defaultPositions = listOf(
            Point((screenWidth * 0.85).toInt(), (screenHeight * 0.75).toInt()),  // 普攻
            Point((screenWidth * 0.75).toInt(), (screenHeight * 0.80).toInt()),  // 技能1
            Point((screenWidth * 0.80).toInt(), (screenHeight * 0.70).toInt()),  // 技能2
            Point((screenWidth * 0.70).toInt(), (screenHeight * 0.75).toInt()),  // 技能3
            Point((screenWidth * 0.75).toInt(), (screenHeight * 0.65).toInt())   // 大招
        )

        for (i in 0 until maxSkills) {
            val baseIndex = skillOffset + i * skillStride
            val isReady = if (output.size > baseIndex + 2) output[baseIndex + 2] > 0.5f else true
            val cooldownRemaining = if (output.size > baseIndex + 3) {
                (output[baseIndex + 3] * 10000).toInt()  // 假设冷却时间最大10秒
            } else 0

            skills.add(SkillState(
                index = i,
                isReady = isReady,
                cooldownRemaining = cooldownRemaining,
                position = defaultPositions.getOrNull(i)
            ))
        }

        return skills
    }

    /**
     * 结合知识库增强解析
     */
    fun parseWithKnowledge(output: FloatArray, knowledge: List<Knowledge>, screenWidth: Int, screenHeight: Int): GameState {
        val baseState = parseFromModelOutput(output, screenWidth, screenHeight)

        // 使用知识库增强技能识别
        val enhancedSkills = baseState.skills.mapIndexed { index, skill ->
            // 在知识库中查找匹配的技能
            val matchedKnowledge = knowledge.filterIsInstance<SkillKnowledge>()
                .find { it.position?.let { pos -> isNearPosition(pos, skill.position, 50) } == true }

            if (matchedKnowledge != null) {
                skill.copy(name = matchedKnowledge.name)
            } else {
                skill
            }
        }

        return baseState.copy(skills = enhancedSkills)
    }

    /**
     * 检查两个位置是否接近
     */
    private fun isNearPosition(p1: Point?, p2: Point?, threshold: Int): Boolean {
        if (p1 == null || p2 == null) return false
        val distance = sqrt((p1.x - p2.x).toDouble().pow(2) + (p1.y - p2.y).toDouble().pow(2))
        return distance < threshold
    }

    /**
     * 使用模板匹配识别UI元素
     * 这是备选方案，当AI模型不可用时使用
     */
    fun detectUIElements(imageData: IntArray, width: Int, height: Int): GameState {
        // 简单的颜色检测来识别血条等UI元素

        val healthBarRegion = Rect(
            (width * 0.1).toInt(),
            (height * 0.02).toInt(),
            (width * 0.3).toInt(),
            (height * 0.05).toInt()
        )

        val healthPercent = detectBarPercentage(imageData, width, healthBarRegion, intArrayOf(200, 50, 50))

        val manaBarRegion = Rect(
            (width * 0.1).toInt(),
            (height * 0.05).toInt(),
            (width * 0.3).toInt(),
            (height * 0.07).toInt()
        )

        val manaPercent = detectBarPercentage(imageData, width, manaBarRegion, intArrayOf(50, 50, 200))

        return GameState(
            screenType = ScreenType.BATTLE,
            playerState = PlayerState(
                healthPercent = healthPercent,
                manaPercent = manaPercent,
                position = Point(width / 2, height / 2)
            ),
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * 检测血条/蓝条的百分比
     */
    private fun detectBarPercentage(imageData: IntArray, width: Int, region: Rect, targetColor: IntArray): Int {
        var coloredPixels = 0
        var totalPixels = 0

        for (y in region.top until region.bottom) {
            for (x in region.left until region.right) {
                val index = y * width + x
                if (index < imageData.size) {
                    val pixel = imageData[index]
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF

                    // 检查颜色是否接近目标颜色
                    if (abs(r - targetColor[0]) < 50 &&
                        abs(g - targetColor[1]) < 50 &&
                        abs(b - targetColor[2]) < 50) {
                        coloredPixels++
                    }
                    totalPixels++
                }
            }
        }

        return if (totalPixels > 0) (coloredPixels * 100 / totalPixels) else 100
    }
}
