
package com.example.clipboxclosead

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdCloseAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val targetPackage = "org.clipbox.player"

    private var scanUntil = 0L
    private var lastClickAt = 0L

    private val rescan = object : Runnable {
        override fun run() {
            if (System.currentTimeMillis() > scanUntil) return

            scanAndClick()

            if (System.currentTimeMillis() <= scanUntil) {
                handler.postDelayed(this, 250L)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Never start scans from another app's events.
        if (event.packageName?.toString() != targetPackage) {
            scanUntil = 0L
            handler.removeCallbacks(rescan)
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                scanUntil = System.currentTimeMillis() + 12_000L
                handler.removeCallbacks(rescan)
                handler.post(rescan)
            }
        }
    }

    override fun onInterrupt() {
        scanUntil = 0L
        handler.removeCallbacks(rescan)
    }

    override fun onDestroy() {
        scanUntil = 0L
        handler.removeCallbacks(rescan)
        super.onDestroy()
    }

    private fun scanAndClick() {
        if (System.currentTimeMillis() > scanUntil) return
        if (System.currentTimeMillis() - lastClickAt < 1_000L) return

        val root = rootInActiveWindow ?: return

        try {
            // Critical safety check: the active window must be ClipBox.
            if (root.packageName?.toString() != targetPackage) return

            val screen = Rect(
                0,
                0,
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels
            )

            val candidates = mutableListOf<AccessibilityNodeInfo>()

            try {
                collectCandidates(root, screen, candidates)

                val chosen = candidates.firstOrNull { node ->
                    node.isEnabled &&
                        node.isClickable &&
                        isCloseButton(node, screen)
                }

                if (chosen != null &&
                    chosen.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ) {
                    lastClickAt = System.currentTimeMillis()
                }
            } finally {
                candidates.forEach { it.recycle() }
            }
        } finally {
            root.recycle()
        }
    }

    private fun isCloseButton(
        node: AccessibilityNodeInfo,
        screen: Rect
    ): Boolean {
        val label = (
            (node.text?.toString() ?: "") + " " +
            (node.contentDescription?.toString() ?: "") + " " +
            (node.viewIdResourceName ?: "")
        ).lowercase().trim()

        val explicitCloseLabel = listOf(
            "close", "dismiss", "skip ad", "close ad", "cancel"
        ).any { label.contains(it) }

        val xLabel = label in setOf(
            "x", "×", "✕", "✖", "close_button", "iv_close", "btn_close"
        )

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val topRight =
            bounds.right >= screen.width() * 0.65f &&
            bounds.top <= screen.height() * 0.25f &&
            bounds.width() <= screen.width() * 0.25f &&
            bounds.height() <= screen.height() * 0.20f

        return node.isVisibleToUser &&
            topRight &&
            (explicitCloseLabel || xLabel)
    }

    private fun collectCandidates(
        node: AccessibilityNodeInfo,
        screen: Rect,
        output: MutableList<AccessibilityNodeInfo>
    ) {
        if (!node.isVisibleToUser) return

        if (node.isEnabled &&
            node.isClickable &&
            isCloseButton(node, screen)
        ) {
            output.add(AccessibilityNodeInfo.obtain(node))
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            try {
                collectCandidates(child, screen, output)
            } finally {
                child.recycle()
            }
        }
    }
}
