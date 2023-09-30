package com.open.camera.utils

import android.content.Context
import android.os.Build
import android.os.StatFs
import android.os.storage.StorageManager
import android.text.TextUtils
import java.lang.reflect.Array
import java.lang.reflect.InvocationTargetException

object StorageUtils {
    /**
     * @param is_removale true 外置
     * false 内置
     * 获取外置/内置内存卡的地址
     */
    private fun getStoragePath(
        mContext: Context,
        is_removale: Boolean
    ): String? {
        val mStorageManager =
            mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        var storageVolumeClazz: Class<*>? = null
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
            val getVolumeList =
                mStorageManager.javaClass.getMethod("getVolumeList")
            val getPath = storageVolumeClazz.getMethod("getPath")
            val isRemovable = storageVolumeClazz.getMethod("isRemovable")
            val result = getVolumeList.invoke(mStorageManager)
            val length = Array.getLength(result)
            for (i in 0 until length) {
                val storageVolumeElement = Array.get(result, i)
                val path = getPath.invoke(storageVolumeElement) as String
                val removable =
                    isRemovable.invoke(storageVolumeElement) as Boolean
                if (is_removale == removable) {
                    return path
                }
            }
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * @param removale true 外置
     * false 内置
     *
     * 获取剩余内存
     */
    fun readSDCard(
        context: Context,
        removale: Boolean
    ): Int { //1内存大于80%，2小余80%，其它没有外存卡
        try {
            val storagePath = getStoragePath(context, removale)
            if (!TextUtils.isEmpty(storagePath)) {
                val sf = StatFs(storagePath)
                val blockSize: Long
                val blockCount: Long
                val availCount: Long
                if (Build.VERSION.SDK_INT > 18) {
                    blockSize = sf.blockSizeLong //文件存储时每一个存储块的大小为4KB
                    blockCount = sf.blockCountLong //存储区域的存储块的总个数
                    availCount = sf.availableBlocksLong //存储区域中可用的存储块的个数（剩余的存储大小）
                } else {
                    blockSize = sf.blockSize.toLong()
                    blockCount = sf.blockCount.toLong()
                    availCount = sf.availableBlocks.toLong()
                }
//                LogUtils.d(
//                    "readSDCard",
//                    "${availCount * blockSize}===${blockSize * blockCount} ==${availCount * blockSize * 100 / (blockSize * blockCount)}"
//                )
                return if (availCount * blockSize * 100 / (blockSize * blockCount) < 20) -3 else 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return -2
    }
}