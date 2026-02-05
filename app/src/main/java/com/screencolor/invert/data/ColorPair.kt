package com.screencolor.invert.data

import android.graphics.Color
import android.os.Parcel
import android.os.Parcelable

/**
 * Data class representing a color pair configuration
 */
data class ColorPair(
    var id: Long = System.currentTimeMillis(),
    var targetColor: Int = Color.RED,
    var replacementColor: Int = Color.GREEN,
    var tolerance: Float = 0.3f, // 0.0 - 1.0
    var isEnabled: Boolean = true,
    var priority: Int = 0
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        targetColor = parcel.readInt(),
        replacementColor = parcel.readInt(),
        tolerance = parcel.readFloat(),
        isEnabled = parcel.readByte() != 0.toByte(),
        priority = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeInt(targetColor)
        parcel.writeInt(replacementColor)
        parcel.writeFloat(tolerance)
        parcel.writeByte(if (isEnabled) 1 else 0)
        parcel.writeInt(priority)
    }

    override fun describeContents(): Int = 0

    companion object {
        const val MAX_COLOR_PAIRS = 8
        const val DEFAULT_TOLERANCE = 0.3f

        @JvmField
        val CREATOR = object : Parcelable.Creator<ColorPair> {
            override fun createFromParcel(parcel: Parcel): ColorPair = ColorPair(parcel)
            override fun newArray(size: Int): Array<ColorPair?> = arrayOfNulls(size)
        }
    }

    /**
     * Convert target color to normalized RGB float array
     */
    fun getTargetColorFloatArray(): FloatArray {
        return floatArrayOf(
            Color.red(targetColor) / 255f,
            Color.green(targetColor) / 255f,
            Color.blue(targetColor) / 255f
        )
    }

    /**
     * Convert replacement color to normalized RGB float array
     */
    fun getReplacementColorFloatArray(): FloatArray {
        return floatArrayOf(
            Color.red(replacementColor) / 255f,
            Color.green(replacementColor) / 255f,
            Color.blue(replacementColor) / 255f
        )
    }

    /**
     * Get a copy of this ColorPair
     */
    fun copy(): ColorPair {
        return ColorPair(
            id = id,
            targetColor = targetColor,
            replacementColor = replacementColor,
            tolerance = tolerance,
            isEnabled = isEnabled,
            priority = priority
        )
    }
}

/**
 * Settings data class for app configuration
 */
data class AppSettings(
    var frameRate: Int = 30, // 30 or 60
    var resolutionScale: Float = 1.0f, // 0.5, 0.75, 1.0
    var edgeSmooth: Boolean = true,
    var useHSV: Boolean = false,
    var showMask: Boolean = false
) {
    companion object {
        const val FRAME_RATE_30 = 30
        const val FRAME_RATE_60 = 60
        const val RESOLUTION_FULL = 1.0f
        const val RESOLUTION_75 = 0.75f
        const val RESOLUTION_50 = 0.5f
    }
}
