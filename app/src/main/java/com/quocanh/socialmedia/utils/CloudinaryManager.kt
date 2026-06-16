package com.quocanh.socialmedia.utils

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

object CloudinaryManager {
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        
        val config = mapOf(
            "cloud_name" to "dlbo8b4hh",
            "api_key" to "615746888581335",
            "api_secret" to "bC3Moxfqq5UGB2Bo8ekPI8vk54o"
        )
        MediaManager.init(context, config)
        isInitialized = true
    }

    fun uploadImage(
        uri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        MediaManager.get().upload(uri)
            .option("folder", "avatars")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val url = resultData?.get("secure_url") as? String
                    if (url != null) {
                        onSuccess(url)
                    } else {
                        onError("Không lấy được URL ảnh")
                    }
                }
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onError(error?.description ?: "Lỗi upload ảnh")
                }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }
}
