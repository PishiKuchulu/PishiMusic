package com.tuned.app.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTextTest {
    @Test
    fun throwableFormatIncludesTheCauseChain() {
        val root = IllegalStateException("player closed")
        val wrapped = RuntimeException("resolve failed", root)
        val text = formatThrowable(wrapped)
        assertTrue(text.contains("RuntimeException: resolve failed"))
        assertTrue(text.contains("Caused by: java.lang.IllegalStateException: player closed"))
        assertTrue(text.contains("CrashReportTextTest"))
    }

    @Test
    fun threadContextNamesTheFailingThread() {
        val text = formatThreadContext(Thread.currentThread())
        assertTrue(text.contains("name=${Thread.currentThread().name}"))
        assertTrue(text.contains("state="))
        assertTrue(text.contains("id="))
    }

    @Test
    fun otherThreadsSkipTheCrashingThreadAndKeepAFrame() {
        val crashing = Thread("crash")
        val worker = Thread("worker")
        val traces = mapOf(
            crashing to arrayOf(StackTraceElement("com.tuned.app.Boom", "fail", "Boom.kt", 4)),
            worker to arrayOf(StackTraceElement("com.tuned.app.Player", "play", "Player.kt", 10)),
        )
        val text = formatOtherThreads(traces, skip = crashing)
        assertTrue(text.contains("name=worker"))
        assertTrue(text.contains("com.tuned.app.Player.play"))
        assertFalse(text.contains("name=crash"))
    }
}
