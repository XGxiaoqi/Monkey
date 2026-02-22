package com.kevinluo.autoglm.localmodel

import android.content.Context
import com.kevinluo.autoglm.util.Logger
import org.json.JSONObject
import java.io.File

class MemoryManager(private val context: Context) {
    companion object {
        private const val TAG = "MemoryManager"
        @Volatile private var instance: MemoryManager? = null
        fun getInstance(context: Context): MemoryManager = instance ?: synchronized(this) {
            instance ?: MemoryManager(context.applicationContext).also { instance = it }
        }
    }

    private val memoryFile = File(context.filesDir, "memory.json")
    private val memory = mutableMapOf<String, ElementInfo>()

    data class ElementInfo(val id: String, val text: String?, val cx: Int, val cy: Int, val count: Int)

    fun load(): Boolean {
        return try {
            if (memoryFile.exists()) {
                val json = JSONObject(memoryFile.readText())
                json.optJSONArray("elements")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        memory[obj.getString("id")] = ElementInfo(
                            obj.getString("id"), obj.optString("text"), obj.getInt("cx"), obj.getInt("cy"), obj.getInt("count")
                        )
                    }
                }
            }
            Logger.i(TAG, "Memory loaded: ${memory.size} elements")
            true
        } catch (e: Exception) { Logger.e(TAG, "Memory load failed: ${e.message}"); false }
    }

    fun save(): Boolean {
        return try {
            val json = JSONObject().put("elements", org.json.JSONArray().apply {
                memory.values.forEach { info ->
                    put(JSONObject().apply {
                        put("id", info.id); put("text", info.text ?: ""); put("cx", info.cx); put("cy", info.cy); put("count", info.count)
                    })
                }
            })
            memoryFile.writeText(json.toString())
            true
        } catch (e: Exception) { Logger.e(TAG, "Memory save failed: ${e.message}"); false }
    }

    fun remember(id: String, text: String?, cx: Int, cy: Int) {
        val existing = memory[id]
        memory[id] = if (existing != null) existing.copy(count = existing.count + 1) else ElementInfo(id, text, cx, cy, 1)
    }

    fun find(id: String): ElementInfo? = memory[id]
    fun findByText(text: String): ElementInfo? = memory.values.find { it.text?.contains(text, ignoreCase = true) == true }
}
