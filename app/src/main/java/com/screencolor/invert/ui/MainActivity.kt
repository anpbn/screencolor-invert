package com.screencolor.invert.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.slider.Slider
import com.screencolor.invert.R
import com.screencolor.invert.data.AppSettings
import com.screencolor.invert.data.ColorPair
import com.screencolor.invert.databinding.ActivityMainBinding
import com.screencolor.invert.service.OverlayService
import com.screencolor.invert.service.ScreenCaptureService
import com.screencolor.invert.utils.PreferenceManager

/**
 * Main activity for ScreenColor Invert System
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_MEDIA_PROJECTION = 1001
        private const val REQUEST_OVERLAY_PERMISSION = 1002
        private const val REQUEST_NOTIFICATION_PERMISSION = 1003
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var colorPairAdapter: ColorPairAdapter
    
    private var appSettings = AppSettings()
    private var isServiceRunning = false

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startCaptureService(result.resultCode, result.data)
        } else {
            Toast.makeText(this, R.string.permission_screen_capture_required, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferenceManager = PreferenceManager(this)
        preferenceManager.initializeDefaults()
        
        setupToolbar()
        setupRecyclerView()
        setupSettings()
        setupListeners()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    /**
     * Setup toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    /**
     * Setup RecyclerView for color pairs
     */
    private fun setupRecyclerView() {
        colorPairAdapter = ColorPairAdapter(
            onColorClick = { pair, isTarget ->
                showColorPicker(pair, isTarget)
            },
            onToleranceChange = { pair, tolerance ->
                pair.tolerance = tolerance / 100f
                preferenceManager.updateColorPair(pair)
                updateServiceColorPairs()
            },
            onEnableChange = { pair, enabled ->
                pair.isEnabled = enabled
                preferenceManager.updateColorPair(pair)
                updateServiceColorPairs()
            },
            onDeleteClick = { pair ->
                showDeleteConfirmDialog(pair)
            }
        )
        
        binding.recyclerColorPairs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = colorPairAdapter
        }
    }

    /**
     * Setup settings controls
     */
    private fun setupSettings() {
        // Frame rate chips
        binding.chipGroupFrameRate.setOnCheckedStateChangeListener { _, checkedIds ->
            appSettings.frameRate = when {
                checkedIds.contains(R.id.chip60fps) -> AppSettings.FRAME_RATE_60
                else -> AppSettings.FRAME_RATE_30
            }
            saveSettings()
        }
        
        // Resolution slider
        binding.sliderResolution.addOnChangeListener(Slider.OnChangeListener { _, value, _ ->
            appSettings.resolutionScale = value / 100f
            saveSettings()
        })
        
        // Edge smooth switch
        binding.switchEdgeSmooth.setOnCheckedChangeListener { _, isChecked ->
            appSettings.edgeSmooth = isChecked
            saveSettings()
        }
        
        // Color space chips
        binding.chipGroupColorSpace.setOnCheckedStateChangeListener { _, checkedIds ->
            appSettings.useHSV = checkedIds.contains(R.id.chipHSV)
            saveSettings()
        }
    }

    /**
     * Setup button listeners
     */
    private fun setupListeners() {
        binding.btnToggleService.setOnClickListener {
            if (isServiceRunning) {
                stopService()
            } else {
                checkPermissionsAndStart()
            }
        }
        
        binding.btnAddColorPair.setOnClickListener {
            addNewColorPair()
        }
        
        binding.btnToggleMask.setOnClickListener {
            appSettings.showMask = !appSettings.showMask
            binding.btnToggleMask.text = if (appSettings.showMask) {
                getString(R.string.preview_normal_mode)
            } else {
                getString(R.string.preview_mask_mode)
            }
            saveSettings()
        }
    }

    /**
     * Load saved data
     */
    private fun loadData() {
        // Load color pairs
        val colorPairs = preferenceManager.loadColorPairs()
        colorPairAdapter.submitList(colorPairs)
        
        // Load settings
        appSettings = preferenceManager.loadSettings()
        
        // Apply settings to UI
        when (appSettings.frameRate) {
            AppSettings.FRAME_RATE_60 -> binding.chip60fps.isChecked = true
            else -> binding.chip30fps.isChecked = true
        }
        
        binding.sliderResolution.value = appSettings.resolutionScale * 100
        binding.switchEdgeSmooth.isChecked = appSettings.edgeSmooth
        
        if (appSettings.useHSV) {
            binding.chipHSV.isChecked = true
        } else {
            binding.chipRGB.isChecked = true
        }
    }

    /**
     * Check permissions and start service
     */
    private fun checkPermissionsAndStart() {
        when {
            !Settings.canDrawOverlays(this) -> {
                requestOverlayPermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED -> {
                requestNotificationPermission()
            }
            else -> {
                requestMediaProjection()
            }
        }
    }

    /**
     * Request overlay permission
     */
    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.permission_overlay_required)
            .setPositiveButton(R.string.btn_grant_permission) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    /**
     * Request notification permission
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }

    /**
     * Request MediaProjection for screen capture
     */
    private fun requestMediaProjection() {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    /**
     * Start capture service
     */
    private fun startCaptureService(resultCode: Int, resultData: Intent) {
        ScreenCaptureService.start(this, resultCode, resultData)
        OverlayService.start(this)
        isServiceRunning = true
        updateServiceStatus()
        Toast.makeText(this, R.string.msg_service_started, Toast.LENGTH_SHORT).show()
    }

    /**
     * Stop service
     */
    private fun stopService() {
        ScreenCaptureService.stop(this)
        OverlayService.stop(this)
        isServiceRunning = false
        updateServiceStatus()
        Toast.makeText(this, R.string.msg_service_stopped, Toast.LENGTH_SHORT).show()
    }

    /**
     * Update service status UI
     */
    private fun updateServiceStatus() {
        binding.btnToggleService.text = if (isServiceRunning) {
            getString(R.string.btn_stop_service)
        } else {
            getString(R.string.btn_start_service)
        }
        
        binding.tvServiceStatus.text = if (isServiceRunning) {
            getString(R.string.service_running)
        } else {
            getString(R.string.service_tap_to_configure)
        }
    }

    /**
     * Add new color pair
     */
    private fun addNewColorPair() {
        val colorPairs = preferenceManager.loadColorPairs()
        if (colorPairs.size >= ColorPair.MAX_COLOR_PAIRS) {
            Toast.makeText(this, R.string.msg_max_colors_reached, Toast.LENGTH_SHORT).show()
            return
        }
        
        val newPair = ColorPair()
        if (preferenceManager.addColorPair(newPair)) {
            colorPairAdapter.addItem(newPair)
            updateServiceColorPairs()
            Toast.makeText(this, R.string.msg_color_added, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Show color picker dialog
     */
    private fun showColorPicker(pair: ColorPair, isTarget: Boolean) {
        val colorPickerDialog = ColorPickerDialog(this) { color ->
            if (isTarget) {
                pair.targetColor = color
            } else {
                pair.replacementColor = color
            }
            preferenceManager.updateColorPair(pair)
            colorPairAdapter.updateItem(pair)
            updateServiceColorPairs()
        }
        colorPickerDialog.show()
    }

    /**
     * Show delete confirmation dialog
     */
    private fun showDeleteConfirmDialog(pair: ColorPair) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_color_pair)
            .setMessage("确定要删除这组颜色配置吗？")
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                preferenceManager.deleteColorPair(pair.id)
                colorPairAdapter.removeItem(pair)
                updateServiceColorPairs()
                Toast.makeText(this, R.string.msg_color_deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    /**
     * Update service color pairs
     */
    private fun updateServiceColorPairs() {
        // Send broadcast or use other mechanism to update service
        // For now, service reads from preferences on each frame
    }

    /**
     * Save settings
     */
    private fun saveSettings() {
        preferenceManager.saveSettings(appSettings)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_OVERLAY_PERMISSION -> {
                if (Settings.canDrawOverlays(this)) {
                    checkPermissionsAndStart()
                } else {
                    Toast.makeText(this, R.string.permission_overlay_required, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermissionsAndStart()
                } else {
                    Toast.makeText(this, R.string.permission_notification_required, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
