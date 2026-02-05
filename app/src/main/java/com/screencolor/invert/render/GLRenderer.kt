package com.screencolor.invert.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.screencolor.invert.data.AppSettings
import com.screencolor.invert.data.ColorPair
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL Renderer for screen color processing
 */
class GLRenderer(private val context: Context) : android.opengl.GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "GLRenderer"
        private const val MAX_COLOR_PAIRS = 8
    }

    private var shaderProgram: ShaderProgram? = null
    private var externalTextureId: Int = 0
    private var surfaceTexture: SurfaceTexture? = null
    
    // Color data
    private val targetColors = Array(MAX_COLOR_PAIRS) { FloatArray(3) { 0f } }
    private val replacementColors = Array(MAX_COLOR_PAIRS) { FloatArray(3) { 0f } }
    private val tolerances = FloatArray(MAX_COLOR_PAIRS) { 0.3f }
    private var activeCount = 0
    
    // Settings
    private var showMask = false
    private var useHSV = false
    private var edgeSmooth = true
    
    // Screen dimensions
    private var screenWidth = 0
    private var screenHeight = 0
    
    // Texture transform matrix
    private val textureMatrix = FloatArray(16)
    
    // Callbacks
    var onFrameAvailable: (() -> Unit)? = null
    
    /**
     * Initialize OpenGL ES context
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")
        
        // Set clear color to transparent
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        
        // Initialize shader program
        initShaderProgram()
        
        // Create external texture for SurfaceTexture
        createExternalTexture()
    }
    
    /**
     * Handle surface size changes
     */
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: $width x $height")
        GLES20.glViewport(0, 0, width, height)
        screenWidth = width
        screenHeight = height
    }
    
    /**
     * Render frame
     */
    override fun onDrawFrame(gl: GL10?) {
        // Clear buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        // Update SurfaceTexture
        surfaceTexture?.updateTexImage()
        surfaceTexture?.getTransformMatrix(textureMatrix)
        
        // Use shader program
        shaderProgram?.use()
        shaderProgram?.bindVertexAttributes()
        
        // Bind external texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTextureId)
        GLES20.glUniform1i(shaderProgram?.externalTextureHandle ?: 0, 0)
        
        // Set uniforms
        setUniforms()
        
        // Draw
        shaderProgram?.draw()
        
        // Notify frame available
        onFrameAvailable?.invoke()
    }
    
    /**
     * Initialize shader program
     */
    private fun initShaderProgram() {
        val vertexShaderSource = loadShaderSource("vertex_shader.glsl")
        val fragmentShaderSource = loadShaderSource("fragment_shader.glsl")
        
        if (vertexShaderSource.isNotEmpty() && fragmentShaderSource.isNotEmpty()) {
            shaderProgram = ShaderProgram(context).apply {
                if (!initialize(vertexShaderSource, fragmentShaderSource)) {
                    Log.e(TAG, "Failed to initialize shader program")
                }
            }
        } else {
            Log.e(TAG, "Failed to load shader sources")
        }
    }
    
    /**
     * Load shader source from raw resources
     */
    private fun loadShaderSource(fileName: String): String {
        return try {
            val resourceId = context.resources.getIdentifier(
                fileName.substringBeforeLast("."),
                "raw",
                context.packageName
            )
            context.resources.openRawResource(resourceId).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load shader: $fileName", e)
            ""
        }
    }
    
    /**
     * Create external OES texture for SurfaceTexture
     */
    private fun createExternalTexture() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        externalTextureId = textures[0]
        
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        // Create SurfaceTexture
        surfaceTexture = SurfaceTexture(externalTextureId)
    }
    
    /**
     * Set shader uniforms
     */
    private fun setUniforms() {
        shaderProgram?.let { shader ->
            // Set color arrays
            for (i in 0 until MAX_COLOR_PAIRS) {
                GLES20.glUniform3fv(shader.targetsHandle + i, 1, targetColors[i], 0)
                GLES20.glUniform3fv(shader.replacementsHandle + i, 1, replacementColors[i], 0)
            }
            
            // Set tolerances
            GLES20.glUniform1fv(shader.tolerancesHandle, MAX_COLOR_PAIRS, tolerances, 0)
            
            // Set active count
            GLES20.glUniform1i(shader.activeCountHandle, activeCount)
            
            // Set boolean uniforms
            GLES20.glUniform1i(shader.useExternalTextureHandle, 1)
            GLES20.glUniform1i(shader.showMaskHandle, if (showMask) 1 else 0)
            GLES20.glUniform1i(shader.useHSVHandle, if (useHSV) 1 else 0)
            GLES20.glUniform1i(shader.edgeSmoothHandle, if (edgeSmooth) 1 else 0)
        }
    }
    
    /**
     * Update color pairs data
     */
    fun updateColorPairs(colorPairs: List<ColorPair>) {
        activeCount = minOf(colorPairs.size, MAX_COLOR_PAIRS)
        
        for (i in 0 until activeCount) {
            val pair = colorPairs[i]
            val target = pair.getTargetColorFloatArray()
            val replacement = pair.getReplacementColorFloatArray()
            
            targetColors[i][0] = target[0]
            targetColors[i][1] = target[1]
            targetColors[i][2] = target[2]
            
            replacementColors[i][0] = replacement[0]
            replacementColors[i][1] = replacement[1]
            replacementColors[i][2] = replacement[2]
            
            tolerances[i] = pair.tolerance
        }
    }
    
    /**
     * Update settings
     */
    fun updateSettings(settings: AppSettings) {
        showMask = settings.showMask
        useHSV = settings.useHSV
        edgeSmooth = settings.edgeSmooth
    }
    
    /**
     * Get SurfaceTexture for VirtualDisplay
     */
    fun getSurfaceTexture(): SurfaceTexture? = surfaceTexture
    
    /**
     * Get Surface for VirtualDisplay
     */
    fun getSurface(): android.view.Surface? {
        return surfaceTexture?.let { Surface(it) }
    }
    
    /**
     * Release resources
     */
    fun release() {
        shaderProgram?.release()
        shaderProgram = null
        
        if (externalTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(externalTextureId), 0)
            externalTextureId = 0
        }
        
        surfaceTexture?.release()
        surfaceTexture = null
    }
}
