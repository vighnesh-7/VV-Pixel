package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CapsulePreferences {
    private const val PREF_NAME = "com.example.vvpixel.CAPSULE_PREFS"

    // Key names
    const val KEY_PLAYER_ENABLED = "player_control_enabled"
    const val KEY_VOLUME_ENABLED = "volume_control_enabled"
    const val KEY_PROGRESS_ENABLED = "progress_enabled"
    const val KEY_TIMER_ENABLED = "timer_enabled"
    const val KEY_NOTIF_ENABLED = "notification_enabled"

    const val KEY_PLAYER_SIZE = "player_size" // "COMPACT", "FULL"
    const val KEY_USE_COVER_COLORS = "use_cover_colors"
    const val KEY_COLOR_PRESET = "default_color_preset"
    const val KEY_ALBUM_SHAPE = "album_cover_shape" // "BLOB", "SQUARE", "ROUNDED_RECT", "HEXAGON", "SHIELD", "OCTAGON"
    const val KEY_BUTTONS_TYPE = "buttons_type" // "CIRCLE", "STAR8", "STAR12", "BLOB", "CLOVER"
    const val KEY_COVER_TYPE = "cover_type" // "STANDARD", "VINYL"
    const val KEY_SHOW_MEDIA_APP = "show_media_app" // "NONE", "ICON_ONLY", "ICON_AND_NAME"
    const val KEY_CARD_BG = "card_background" // "AMOLED_BLACK", "TRANSPARENT"
    const val KEY_SLIDER_STYLE = "slider_style" // "THIN", "PILL"
    const val KEY_VIBRATION = "vibration"
    const val KEY_SHOW_SLIDER = "show_slider"
    const val KEY_COLLAPSE_DELAY = "collapse_delay" // seconds: 0 (Instantly), 1, 2, 3, 5, 10, -1 (Never)

    const val KEY_NOTIF_AUTOHIDE = "notif_autohide_delay" // seconds
    const val KEY_NOTIF_THEME = "notif_theme" // "SYSTEM", "DARK", "LIGHT"
    const val KEY_NOTIF_GLOW = "notif_glow"

    data class State(
        val playerEnabled: Boolean = true,
        val volumeEnabled: Boolean = true,
        val progressEnabled: Boolean = false,
        val timerEnabled: Boolean = false,
        val notificationEnabled: Boolean = true,

        val playerSize: String = "COMPACT",
        val useCoverColors: Boolean = true,
        val colorPreset: Int = 0,
        val albumShape: String = "BLOB",
        val buttonsType: String = "CIRCLE",
        val coverType: String = "STANDARD",
        val showMediaApp: String = "NONE",
        val cardBg: String = "AMOLED_BLACK",
        val sliderStyle: String = "THIN",
        val vibration: Boolean = true,
        val showSlider: Boolean = false,
        val collapseDelay: Int = 3,

        val notificationAutohideDelay: Int = 3,
        val notificationTheme: String = "DARK",
        val notificationGlow: Boolean = true
    )

    private val _stateFlow = MutableStateFlow(State())
    val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            reload()
        }
    }

    fun reload() {
        val p = prefs ?: return
        _stateFlow.value = State(
            playerEnabled = p.getBoolean(KEY_PLAYER_ENABLED, true),
            volumeEnabled = p.getBoolean(KEY_VOLUME_ENABLED, true),
            progressEnabled = p.getBoolean(KEY_PROGRESS_ENABLED, false),
            timerEnabled = p.getBoolean(KEY_TIMER_ENABLED, false),
            notificationEnabled = p.getBoolean(KEY_NOTIF_ENABLED, true),

            playerSize = p.getString(KEY_PLAYER_SIZE, "COMPACT") ?: "COMPACT",
            useCoverColors = p.getBoolean(KEY_USE_COVER_COLORS, true),
            colorPreset = p.getInt(KEY_COLOR_PRESET, 0),
            albumShape = p.getString(KEY_ALBUM_SHAPE, "BLOB") ?: "BLOB",
            buttonsType = p.getString(KEY_BUTTONS_TYPE, "CIRCLE") ?: "CIRCLE",
            coverType = p.getString(KEY_COVER_TYPE, "STANDARD") ?: "STANDARD",
            showMediaApp = p.getString(KEY_SHOW_MEDIA_APP, "NONE") ?: "NONE",
            cardBg = p.getString(KEY_CARD_BG, "AMOLED_BLACK") ?: "AMOLED_BLACK",
            sliderStyle = p.getString(KEY_SLIDER_STYLE, "THIN") ?: "THIN",
            vibration = p.getBoolean(KEY_VIBRATION, true),
            showSlider = p.getBoolean(KEY_SHOW_SLIDER, false),
            collapseDelay = p.getInt(KEY_COLLAPSE_DELAY, 3),

            notificationAutohideDelay = p.getInt(KEY_NOTIF_AUTOHIDE, 3),
            notificationTheme = p.getString(KEY_NOTIF_THEME, "DARK") ?: "DARK",
            notificationGlow = p.getBoolean(KEY_NOTIF_GLOW, true)
        )
    }

    fun update(block: (State) -> State) {
        val newState = block(_stateFlow.value)
        _stateFlow.value = newState

        prefs?.edit()?.apply {
            putBoolean(KEY_PLAYER_ENABLED, newState.playerEnabled)
            putBoolean(KEY_VOLUME_ENABLED, newState.volumeEnabled)
            putBoolean(KEY_PROGRESS_ENABLED, newState.progressEnabled)
            putBoolean(KEY_TIMER_ENABLED, newState.timerEnabled)
            putBoolean(KEY_NOTIF_ENABLED, newState.notificationEnabled)

            putString(KEY_PLAYER_SIZE, newState.playerSize)
            putBoolean(KEY_USE_COVER_COLORS, newState.useCoverColors)
            putInt(KEY_COLOR_PRESET, newState.colorPreset)
            putString(KEY_ALBUM_SHAPE, newState.albumShape)
            putString(KEY_BUTTONS_TYPE, newState.buttonsType)
            putString(KEY_COVER_TYPE, newState.coverType)
            putString(KEY_SHOW_MEDIA_APP, newState.showMediaApp)
            putString(KEY_CARD_BG, newState.cardBg)
            putString(KEY_SLIDER_STYLE, newState.sliderStyle)
            putBoolean(KEY_VIBRATION, newState.vibration)
            putBoolean(KEY_SHOW_SLIDER, newState.showSlider)
            putInt(KEY_COLLAPSE_DELAY, newState.collapseDelay)

            putInt(KEY_NOTIF_AUTOHIDE, newState.notificationAutohideDelay)
            putString(KEY_NOTIF_THEME, newState.notificationTheme)
            putBoolean(KEY_NOTIF_GLOW, newState.notificationGlow)
            apply()
        }
    }
}
