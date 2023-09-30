package com.linkstar.visiongrader.utils

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.linkstar.visiongrader.VisionGraderApplication
import com.open.camera.utils.SDCardUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object ImageFileUtil {

    const val TAG = "FileUtils"

    @SuppressLint("SimpleDateFormat")
    private val dateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss") // 日志的输出格式 10

    private fun getName(type: Int): String { //1包裹图片，2取件人图片3.取件人和包裹合成图片
        return when (type) {
            0 -> {
                "-p"
            }
            1 -> {
                "-j"
            }
            else -> {
                "-w"
            }
        }
    }

    val time: String
        get() = dateFormat.format(Date())

    private fun getFileName(int: Int): String {
        return if (int == 0) {
            "Picture"
        } else {
            "Picture"
        }

    }

    private fun getFileNameType(int: Int): String {
        return when (int) {
            0 -> {
                ".png"
            }
            1 -> {
                ".jpg"
            }
            else -> {
                ".webp"
            }
        }

    }


    /**
     * 保存bitmap到本地
     * @param mBitmap 图片
     * @param compressSize 压缩大小0-100
     * @return
     */
    fun saveBitmap(
        mBitmap: Bitmap?,
        fileName: String,
        type: Int,
        time: String,
        compressSize: Int
    ): String {

        try {
            val savePath = SDCardUtils.getSDMouthPath(VisionGraderApplication.getApplication(), getFileName(type))
            if (!savePath.exists())
                return ""

            val filePic = File(savePath, getFileName(fileName, time, type))
            if (!filePic.exists()) {
                filePic.createNewFile()
            }
//            LogUtils.i(TAG, filePic.path)
            val fos = FileOutputStream(filePic)
            when (type) {
                0 -> {
                    mBitmap?.compress(Bitmap.CompressFormat.PNG, compressSize, fos)
                }
                1 -> {
                    mBitmap?.compress(Bitmap.CompressFormat.JPEG, compressSize, fos)
                }
                else -> {
                    mBitmap?.compress(Bitmap.CompressFormat.WEBP, compressSize, fos)
                }
            }
            fos.flush()
            fos.close()
            return filePic.path
        } catch (e: IOException) {

            e.printStackTrace()
        }
        return ""

    }

    private fun getFileName(
        fileName: String,
        time: String,
        type: Int
    ) = "$fileName-${time.replace(":", "_")}${getName(type)}${getFileNameType(type)}"

    fun mergeThumbnailTopBitmap(firstBitmap: Bitmap, secondBitmap: Bitmap): Bitmap {
        //以其中一张图片的大小作为画布的大小，或者也可以自己自定义
        val bitmap = Bitmap.createBitmap(
            firstBitmap.width, firstBitmap
                .height * 2, firstBitmap.config
        )
        //生成画布
        val canvas = Canvas(bitmap)
        //因为我传入的secondBitmap的大小是不固定的，所以我要将传来的secondBitmap调整到和画布一样的大小
        val w = firstBitmap.width.toFloat()
        val h = firstBitmap.height.toFloat()
        val m = Matrix()
        //确定secondBitmap大小比例
        m.setScale(w / secondBitmap.width, h / secondBitmap.height)
        //给画笔设定透明值，想将哪个图片进行透明化，就将画笔用到那张图片上
        canvas.drawBitmap(firstBitmap, 0f, 0f, null)
        canvas.drawBitmap(secondBitmap, 0f, h, null)
        return bitmap
    }

    fun mergeThumbnailLeftBitmap(firstBitmap: Bitmap, secondBitmap: Bitmap): Bitmap {
        //以其中一张图片的大小作为画布的大小，或者也可以自己自定义
        val bitmap = Bitmap.createBitmap(
            firstBitmap.width * 2, firstBitmap
                .height, firstBitmap.config
        )
        //生成画布
        val canvas = Canvas(bitmap)
        //因为我传入的secondBitmap的大小是不固定的，所以我要将传来的secondBitmap调整到和画布一样的大小
        val w = firstBitmap.width.toFloat()
        val h = firstBitmap.height.toFloat()
        val m = Matrix()
        //确定secondBitmap大小比例
        m.setScale(w / secondBitmap.width, h / secondBitmap.height)
        //给画笔设定透明值，想将哪个图片进行透明化，就将画笔用到那张图片上
        canvas.drawBitmap(firstBitmap, 0f, 0f, null)
        canvas.drawBitmap(secondBitmap, w, 0f, null)
        return bitmap
    }

    /**
     * @param fileName 文件名
     * @param context  上下文
     */
    fun openAssignFolder(fileName: String, context: Context) {
        val file = SDCardUtils.getSDMouthPath(VisionGraderApplication.getApplication(), fileName)

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        intent.setDataAndType(uri, "file/*")
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }


    /**
     * 调用系统安装apk
     *
     * @param apkPath 文件路径
     */
    fun installApk(context: Context, apkPath: String) {
        val file = File(apkPath)

        try {
            /**
             * provider
             * 处理android 7.0 及以上系统安装异常问题
             */
            val install = Intent()
            install.action = Intent.ACTION_VIEW
            install.addCategory(Intent.CATEGORY_DEFAULT)
            install.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "com.face.sweepplus.fileprovider",
                    file
                ) //在AndroidManifest中的android:authorities值
//                LogUtils.d("======", "file=${file.path}")
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) //添加这一句表示对目标应用临时授权该Uri所代表的文件
                install.setDataAndType(apkUri, "application/vnd.android.package-archive")
            } else {
                install.setDataAndType(
                    Uri.fromFile(file),
                    "application/vnd.android.package-archive"
                )
            }
            context.startActivity(install)
        } catch (e: Exception) {
            e.printStackTrace()

        }
    }


}