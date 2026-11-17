package com.ignatrozhko.governedworkflow.data.api

import java.io.IOException

/** Failures talking to the admin panel; [message] is always user-presentable. */
sealed class PanelException(message: String, cause: Throwable? = null) : Exception(message, cause)

class AuthRequiredException(message: String) : PanelException(message)

class HttpStatusException(val status: Int, val code: String, message: String) : PanelException(message)

class PanelUnreachableException(message: String, cause: IOException) : PanelException(message, cause)

class MalformedResponseException(message: String, cause: Throwable) : PanelException(message, cause)

class NotConnectedException : PanelException("Not connected to an admin panel.")
