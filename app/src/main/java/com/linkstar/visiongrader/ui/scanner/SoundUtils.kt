package com.linkstar.visiongrader.ui.scanner

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import android.widget.Toast
import androidx.annotation.MainThread
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