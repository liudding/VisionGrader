package com.linkstar.visiongrader.ui.scanner

import android.graphics.Bitmap

data class ScannedImages(val origin: Bitmap,
                         val cropped: Bitmap,
                         val twoPage: Boolean,
                         val left: Bitmap? = null,
                         val right: Bitmap? = null)
