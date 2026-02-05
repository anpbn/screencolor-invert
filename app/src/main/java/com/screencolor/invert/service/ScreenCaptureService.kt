package com.screencolor.invert.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.opengl.GLSurfaceView
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.screencolor.invert.R
import com.screencolor.invert.data.AppSettings
import com.screencolor.invert.data.ColorPair
import com.screencolor.invert.render.GLRenderer
import com.screencolor.invert.ui.MainActivity
import com.screencolor.invert.utils.PreferenceManager
import kotlinx.coroutines.*

/**
 * Foreground service for screen capture and color replacement
 */
class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_capture_channel"
        private const val ACTION_START = "action_start"
        private const val ACTION_STOP = "action_stop"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var glSurfaceView: GLSurfaceView? = null
    private var glRenderer: GLRenderer? = null
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var preferenceManager: PreferenceManager
    
    private var isRunning = false
    private var isPaused = false
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        preferenceManager = PreferenceManager(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initScreenMetrics()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    startForegroundService()
                    initMediaProjection(resultCode, resultData)
                    initGLSurface()
                    createOverlay()
                    isRunning = true
                } else {
                    Log.e(TAG, "Invalid result code or data")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopService()
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Initialize screen metrics
     */
    private fun initScreenMetrics() {
        val metrics = DisplayMetrics()
        val display = windowManager?.defaultDisplay
        display?.getRealMetrics(metrics)
        
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi
        
        Log.d(TAG, "Screen metrics: ${screenWidth}x$screenHeight, density: $screenDensity")
    }

    /**
     * Start as foreground service with notification
     */
    private fun startForegroundService() {
        createNotificationChannel()
        
        val notification = createNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_running),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_tap_to_configure)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create service notification
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, ScreenCaptureService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_running))
            .setContentText(getString(R.string.service_tap_to_configure))
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.service_stop), stopIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * Initialize MediaProjection
     */
    private fun initMediaProjection(resultCode: Int, resultData: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
        
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d(TAG, "MediaProjection stopped")
                stopService()
            }
        }, Handler(Looper.getMainLooper()))
    }

    /**
     * Initialize GLSurfaceView for rendering
     */
    private fun initGLSurface() {
        glRenderer = GLRenderer(this).apply {
            updateColorPairs(preferenceManager.getEnabledColorPairs())
            updateSettings(preferenceManager.loadSettings())
        }
        
        glSurfaceView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 0, 0)
            holder.setFormat(PixelFormat.TRANSLUCENT)
            setRenderer(glRenderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        
        // Wait for GL context to be ready, then create VirtualDisplay
        glSurfaceView?.post {
            createVirtualDisplay()
        }
    }

    /**
     * Create VirtualDisplay to capture screen
     */
    private fun createVirtualDisplay() {
        val surface = glRenderer?.getSurface() ?: run {
            Log.e(TAG, "Surface not ready")
            return
        }
        
        val settings = preferenceManager.loadSettings()
        val width = (screenWidth * settings.resolutionScale).toInt()
        val height = (screenHeight * settings.resolutionScale).toInt()
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            object : VirtualDisplay.Callback() {
                override fun onPaused() {
                    Log.d(TAG, "VirtualDisplay paused")
                }
                
                override fun onResumed() {
                    Log.d(TAG, "VirtualDisplay resumed")
                }
                
                override fun onStopped() {
                    Log.d(TAG, "VirtualDisplay stopped")
                }
            },
            Handler(Looper.getMainLooper())
        )
        
        Log.d(TAG, "VirtualDisplay created: ${width}x$height")
    }

    /**
     * Create overlay window
     */
    private fun createOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        
        val container = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        glSurfaceView?.let { glView ->
            container.addView(glView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }
        
        overlayView = container
        windowManager?.addView(overlayView, params)
    }

    /**
     * Update color pairs
     */
    fun updateColorPairs(colorPairs: List<ColorPair>) {
        glRenderer?.updateColorPairs(colorPairs)
    }

    /**
     * Update settings
     */
    fun updateSettings(settings: AppSettings) {
        glRenderer?.updateSettings(settings)
    }

    /**
     * Pause rendering
     */
    fun pauseRendering() {
        if (!isPaused) {
            isPaused = true
            glSurfaceView?.onPause()
            virtualDisplay?.surface?.let { surface ->
                // Keep surface but stop updating
            }
        }
    }

    /**
     * Resume rendering
     */
    fun resumeRendering() {
        if (isPaused) {
            isPaused = false
            glSurfaceView?.onResume()
        }
    }

    /**
     * Check if service is running
     */
    fun isServiceRunning(): Boolean = isRunning

    /**
     * Check if rendering is paused
     */
    fun isRenderingPaused(): Boolean = isPaused

    /**
     * Stop the service
     */
    private fun stopService() {
        Log.d(TAG, "stopService")
        isRunning = false
        
        // Remove overlay
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay", e)
            }
        }
        overlayView = null
        
        // Release GL resources
        glSurfaceView?.onPause()
        glRenderer?.release()
        glSurfaceView = null
        glRenderer = null
        
        // Release VirtualDisplay
        virtualDisplay?.release()
        virtualDisplay = null
        
        // Stop MediaProjection
        mediaProjection?.stop()
        mediaProjection = null
        
        // Cancel coroutines
        serviceScope.cancel()
        
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        stopService()
    }
}
