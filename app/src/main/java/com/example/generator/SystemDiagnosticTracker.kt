package com.example.generator

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticLog(
    val timestamp: Long,
    val severity: String,
    val tag: String,
    val message: String
)

object SystemDiagnosticTracker {
    private val _logs = MutableStateFlow<List<DiagnosticLog>>(emptyList())
    val logs: StateFlow<List<DiagnosticLog>> = _logs.asStateFlow()

    fun addLog(tag: String, message: String, severity: String = "INFO") {
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(DiagnosticLog(System.currentTimeMillis(), severity, tag, message))
        _logs.value = currentLogs
    }

    fun getLogs(): List<String> {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
        return _logs.value.map { "[${sdf.format(Date(it.timestamp))}] [${it.severity}] [${it.tag}] ${it.message}" }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    suspend fun runFullSystemAudit(context: Context, force: Boolean = false): String {
        val sb = java.lang.StringBuilder()
        sb.appendLine("=== تقرير الفحص الشامل لعملية إنشاء الفيديو ===")
        val allLogs = getLogs()
        if (allLogs.isEmpty()) {
            sb.appendLine("لا توجد سجلات حالية للعملية.")
        } else {
            allLogs.forEach { sb.appendLine(it) }
        }
        return sb.toString()
    }

    fun saveReportToFilesAndGetPath(context: Context, extraData: String = ""): String {
        return ""
    }
}
