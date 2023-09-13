package com.obrekht.statsview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<StatsView>(R.id.stats).data = listOf(
            1.5F,
            1F,
            -0.5F,
            1F
        )
    }
}