package com.aigame.controller.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aigame.controller.GameControllerApp
import com.aigame.controller.R
import com.aigame.controller.ai.GameStateParser
import com.aigame.controller.data.entity.SkillEntity
import com.aigame.controller.databinding.ActivityLearnBinding
import com.aigame.controller.model.SkillEffectType
import com.aigame.controller.service.ScreenCaptureService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class LearnActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearnBinding
    private val gameStateParser = GameStateParser()
    private var isLearning = false

    private val learnedSkills = mutableListOf<SkillEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearnBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.learn_title)

        setupViews()
    }

    private fun setupViews() {
        binding.btnStartLearn.setOnClickListener {
            if (isLearning) {
                stopLearning()
            } else {
                startLearning()
            }
        }

        binding.btnSave.setOnClickListener {
            saveLearnedKnowledge()
        }

        // 初始状态
        binding.btnSave.visibility = View.GONE
    }

    private fun startLearning() {
        isLearning = true
        binding.btnStartLearn.text = "停止学习"
        binding.tvStatus.text = getString(R.string.learn_scanning)
        binding.progress.visibility = View.VISIBLE
        binding.btnSave.visibility = View.GONE

        lifecycleScope.launch {
            while (isLearning) {
                val frame = ScreenCaptureService.instance?.getCurrentFrame()
                if (frame != null) {
                    analyzeFrame(frame)
                }
                delay(500)
            }
        }
    }

    private fun stopLearning() {
        isLearning = false
        binding.btnStartLearn.text = "开始学习"
        binding.tvStatus.text = getString(R.string.learn_found, learnedSkills.size)
        binding.progress.visibility = View.GONE

        if (learnedSkills.isNotEmpty()) {
            binding.btnSave.visibility = View.VISIBLE
        }
    }

    private fun analyzeFrame(frame: Bitmap) {
        lifecycleScope.launch {
            try {
                // 使用图像分析识别技能图标
                val width = frame.width
                val height = frame.height

                // 简化实现：假设技能在屏幕右下角区域
                // 实际应该使用AI模型进行识别
                val skillRegion = Bitmap.createBitmap(
                    frame,
                    (width * 0.6).toInt(),
                    (height * 0.6).toInt(),
                    (width * 0.35).toInt(),
                    (height * 0.35).toInt()
                )

                // 这里应该调用AI模型识别技能
                // 简化实现：模拟学习过程
                val detectedSkills = detectSkillsFromImage(skillRegion)

                for (skill in detectedSkills) {
                    if (learnedSkills.none { it.name == skill.name }) {
                        learnedSkills.add(skill)
                        updateLearnedList()
                    }
                }

                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = "扫描中... 已发现 ${learnedSkills.size} 个技能"
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun detectSkillsFromImage(image: Bitmap): List<SkillEntity> {
        // 简化实现：模拟AI识别结果
        // 实际应该使用PaliGemma模型进行图像理解

        val skills = mutableListOf<SkillEntity>()

        // 这里可以添加图像处理逻辑来识别技能图标
        // 例如：模板匹配、特征提取等

        return skills
    }

    private fun updateLearnedList() {
        lifecycleScope.launch(Dispatchers.Main) {
            val sb = StringBuilder()
            learnedSkills.forEachIndexed { index, skill ->
                sb.append("${index + 1}. ${skill.name}\n")
                sb.append("   ${skill.description}\n\n")
            }
            binding.tvLearnedList.text = sb.toString()
        }
    }

    private fun saveLearnedKnowledge() {
        lifecycleScope.launch {
            val database = (application as GameControllerApp).knowledgeDatabase

            withContext(Dispatchers.IO) {
                database.skillDao().insertAll(learnedSkills)
            }

            android.widget.Toast.makeText(
                this@LearnActivity,
                "已保存 ${learnedSkills.size} 个知识",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        isLearning = false
    }
}
