package com.example.loginandregistration

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

/**
 * Glide configuration module for optimized image loading
 * Enables disk and memory caching for better performance
 */
@GlideModule
class GlideConfiguration : AppGlideModule() {
    
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Configure memory cache (40MB for better performance)
        val memoryCacheSizeBytes = 1024 * 1024 * 40 // 40MB
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes.toLong()))
        
        // Configure disk cache (200MB for more cached images)
        val diskCacheSizeBytes = 1024 * 1024 * 200 // 200MB
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes.toLong()))
        
        // Set default request options with aggressive caching
        builder.setDefaultRequestOptions(
            RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original and resized
                .skipMemoryCache(false) // Always use memory cache
        )
        
        // Set log level to ERROR only to reduce logcat noise
        builder.setLogLevel(android.util.Log.ERROR)
    }
    
    // Disable manifest parsing for better performance
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}
