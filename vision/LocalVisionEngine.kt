package com.kevinluo.autoglm.vision

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.kevinluo.autoglm.accessibility.AutoGLMAccessibilityService
import com.kevinluo.autoglm.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalVisionEngine(private val context: Context) {
    companion object {
        private const val TAG = "LocalVisionEngine"
        @Volatile private var instance: LocalVisionEngine? = null
        fun getInstance(context: Context): LocalVisionEngine = instance ?: synchronized(this) {
            instance ?: LocalVisionEngine(context.applicationContext).also { instance = it }
        }
    }

    suspend fun analyzeScreen(): ScreenAnalysis = withContext(Dispatchers.Default) {
        val service = AutoGLMAccessibilityService.getInstance()
            ?: return@withContext ScreenAnalysis.Error("Accessibility not enabled")
        try {
            val root = service.rootInActiveWindow ?: return@withContext ScreenAnalysis.Error("No root")
            val elements = mutableListOf<UIElement>()
            collectElements(root, elements, 0)
            ScreenAnalysis.Success(elements, "Found ${elements.size} elements")
        } catch (e: Exception) { ScreenAnalysis.Error(e.message ?: "Error") }
    }

    private fun collectElements(node: AccessibilityNodeInfo, list: MutableList<UIElement>, depth: Int) {
        if (depth > 20) return
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        list.add(UIElement(node.className?.toString() ?: "", node.text?.toString(), bounds, node.isClickable, depth))
        for (i in 0 until node.childCount) node.getChild(i)?.let { collectElements(it, list, depth + 1) }
    }

    /**
     * 分析游戏特定的UI元素
     * 识别技能按钮、血条等
     */
    private fun analyzeGameElements(elements: List<UIElement>): GameElements {
        val skillButtons = mutableListOf<SkillButton>()
        for (element in elements) {
            val text = element.text?.lowercase() ?: ""
            if (element.clickable && element.cx > 600 && element.cy > 500) {
                val skillId = when {
                    text.contains("skill") || text.contains("技能") -> "skill_${skillButtons.size}"
                    text.contains("attack") || text.contains("攻击") -> "attack"
                    text.contains("ultimate") || text.contains("大招") -> "ultimate"
                    element.cx > 800 -> "skill_${skillButtons.size}"
                    else -> null
                }
                skillId?.let {
                    skillButtons.add(SkillButton(it, element.cx, element.cy, element.bounds.width(), element.bounds.height()))
                }
            }
        }
        return GameElements(skillButtons, null, null)
    }

    fun findClickableElement(elements: List<UIElement>, text: String): UIElement? {
        return elements.find { it.clickable && it.text?.contains(text, ignoreCase = true) == true }
    }

    data class UIElement(val className: String, val text: String?, val bounds: Rect, val clickable: Boolean, val depth: Int) {
        val cx: Int get() = bounds.centerX()
        val cy: Int get() = bounds.centerY()
    }

    data class SkillButton(val id: String, val x: Int, val y: Int, val width: Int, val height: Int)
    data class HealthBar(val x: Int, val y: Int, val width: Int, val height: Int)
    data class GameElements(val skillButtons: List<SkillButton>, val playerHealth: HealthBar?, val targetHealth: HealthBar?)

    sealed class ScreenAnalysis {
        data class Success(val elements: List<UIElement>, val desc: String, val gameElements: GameElements? = null) : ScreenAnalysis()
        data class Error(val msg: String) : ScreenAnalysis()
    }
}
