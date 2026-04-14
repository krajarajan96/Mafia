package com.mafia.ui

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandlerEffect(onBack: () -> Unit)
