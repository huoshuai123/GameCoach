package com.example.mahjongcoach.mjai

interface MjaiLogger {
    fun info(message: String)
    fun warn(message: String)
}

object StdoutMjaiLogger : MjaiLogger {
    override fun info(message: String) {
        println("MJAI: $message")
    }

    override fun warn(message: String) {
        println("MJAI warning: $message")
    }
}

fun String.truncatedForLog(maxLength: Int = 240): String {
    val singleLine = replace('\n', ' ').replace('\r', ' ')
    return if (singleLine.length <= maxLength) singleLine else singleLine.take(maxLength) + "..."
}
