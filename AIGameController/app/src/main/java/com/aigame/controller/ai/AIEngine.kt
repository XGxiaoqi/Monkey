package com.aigame.controller.ai

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.aigame.controller.model.GameState
import com.aigame.controller.utils.LogManager
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * AI引擎
 * 管理PaliGemma模型的加载和推理
 */
class AIEngine(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    private val logManager = LogManager(context)

    private var isModelLoaded = false
    private var inputWidth = 224
    private var inputHeight = 224
    private var inputChannels = 3

    // 内存使用统计
    private var modelMemoryMB = 0L

    /**
     * 初始化AI引擎
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            logManager.info(TAG, "Starting AI engine initialization...")

            // 加载模型
            val modelBuffer = loadModelFile()
            if (modelBuffer == null) {
                // 如果没有内置模型，使用模拟模式
                logManager.warn(TAG, "No model file found, using simulation mode")
                isModelLoaded = true
                return Result.success(Unit)
            }

            // 配置解释器选项
            val options = Interpreter.Options().apply {
                // 尝试使用GPU加速
                if (tryEnableGpu()) {
                    logManager.info(TAG, "GPU acceleration enabled")
                } else {
                    // 使用多线程CPU
                    setNumThreads(4)
                    logManager.info(TAG, "Using CPU with 4 threads")
                }
            }

            interpreter = Interpreter(modelBuffer, options)

            // 获取输入输出形状
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            if (inputShape != null) {
                inputHeight = inputShape[1]
                inputWidth = inputShape[2]
                inputChannels = inputShape[3]
            }

            isModelLoaded = true
            modelMemoryMB = modelBuffer.capacity() / (1024 * 1024)

            logManager.info(TAG, "AI engine initialized. Input: ${inputWidth}x${inputHeight}x${inputChannels}, Memory: ${modelMemoryMB}MB")

            Result.success(Unit)

        } catch (e: Exception) {
            logManager.error(TAG, "Failed to initialize AI engine: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 尝试启用GPU加速
     */
    private fun tryEnableGpu(): Boolean {
        val compatList = CompatibilityList()

        return if (compatList.isDelegateSupportedOnThisDevice) {
            try {
                gpuDelegate = GpuDelegate(compatList.bestOptionsForThisDevice)
                interpreter?.addDelegate(gpuDelegate)
                true
            } catch (e: Exception) {
                logManager.warn(TAG, "GPU delegate failed: ${e.message}")
                false
            }
        } else {
            false
        }
    }

    /**
     * 加载模型文件
     */
    private fun loadModelFile(): MappedByteBuffer? {
        return try {
            // 尝试从assets加载模型
            val assetManager = context.assets
            val fileDescriptor = assetManager.openFd(MODEL_FILE)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: IOException) {
            null
        }
    }

    /**
     * 执行推理
     * @param bitmap 输入图像
     * @return 游戏状态
     */
    suspend fun inference(bitmap: Bitmap): Result<GameState> {
        if (!isModelLoaded) {
            return Result.failure(IllegalStateException("AI engine not initialized"))
        }

        return try {
            val startTime = System.currentTimeMillis()

            // 预处理图像
            val inputBuffer = preprocessImage(bitmap)

            // 准备输出缓冲区
            val outputBuffer = Array(1) { FloatArray(OUTPUT_SIZE) }

            // 执行推理
            interpreter?.run(inputBuffer, outputBuffer)

            val inferenceTime = System.currentTimeMillis() - startTime

            // 后处理输出
            val gameState = postprocessOutput(outputBuffer[0], bitmap.width, bitmap.height)

            logManager.debug(TAG, "Inference completed in ${inferenceTime}ms")

            Result.success(gameState)

        } catch (e: Exception) {
            logManager.error(TAG, "Inference error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 流式推理
     */
    suspend fun inferenceStream(bitmap: Bitmap, callback: (GameState) -> Unit) {
        // 对于游戏控制，我们使用单次推理
        // 流式输出在动作生成阶段实现
        val result = inference(bitmap)
        result.getOrNull()?.let { callback(it) }
    }

    /**
     * 预处理图像
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // 缩放到模型输入尺寸
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)

        // 转换为ByteBuffer
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * inputChannels)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputWidth * inputHeight)
        scaledBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        for (pixel in pixels) {
            // 归一化到[-1, 1]
            byteBuffer.putFloat(((pixel shr 16 and 0xFF) - 128) / 128f)  // R
            byteBuffer.putFloat(((pixel shr 8 and 0xFF) - 128) / 128f)   // G
            byteBuffer.putFloat(((pixel and 0xFF) - 128) / 128f)         // B
        }

        return byteBuffer
    }

    /**
     * 后处理模型输出
     * 将模型输出转换为游戏状态
     */
    private fun postprocessOutput(output: FloatArray, originalWidth: Int, originalHeight: Int): GameState {
        // 这里是简化的解析逻辑
        // 实际实现需要根据PaliGemma的输出格式进行调整

        // PaliGemma输出的是文本描述，需要解析为结构化数据
        // 这里使用模拟数据进行演示

        return GameStateParser().parseFromModelOutput(output, originalWidth, originalHeight)
    }

    /**
     * 获取内存使用情况
     */
    fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024 * 1024)  // MB
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = isModelLoaded

    /**
     * 释放资源
     */
    fun release() {
        try {
            interpreter?.close()
            interpreter = null

            gpuDelegate?.close()
            gpuDelegate = null

            isModelLoaded = false

            logManager.info(TAG, "AI engine released")

        } catch (e: Exception) {
            logManager.error(TAG, "Error releasing AI engine: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "AIEngine"
        private const val MODEL_FILE = "paligemma.tflite"
        private const val OUTPUT_SIZE = 256  // 输出向量大小，根据实际模型调整
    }
}
