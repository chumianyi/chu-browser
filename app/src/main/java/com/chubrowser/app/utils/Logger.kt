package com.chubrowser.app.utils

import android.util.Log

object Logger {

    private const val TAG = "ChuBrowser"
    private var isDebugEnabled = true

    fun setDebugEnabled(enabled: Boolean) {
        isDebugEnabled = enabled
    }

    fun d(message: String) {
        if (isDebugEnabled) {
            Log.d(TAG, message)
        }
    }

    fun d(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.d("$TAG/$tag", message)
        }
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun i(tag: String, message: String) {
        Log.i("$TAG/$tag", message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }

    fun w(tag: String, message: String) {
        Log.w("$TAG/$tag", message)
    }

    fun w(message: String, throwable: Throwable) {
        Log.w(TAG, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable) {
        Log.w("$TAG/$tag", message, throwable)
    }

    fun e(message: String) {
        Log.e(TAG, message)
    }

    fun e(tag: String, message: String) {
        Log.e("$TAG/$tag", message)
    }

    fun e(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable) {
        Log.e("$TAG/$tag", message, throwable)
    }

    fun v(message: String) {
        if (isDebugEnabled) {
            Log.v(TAG, message)
        }
    }

    fun v(tag: String, message: String) {
        if (isDebugEnabled) {
            Log.v("$TAG/$tag", message)
        }
    }
}
