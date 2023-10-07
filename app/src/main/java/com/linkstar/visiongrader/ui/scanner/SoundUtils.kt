package com.linkstar.visiongrader.ui.scanner

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat.getSystemService
import com.jiangdg.ausbc.utils.ToastUtils
import com.linkstar.visiongrader.R
import kotlin.properties.Delegates


object SoundUtils {

    private var applicationCtx: Context?= null

    private var beepId by Delegates.notNull<Int>()
    private var beepBrightId by Delegates.notNull<Int>()

    private lateinit var soundPool: SoundPool

    @MainThread
    fun init(ctx: Context) {
        if (applicationCtx != null) {
            return
        }
        this.applicationCtx = ctx.applicationContext

//        beepPlayer = MediaPlayer.create(this.applicationCtx, R.raw.beep_two_tone_basic)
//        beepBrightPlayer = MediaPlayer.create(this.applicationCtx, R.raw.beep_bright)

        soundPool = SoundPool.Builder().setMaxStreams(2).build()
        beepId = soundPool.load(this.applicationCtx, R.raw.beep_two_tone_basic, 1)
        beepBrightId = soundPool.load(this.applicationCtx, R.raw.beep_bright, 1)


        // 下面的代码，测试下来无效。在连接 otg 时，无法将音频转到内置扬声器输出

        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            audioManager.isSpeakerphoneOn = true
        } else {
            audioManager.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                ?.let {
                    audioManager.setCommunicationDevice(it)
                }
        }
    }



    @JvmStatic
    fun playBeep() {
        applicationCtx?.let {
            soundPool.play(beepId,1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    @JvmStatic
    fun playBeepBright() {
        applicationCtx?.let {
            soundPool.play(beepBrightId,1.0f, 1.0f, 1, 0, 1.0f)
        }
    }


    @JvmStatic
    fun release() {
        applicationCtx?.let {
            soundPool.release()
        }

    }

}