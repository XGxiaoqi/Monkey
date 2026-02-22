package com.aigame.controller.utils

import android.content.Context
import android.content.SharedPreferences
import com.aigame.controller.model.AppConfig
import com.aigame.controller.model.StrategyType
import com.google.gson.Gson

/**
 * 配置管理器
 */
class ConfigManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val gson = Gson()

    /**
     * 获取应用配置
     */
    fun getConfig(): AppConfig {
        val configJson = prefs.getString(KEY_CONFIG, null)
        return if (configJson != null) {
            try {
                gson.fromJson(configJson, AppConfig::class.java)
            } catch (e: Exception) {
                AppConfig()
            }
        } else {
            AppConfig()
        }
    }

    /**
     * 保存应用配置
     */
    fun saveConfig(config: AppConfig) {
        val configJson = gson.toJson(config)
        prefs.edit().putString(KEY_CONFIG, configJson).apply()
    }

    /**
     * 重置为默认配置
     */
    fun resetToDefault() {
        saveConfig(AppConfig())
    }

    /**
     * 获取目标游戏包名
     */
    fun getTargetPackageName(): String {
        return prefs.getString(KEY_TARGET_PACKAGE, DEFAULT_TARGET_PACKAGE) ?: DEFAULT_TARGET_PACKAGE
    }

    /**
     * 设置目标游戏包名
     */
    fun setTargetPackageName(packageName: String) {
        prefs.edit().putString(KEY_TARGET_PACKAGE, packageName).apply()
    }

    /**
     * 是否首次启动
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * 设置已启动
     */
    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    /**
     * 是否已显示权限引导
     */
    fun hasShownPermissionGuide(): Boolean {
        return prefs.getBoolean(KEY_PERMISSION_GUIDE_SHOWN, false)
    }

    /**
     * 设置已显示权限引导
     */
    fun setPermissionGuideShown() {
        prefs.edit().putBoolean(KEY_PERMISSION_GUIDE_SHOWN, true).apply()
    }

    /**
     * 获取悬浮窗位置X
     */
    fun getFloatingWindowX(): Int {
        return prefs.getInt(KEY_FLOATING_X, 100)
    }

    /**
     * 获取悬浮窗位置Y
     */
    fun getFloatingWindowY(): Int {
        return prefs.getInt(KEY_FLOATING_Y, 100)
    }

    /**
     * 保存悬浮窗位置
     */
    fun saveFloatingWindowPosition(x: Int, y: Int) {
        prefs.edit()
            .putInt(KEY_FLOATING_X, x)
            .putInt(KEY_FLOATING_Y, y)
            .apply()
    }

    /**
     * 更新单个配置项
     */
    fun updateFrameRate(fps: Int) {
        val config = getConfig()
        saveConfig(config.copy(frameRate = fps.coerceIn(10, 30)))
    }

    fun updateResolutionScale(scale: Float) {
        val config = getConfig()
        saveConfig(config.copy(resolutionScale = scale.coerceIn(0.25f, 1f)))
    }

    fun updateActionDelay(delay: Int) {
        val config = getConfig()
        saveConfig(config.copy(actionDelay = delay.coerceIn(30, 200)))
    }

    fun updateStrategy(strategy: StrategyType) {
        val config = getConfig()
        saveConfig(config.copy(strategy = strategy))
    }

    fun updateAutoPickup(enabled: Boolean) {
        val config = getConfig()
        saveConfig(config.copy(autoPickup = enabled))
    }

    fun updateAutoPotionThreshold(threshold: Int) {
        val config = getConfig()
        saveConfig(config.copy(autoPotionThreshold = threshold.coerceIn(0, 100)))
    }

    companion object {
        private const val PREFS_NAME = "ai_game_controller_prefs"
        private const val KEY_CONFIG = "app_config"
        private const val KEY_TARGET_PACKAGE = "target_package"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_PERMISSION_GUIDE_SHOWN = "permission_guide_shown"
        private const val KEY_FLOATING_X = "floating_x"
        private const val KEY_FLOATING_Y = "floating_y"

        // 火炬之光无限包名
        const val DEFAULT_TARGET_PACKAGE = "com.xd.ro.createworld"
    }
}
