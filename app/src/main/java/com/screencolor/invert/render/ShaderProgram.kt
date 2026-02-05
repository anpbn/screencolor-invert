package com.screencolor.invert.render

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * OpenGL Shader Program manager
 */
class ShaderProgram(context: Context) {

    companion object {
        private const val TAG = "ShaderProgram"
        
        // Vertex shader attribute locations
        const val ATTRIBUTE_POSITION = "aPosition"
        const val ATTRIBUTE_TEXTURE_COORD = "aTextureCoord"
        
        // Uniform locations
        const val UNIFORM_TEXTURE = "uTexture"
        const val UNIFORM_EXTERNAL_TEXTURE = "uExternalTexture"
        const val UNIFORM_TARGETS = "uTargets"
        const val UNIFORM_REPLACEMENTS = "uReplacements"
        const val UNIFORM_TOLERANCES = "uTolerances"
        const val UNIFORM_ACTIVE_COUNT = "uActiveCount"
        const val UNIFORM_USE_EXTERNAL_TEXTURE = "uUseExternalTexture"
        const val UNIFORM_SHOW_MASK = "uShowMask"
        const val UNIFORM_USE_HSV = "uUseHSV"
        const val UNIFORM_EDGE_SMOOTH = "uEdgeSmooth"
        
        // Maximum color pairs supported
        const val MAX_COLOR_PAIRS = 8
        
        // Vertex data for full-screen quad
        private val VERTEX_DATA = floatArrayOf(
            // Position (x, y)    // Texture Coord (u, v)
            -1.0f, -1.0f,         0.0f, 0.0f,  // Bottom-left
             1.0f, -1.0f,         1.0f, 0.0f,  // Bottom-right
            -1.0f,  1.0f,         0.0f, 1.0f,  // Top-left
             1.0f,  1.0f,         1.0f, 1.0f   // Top-right
        )
        
        private const val BYTES_PER_FLOAT = 4
        private const val POSITION_COMPONENT_COUNT = 2
        private const val TEXTURE_COORD_COMPONENT_COUNT = 2
        private const val STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORD_COMPONENT_COUNT) * BYTES_PER_FLOAT
    }
    
    private var programId: Int = 0
    private var vertexShaderId: Int = 0
    private var fragmentShaderId: Int = 0
    
    // Attribute handles
    var positionHandle: Int = 0
        private set
    var textureCoordHandle: Int = 0
        private set
    
    // Uniform handles
    var textureHandle: Int = 0
        private set
    var externalTextureHandle: Int = 0
        private set
    var targetsHandle: Int = 0
        private set
    var replacementsHandle: Int = 0
        private set
    var tolerancesHandle: Int = 0
        private set
    var activeCountHandle: Int = 0
        private set
    var useExternalTextureHandle: Int = 0
        private set
    var showMaskHandle: Int = 0
        private set
    var useHSVHandle: Int = 0
        private set
    var edgeSmoothHandle: Int = 0
        private set
    
    // Vertex buffer
    private val vertexBuffer: FloatBuffer
    
    init {
        // Initialize vertex buffer
        vertexBuffer = ByteBuffer
            .allocateDirect(VERTEX_DATA.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(VERTEX_DATA)
        vertexBuffer.position(0)
    }
    
    /**
     * Initialize shader program with vertex and fragment shader source
     */
    fun initialize(vertexShaderSource: String, fragmentShaderSource: String): Boolean {
        // Compile shaders
        vertexShaderId = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        if (vertexShaderId == 0) {
            Log.e(TAG, "Failed to compile vertex shader")
            return false
        }
        
        fragmentShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)
        if (fragmentShaderId == 0) {
            Log.e(TAG, "Failed to compile fragment shader")
            return false
        }
        
        // Link program
        programId = linkProgram(vertexShaderId, fragmentShaderId)
        if (programId == 0) {
            Log.e(TAG, "Failed to link shader program")
            return false
        }
        
        // Get attribute and uniform handles
        getHandles()
        
        Log.d(TAG, "Shader program initialized successfully")
        return true
    }
    
    /**
     * Compile a shader from source code
     */
    private fun compileShader(type: Int, source: String): Int {
        val shaderId = GLES20.glCreateShader(type)
        if (shaderId == 0) {
            Log.e(TAG, "Failed to create shader of type $type")
            return 0
        }
        
        GLES20.glShaderSource(shaderId, source)
        GLES20.glCompileShader(shaderId)
        
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        
        if (compileStatus[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shaderId)
            Log.e(TAG, "Shader compilation error: $error")
            GLES20.glDeleteShader(shaderId)
            return 0
        }
        
        return shaderId
    }
    
    /**
     * Link vertex and fragment shaders into a program
     */
    private fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
        val programId = GLES20.glCreateProgram()
        if (programId == 0) {
            Log.e(TAG, "Failed to create program")
            return 0
        }
        
        GLES20.glAttachShader(programId, vertexShaderId)
        GLES20.glAttachShader(programId, fragmentShaderId)
        GLES20.glLinkProgram(programId)
        
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(programId)
            Log.e(TAG, "Program link error: $error")
            GLES20.glDeleteProgram(programId)
            return 0
        }
        
        return programId
    }
    
    /**
     * Get attribute and uniform handles
     */
    private fun getHandles() {
        // Attributes
        positionHandle = GLES20.glGetAttribLocation(programId, ATTRIBUTE_POSITION)
        textureCoordHandle = GLES20.glGetAttribLocation(programId, ATTRIBUTE_TEXTURE_COORD)
        
        // Uniforms
        textureHandle = GLES20.glGetUniformLocation(programId, UNIFORM_TEXTURE)
        externalTextureHandle = GLES20.glGetUniformLocation(programId, UNIFORM_EXTERNAL_TEXTURE)
        targetsHandle = GLES20.glGetUniformLocation(programId, UNIFORM_TARGETS)
        replacementsHandle = GLES20.glGetUniformLocation(programId, UNIFORM_REPLACEMENTS)
        tolerancesHandle = GLES20.glGetUniformLocation(programId, UNIFORM_TOLERANCES)
        activeCountHandle = GLES20.glGetUniformLocation(programId, UNIFORM_ACTIVE_COUNT)
        useExternalTextureHandle = GLES20.glGetUniformLocation(programId, UNIFORM_USE_EXTERNAL_TEXTURE)
        showMaskHandle = GLES20.glGetUniformLocation(programId, UNIFORM_SHOW_MASK)
        useHSVHandle = GLES20.glGetUniformLocation(programId, UNIFORM_USE_HSV)
        edgeSmoothHandle = GLES20.glGetUniformLocation(programId, UNIFORM_EDGE_SMOOTH)
    }
    
    /**
     * Use this shader program
     */
    fun use() {
        GLES20.glUseProgram(programId)
    }
    
    /**
     * Bind vertex attributes
     */
    fun bindVertexAttributes() {
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle,
            POSITION_COMPONENT_COUNT,
            GLES20.GL_FLOAT,
            false,
            STRIDE,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        vertexBuffer.position(POSITION_COMPONENT_COUNT)
        GLES20.glVertexAttribPointer(
            textureCoordHandle,
            TEXTURE_COORD_COMPONENT_COUNT,
            GLES20.GL_FLOAT,
            false,
            STRIDE,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(textureCoordHandle)
    }
    
    /**
     * Draw the full-screen quad
     */
    fun draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
    
    /**
     * Release shader resources
     */
    fun release() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
        }
        if (vertexShaderId != 0) {
            GLES20.glDeleteShader(vertexShaderId)
            vertexShaderId = 0
        }
        if (fragmentShaderId != 0) {
            GLES20.glDeleteShader(fragmentShaderId)
            fragmentShaderId = 0
        }
    }
    
    /**
     * Check for OpenGL errors
     */
    fun checkGLError(operation: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL error in $operation: $error")
        }
    }
}
