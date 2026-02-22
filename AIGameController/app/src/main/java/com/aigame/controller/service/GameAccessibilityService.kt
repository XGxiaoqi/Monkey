package com.aigame.controller.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aigame.controller.model.GameAction
import com.aigame.controller.utils.LogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 游戏无障碍服务
 * 负责获取屏幕内容和执行触控操作
 */
class GameAccessibilityService : AccessibilityService() {

    private val logManager by lazy { LogManager(applicationContext) }

    private var screenWidth = 0
    private var screenHeight = 0

    private val _isServiceEnabled = MutableStateFlow(false)
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceEnabled.value = true

        // 获取屏幕尺寸
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
        }

        logManager.info(TAG, "Accessibility service connected. Screen: ${screenWidth}x${screenHeight}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 可以在这里处理窗口变化事件
    }

    override fun onInterrupt() {
        logManager.warn(TAG, "Accessibility service interrupted")
        _isServiceEnabled.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceEnabled.value = false
        logManager.info(TAG, "Accessibility service destroyed")
    }

    /**
     * 获取屏幕尺寸
     */
    fun getScreenSize(): Point = Point(screenWidth, screenHeight)

    /**
     * 获取当前窗口根节点
     */
    fun getRootNode(): AccessibilityNodeInfo? = rootInActiveWindow

    /**
     * 截取当前屏幕（通过节点信息）
     */
    fun captureScreenViaNode(): Bitmap? {
        val node = rootInActiveWindow ?: return null
        // 注意：这种方式获取的不是真正的截图，而是节点结构
        // 真正的截图需要使用MediaProjection API
        return null
    }

    /**
     * 检测目标应用是否在前台
     */
    fun isTargetAppForeground(packageName: String): Boolean {
        val node = rootInActiveWindow ?: return false
        return node.packageName?.toString()?.contains(packageName) == true
    }

    /**
     * 执行点击操作
     * @param x X坐标
     * @param y Y坐标
     * @param duration 持续时间(毫秒)
     * @return 是否成功发起手势
     */
    fun performClick(x: Int, y: Int, duration: Long = 100): Boolean {
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    /**
     * 执行长按操作
     */
    fun performLongClick(x: Int, y: Int, duration: Long = 500): Boolean {
        return performClick(x, y, duration)
    }

    /**
     * 执行滑动操作
     */
    fun performSwipe(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        duration: Long = 300
    ): Boolean {
        val path = Path().apply {
            moveTo(startX.toFloat(), startY.toFloat())
            lineTo(endX.toFloat(), endY.toFloat())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    /**
     * 执行多点触控
     */
    fun performMultiTouch(touches: List<TouchPoint>): Boolean {
        val builder = GestureDescription.Builder()

        touches.forEach { touch ->
            val path = Path().apply {
                moveTo(touch.x.toFloat(), touch.y.toFloat())
            }
            builder.addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    touch.startTime,
                    touch.duration
                )
            )
        }

        return dispatchGesture(builder.build(), null, null)
    }

    /**
     * 执行游戏动作
     */
    fun executeAction(action: GameAction): Boolean {
        return when (action) {
            is GameAction.Click -> {
                logManager.debug(TAG, "Click at (${action.x}, ${action.y})")
                performClick(action.x, action.y, action.duration)
            }
            is GameAction.Swipe -> {
                logManager.debug(TAG, "Swipe from (${action.startX}, ${action.startY}) to (${action.endX}, ${action.endY})")
                performSwipe(
                    action.startX,
                    action.startY,
                    action.endX,
                    action.endY,
                    action.duration
                )
            }
            is GameAction.Move -> {
                executeMoveAction(action)
            }
            is GameAction.UseSkill -> {
                executeSkillAction(action)
            }
            is GameAction.UseItem -> {
                executeItemAction(action)
            }
            is GameAction.Wait -> {
                Thread.sleep(action.durationMs)
                true
            }
            is GameAction.CompositeAction -> {
                action.actions.forEach { executeAction(it) }
                true
            }
        }
    }

    /**
     * 执行移动动作
     * 方向轮盘通常在屏幕左下角
     */
    private fun executeMoveAction(action: GameAction.Move): Boolean {
        // 假设虚拟摇杆中心在屏幕左下角区域
        val joystickCenterX = (screenWidth * 0.15).toInt()
        val joystickCenterY = (screenHeight * 0.75).toInt()
        val joystickRadius = (screenHeight * 0.1).toInt()

        // 计算终点坐标
        val radians = Math.toRadians(action.direction.toDouble())
        val endX = (joystickCenterX + joystickRadius * action.distance * Math.cos(radians)).toInt()
        val endY = (joystickCenterY - joystickRadius * action.distance * Math.sin(radians)).toInt()

        return performSwipe(joystickCenterX, joystickCenterY, endX, endY, 50)
    }

    /**
     * 执行技能动作
     * 技能按钮通常在屏幕右下角
     */
    private fun executeSkillAction(action: GameAction.UseSkill): Boolean {
        // 技能按钮位置（从右下角排列）
        val skillPositions = listOf(
            Point((screenWidth * 0.85).toInt(), (screenHeight * 0.75).toInt()),  // 普通攻击
            Point((screenWidth * 0.75).toInt(), (screenHeight * 0.80).toInt()),  // 技能1
            Point((screenWidth * 0.80).toInt(), (screenHeight * 0.70).toInt()),  // 技能2
            Point((screenWidth * 0.70).toInt(), (screenHeight * 0.75).toInt()),  // 技能3
            Point((screenWidth * 0.75).toInt(), (screenHeight * 0.65).toInt())   // 大招
        )

        val skillPos = skillPositions.getOrNull(action.skillIndex) ?: return false

        // 如果有目标位置，先滑动到目标位置再释放
        if (action.targetX != null && action.targetY != null) {
            return performSwipe(skillPos.x, skillPos.y, action.targetX, action.targetY, 150)
        }

        return performClick(skillPos.x, skillPos.y, 100)
    }

    /**
     * 执行物品使用动作
     */
    private fun executeItemAction(action: GameAction.UseItem): Boolean {
        // 物品栏通常在屏幕右侧
        val itemY = (screenHeight * 0.3).toInt()
        val itemX = (screenWidth * 0.9 - action.itemIndex * screenHeight * 0.05).toInt()

        return performClick(itemX, itemY, 100)
    }

    /**
     * 执行手势（带回调）
     */
    fun performGestureWithCallback(
        gesture: GestureDescription,
        callback: GestureResultCallback?
    ): Boolean {
        return dispatchGesture(gesture, callback, null)
    }

    /**
     * 查找包含特定文本的节点
     */
    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull()
    }

    /**
     * 查找包含特定描述的节点
     */
    fun findNodeByDescription(description: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByText(description)
        return nodes.firstOrNull()
    }

    /**
     * 点击特定节点
     */
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    companion object {
        private const val TAG = "GameAccessibilityService"

        // 服务实例（用于外部访问）
        var instance: GameAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance?._isServiceEnabled?.value ?: false
    }

    data class TouchPoint(
        val x: Int,
        val y: Int,
        val startTime: Long = 0,
        val duration: Long = 100
    )
}
