package com.aigame.controller.data

import android.content.Context
import android.graphics.Point
import com.aigame.controller.data.entity.ItemEntity
import com.aigame.controller.data.entity.SkillEntity
import com.aigame.controller.model.SkillEffectType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 火炬之光无限预设知识库
 * 内置常见技能和装备数据
 */
object TorchlightInfiniteData {

    // 预设技能数据
    val presetSkills = listOf(
        // 猎人技能
        SkillEntity(
            id = "skill_hunter_1",
            name = "迅影斩",
            iconFeature = FloatArray(128),
            description = "快速斩击敌人，造成物理伤害",
            cooldown = 3000,
            effectType = SkillEffectType.DAMAGE.name,
            effectValue = 150,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        SkillEntity(
            id = "skill_hunter_2",
            name = "幻影步",
            iconFeature = FloatArray(128),
            description = "快速闪避，短暂无敌",
            cooldown = 5000,
            effectType = SkillEffectType.MOBILITY.name,
            effectValue = 0,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        SkillEntity(
            id = "skill_hunter_3",
            name = "影袭",
            iconFeature = FloatArray(128),
            description = "瞬移到敌人背后并造成暴击伤害",
            cooldown = 8000,
            effectType = SkillEffectType.DAMAGE.name,
            effectValue = 300,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        SkillEntity(
            id = "skill_hunter_4",
            name = "刀扇",
            iconFeature = FloatArray(128),
            description = "向四周发射刀刃，造成范围伤害",
            cooldown = 6000,
            effectType = SkillEffectType.DAMAGE.name,
            effectValue = 200,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),

        // 狂战士技能
        SkillEntity(
            id = "skill_berserker_1",
            name = "狂暴打击",
            iconFeature = FloatArray(128),
            description = "强力打击，造成大量物理伤害",
            cooldown = 4000,
            effectType = SkillEffectType.DAMAGE.name,
            effectValue = 250,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        SkillEntity(
            id = "skill_berserker_2",
            name = "战吼",
            iconFeature = FloatArray(128),
            description = "发出战吼，提升攻击力和防御力",
            cooldown = 20000,
            effectType = SkillEffectType.BUFF.name,
            effectValue = 50,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        SkillEntity(
            id = "skill_berserker_3",
            name = "旋风斩",
            iconFeature = FloatArray(128),
            description = "旋转攻击周围敌人",
            cooldown = 8000,
            effectType = SkillEffectType.DAMAGE.name,
            effectValue = 180,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),

        // 法师技能
        SkillEntity(
            id = "skill_mage_1",
            name = "火球术",
            iconFeature = FloatArray(128),
            description = "发射火球，造成火焰伤害",
            cooldown = 2000,
            effectType = SkillEffectType.DAMAGE.name,
            effectValue = 120,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        SkillEntity(
            id = "skill_mage_2",
            name = "冰霜新星",
            iconFeature = FloatArray(128),
            description = "释放冰霜冲击，冻结周围敌人",
            cooldown = 10000,
            effectType = SkillEffectType.CONTROL.name,
            effectValue = 0,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        SkillEntity(
            id = "skill_mage_3",
            name = "传送",
            iconFeature = FloatArray(128),
            description = "瞬间传送到指定位置",
            cooldown = 8000,
            effectType = SkillEffectType.MOBILITY.name,
            effectValue = 0,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        SkillEntity(
            id = "skill_mage_4",
            name = "陨石术",
            iconFeature = FloatArray(128),
            description = "召唤陨石砸向敌人，造成大量火焰伤害",
            cooldown = 15000,
            effectType = SkillEffectType.DAMAGE.name,
            effectValue = 500,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),

        // 圣枪技能
        SkillEntity(
            id = "skill_gunslinger_1",
            name = "穿透射击",
            iconFeature = FloatArray(128),
            description = "发射穿透敌人的子弹",
            cooldown = 3000,
            effectType = SkillEffectType.DAMAGE.name,
            effectValue = 140,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        SkillEntity(
            id = "skill_gunslinger_2",
            name = "弹幕",
            iconFeature = FloatArray(128),
            description = "快速连续射击",
            cooldown = 6000,
            effectType = SkillEffectType.DAMAGE.name,
            effectValue = 100,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        SkillEntity(
            id = "skill_gunslinger_3",
            name = "烟雾弹",
            iconFeature = FloatArray(128),
            description = "释放烟雾，降低敌人命中",
            cooldown = 12000,
            effectType = SkillEffectType.DEBUFF.name,
            effectValue = 30,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),

        // 通用技能
        SkillEntity(
            id = "skill_common_heal",
            name = "生命药剂",
            iconFeature = FloatArray(128),
            description = "恢复生命值",
            cooldown = 10000,
            effectType = SkillEffectType.HEAL.name,
            effectValue = 50,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        SkillEntity(
            id = "skill_common_dash",
            name = "闪避冲刺",
            iconFeature = FloatArray(128),
            description = "快速闪避移动",
            cooldown = 3000,
            effectType = SkillEffectType.MOBILITY.name,
            effectValue = 0,
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        )
    )

    // 预设物品数据
    val presetItems = listOf(
        ItemEntity(
            id = "item_potion_health",
            name = "生命药剂",
            iconFeature = FloatArray(128),
            description = "恢复30%生命值",
            attributeJson = "{\"health\":30}",
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        ItemEntity(
            id = "item_potion_mana",
            name = "魔力药剂",
            iconFeature = FloatArray(128),
            description = "恢复30%魔力值",
            attributeJson = "{\"mana\":30}",
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        ItemEntity(
            id = "item_scroll_identify",
            name = "鉴定卷轴",
            iconFeature = FloatArray(128),
            description = "鉴定未鉴定的物品",
            attributeJson = "{}",
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        ),
        ItemEntity(
            id = "item_scroll_portal",
            name = "传送卷轴",
            iconFeature = FloatArray(128),
            description = "开启传送门",
            attributeJson = "{}",
            positionX = null,
            positionY = null,
            learnTime = System.currentTimeMillis()
        )
    )

    /**
     * 初始化预设数据到数据库
     */
    suspend fun initializePresetData(context: Context) {
        val database = com.aigame.controller.GameControllerApp.instance.knowledgeDatabase

        withContext(Dispatchers.IO) {
            // 检查是否已有数据
            val existingCount = database.skillDao().getCount()
            if (existingCount == 0) {
                // 插入预设技能
                database.skillDao().insertAll(presetSkills)
            }

            val existingItemCount = database.itemDao().getCount()
            if (existingItemCount == 0) {
                // 插入预设物品
                database.itemDao().insertAll(presetItems)
            }
        }
    }
}
