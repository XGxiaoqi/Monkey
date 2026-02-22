package com.aigame.controller.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 日志级别
 */
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

/**
 * 日志条目
 */
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    fun format(): String {
        val timeStr = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
        return "$timeStr [${level.name.padEnd(5)}] $tag: $message"
    }
}

/**
 * 日志管理器
 */
class LogManager(private val context: Context) {

    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val maxQueueSize = 1000
    private val maxLogFileSize = 100 * 1024 * 1024  // 100MB
    private val logRetentionDays = 7

    private val logDir = File(context.filesDir, "logs")
    private val currentLogFile: File
        get() = File(logDir, "log_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.txt")

    private var isInitialized = false

    init {
        initLogDir()
    }

    private fun initLogDir() {
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        cleanOldLogs()
        isInitialized = true
    }

    /**
     * 记录调试日志
     */
    fun debug(tag: String, message: String) {
        log(LogLevel.DEBUG, tag, message)
    }

    /**
     * 记录信息日志
     */
    fun info(tag: String, message: String) {
        log(LogLevel.INFO, tag, message)
    }

    /**
     * 记录警告日志
     */
    fun warn(tag: String, message: String) {
        log(LogLevel.WARN, tag, message)
    }

    /**
     * 记录错误日志
     */
    fun error(tag: String, message: String) {
        log(LogLevel.ERROR, tag, message)
    }

    /**
     * 记录错误日志（带异常）
     */
    fun error(tag: String, message: String, throwable: Throwable) {
        val fullMessage = "$message\n${Log.getStackTraceString(throwable)}"
        log(LogLevel.ERROR, tag, fullMessage)
    }

    /**
     * 核心日志方法
     */
    private fun log(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message
        )

        // 添加到队列
        if (logQueue.size >= maxQueueSize) {
            logQueue.poll()
        }
        logQueue.offer(entry)

        // 输出到Logcat
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }

        // 写入文件
        writeToFile(entry)
    }

    /**
     * 记录AI决策日志
     */
    fun logDecision(stateSummary: String, actions: List<String>) {
        val message = "Decision: state=$stateSummary, actions=${actions.joinToString(",")}"
        info("AI_DECISION", message)
    }

    /**
     * 写入文件
     */
    private fun writeToFile(entry: LogEntry) {
        try {
            val logFile = currentLogFile

            // 检查文件大小
            if (logFile.exists() && logFile.length() > maxLogFileSize) {
                // 创建新文件
                val newName = "log_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
                logFile.renameTo(File(logDir, newName))
            }

            FileWriter(logFile, true).use { writer ->
                PrintWriter(writer).use { printer ->
                    printer.println(entry.format())
                }
            }
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to write log file: ${e.message}")
        }
    }

    /**
     * 清理旧日志
     */
    private fun cleanOldLogs() {
        val cutoffTime = System.currentTimeMillis() - (logRetentionDays * 24 * 60 * 60 * 1000L)

        logDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
            }
        }
    }

    /**
     * 获取日志列表
     */
    fun getLogs(level: LogLevel? = null, limit: Int = 50): List<LogEntry> {
        return logQueue.toList()
            .filter { level == null || it.level == level }
            .takeLast(limit)
    }

    /**
     * 导出日志
     */
    fun exportLogs(outputFile: File): Boolean {
        return try {
            FileWriter(outputFile).use { writer ->
                PrintWriter(writer).use { printer ->
                    logQueue.forEach { entry ->
                        printer.println(entry.format())
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to export logs: ${e.message}")
            false
        }
    }

    /**
     * 清除日志
     */
    fun clearLogs() {
        logQueue.clear()
        logDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * 获取日志文件大小
     */
    fun getLogFileSize(): Long {
        return logDir.listFiles()?.sumOf { it.length() } ?: 0
    }

    companion object {
        lateinit var instance: LogManager
            private set

        fun initialize(context: Context) {
            instance = LogManager(context)
        }
    }
}
