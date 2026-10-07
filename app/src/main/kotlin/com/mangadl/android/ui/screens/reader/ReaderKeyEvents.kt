package com.mangadl.android.ui.screens.reader

/**
 * Bridges hardware volume-key events from [com.mangadl.android.MainActivity.dispatchKeyEvent]
 * (Compose has no reliable way to intercept them directly — they're dispatched to the Activity,
 * not routed through the focused composable) to whichever [ReaderScreen] is currently on screen.
 * Set by the reader while it's active (gated on the "Volume keys turn pages" preference), cleared
 * on dispose so volume keys behave normally everywhere else.
 */
object ReaderKeyEvents {
    var onVolumeKey: ((isVolumeUp: Boolean) -> Boolean)? = null
}
