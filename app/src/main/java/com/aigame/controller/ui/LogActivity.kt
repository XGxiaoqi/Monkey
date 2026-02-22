package com.aigame.controller.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aigame.controller.GameControllerApp
import com.aigame.controller.R
import com.aigame.controller.databinding.ActivityLogBinding
import com.aigame.controller.utils.LogEntry
import com.aigame.controller.utils.LogLevel
import com.aigame.controller.utils.LogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class LogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogBinding
    private val scope = CoroutineScope(Dispatchers.Main)
    private val logManager by lazy { (application as GameControllerApp).logManager }

    private var currentLevel: LogLevel? = null
    private var logs: List<LogEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.log_title)

        setupViews()
        loadLogs()
    }

    private fun setupViews() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.btnAll.setOnClickListener {
            currentLevel = null
            updateButtonStates()
            loadLogs()
        }

        binding.btnDebug.setOnClickListener {
            currentLevel = LogLevel.DEBUG
            updateButtonStates()
            loadLogs()
        }

        binding.btnInfo.setOnClickListener {
            currentLevel = LogLevel.INFO
            updateButtonStates()
            loadLogs()
        }

        binding.btnWarn.setOnClickListener {
            currentLevel = LogLevel.WARN
            updateButtonStates()
            loadLogs()
        }

        binding.btnError.setOnClickListener {
            currentLevel = LogLevel.ERROR
            updateButtonStates()
            loadLogs()
        }

        binding.btnExport.setOnClickListener {
            exportLogs()
        }

        binding.btnClear.setOnClickListener {
            clearLogs()
        }

        binding.btnAll.isSelected = true
    }

    private fun updateButtonStates() {
        binding.btnAll.isSelected = currentLevel == null
        binding.btnDebug.isSelected = currentLevel == LogLevel.DEBUG
        binding.btnInfo.isSelected = currentLevel == LogLevel.INFO
        binding.btnWarn.isSelected = currentLevel == LogLevel.WARN
        binding.btnError.isSelected = currentLevel == LogLevel.ERROR
    }

    private fun loadLogs() {
        scope.launch {
            logs = withContext(Dispatchers.IO) {
                logManager.getLogs(currentLevel, 200)
            }

            binding.recyclerView.adapter = LogAdapter(logs)

            if (logs.isEmpty()) {
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun exportLogs() {
        scope.launch {
            val file = java.io.File(
                getExternalFilesDir(null),
                "log_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            )

            val success = withContext(Dispatchers.IO) {
                logManager.exportLogs(file)
            }

            if (success) {
                android.widget.Toast.makeText(
                    this@LogActivity,
                    "日志已导出到: ${file.absolutePath}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else {
                android.widget.Toast.makeText(
                    this@LogActivity,
                    "导出失败",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun clearLogs() {
        android.app.AlertDialog.Builder(this)
            .setTitle("清除日志")
            .setMessage("确定要清除所有日志吗？")
            .setPositiveButton("确定") { _, _ ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        logManager.clearLogs()
                    }
                    loadLogs()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    inner class LogAdapter(private val data: List<LogEntry>) :
        RecyclerView.Adapter<LogAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = data[position]
            val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                .format(Date(entry.timestamp))

            holder.title.text = "[${entry.level.name}] ${entry.tag}"
            holder.subtitle.text = "$timeStr: ${entry.message}"

            val colorRes = when (entry.level) {
                LogLevel.DEBUG -> android.graphics.Color.GRAY
                LogLevel.INFO -> android.graphics.Color.CYAN
                LogLevel.WARN -> android.graphics.Color.YELLOW
                LogLevel.ERROR -> android.graphics.Color.RED
            }
            holder.title.setTextColor(colorRes)
        }

        override fun getItemCount() = data.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(android.R.id.text1)
            val subtitle: TextView = view.findViewById(android.R.id.text2)
        }
    }
}
