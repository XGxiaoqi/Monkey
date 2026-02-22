package com.aigame.controller.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.aigame.controller.CHANNEL_ID
import com.aigame.controller.R
import com.aigame.controller.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * 屏幕截图服务
 * 使用MediaProjection API进行高性能截图
 */
class ScreenCaptureService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0
    private var scaleFactor = 0.5f

    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    private var isCapturing = false
    private var frameRate = 20 // 默认20fps
    private var frameIntervalMs: Long = 50

    private val _currentFrame = MutableStateFlow<Bitmap?>(null)
    val currentFrame: StateFlow<Bitmap?> = _currentFrame

    private val _captureCount = MutableStateFlow(0L)
    val captureCount: StateFlow<Long> = _captureCount

    private var lastCaptureTime = 0L

    // 截图回调
    private var onFrameCaptured: ((Bitmap) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        initDisplayMetrics()
        startForeground()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    override fun onDestroy() {
        stopCapture()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun initDisplayMetrics() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
            screenDensity = resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenDensity = displayMetrics.densityDpi
        }

        Log.d(TAG, "Screen metrics: ${screenWidth}x${screenHeight}, density: $screenDensity")
    }

    private fun startForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_running))
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_START_CAPTURE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

                if (resultCode != -1 && data != null) {
                    scaleFactor = intent.getFloatExtra(EXTRA_SCALE_FACTOR, 0.5f)
                    frameRate = intent.getIntExtra(EXTRA_FRAME_RATE, 20)
                    frameIntervalMs = 1000L / frameRate

                    startMediaProjection(resultCode, data)
                }
            }
            ACTION_STOP_CAPTURE -> {
                stopCapture()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_CONFIG -> {
                scaleFactor = intent.getFloatExtra(EXTRA_SCALE_FACTOR, scaleFactor)
                frameRate = intent.getIntExtra(EXTRA_FRAME_RATE, frameRate)
                frameIntervalMs = 1000L / frameRate
                updateVirtualDisplay()
            }
        }
    }

    private fun startMediaProjection(resultCode: Int, data: Intent) {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, data)

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d(TAG, "MediaProjection stopped")
                stopCapture()
            }
        }, Handler(Looper.getMainLooper()))

        setupImageReader()
        createVirtualDisplay()
        startCaptureLoop()
    }

    private fun setupImageReader() {
        val scaledWidth = (screenWidth * scaleFactor).toInt()
        val scaledHeight = (screenHeight * scaleFactor).toInt()

        imageReader = ImageReader.newInstance(
            scaledWidth, scaledHeight,
            android.graphics.PixelFormat.RGBA_8888,
            2  // maxImages
        )

        Log.d(TAG, "ImageReader created: ${scaledWidth}x${scaledHeight}")
    }

    private fun createVirtualDisplay() {
        val scaledWidth = (screenWidth * scaleFactor).toInt()
        val scaledHeight = (screenHeight * scaleFactor).toInt()

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            scaledWidth, scaledHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null, null
        )

        Log.d(TAG, "VirtualDisplay created")
    }

    private fun updateVirtualDisplay() {
        val scaledWidth = (screenWidth * scaleFactor).toInt()
        val scaledHeight = (screenHeight * scaleFactor).toInt()

        virtualDisplay?.resize(scaledWidth, scaledHeight, screenDensity)
    }

    private fun startCaptureLoop() {
        if (isCapturing) return

        isCapturing = true

        captureThread = HandlerThread("CaptureThread").apply { start() }
        captureHandler = Handler(captureThread!!.looper)

        serviceScope.launch {
            while (isActive && isCapturing) {
                captureFrame()
                kotlinx.coroutines.delay(frameIntervalMs)
            }
        }
    }

    private fun captureFrame() {
        val image = imageReader?.acquireLatestImage() ?: return

        try {
            val bitmap = imageToBitmap(image)
            bitmap?.let {
                _currentFrame.value = it
                _captureCount.value++
                lastCaptureTime = System.currentTimeMillis()
                onFrameCaptured?.invoke(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing frame", e)
        } finally {
            image.close()
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        // 裁剪掉多余的padding
        return if (rowPadding > 0) {
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        } else {
            bitmap
        }
    }

    private fun stopCapture() {
        isCapturing = false

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.close()
        imageReader = null

        mediaProjection?.stop()
        mediaProjection = null

        captureThread?.quitSafely()
        captureThread = null
        captureHandler = null

        Log.d(TAG, "Capture stopped")
    }

    /**
     * 设置帧回调
     */
    fun setFrameCallback(callback: (Bitmap) -> Unit) {
        onFrameCaptured = callback
    }

    /**
     * 获取当前帧
     */
    fun getCurrentFrame(): Bitmap? = _currentFrame.value

    /**
     * 更新配置
     */
    fun updateConfig(newScaleFactor: Float, newFrameRate: Int) {
        scaleFactor = newScaleFactor
        frameRate = newFrameRate
        frameIntervalMs = 1000L / frameRate
        updateVirtualDisplay()
    }

    /**
     * 获取统计信息
     */
    fun getStats(): CaptureStats {
        return CaptureStats(
            totalFrames = _captureCount.value,
            currentFps = if (lastCaptureTime > 0) {
                val elapsed = System.currentTimeMillis() - lastCaptureTime
                if (elapsed > 0) 1000f / elapsed else 0f
            } else 0f,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            scaleFactor = scaleFactor,
            targetFrameRate = frameRate
        )
    }

    data class CaptureStats(
        val totalFrames: Long,
        val currentFps: Float,
        val screenWidth: Int,
        val screenHeight: Int,
        val scaleFactor: Float,
        val targetFrameRate: Int
    )

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_CAPTURE = "com.aigame.controller.START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "com.aigame.controller.STOP_CAPTURE"
        const val ACTION_UPDATE_CONFIG = "com.aigame.controller.UPDATE_CONFIG"

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_SCALE_FACTOR = "scale_factor"
        const val EXTRA_FRAME_RATE = "frame_rate"

        /**
         * 启动截图服务的辅助方法
         */
        fun start(
            context: Context,
            resultCode: Int,
            data: Intent,
            scaleFactor: Float = 0.5f,
            frameRate: Int = 20
        ) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_START_CAPTURE
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
                putExtra(EXTRA_SCALE_FACTOR, scaleFactor)
                putExtra(EXTRA_FRAME_RATE, frameRate)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_STOP_CAPTURE
            }
            context.startService(intent)
        }
    }
}
