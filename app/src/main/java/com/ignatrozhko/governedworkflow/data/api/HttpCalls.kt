package com.ignatrozhko.governedworkflow.data.api

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class HttpResult(val code: Int, val body: String, val etag: String?)

/** Executes the call without blocking a coroutine thread; cancelling the coroutine cancels the call. */
internal suspend fun Call.awaitResult(): HttpResult = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val result = response.use { HttpResult(it.code, it.body?.string().orEmpty(), it.header("ETag")) }
                continuation.resume(result)
            } catch (e: IOException) {
                continuation.resumeWithException(e)
            }
        }
    })
}
