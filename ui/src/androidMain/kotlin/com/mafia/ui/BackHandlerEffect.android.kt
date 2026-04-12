package com.mafia.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun BackHandlerEffect(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}
