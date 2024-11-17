package com.ap.neighborrentapplication.ui.activity

import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ap.neighborrentapplication.R

open class BaseActivity : AppCompatActivity() {

    protected fun setupToolbar(title: String, showBack: Boolean = true) {
        // Inflate custom toolbar
        layoutInflater.inflate(R.layout.custom_toolbar, findViewById(android.R.id.content), true)

        // Set toolbar title
        findViewById<TextView>(R.id.toolbarTitle).text = title

        // Setup back button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        if (showBack) {
            backButton.apply {
                visibility = android.view.View.VISIBLE
                setOnClickListener {
                    // Apply rotation animation
                    startAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate_back))
                    // Delay finish() slightly to show animation
                    postDelayed({ finish() }, 200)
                }
            }
        } else {
            backButton.visibility = android.view.View.GONE
        }
    }

}
