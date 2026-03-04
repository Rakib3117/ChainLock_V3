package com.chainlock.app
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private lateinit var chainList: LinearLayout
    private lateinit var statusTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0a0a0f"))
            setPadding(24, 48, 24, 48)
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "ChainLock"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Serial & Cyclic App Unlock"
            textSize = 13f
            setTextColor(Color.parseColor("#6b6b8a"))
            gravity = Gravity.CENTER
        })
        root.addView(Space(this).apply { minimumHeight = 24 })

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            setPadding(32, 24, 32, 24)
        }
        statusTv = TextView(this).apply { textSize = 13f; setTextColor(Color.parseColor("#a0a0c0")) }
        statusCard.addView(statusTv)
        statusCard.addView(Space(this).apply { minimumHeight = 12 })
        statusCard.addView(Button(this).apply {
            text = "Grant Permissions"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#7c3aed"))
            setOnClickListener { requestPerms() }
        })
        root.addView(statusCard)
        root.addView(Space(this).apply { minimumHeight = 20 })

        root.addView(Button(this).apply {
            text = "+ New Chain"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#ff6b35"))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 32, 0, 32)
            setOnClickListener { addChainDialog() }
        })
        root.addView(Space(this).apply { minimumHeight = 28 })
        root.addView(TextView(this).apply {
            text = "YOUR CHAINS"
            textSize = 11f
            setTextColor(Color.parseColor("#6b6b8a"))
        })
        root.addView(Space(this).apply { minimumHeight = 12 })

        chainList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(chainList)
        root.addView(Space(this).apply { minimumHeight = 20 })

        root.addView(Button(this).apply {
            text = "Reset Daily Progress"
            setTextColor(Color.parseColor("#6b6b8a"))
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            setOnClickListener {
                ChainStorage.resetDailyProgress(this@MainActivity)
                ChainStorage.loadChains(this@MainActivity).forEach {
                    ChainStorage.setUnlockedStep(this@MainActivity, it.id, 0)
                }
                Toast.makeText(this@MainActivity, "Reset!", Toast.LENGTH_SHORT).show()
                refreshList()
            }
        })
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        refreshList()
        startService(Intent(this, UsageTrackerService::class.java))
    }

    private fun updateStatus() {
        val a = isAccessOn()
        val u = isUsageOn()
        val aStr = if (a) "ON" else "OFF"
        val uStr = if (u) "ON" else "OFF"
        val activeStr = if (a && u) "ACTIVE" else "Grant permissions above"
        statusTv.text = "Accessibility: " + aStr + "\nUsage Stats: " + uStr + "\n\n" + activeStr
    }

    private fun isAccessOn(): Boolean {
        val svc = packageName + "/" + AccessibilityLockService::class.java.canonicalName
        val en = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return TextUtils.SimpleStringSplitter(':').apply { setString(en) }.any { it.equals(svc, true) }
    }

    private fun isUsageOn(): Boolean {
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            val ops = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
            ops.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, info.applicationInfo.uid, packageName) == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) { false }
    }

    private fun requestPerms() {
        when {
            !isUsageOn() -> { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); Toast.makeText(this, "Enable ChainLock in Usage Access", Toast.LENGTH_LONG).show() }
            !isAccessOn() -> { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); Toast.makeText(this, "Enable ChainLock in Accessibility", Toast.LENGTH_LONG).show() }
            else -> Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshList() {
        chainList.removeAllViews()
        val chains = ChainStorage.loadChains(this)
        val progress = ChainStorage.loadProgress(this)
        if (chains.isEmpty()) {
            chainList.addView(TextView(this).apply {
                text = "No chains yet.\nTap + New Chain!"
                textSize = 14f
                setTextColor(Color.parseColor("#6b6b8a"))
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 40)
            })
            return
        }
        for (chain in chains) {
            chainList.addView(buildCard(chain, progress))
            chainList.addView(Space(this).apply { minimumHeight = 16 })
        }
    }

    private fun buildCard(chain: AppChain, progress: Map<String, Long>): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            setPadding(28, 24, 28, 24)
        }

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(TextView(this).apply {
            text = chain.name
            textSize = 17f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val modeColor = if (chain.isCyclic) Color.parseColor("#f59e0b") else Color.parseColor("#22c55e")
        row.addView(TextView(this).apply {
            text = if (chain.isCyclic) "CYCLIC" else "SERIAL"
            textSize = 10f
            setTextColor(modeColor)
            setPadding(12, 6, 12, 6)
        })
        row.addView(TextView(this).apply {
            text = if (chain.isActive) "ON" else "OFF"
            textSize = 10f
            setTextColor(if (chain.isActive) Color.parseColor("#22c55e") else Color.parseColor("#6b6b8a"))
            setPadding(8, 6, 8, 6)
        })
        card.addView(row)
        card.addView(Space(this).apply { minimumHeight = 16 })

        val unlocked = ChainStorage.getUnlockedStep(this, chain.id)
        for ((i, step) in chain.steps.withIndex()) {
            val usedMs = progress[step.packageName] ?: 0L
            val usedMins = (usedMs / 60000).toInt()
            val isDone = !chain.isCyclic && i < unlocked
            val isCur = !chain.isCyclic && i == unlocked

            val sr = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 6, 0, 6) }
            sr.addView(TextView(this).apply {
                text = if (isDone) "OK" else if (isCur) ">" else "${i + 1}."
                textSize = 14f
                setTextColor(if (isDone) Color.parseColor("#22c55e") else if (isCur) Color.parseColor("#ff6b35") else Color.parseColor("#7c3aed"))
                setPadding(0, 0, 16, 0)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            val si = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
            si.addView(TextView(this).apply {
                text = step.appName
                textSize = 14f
                setTextColor(if (isDone) Color.parseColor("#22c55e") else if (isCur) Color.WHITE else Color.parseColor("#a0a0c0"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            si.addView(TextView(this).apply {
                text = "$usedMins / ${step.requiredMinutes} min"
                textSize = 12f
                setTextColor(Color.parseColor("#6b6b8a"))
            })
            sr.addView(si)
            card.addView(sr)
        }

        card.addView(Space(this).apply { minimumHeight = 16 })
        val br = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        br.addView(Button(this).apply {
            text = if (chain.isActive) "Pause" else "Resume"
            setTextColor(Color.parseColor("#a0a0c0"))
            setBackgroundColor(Color.parseColor("#2a2a45"))
            textSize = 12f
            setOnClickListener {
                val cs = ChainStorage.loadChains(this@MainActivity).toMutableList()
                val idx = cs.indexOfFirst { it.id == chain.id }
                if (idx >= 0) { cs[idx] = cs[idx].copy(isActive = !cs[idx].isActive); ChainStorage.saveChains(this@MainActivity, cs); refreshList() }
            }
        })
        br.addView(Space(this).apply { minimumWidth = 8 })
        br.addView(Button(this).apply {
            text = "Delete"
            setTextColor(Color.parseColor("#ef4444"))
            setBackgroundColor(Color.parseColor("#1a1a2e"))
            textSize = 12f
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity).setTitle("Delete?").setMessage("Delete chain?")
                    .setPositiveButton("Delete") { _, _ ->
                        val cs = ChainStorage.loadChains(this@MainActivity).toMutableList()
                        cs.removeAll { it.id == chain.id }
                        ChainStorage.saveChains(this@MainActivity, cs)
                        refreshList()
                    }.setNegativeButton("Cancel", null).show()
            }
        })
        card.addView(br)
        return card
    }

    private fun addChainDialog() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { pm.getApplicationLabel(it).toString() }
        val names = apps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
        val pkgs = apps.map { it.packageName }

        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 32, 48, 16) }
        val nameInput = EditText(this).apply { hint = "Chain name e.g. Study Mode"; textSize = 15f }
        layout.addView(TextView(this).apply { text = "Chain Name"; textSize = 12f; setTextColor(Color.GRAY) })
        layout.addView(nameInput)
        layout.addView(Space(this).apply { minimumHeight = 16 })

        val cyclicCheck = CheckBox(this).apply { text = "Cyclic Mode (last app unlocks first app again)"; setTextColor(Color.parseColor("#a0a0c0")); textSize = 13f }
        layout.addView(cyclicCheck)
        layout.addView(Space(this).apply { minimumHeight = 16 })

        layout.addView(TextView(this).apply { text = "Steps (in order):"; textSize = 13f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        layout.addView(Space(this).apply { minimumHeight = 12 })

        val sc = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(sc)

        fun addStep() {
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 8, 0, 8) }
            r.addView(TextView(this).apply { text = "${sc.childCount + 1}."; textSize = 14f; setTextColor(Color.parseColor("#7c3aed")); typeface = android.graphics.Typeface.DEFAULT_BOLD; setPadding(0, 0, 12, 0) })
            val sp = Spinner(this).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, names).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val mi = EditText(this).apply { hint = "min"; setText("30"); textSize = 13f; inputType = android.text.InputType.TYPE_CLASS_NUMBER; layoutParams = LinearLayout.LayoutParams(130, ViewGroup.LayoutParams.WRAP_CONTENT) }
            r.addView(sp); r.addView(Space(this).apply { minimumWidth = 8 }); r.addView(mi)
            sc.addView(r)
        }
        addStep(); addStep()
        layout.addView(Button(this).apply { text = "+ Add Step"; textSize = 13f; setOnClickListener { addStep() } })

        AlertDialog.Builder(this).setTitle("Create Chain").setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString().trim().ifEmpty { "Chain " + System.currentTimeMillis() }
                val steps = mutableListOf<ChainStep>()
                for (i in 0 until sc.childCount) {
                    val r = sc.getChildAt(i) as LinearLayout
                    val sp = r.getChildAt(1) as Spinner
                    val mi = r.getChildAt(3) as EditText
                    steps.add(ChainStep(pkgs[sp.selectedItemPosition], names[sp.selectedItemPosition], mi.text.toString().toIntOrNull() ?: 30))
                }
                if (steps.size < 2) { Toast.makeText(this, "Need 2+ steps!", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val cs = ChainStorage.loadChains(this).toMutableList()
                cs.add(AppChain(System.currentTimeMillis().toString(), name, steps, isCyclic = cyclicCheck.isChecked))
                ChainStorage.saveChains(this, cs)
                refreshList()
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }
}
