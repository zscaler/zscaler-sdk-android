package com.zscaler.sdk.demoapp

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtility {

    private const val TAG = "ZipUtility"

    fun createEmptyZipFile(context: Context, zipFileName: String): String? {
        return try {
            // Get the internal storage directory of the app
            val internalStorageDir = context.filesDir
            val zipFile = File(internalStorageDir, zipFileName)

            // Create a new empty zip file
            val fos = FileOutputStream(zipFile)
            val zos = ZipOutputStream(fos)

            // Add an empty entry to the zip file to avoid crash on Android 28 and below as empty zip cannot be created
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val entry = ZipEntry("empty_file.txt")
                zos.putNextEntry(entry)
                zos.closeEntry()
            }

            zos.close()

            // Log the URI of the created zip file
            Log.d(TAG, "Created ZIP file: ${zipFile.absolutePath}")

            // Return the URI of the created zip file
            zipFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ZIP file: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
