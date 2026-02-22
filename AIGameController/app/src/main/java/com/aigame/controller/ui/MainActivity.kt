package com.aigame.controller.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aigame.controller.GameControllerApp
import com.aigame.controller.R
import com.aigame.controller.databinding.ActivityMainBinding
import com.aigame.controller.model.RunStatus
import com.aigame.controller.service.GameAccessibilityService
import com.aigame.controller.service.ScreenCaptureService
import com.aigame.controller.utils.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var configManager: ConfigManager

    private var currentStatus = RunStatus.IDLE

    // MediaProjection请求
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            startWithMediaProjection(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "需要屏幕截图权限才能运行", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configManager = (application as GameControllerApp).configManager

        setupViews()
        checkPermissions()
        startStatusUpdates()
    }

    private fun setupViews() {
        // 开始按钮
        binding.btnStart.setOnClickListener {
            if (checkAccessibilityPermission()) {
                requestMediaProjection()
            }
        }

        // 暂停按钮
        binding.btnPause.setOnClickListener {
            // 暂停逻辑
            updateStatus(RunStatus.PAUSED)
        }

        // 停止按钮
        binding.btnStop.setOnClickListener {
            stopAI()
        }

        // 预学习
        binding.cardLearn.setOnClickListener {
            startActivity(Intent(this, LearnActivity::class.java))
        }

        // 设置
        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 知识库
        binding.cardKnowledge.setOnClickListener {
            startActivity(Intent(this, KnowledgeActivity::class.java))
        }

        // 日志
        binding.cardLogs.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        // 权限授予
        binding.btnGrantPermission.setOnClickListener {
            openAccessibilitySettings()
        }
    }

    private fun checkPermissions() {
        val hasAccessibility = checkAccessibilityPermission()
        val hasOverlay = Settings.canDrawOverlays(this)

        if (!hasAccessibility || !hasOverlay) {
            binding.permissionCard.visibility = View.VISIBLE

            when {
                !hasAccessibility -> {
                    binding.tvPermissionTitle.text = getString(R.string.permission_accessibility_title)
                    binding.tvPermissionMessage.text = getString(R.string.permission_accessibility_message)
                }
                !hasOverlay -> {
                    binding.tvPermissionTitle.text = getString(R.string.permission_overlay_title)
                    binding.tvPermissionMessage.text = getString(R.string.permission_overlay_message)
                    binding.btnGrantPermission.setOnClickListener {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                }
            }
        } else {
            binding.permissionCard.visibility = View.GONE
        }
    }

    private fun checkAccessibilityPermission(): Boolean {
        val service = GameAccessibilityService.instance
        return service != null && service.isServiceEnabled.value
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun requestMediaProjection() {
        val mediaProjectionManager = getSystemService(android.media.projection.MediaProjectionManager::class.java)
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        mediaProjectionLauncher.launch(intent)
    }

    private fun startWithMediaProjection(resultCode: Int, data: android.content.Intent) {
        val config = configManager.getConfig()

        // 启动截图服务
        ScreenCaptureService.start(
            this,
            resultCode,
            data,
            config.resolutionScale,
            config.frameRate
        )

        updateStatus(RunStatus.RUNNING)

        Toast.makeText(this, "AI控制已启动", Toast.LENGTH_SHORT).show()
    }

    private fun stopAI() {
        ScreenCaptureService.stop(this)
        updateStatus(RunStatus.IDLE)
    }

    private fun updateStatus(status: RunStatus) {
        currentStatus = status

        runOnUiThread {
            // 更新状态文本
            val statusText = when (status) {
                RunStatus.IDLE -> getString(R.string.status_idle)
                RunStatus.INITIALIZING -> getString(R.string.status_initializing)
                RunStatus.RUNNING -> getString(R.string.status_running)
                RunStatus.PAUSED -> getString(R.string.status_paused)
                RunStatus.ERROR -> getString(R.string.status_error)
            }
            binding.tvStatus.text = statusText

            // 更新状态指示器颜色
            val colorRes = when (status) {
                RunStatus.IDLE -> R.color.status_idle
                RunStatus.INITIALIZING -> R.color.status_paused
                RunStatus.RUNNING -> R.color.status_running
                RunStatus.PAUSED -> R.color.status_paused
                RunStatus.ERROR -> R.color.status_error
            }
            binding.statusIndicator.setBackgroundResource(colorRes)

            // 更新按钮可见性
            when (status) {
                RunStatus.IDLE, RunStatus.ERROR -> {
                    binding.btnStart.visibility = View.VISIBLE
                    binding.btnPause.visibility = View.GONE
                    binding.btnStop.visibility = View.GONE
                }
                RunStatus.RUNNING -> {
                    binding.btnStart.visibility = View.GONE
                    binding.btnPause.visibility = View.VISIBLE
                    binding.btnStop.visibility = View.VISIBLE
                }
                RunStatus.PAUSED -> {
                    binding.btnStart.visibility = View.VISIBLE
                    binding.btnPause.visibility = View.GONE
                    binding.btnStop.visibility = View.VISIBLE
                }
                RunStatus.INITIALIZING -> {
                    binding.btnStart.visibility = View.GONE
                    binding.btnPause.visibility = View.GONE
                    binding.btnStop.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun startStatusUpdates() {
        lifecycleScope.launch {
            while (true) {
                updateMemoryInfo()
                updatePerformanceInfo()
                delay(1000)
            }
        }
    }

    private fun updateMemoryInfo() {
        val runtime = Runtime.getRuntime()
        val usedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val totalMB = runtime.totalMemory() / (1024 * 1024)

        binding.tvMemory.text = getString(R.string.memory_usage, usedMB, totalMB)
    }

    private fun updatePerformanceInfo() {
        // 从ScreenCaptureService获取统计信息
        val stats = ScreenCaptureService.instance?.getStats()
        if (stats != null) {
            binding.tvFps.text = getString(R.string.fps_info, stats.targetFrameRate)
        }

        // 从GameControlService获取延迟
        val controlStats = com.aigame.controller.service.GameControlService.instance?.stats?.value
        if (controlStats != null) {
            binding.tvLatency.text = getString(R.string.latency_info, controlStats.avgInferenceTime)
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }
}
