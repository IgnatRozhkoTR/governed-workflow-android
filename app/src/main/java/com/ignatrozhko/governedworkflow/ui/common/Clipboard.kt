package com.ignatrozhko.governedworkflow.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

fun readClipboardText(context: Context): String {
    val clip = context.getSystemService(ClipboardManager::class.java).primaryClip ?: return ""
    if (clip.itemCount == 0) return ""
    return clip.getItemAt(0).coerceToText(context).toString()
}

fun writeClipboardText(context: Context, label: String, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
}
