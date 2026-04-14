package com.mafia.shared

import com.mafia.shared.di.sharedModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin { modules(sharedModule()) }
}
