package com.aigame.controller.ui

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aigame.controller.GameControllerApp
import com.aigame.controller.R
import com.aigame.controller.databinding.ActivitySettingsBinding
import com.aigame.controller.model.AppConfig
import com.aigame.controller.model.StrategyType

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var currentConfig: AppConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        currentConfig = (application as GameControllerApp).configManager.getConfig()

        setupViews()
        loadConfig()
    }

    private fun setupViews() {
        // 帧率
        binding.seekFrameRate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fps = progress + 10  // 10-30
                binding.tvFrameRateValue.text = "$fps FPS"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveConfig()
            }
        })

        // 分辨率
        binding.seekResolution.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = (progress + 25) / 100f  // 0.25-1.0
                binding.tvResolutionValue.text = "${(scale * 100).toInt()}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveConfig()
            }
        })

        // 操作延迟
        binding.seekActionDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val delay = progress + 30  // 30-200
                binding.tvActionDelayValue.text = "${delay}ms"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveConfig()
            }
        })

        // 策略
        binding.rgStrategy.setOnCheckedChangeListener { _, checkedId ->
            saveConfig()
        }

        // 自动拾取
        binding.switchAutoPickup.setOnCheckedChangeListener { _, _ ->
            saveConfig()
        }

        // 自动吃药阈值
        binding.etPotionThreshold.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveConfig()
            }
        }

        // 重置按钮
        binding.btnReset.setOnClickListener {
            resetToDefault()
        }
    }

    private fun loadConfig() {
        // 帧率
        binding.seekFrameRate.progress = currentConfig.frameRate - 10
        binding.tvFrameRateValue.text = "${currentConfig.frameRate} FPS"

        // 分辨率
        binding.seekResolution.progress = (currentConfig.resolutionScale * 100 - 25).toInt()
        binding.tvResolutionValue.text = "${(currentConfig.resolutionScale * 100).toInt()}%"

        // 操作延迟
        binding.seekActionDelay.progress = currentConfig.actionDelay - 30
        binding.tvActionDelayValue.text = "${currentConfig.actionDelay}ms"

        // 策略
        when (currentConfig.strategy) {
            StrategyType.AGGRESSIVE -> binding.rbAggressive.isChecked = true
            StrategyType.BALANCED -> binding.rbBalanced.isChecked = true
            StrategyType.CONSERVATIVE -> binding.rbConservative.isChecked = true
        }

        // 自动拾取
        binding.switchAutoPickup.isChecked = currentConfig.autoPickup

        // 自动吃药阈值
        binding.etPotionThreshold.setText(currentConfig.autoPotionThreshold.toString())
    }

    private fun saveConfig() {
        val fps = binding.seekFrameRate.progress + 10
        val scale = (binding.seekResolution.progress + 25) / 100f
        val delay = binding.seekActionDelay.progress + 30

        val strategy = when {
            binding.rbAggressive.isChecked -> StrategyType.AGGRESSIVE
            binding.rbConservative.isChecked -> StrategyType.CONSERVATIVE
            else -> StrategyType.BALANCED
        }

        val autoPickup = binding.switchAutoPickup.isChecked

        val potionThreshold = binding.etPotionThreshold.text.toString().toIntOrNull() ?: 30

        val newConfig = AppConfig(
            frameRate = fps,
            resolutionScale = scale,
            actionDelay = delay,
            strategy = strategy,
            autoPickup = autoPickup,
            autoPotionThreshold = potionThreshold.coerceIn(0, 100),
            targetPackageName = currentConfig.targetPackageName
        )

        (application as GameControllerApp).configManager.saveConfig(newConfig)
        currentConfig = newConfig

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
    }

    private fun resetToDefault() {
        (application as GameControllerApp).configManager.resetToDefault()
        currentConfig = AppConfig()
        loadConfig()
        Toast.makeText(this, "已重置为默认设置", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
