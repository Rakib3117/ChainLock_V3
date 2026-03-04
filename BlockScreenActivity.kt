package com.chainlock.app
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.*

class BlockScreenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val lockedApp = intent.getStringExtra("locked_app") ?: "This App"
        val unlockApp = intent.getStringExtra("unlock_app") ?: "required app"
        val remainMins = intent.getIntExtra("remaining_minutes", 0)
        val reqMins = intent.getIntExtra("required_minutes", 1)
        val usedMs = intent.getLongExtra("used_ms", 0L)
        val usedMins = (usedMs / 60000).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0a0a0f"))
            setPadding(60, 60, 60, 60)
        }

        root.addView(TextView(this).apply { text = "LOCKED"; textSize = 72f; gravity = Gravity.CENTER; setTextColor(Color.parseColor("#ef4444")) })
        root.addView(Space(this).apply { minimumHeight = 16 })
        root.addView(TextView(this).apply { text = lockedApp; textSize = 28f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD })
        root.addView(Space(this).apply { minimumHeight = 40 })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            setPadding(40, 40, 40, 40)
            gravity = Gravity.CENTER
        }
        card.addView(TextView(this).apply { text = "Use $unlockApp first"; textSize = 16f; setTextColor(Color.parseColor("#a0a0c0")); gravity = Gravity.CENTER })
        card.addView(Space(this).apply { minimumHeight = 16 })
        card.addView(TextView(this).apply { text = "$usedMins / $reqMins min"; textSize = 22f; setTextColor(Color.parseColor("#ff6b35")); gravity = Gravity.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD })
        card.addView(TextView(this).apply { text = "$remainMins min remaining"; textSize = 14f; setTextColor(Color.parseColor("#6b6b8a")); gravity = Gravity.CENTER })
        card.addView(Space(this).apply { minimumHeight = 20 })
        card.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = reqMins; progress = usedMins.coerceAtMost(reqMins); minimumHeight = 16
            progressDrawable.setColorFilter(Color.parseColor("#7c3aed"), android.graphics.PorterDuff.Mode.SRC_IN)
        })

        root.addView(card)
        root.addView(Space(this).apply { minimumHeight = 40 })
        root.addView(Button(this).apply {
            text = "Go Back"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2a2a45"))
            textSize = 15f
            setOnClickListener { goHome() }
        })
        setContentView(root)
    }

    private fun goHome() {
        startActivity(android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    override fun onBackPressed() = goHome()
}
