package com.aigame.controller

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.aigame.controller.data.KnowledgeDatabase
import com.aigame.controller.utils.ConfigManager
import com.aigame.controller.utils.LogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class GameControllerApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    lateinit var configManager: ConfigManager
        private set
    lateinit var logManager: LogManager
        private set
    lateinit var knowledgeDatabase: KnowledgeDatabase
        private set

    companion object {
        lateinit var instance: GameControllerApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化组件
        configManager = ConfigManager(this)
        logManager = LogManager(this)
        knowledgeDatabase = KnowledgeDatabase.getDatabase(this)

        // 创建通知渠道
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

const val CHANNEL_ID = "ai_game_controller_channel"
