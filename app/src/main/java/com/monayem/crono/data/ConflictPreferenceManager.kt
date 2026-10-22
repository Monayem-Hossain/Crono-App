package com.monayem.crono.data

import android.content.Context

object ConflictPreferenceManager {

    private fun getPrefs(context: Context) =
        context.applicationContext.getSharedPreferences("conflict_settings", Context.MODE_PRIVATE)

    fun isClusterResolved(context: Context, clusterId: String): Boolean {
        return getPrefs(context).contains("resolved_$clusterId")
    }

    fun saveResolvedChoice(context: Context, clusterId: String, chosenEventId: String) {
        getPrefs(context).edit().putString("resolved_$clusterId", chosenEventId).apply()
    }

    fun isClusterDismissed(context: Context, clusterId: String): Boolean {
        return getPrefs(context).getBoolean("dismissed_$clusterId", false)
    }

    fun saveDismissedCluster(context: Context, clusterId: String) {
        getPrefs(context).edit().putBoolean("dismissed_$clusterId", true).apply()
    }

    fun isSwitchedPromptHandled(context: Context, clusterId: String): Boolean {
        return getPrefs(context).getBoolean("switched_handled_$clusterId", false)
    }

    fun saveSwitchedPromptHandled(context: Context, clusterId: String) {
        getPrefs(context).edit().putBoolean("switched_handled_$clusterId", true).apply()
    }

    fun isSwitchedPromptDismissed(context: Context, clusterId: String): Boolean {
        return getPrefs(context).getBoolean("switched_dismissed_$clusterId", false)
    }

    fun saveSwitchedPromptDismissed(context: Context, clusterId: String) {
        getPrefs(context).edit().putBoolean("switched_dismissed_$clusterId", true).apply()
    }

    fun getSavedSelectionsMap(context: Context): Map<String, String> {
        val prefs = getPrefs(context)
        val result = mutableMapOf<String, String>()
        for ((key, value) in prefs.all) {
            if (key.startsWith("resolved_") && value is String) {
                val clusterId = key.removePrefix("resolved_")
                result[clusterId] = value
            }
        }
        return result
    }

    fun getSavedDismissedSet(context: Context): Set<String> {
        val prefs = getPrefs(context)
        val result = mutableSetOf<String>()
        for ((key, value) in prefs.all) {
            if (key.startsWith("dismissed_") && value == true) {
                val clusterId = key.removePrefix("dismissed_")
                result.add(clusterId)
            }
        }
        return result
    }
}
