package com.example.kutuphaneapp.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val DEFAULT_SORT_ORDER = stringPreferencesKey("default_sort_order")
        val DEFAULT_VIEW_MODE = stringPreferencesKey("default_view_mode")
    }

    val themeMode: Flow<String> = dataStore.data.map { it[THEME_MODE] ?: "system" }
    val appLanguage: Flow<String> = dataStore.data.map { it[APP_LANGUAGE] ?: "tr" }
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: false }
    val reminderHour: Flow<Int> = dataStore.data.map { it[REMINDER_HOUR] ?: 20 }
    val reminderMinute: Flow<Int> = dataStore.data.map { it[REMINDER_MINUTE] ?: 0 }
    val defaultSortOrder: Flow<String> = dataStore.data.map { it[DEFAULT_SORT_ORDER] ?: "date" }
    val defaultViewMode: Flow<String> = dataStore.data.map { it[DEFAULT_VIEW_MODE] ?: "list" }

    fun setThemeMode(mode: String) = viewModelScope.launch {
        dataStore.edit { it[THEME_MODE] = mode }
    }

    fun setAppLanguage(lang: String) = viewModelScope.launch {
        dataStore.edit { it[APP_LANGUAGE] = lang }
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    fun setReminderTime(hour: Int, minute: Int) = viewModelScope.launch {
        dataStore.edit {
            it[REMINDER_HOUR] = hour
            it[REMINDER_MINUTE] = minute
        }
    }

    fun setDefaultSortOrder(order: String) = viewModelScope.launch {
        dataStore.edit { it[DEFAULT_SORT_ORDER] = order }
    }

    fun setDefaultViewMode(mode: String) = viewModelScope.launch {
        dataStore.edit { it[DEFAULT_VIEW_MODE] = mode }
    }
}
