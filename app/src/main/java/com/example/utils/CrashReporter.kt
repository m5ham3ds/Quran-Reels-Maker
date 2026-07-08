package com.example.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashReporter(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            saveCrashLog(thread, exception)
        } catch (e: Exception) {
            Log.e("CrashReporter", "Error saving crash log", e)
        }
        
        defaultHandler?.uncaughtException(thread, exception)
    }

    private fun saveCrashLog(thread: Thread, exception: Throwable) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "crash_$timeStamp.txt"
        
        // Attempt to save to Movies/Quran Reels/ERROR
        try {
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val appFolder = File(moviesDir, "Quran Reels")
            val errorFolder = File(appFolder, "ERROR")
            
            if (!errorFolder.exists()) {
                errorFolder.mkdirs()
            }
            
            val externalCrashFile = File(errorFolder, fileName)
            writeCrashLog(externalCrashFile, thread, exception)
        } catch (e: Exception) {
            Log.e("CrashReporter", "Failed to write crash log to external storage", e)
        }
        
        // Fallback: Always save to internal app data /files/ERROR
        try {
            val internalErrorFolder = File(context.filesDir, "ERROR")
            if (!internalErrorFolder.exists()) {
                internalErrorFolder.mkdirs()
            }
            val internalCrashFile = File(internalErrorFolder, fileName)
            writeCrashLog(internalCrashFile, thread, exception)
        } catch (e: Exception) {
            Log.e("CrashReporter", "Failed to write crash log to internal storage", e)
        }
    }
    
    private fun writeCrashLog(file: File, thread: Thread, exception: Throwable) {
        PrintWriter(FileWriter(file)).use { writer ->
            writer.println("=== Quran Reels Crash Report ===")
            writer.println("Time: ${Date()}")
            writer.println("Thread: ${thread.name} (ID: ${thread.id})")
            writer.println("Exception: ${exception.javaClass.name}")
            writer.println("Message: ${exception.message}")
            writer.println()
            writer.println("--- Stack Trace ---")
            exception.printStackTrace(writer)
            writer.println()
            
            var cause = exception.cause
            while (cause != null) {
                writer.println("--- Cause: ${cause.javaClass.name} ---")
                writer.println("Message: ${cause.message}")
                cause.printStackTrace(writer)
                cause = cause.cause
            }
            
            writer.println()
            writer.println("--- Device Info ---")
            writer.println("OS Version: ${System.getProperty("os.version")} (${android.os.Build.VERSION.INCREMENTAL})")
            writer.println("OS API Level: ${android.os.Build.VERSION.SDK_INT}")
            writer.println("Device: ${android.os.Build.DEVICE}")
            writer.println("Model: ${android.os.Build.MODEL}")
            writer.println("Product: ${android.os.Build.PRODUCT}")
        }
    }
    
    companion object {
        fun initialize(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (defaultHandler !is CrashReporter) {
                Thread.setDefaultUncaughtExceptionHandler(CrashReporter(context.applicationContext, defaultHandler))
            }
        }
    }
}
