package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DataPlanConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fluxnet_plan_prefs")

class PlanPreferencesRepository(private val context: Context) {

    private object Keys {
        val OPERATOR_NAME = stringPreferencesKey("plan_operator_name")
        val QUOTA_MB = longPreferencesKey("plan_quota_mb")
        val VALIDITY_DAYS = intPreferencesKey("plan_validity_days")
        val COST = doublePreferencesKey("plan_cost")
        val CURRENCY = stringPreferencesKey("plan_currency")
        val START_DATE_MILLIS = longPreferencesKey("plan_start_date_millis")
        val ALERT_THRESHOLD_PERCENT = intPreferencesKey("alert_threshold_percent")
        val AUTO_BLOCK_BG = booleanPreferencesKey("auto_block_bg_heavy")
        val BG_THRESHOLD_MB_MIN = intPreferencesKey("bg_threshold_mb_min")
        val FIREWALL_ENABLED = booleanPreferencesKey("firewall_enabled")
    }

    val planConfigFlow: Flow<DataPlanConfig> = context.dataStore.data.map { prefs ->
        val defaultStart = System.currentTimeMillis() - 3L * 86_400_000L
        DataPlanConfig(
            operatorName = prefs[Keys.OPERATOR_NAME] ?: "MTN",
            quotaMb = prefs[Keys.QUOTA_MB] ?: 20_480L,
            validityDays = prefs[Keys.VALIDITY_DAYS] ?: 30,
            cost = prefs[Keys.COST] ?: 2000.0,
            currency = prefs[Keys.CURRENCY] ?: "FCFA",
            startDateMillis = prefs[Keys.START_DATE_MILLIS] ?: defaultStart,
            alertThresholdPercent = prefs[Keys.ALERT_THRESHOLD_PERCENT] ?: 80,
            autoBlockBackgroundHeavy = prefs[Keys.AUTO_BLOCK_BG] ?: true,
            backgroundThresholdMbPerMin = prefs[Keys.BG_THRESHOLD_MB_MIN] ?: 5,
            firewallEnabled = prefs[Keys.FIREWALL_ENABLED] ?: false
        )
    }

    suspend fun updatePlan(
        operatorName: String,
        quotaMb: Long,
        validityDays: Int,
        cost: Double,
        currency: String,
        startDateMillis: Long
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OPERATOR_NAME] = operatorName.ifBlank { "MTN" }
            prefs[Keys.QUOTA_MB] = quotaMb.coerceAtLeast(100L)
            prefs[Keys.VALIDITY_DAYS] = validityDays.coerceIn(1, 365)
            prefs[Keys.COST] = cost.coerceAtLeast(0.0)
            prefs[Keys.CURRENCY] = currency.ifBlank { "FCFA" }
            prefs[Keys.START_DATE_MILLIS] = startDateMillis
        }
    }

    suspend fun updateAlertThreshold(percent: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ALERT_THRESHOLD_PERCENT] = percent.coerceIn(10, 100)
        }
    }

    suspend fun updateAutoBlockBackground(enabled: Boolean, thresholdMbPerMin: Int? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_BLOCK_BG] = enabled
            if (thresholdMbPerMin != null) {
                prefs[Keys.BG_THRESHOLD_MB_MIN] = thresholdMbPerMin.coerceIn(1, 100)
            }
        }
    }

    suspend fun setFirewallEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FIREWALL_ENABLED] = enabled
        }
    }
}
