package com.obrekht.statsview

import android.content.Context
import kotlin.math.ceil

fun Int.dp(context: Context): Int = ceil(context.resources.displayMetrics.density * this).toInt()