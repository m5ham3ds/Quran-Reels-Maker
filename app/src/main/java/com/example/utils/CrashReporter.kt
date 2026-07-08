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
        
        val directoriesToTry = mutableListOf<File>()
        
        // 1. Android/data/com.../files/Documents/ERROR (Guaranteed write without permissions, accessible via USB)
        try {
            val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (docsDir != null) {
                directoriesToTry.add(File(docsDir, "ERROR"))
            }
        } catch (e: Exception) {}

        // 2. Android/data/com.../files/ERROR (Guaranteed write, accessible via USB)
        try {
            val extFilesDir = context.getExternalFilesDir(null)
            if (extFilesDir != null) {
                directoriesToTry.add(File(extFilesDir, "ERROR"))
            }
        } catch (e: Exception) {}
        
        // 3. Movies/Quran Reels/ERROR (Best for user visibility, but may fail due to scoped storage)
        try {
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val appFolder = File(moviesDir, "Quran Reels")
            directoriesToTry.add(File(appFolder, "ERROR"))
        } catch (e: Exception) {}
        
        // 4. Internal app data (Last resort, user might not find it)
        directoriesToTry.add(File(context.filesDir, "ERROR"))

        var writtenSuccessfully = false
        for (dir in directoriesToTry) {
            try {
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val crashFile = File(dir, fileName)
                writeCrashLog(crashFile, thread, exception)
                writtenSuccessfully = true
                Log.d("CrashReporter", "Successfully wrote crash log to ${crashFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("CrashReporter", "Failed to write crash log to ${dir.absolutePath}", e)
            }
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
            writer.println("--- System Diagnostic Logs ---")
            val logs = com.example.generator.SystemDiagnosticTracker.getLogs()
            if (logs.isEmpty()) {
                writer.println("No diagnostic logs found.")
            } else {
                for (log in logs) {
                    writer.println(log)
                }
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
