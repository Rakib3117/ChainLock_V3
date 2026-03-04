package com.chainlock.app
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AccessibilityLockService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == "com.chainlock.app" || pkg == "com.android.systemui" || pkg == "android") return

        val chains = ChainStorage.loadChains(this)
        val progress = ChainStorage.loadProgress(this)

        for (chain in chains) {
            if (!chain.isActive) continue

            if (chain.isCyclic) {
                // Cyclic mode: each app unlocks the next, last unlocks first
                for ((i, step) in chain.steps.withIndex()) {
                    if (step.packageName == pkg) {
                        val prevIndex = if (i == 0) chain.steps.size - 1 else i - 1
                        val prev = chain.steps[prevIndex]
                        val usedMs = progress[prev.packageName] ?: 0L
                        val reqMs = prev.requiredMinutes * 60 * 1000L
                        if (usedMs < reqMs) {
                            showBlockScreen(step, prev, usedMs, reqMs)
                            return
                        }
                    }
                }
            } else {
                // Serial mode
                val unlocked = ChainStorage.getUnlockedStep(this, chain.id)
                for (i in unlocked + 1 until chain.steps.size) {
                    val step = chain.steps[i]
                    if (step.packageName == pkg) {
                        val prev = chain.steps[i - 1]
                        val usedMs = progress[prev.packageName] ?: 0L
                        val reqMs = prev.requiredMinutes * 60 * 1000L
                        if (usedMs < reqMs) {
                            showBlockScreen(step, prev, usedMs, reqMs)
                            return
                        } else {
                            ChainStorage.setUnlockedStep(this, chain.id, i)
                        }
                    }
                }
            }
        }
    }

    private fun showBlockScreen(step: ChainStep, prev: ChainStep, usedMs: Long, reqMs: Long) {
        val intent = Intent(this, BlockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("locked_app", step.appName)
            putExtra("unlock_app", prev.appName)
            putExtra("remaining_minutes", ((reqMs - usedMs) / 60000).toInt() + 1)
            putExtra("required_minutes", prev.requiredMinutes)
            putExtra("used_ms", usedMs)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}
