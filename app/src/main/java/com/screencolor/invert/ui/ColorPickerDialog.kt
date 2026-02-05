package com.screencolor.invert.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.SeekBar
import com.screencolor.invert.databinding.DialogColorPickerBinding

/**
 * Color picker dialog
 */
class ColorPickerDialog(
    context: Context,
    private val onColorSelected: (Int) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogColorPickerBinding
    private var currentColor = Color.RED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogColorPickerBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        setupSeekBars()
        setupButtons()
        updateColorPreview()
    }

    private fun setupSeekBars() {
        binding.seekBarRed.setOnSeekBarChangeListener(createSeekBarListener())
        binding.seekBarGreen.setOnSeekBarChangeListener(createSeekBarListener())
        binding.seekBarBlue.setOnSeekBarChangeListener(createSeekBarListener())
    }

    private fun createSeekBarListener() = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            updateCurrentColor()
            updateColorPreview()
        }
        
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private fun updateCurrentColor() {
        val red = binding.seekBarRed.progress
        val green = binding.seekBarGreen.progress
        val blue = binding.seekBarBlue.progress
        currentColor = Color.rgb(red, green, blue)
    }

    private fun updateColorPreview() {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.OVAL
        drawable.setColor(currentColor)
        binding.viewColorPreview.background = drawable
        
        binding.tvColorHex.text = String.format("#%06X", 0xFFFFFF and currentColor)
    }

    private fun setupButtons() {
        binding.btnConfirm.setOnClickListener {
            onColorSelected(currentColor)
            dismiss()
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        // Preset colors
        val presetColors = listOf(
            Color.RED, Color.GREEN, Color.BLUE,
            Color.YELLOW, Color.CYAN, Color.MAGENTA,
            Color.BLACK, Color.WHITE
        )
        
        // Setup preset buttons (simplified)
        binding.viewColorPreview.setOnClickListener {
            // Could show more advanced color picker
        }
    }
}
