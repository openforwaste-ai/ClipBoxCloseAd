package com.example.clipboxclosead

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
        }

        val message = TextView(this).apply {
            text = """
                ClipBox Close Ad

                1. Enable the accessibility service.
                2. Open ClipBox.
                3. The service will look for an enabled close button.

                The service cannot click a disabled button or
                guarantee detection if the X is inaccessible.
            """.trimIndent()
            textSize = 17f
        }

        val button = Button(this).apply {
            text = "Open Accessibility Settings"

            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            }
        }

        layout.addView(message)
        layout.addView(button)

        setContentView(layout)
    }
}
