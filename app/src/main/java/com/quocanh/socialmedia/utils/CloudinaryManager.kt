package com.quocanh.socialmedia.utils

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
            .option("folder", "chat_images")
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

    fun uploadMultipleImages(
        uris: List<Uri>,
        onAllSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        val urls = mutableListOf<String>()
        var completed = 0
        val total = uris.size
        var hasError = false

        for (uri in uris) {
            uploadImage(
                uri = uri,
                onSuccess = { url ->
                    if (!hasError) {
                        urls.add(url)
                        completed++
                        if (completed == total) {
                            onAllSuccess(urls)
                        }
                    }
                },
                onError = { error ->
                    if (!hasError) {
                        hasError = true
                        onError(error)
                    }
                }
            )
        }
    }

    fun uploadFile(
        uri: Uri,
        fileName: String,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        MediaManager.get().upload(uri)
            .option("folder", "chat_files")
            .option("resource_type", "auto")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val url = resultData?.get("secure_url") as? String
                    if (url != null) {
                        onSuccess(url, fileName)
                    } else {
                        onError("Không lấy được URL file")
                    }
                }
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onError(error?.description ?: "Lỗi upload file")
                }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }
}
