package com.kevinluo.autoglm.localmodel

import android.content.Context
import com.kevinluo.autoglm.util.Logger
import com.kevinluo.autoglm.vision.LocalVisionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class LocalAIEngine(private val context: Context) {
    companion object {
        private const val TAG = "LocalAIEngine"
        private const val MAX_LATENCY_MS = 200L
        private const val MEMORY_FILE = "game_memory.json"
        @Volatile private var instance: LocalAIEngine? = null
        fun getInstance(context: Context): LocalAIEngine = instance ?: synchronized(this) {
            instance ?: LocalAIEngine(context.applicationContext).also { instance = it }
        }
    }

    private val isInitialized = AtomicBoolean(false)
    private var modelLoadTime = 0L
    private val memory = GameMemory()
    private val visionEngine by lazy { LocalVisionEngine.getInstance(context) }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized.get()) return@withContext true
        try {
            Logger.i(TAG, "Loading local AI engine...")
            loadMemory()
            modelLoadTime = System.currentTimeMillis()
            isInitialized.set(true)
            Logger.i(TAG, "Local AI engine loaded successfully")
            true
        } catch (e: Exception) {
            Logger.e(TAG, "Engine load failed: ${e.message}")
            false
        }
    }

    fun inferStream(task: String): Flow<StreamAction> = flow {
        if (!isInitialized.get()) { emit(StreamAction.Error("Not initialized")); return@flow }
        val intent = parseTask(task)
        emit(StreamAction.Thinking("Analyzing: ${intent.action}"))
        val actions = generateActions(intent)
        actions.forEachIndexed { idx, action -> emit(StreamAction.ActionReady(action, idx, actions.size)) }
        emit(StreamAction.Complete(System.currentTimeMillis()))
    }.flowOn(Dispatchers.Default)

    private fun parseTask(task: String): TaskIntent {
        val t = task.lowercase()
        return when {
            t.contains("forward") || t.contains("forward") -> TaskIntent("game_control", direction = "forward", intensity = 1f)
            t.contains("backward") || t.contains("backward") -> TaskIntent("game_control", direction = "backward", intensity = 1f)
            t.contains("left") || t.contains("left") -> TaskIntent("game_control", direction = "left", intensity = 1f)
            t.contains("right") || t.contains("right") -> TaskIntent("game_control", direction = "right", intensity = 1f)
            t.contains("stop") -> TaskIntent("game_control", direction = "stop", intensity = 0f)
            t.contains("skill1") -> TaskIntent("skill_cast", skillId = "skill1")
            t.contains("skill2") -> TaskIntent("skill_cast", skillId = "skill2")
            t.contains("ultimate") -> TaskIntent("skill_cast", skillId = "ultimate")
            t.contains("attack") -> TaskIntent("skill_cast", skillId = "attack")
            t.contains("back") -> TaskIntent("navigation", navAction = "back")
            t.contains("home") -> TaskIntent("navigation", navAction = "home")
            else -> TaskIntent("unknown", originalTask = task)
        }
    }

    private fun generateActions(intent: TaskIntent): List<AgentAction> = when (intent.action) {
        "game_control" -> listOf(AgentAction.GameJoystick(intent.direction ?: "forward", intent.intensity ?: 0.7f, 1f))
        "skill_cast" -> listOf(AgentAction.SkillCast(intent.skillId ?: "attack"))
        "navigation" -> when (intent.navAction) {
            "back" -> listOf(AgentAction.Back())
            "home" -> listOf(AgentAction.Home())
            else -> listOf(AgentAction.Finish("Unknown navigation"))
        }
        else -> listOf(AgentAction.Finish("Unknown task"))
    }

    private fun loadMemory() {
        try {
            val file = File(context.filesDir, MEMORY_FILE)
            if (file.exists()) {
                val json = JSONObject(file.readText())
                memory.fromJson(json)
            }
        } catch (e: Exception) { Logger.e(TAG, "Failed to load memory: ${e.message}") }
    }

    private fun saveMemory() {
        try {
            val file = File(context.filesDir, MEMORY_FILE)
            file.writeText(memory.toJson().toString())
        } catch (e: Exception) { Logger.e(TAG, "Failed to save memory: ${e.message}") }
    }

    fun saveSkillPosition(skillId: String, x: Int, y: Int) { memory.setSkillPosition(skillId, x, y); saveMemory() }
    fun saveCombo(name: String, actions: List<ComboAction>) { memory.setCombo(name, actions); saveMemory() }
    fun isReady(): Boolean = isInitialized.get()
    fun getModelInfo() = ModelInfo("Local Game Agent", "2.0", modelLoadTime, isInitialized.get(), MAX_LATENCY_MS)

    data class TaskIntent(val action: String, val direction: String? = null, val intensity: Float? = null, val skillId: String? = null, val navAction: String? = null, val originalTask: String? = null)
    data class ModelInfo(val name: String, val version: String, val loadTimeMs: Long, val isReady: Boolean, val maxLatencyMs: Long)

    sealed class AgentAction {
        data class Tap(val x: Float, val y: Float) : AgentAction()
        data class Swipe(val sx: Float, val sy: Float, val ex: Float, val ey: Float) : AgentAction()
        data class Back(val dummy: Unit? = null) : AgentAction()
        data class Home(val dummy: Unit? = null) : AgentAction()
        data class GameJoystick(val direction: String, val intensity: Float, val duration: Float) : AgentAction()
        data class SkillCast(val skillId: String) : AgentAction()
        data class Wait(val delayMs: Long) : AgentAction()
        data class Finish(val message: String) : AgentAction()
    }
}

class GameMemory {
    private val skillPositions = mutableMapOf<String, Pair<Int, Int>>()
    private val combos = mutableMapOf<String, Combo>()
    val skillCount: Int get() = skillPositions.size
    val comboCount: Int get() = combos.size

    fun setSkillPosition(skillId: String, x: Int, y: Int) { skillPositions[skillId] = Pair(x, y) }
    fun getSkillPosition(skillId: String): Pair<Int, Int>? = skillPositions[skillId]
    fun setCombo(name: String, actions: List<ComboAction>) { combos[name] = Combo(name, actions) }
    fun getCombo(name: String): Combo? = combos[name]

    fun fromJson(json: JSONObject) {
        json.optJSONObject("skills")?.let { skillsJson ->
            skillsJson.keys().forEach { key ->
                val pos = skillsJson.getJSONObject(key)
                skillPositions[key] = Pair(pos.getInt("x"), pos.getInt("y"))
            }
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("skills", JSONObject().apply { skillPositions.forEach { (id, pos) -> put(id, JSONObject().apply { put("x", pos.first); put("y", pos.second) }) } })
    }
}

data class ComboAction(val type: String, val target: String = "", val delay: Long = 300L)
data class Combo(val name: String, val actions: List<ComboAction>)

sealed class StreamAction {
    data class Thinking(val message: String) : StreamAction()
    data class ActionReady(val action: LocalAIEngine.AgentAction, val index: Int, val total: Int) : StreamAction()
    data class Complete(val timeMs: Long) : StreamAction()
    data class Error(val message: String) : StreamAction()
}
