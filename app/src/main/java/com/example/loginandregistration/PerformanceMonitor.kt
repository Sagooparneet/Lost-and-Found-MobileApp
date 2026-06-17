package com.example.loginandregistration

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer

/**
 * Performance monitoring utility for debugging and testing.
 * This class helps track frame drops and main thread blocking.
 * 
 * Usage in debug builds only:
 * ```
 * if (BuildConfig.DEBUG) {
 *     PerformanceMonitor.startMonitoring()
 * }
 * ```
 */
object PerformanceMonitor {
    
    const val TAG = "PerformanceMonitor"
    const val FRAME_THRESHOLD_MS = 16 // 60 FPS = 16ms per frame
    private const val WARNING_THRESHOLD_MS = 32 // 2 frames
    
    private var isMonitoring = false
    private var frameCallback: Choreographer.FrameCallback? = null
    private var lastFrameTime = 0L
    private var droppedFrameCount = 0
    private var totalFrames = 0
    
    /**
     * Start monitoring frame performance.
     * Call this in Application.onCreate() or Activity.onCreate() for debug builds.
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "Performance monitoring already started")
            return
        }
        
        isMonitoring = true
        lastFrameTime = System.nanoTime()
        
        frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isMonitoring) return
                
                val currentTime = System.nanoTime()
                val frameDuration = (currentTime - lastFrameTime) / 1_000_000 // Convert to ms
                
                totalFrames++
                
                if (frameDuration > WARNING_THRESHOLD_MS) {
                    droppedFrameCount++
                    val droppedFrames = (frameDuration / FRAME_THRESHOLD_MS).toInt()
                    Log.w(
                        TAG,
                        "Frame drop detected! Duration: ${frameDuration}ms (~$droppedFrames frames dropped)"
                    )
                }
                
                lastFrameTime = currentTime
                
                // Schedule next frame
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        
        Choreographer.getInstance().postFrameCallback(frameCallback!!)
        Log.i(TAG, "Performance monitoring started")
    }
    
    /**
     * Stop monitoring frame performance.
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            Log.w(TAG, "Performance monitoring not started")
            return
        }
        
        isMonitoring = false
        frameCallback?.let {
            Choreographer.getInstance().removeFrameCallback(it)
        }
        
        Log.i(TAG, "Performance monitoring stopped")
        logSummary()
    }
    
    /**
     * Log a summary of performance metrics.
     */
    fun logSummary() {
        val dropRate = if (totalFrames > 0) {
            (droppedFrameCount.toFloat() / totalFrames * 100)
        } else {
            0f
        }
        
        Log.i(TAG, "=== Performance Summary ===")
        Log.i(TAG, "Total frames: $totalFrames")
        Log.i(TAG, "Dropped frames: $droppedFrameCount")
        Log.i(TAG, "Drop rate: ${"%.2f".format(dropRate)}%")
        Log.i(TAG, "===========================")
    }
    
    /**
     * Reset performance counters.
     */
    fun reset() {
        droppedFrameCount = 0
        totalFrames = 0
        lastFrameTime = System.nanoTime()
        Log.i(TAG, "Performance counters reset")
    }
    
    /**
     * Check if an operation is running on the main thread.
     * Logs a warning if called from main thread.
     */
    fun checkMainThread(operationName: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w(TAG, "⚠️ WARNING: $operationName is running on MAIN THREAD!")
            
            // Log stack trace to help identify the source
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val stackTrace = Thread.currentThread().stackTrace
                    .take(10)
                    .joinToString("\n") { "  at $it" }
                Log.w(TAG, "Stack trace:\n$stackTrace")
            }
        } else {
            Log.d(TAG, "✓ $operationName is running on background thread")
        }
    }
    
    /**
     * Measure the execution time of a block of code.
     */
    inline fun <T> measureTime(operationName: String, block: () -> T): T {
        val startTime = System.nanoTime()
        val result = block()
        val duration = (System.nanoTime() - startTime) / 1_000_000 // Convert to ms
        
        if (duration > FRAME_THRESHOLD_MS) {
            Log.w(TAG, "⚠️ $operationName took ${duration}ms (> ${FRAME_THRESHOLD_MS}ms threshold)")
        } else {
            Log.d(TAG, "✓ $operationName completed in ${duration}ms")
        }
        
        return result
    }
    
    /**
     * Log current thread information.
     */
    fun logThreadInfo(context: String) {
        val thread = Thread.currentThread()
        val isMainThread = Looper.myLooper() == Looper.getMainLooper()
        
        Log.d(TAG, "[$context] Thread: ${thread.name}, Main: $isMainThread")
    }
}
