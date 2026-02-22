package com.kevinluo.autoglm.localmodel

import android.content.Context
import com.kevinluo.autoglm.accessibility.AccessibilityServiceManager
import com.kevinluo.autoglm.accessibility.AutoGLMAccessibilityService
import com.kevinluo.autoglm.util.Logger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class LocalAIAgent(private val context: Context) {
    companion object {
        private const val TAG = "LocalAIAgent"
        @Volatile private var instance: LocalAIAgent? = null
        fun getInstance(context: Context): LocalAIAgent = instance ?: synchronized(this) {
            instance ?: LocalAIAgent(context.applicationContext).also { instance = it }
        }
    }

    private val engine = LocalAIEngine.getInstance(context)
    private val memory = MemoryManager.getInstance(context)

    suspend fun initialize(): Boolean { memory.load(); return engine.initialize() }

    fun executeTaskStream(task: String): Flow<State> = flow {
        if (!AccessibilityServiceManager.isServiceReady()) { emit(State.Error("Accessibility not enabled")); return@flow }
        if (!engine.isReady()) { emit(State.Loading("Initializing...")); engine.initialize() }
        emit(State.Started(task))
        engine.inferStream(task).collect { action ->
            when (action) {
                is StreamAction.Thinking -> emit(State.Thinking(action.message))
                is StreamAction.ActionReady -> {
                    emit(State.Action(action.action, action.index, action.total))
                    val result = execute(action.action)
                    emit(State.Executed(action.action, result.success, action.index, action.total))
                }
                is StreamAction.Complete -> { memory.save(); emit(State.Done(action.timeMs)) }
                is StreamAction.Error -> emit(State.Error(action.message))
            }
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun execute(action: LocalAIEngine.AgentAction): Result = withContext(Dispatchers.Main) {
        val service = AutoGLMAccessibilityService.getInstance() ?: return@withContext Result(false, "No service")
        try {
            when (action) {
                is LocalAIEngine.AgentAction.Back -> Result(service.performBack(), "Back")
                is LocalAIEngine.AgentAction.Home -> Result(service.performHome(), "Home")
                is LocalAIEngine.AgentAction.GameJoystick -> {
                    // 增强的游戏摇杆控制 - 支持连续移动和精确方向
                    // 适配2712x1220分辨率，左摇杆区域
                    val jx = 200  // 摇杆中心X
                    val jy = 1700 // 摇杆中心Y
                    val r = service.executeGameMovement(action.direction, 200, 1700, action.intensity, (action.duration * 1000).toLong())
                    Result(r.success, r.message)
                }
                is LocalAIEngine.AgentAction.SkillCast -> {
                    // 技能释放 - 使用记忆系统定位技能按钮
                    val skillPos = memory.find(action.skillId)
                    if (skillPos != null) {
                        val tapResult = service.tap(skillPos.cx, skillPos.cy, 100)
                        Result(tapResult.success, "Skill ${action.skillId}")
                    } else {
                        // 使用默认技能位置 (游戏右下角区域)
                        val defaultPos = when (action.skillId) {
                            "skill1" -> Pair(850, 900)
                            "skill2" -> Pair(920, 850)
                            "ultimate" -> Pair(980, 800)
                            "attack" -> Pair(900, 950)
                            else -> Pair(900, 900)
                        }
                        val tapResult = service.tap(defaultPos.first, defaultPos.second, 100)
                        Result(tapResult.success, "Skill ${action.skillId} default")
                    }
                }
                is LocalAIEngine.AgentAction.Tap -> {
                    val tapResult = service.tap(action.x.toInt(), action.y.toInt(), 100)
                    Result(tapResult.success, "Tap (${action.x}, ${action.y})")
                }
                is LocalAIEngine.AgentAction.Swipe -> {
                    val swipeResult = service.swipe(action.sx.toInt(), action.sy.toInt(), action.ex.toInt(), action.ey.toInt(), 300)
                    Result(swipeResult.success, "Swipe")
                }
                is LocalAIEngine.AgentAction.Wait -> {
                    delay(action.delayMs)
                    Result(true, "Waited ${action.delayMs}ms")
                }
                is LocalAIEngine.AgentAction.Finish -> Result(true, action.message)
                else -> Result(false, "Not implemented")
            }
        } catch (e: Exception) { Result(false, e.message ?: "Error") }
    }

    fun isReady(): Boolean = AccessibilityServiceManager.isServiceReady() && engine.isReady()

    // 保存技能位置到记忆系统
    fun rememberSkillPosition(skillId: String, x: Int, y: Int) {
        memory.remember(skillId, null, x, y)
        memory.save()
        engine.saveSkillPosition(skillId, x, y)
    }

    // 保存连招配置
    fun saveCombo(name: String, actions: List<ComboAction>) {
        engine.saveCombo(name, actions)
    }

    sealed class State {
        data class Loading(val msg: String) : State()
        data class Started(val task: String) : State()
        data class Thinking(val msg: String) : State()
        data class Action(val action: LocalAIEngine.AgentAction, val idx: Int, val total: Int) : State()
        data class Executed(val action: LocalAIEngine.AgentAction, val success: Boolean, val idx: Int, val total: Int) : State()
        data class Done(val timeMs: Long) : State()
        data class Error(val msg: String) : State()
    }
    data class Result(val success: Boolean, val msg: String)
}
