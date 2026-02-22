package com.aigame.controller.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aigame.controller.CHANNEL_ID
import com.aigame.controller.GameControllerApp
import com.aigame.controller.R
import com.aigame.controller.ai.AIEngine
import com.aigame.controller.ai.ActionGenerator
import com.aigame.controller.ai.GameStateParser
import com.aigame.controller.data.KnowledgeDatabase
import com.aigame.controller.model.AppConfig
import com.aigame.controller.model.GameAction
import com.aigame.controller.model.GameState
import com.aigame.controller.model.RunStatus
import com.aigame.controller.ui.MainActivity
import com.aigame.controller.utils.ConfigManager
import com.aigame.controller.utils.LogManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 游戏控制服务
 * 协调截图、AI推理和操作执行
 */
class GameControlService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var configManager: ConfigManager
    private lateinit var logManager: LogManager
    private lateinit var knowledgeDatabase: KnowledgeDatabase

    private var aiEngine: AIEngine? = null
    private var gameStateParser: GameStateParser? = null
    private var actionGenerator: ActionGenerator? = null

    private var accessibilityService: GameAccessibilityService? = null

    private val _status = MutableStateFlow(RunStatus.IDLE)
    val status: StateFlow<RunStatus> = _status

    private val _currentGameState = MutableStateFlow<GameState?>(null)
    val currentGameState: StateFlow<GameState?> = _currentGameState

    private val _stats = MutableStateFlow(ControlStats())
    val stats: StateFlow<ControlStats> = _stats

    private var controlJob: Job? = null
    private var isInitialized = false

    override fun onCreate() {
        super.onCreate()
        configManager = (application as GameControllerApp).configManager
        logManager = (application as GameControllerApp).logManager
        knowledgeDatabase = (application as GameControllerApp).knowledgeDatabase

        startForeground()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    override fun onDestroy() {
        stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_running))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_START -> start()
            ACTION_PAUSE -> pause()
            ACTION_STOP -> stop()
            ACTION_UPDATE_CONFIG -> {
                // 配置更新会在下次循环生效
            }
        }
    }

    /**
     * 设置无障碍服务引用
     */
    fun setAccessibilityService(service: GameAccessibilityService) {
        accessibilityService = service
    }

    /**
     * 初始化AI引擎
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.Default) {
        if (isInitialized) return@withContext true

        _status.value = RunStatus.INITIALIZING
        logManager.info(TAG, "Initializing AI engine...")

        try {
            // 初始化AI引擎
            aiEngine = AIEngine(applicationContext)
            val initResult = aiEngine!!.initialize()

            if (initResult.isFailure) {
                logManager.error(TAG, "Failed to initialize AI engine: ${initResult.exceptionOrNull()}")
                _status.value = RunStatus.ERROR
                return@withContext false
            }

            // 初始化游戏状态解析器
            gameStateParser = GameStateParser()

            // 初始化动作生成器
            val config = configManager.getConfig()
            actionGenerator = ActionGenerator(config, knowledgeDatabase)

            isInitialized = true
            logManager.info(TAG, "AI engine initialized successfully")
            return@withContext true

        } catch (e: Exception) {
            logManager.error(TAG, "Error initializing: ${e.message}")
            _status.value = RunStatus.ERROR
            return@withContext false
        }
    }

    /**
     * 开始控制
     */
    fun start() {
        if (_status.value == RunStatus.RUNNING) return

        serviceScope.launch {
            if (!isInitialized) {
                val success = initialize()
                if (!success) return@launch
            }

            _status.value = RunStatus.RUNNING
            logManager.info(TAG, "Control started")

            startControlLoop()
        }
    }

    /**
     * 开始控制循环
     */
    private fun startControlLoop() {
        controlJob = serviceScope.launch {
            val config = configManager.getConfig()
            var frameCount = 0L
            var totalInferenceTime = 0L
            var totalActionTime = 0L

            while (isActive && _status.value == RunStatus.RUNNING) {
                val frameStartTime = System.currentTimeMillis()

                try {
                    // 1. 获取当前帧
                    val frame = ScreenCaptureService.instance?.getCurrentFrame()
                    if (frame == null) {
                        delay(50)
                        continue
                    }

                    // 2. AI推理
                    val inferenceStartTime = System.currentTimeMillis()
                    val gameStateResult = aiEngine?.inference(frame)
                    val inferenceTime = System.currentTimeMillis() - inferenceStartTime
                    totalInferenceTime += inferenceTime

                    if (gameStateResult?.isFailure == true) {
                        logManager.warn(TAG, "Inference failed: ${gameStateResult.exceptionOrNull()}")
                        delay(config.actionDelay.toLong())
                        continue
                    }

                    val gameState = gameStateResult?.getOrNull() ?: continue
                    _currentGameState.value = gameState

                    // 3. 生成动作
                    val actions = actionGenerator?.generateActions(gameState) ?: emptyList()

                    // 4. 执行动作
                    val actionStartTime = System.currentTimeMillis()
                    executeActions(actions)
                    val actionTime = System.currentTimeMillis() - actionStartTime
                    totalActionTime += actionTime

                    // 5. 更新统计
                    frameCount++
                    val totalTime = System.currentTimeMillis() - frameStartTime

                    _stats.value = ControlStats(
                        frameCount = frameCount,
                        avgInferenceTime = if (frameCount > 0) totalInferenceTime / frameCount else 0,
                        avgActionTime = if (frameCount > 0) totalActionTime / frameCount else 0,
                        currentFps = if (totalTime > 0) 1000f / totalTime else 0f,
                        status = _status.value
                    )

                    // 日志记录（每100帧记录一次）
                    if (frameCount % 100 == 0L) {
                        logManager.info(TAG, "Frame #$frameCount: inference=${inferenceTime}ms, action=${actionTime}ms")
                    }

                    // 6. 延迟
                    delay(config.actionDelay.toLong())

                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    logManager.error(TAG, "Error in control loop: ${e.message}")
                    delay(100)
                }
            }
        }
    }

    /**
     * 执行动作序列
     */
    private fun executeActions(actions: List<GameAction>) {
        val service = accessibilityService ?: return

        for (action in actions) {
            if (_status.value != RunStatus.RUNNING) break

            service.executeAction(action)
        }
    }

    /**
     * 暂停控制
     */
    fun pause() {
        if (_status.value != RunStatus.RUNNING) return

        _status.value = RunStatus.PAUSED
        controlJob?.cancel()
        controlJob = null

        logManager.info(TAG, "Control paused")
        updateNotification(R.string.notification_paused)
    }

    /**
     * 停止控制
     */
    fun stop() {
        _status.value = RunStatus.IDLE
        controlJob?.cancel()
        controlJob = null

        aiEngine?.release()
        aiEngine = null
        isInitialized = false

        logManager.info(TAG, "Control stopped")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * 更新通知
     */
    private fun updateNotification(titleRes: Int) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(titleRes))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    /**
     * 更新配置
     */
    fun updateConfig(config: AppConfig) {
        actionGenerator?.updateConfig(config)
    }

    /**
     * 获取当前状态
     */
    fun getStatus(): RunStatus = _status.value

    data class ControlStats(
        val frameCount: Long = 0,
        val avgInferenceTime: Long = 0,
        val avgActionTime: Long = 0,
        val currentFps: Float = 0f,
        val status: RunStatus = RunStatus.IDLE
    )

    companion object {
        private const val TAG = "GameControlService"
        private const val NOTIFICATION_ID = 1002

        const val ACTION_START = "com.aigame.controller.START"
        const val ACTION_PAUSE = "com.aigame.controller.PAUSE"
        const val ACTION_STOP = "com.aigame.controller.STOP"
        const val ACTION_UPDATE_CONFIG = "com.aigame.controller.UPDATE_CONFIG"

        // 服务实例
        var instance: GameControlService? = null
            private set

        fun start(context: Context) {
            val intent = Intent(context, GameControlService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun pause(context: Context) {
            val intent = Intent(context, GameControlService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, GameControlService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
