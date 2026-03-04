package com.chainlock.app
import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class UsageTrackerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val CH = "chainlock_channel"
    private val ticker = object : Runnable {
        override fun run() { updateProgress(); handler.postDelayed(this, 30_000L) }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1, buildNotif())
        handler.post(ticker)
    }

    private fun updateProgress() {
        val um = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val stats = um.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, now)
        val progress = ChainStorage.loadProgress(this).toMutableMap()
        for (s in stats) if (s.totalTimeInForeground > 0) progress[s.packageName] = s.totalTimeInForeground
        ChainStorage.saveProgress(this, progress)

        for (chain in ChainStorage.loadChains(this)) {
            if (!chain.isActive || chain.isCyclic) continue
            val cur = ChainStorage.getUnlockedStep(this, chain.id)
            if (cur < chain.steps.size - 1) {
                val step = chain.steps[cur]
                if ((progress[step.packageName] ?: 0L) >= step.requiredMinutes * 60 * 1000L) {
                    ChainStorage.setUnlockedStep(this, chain.id, cur + 1)
                    notify("Unlocked: " + chain.steps[cur + 1].appName)
                }
            }
        }
    }

    private fun notify(title: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), NotificationCompat.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(title).setContentText("You can now open it!").setAutoCancel(true).build())
    }

    private fun buildNotif() = NotificationCompat.Builder(this, CH)
        .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
        .setContentTitle("ChainLock Active").setContentText("Monitoring...").build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(CH, "ChainLock", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }
}
