package com.screencolor.invert.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.ImageButton
import com.screencolor.invert.R
import com.screencolor.invert.ui.MainActivity

/**
 * Service for overlay control bar
 */
class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        
        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.startService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    private val binder = OverlayBinder()
    private var listener: OverlayListener? = null

    interface OverlayListener {
        fun onPauseResume()
        fun onSettings()
        fun onStop()
    }

    inner class OverlayBinder : Binder() {
        fun getService(): OverlayService = this@OverlayService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null) {
            createOverlay()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    /**
     * Create overlay control bar
     */
    private fun createOverlay() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_control, null)
        
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }
        
        setupControls()
        setupDrag()
        
        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
        }
    }

    /**
     * Setup control buttons
     */
    private fun setupControls() {
        overlayView?.findViewById<ImageButton>(R.id.btnPauseResume)?.setOnClickListener {
            listener?.onPauseResume()
        }
        
        overlayView?.findViewById<ImageButton>(R.id.btnSettings)?.setOnClickListener {
            listener?.onSettings()
        }
        
        overlayView?.findViewById<ImageButton>(R.id.btnStop)?.setOnClickListener {
            listener?.onStop()
        }
    }

    /**
     * Setup drag functionality
     */
    private fun setupDrag() {
        val dragButton = overlayView?.findViewById<ImageButton>(R.id.btnDrag)
        
        dragButton?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.x = initialX + (event.rawX - initialTouchX).toInt()
                    params?.y = initialY + (event.rawY - initialTouchY).toInt()
                    
                    try {
                        windowManager?.updateViewLayout(overlayView, params)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update overlay position", e)
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Set overlay listener
     */
    fun setListener(listener: OverlayListener) {
        this.listener = listener
    }

    /**
     * Show overlay
     */
    fun show() {
        overlayView?.visibility = View.VISIBLE
    }

    /**
     * Hide overlay
     */
    fun hide() {
        overlayView?.visibility = View.GONE
    }

    /**
     * Update pause/resume button icon
     */
    fun setPaused(isPaused: Boolean) {
        overlayView?.findViewById<ImageButton>(R.id.btnPauseResume)?.setImageResource(
            if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay", e)
            }
        }
        overlayView = null
    }
}
