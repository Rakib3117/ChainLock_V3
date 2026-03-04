package com.chainlock.app
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ChainStep(val packageName: String, val appName: String, val requiredMinutes: Int)
data class AppChain(
    val id: String,
    val name: String,
    val steps: List<ChainStep>,
    var isActive: Boolean = true,
    var isCyclic: Boolean = false
)

object ChainStorage {
    private const val PREF = "chainlock_prefs"

    fun saveChains(context: Context, chains: List<AppChain>) {
        val arr = JSONArray()
        for (c in chains) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("isActive", c.isActive)
            obj.put("isCyclic", c.isCyclic)
            val steps = JSONArray()
            for (s in c.steps) {
                val so = JSONObject()
                so.put("packageName", s.packageName)
                so.put("appName", s.appName)
                so.put("requiredMinutes", s.requiredMinutes)
                steps.put(so)
            }
            obj.put("steps", steps)
            arr.put(obj)
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString("chains", arr.toString()).apply()
    }

    fun loadChains(context: Context): MutableList<AppChain> {
        val json = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("chains", "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<AppChain>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val sa = obj.getJSONArray("steps")
            val steps = mutableListOf<ChainStep>()
            for (j in 0 until sa.length()) {
                val s = sa.getJSONObject(j)
                steps.add(ChainStep(s.getString("packageName"), s.getString("appName"), s.getInt("requiredMinutes")))
            }
            list.add(AppChain(
                obj.getString("id"), obj.getString("name"), steps,
                obj.optBoolean("isActive", true), obj.optBoolean("isCyclic", false)
            ))
        }
        return list
    }

    fun saveProgress(context: Context, progress: Map<String, Long>) {
        val obj = JSONObject()
        for ((k, v) in progress) obj.put(k, v)
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString("progress", obj.toString()).apply()
    }

    fun loadProgress(context: Context): MutableMap<String, Long> {
        val json = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("progress", "{}") ?: "{}"
        val obj = JSONObject(json)
        val map = mutableMapOf<String, Long>()
        for (k in obj.keys()) map[k] = obj.getLong(k)
        return map
    }

    fun resetDailyProgress(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove("progress").apply()
    }

    fun getUnlockedStep(context: Context, chainId: String): Int {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt("unlocked_$chainId", 0)
    }

    fun setUnlockedStep(context: Context, chainId: String, step: Int) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putInt("unlocked_$chainId", step).apply()
    }
}
