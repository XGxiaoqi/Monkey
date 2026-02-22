package com.aigame.controller.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aigame.controller.model.SkillEffectType

/**
 * 技能实体
 */
@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconFeature: ByteArray,        // 图标特征向量
    val description: String,
    val cooldown: Int?,                // 冷却时间(ms)
    val effectType: String,            // 效果类型
    val effectValue: Int?,             // 效果数值
    val positionX: Int?,               // 在屏幕上的位置X
    val positionY: Int?,               // 在屏幕上的位置Y
    val learnTime: Long                // 学习时间戳
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SkillEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * 物品实体
 */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconFeature: ByteArray,        // 图标特征向量
    val description: String?,
    val attributeJson: String,         // 属性JSON字符串
    val positionX: Int?,               // 在屏幕上的位置X
    val positionY: Int?,               // 在屏幕上的位置Y
    val learnTime: Long                // 学习时间戳
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ItemEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * 游戏配置实体
 */
@Entity(tableName = "game_configs")
data class GameConfigEntity(
    @PrimaryKey
    val id: String,                    // 配置ID，通常是游戏包名
    val gameName: String,              // 游戏名称
    val configJson: String,            // 配置JSON字符串
    val updateTime: Long               // 更新时间戳
)

/**
 * 学习会话实体
 */
@Entity(tableName = "learn_sessions")
data class LearnSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val skillCount: Int,
    val itemCount: Int,
    val status: String                 // running, completed, cancelled
)
