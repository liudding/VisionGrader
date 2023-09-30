package com.linkstar.visiongrader.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.MainThread
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Paths
import java.time.Instant

object Utils {

    private var applicationCtx: Context?= null

    @MainThread
    fun init(ctx: Context) {
        if (applicationCtx != null) {
            return
        }
        this.applicationCtx = ctx.applicationContext
    }

    fun toMap(vararg params: String): Map<String, String> {
        val map = HashMap<String, String>()
        var i = 0
        while (i < params.size) {
            map[params[i]] = params[i + 1]
            i += 2
        }
        return map
    }

    fun getExternalFilesDir(dir: String?): String {
        return applicationCtx?.getExternalFilesDir(dir)?.absolutePath ?: return ""
    }

    fun joinPath(vararg parts: String): String {
        return Paths.get("", *parts).toString()
    }

    fun ensurePathExists(path: String) {
        val directory = File(path)

        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

     fun generateFilename(): String {
        val filename = "${Instant.now().toEpochMilli()}${(0..999).random()}"
        return getExternalFilesDir("pics") + File.separator + "$filename.png"
    }

     fun saveImage(image: Bitmap, filepath: String) {
        try {
            val outputStream = FileOutputStream(File(filepath))
            image.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}