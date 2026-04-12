package com.mafia.ui

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandlerEffect(onBack: () -> Unit) {
    // iOS uses native swipe-back gesture; no-op here.
}
