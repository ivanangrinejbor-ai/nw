package org.catrobat.catroid.util

object Logger {
    fun d(tag: String, msg: String) {
        println("[$tag] DEBUG: $msg")
    }

    fun i(tag: String, msg: String) {
        println("[$tag] INFO: $msg")
    }

    fun e(tag: String, msg: String) {
        System.err.println("[$tag] ERROR: $msg")
    }

    fun e(tag: String, msg: String, tr: Throwable) {
        System.err.println("[$tag] ERROR: $msg")
        tr.printStackTrace()
    }
}
