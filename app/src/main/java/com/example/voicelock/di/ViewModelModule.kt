package com.example.voicelock.di

import com.example.voicelock.feature.home.HomeViewModel
import com.example.voicelock.feature.patternlock.PatternLockViewModel
import com.example.voicelock.feature.pinlock.PinLockViewModel
import com.example.voicelock.feature.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::PinLockViewModel)
    viewModelOf(::PatternLockViewModel)
}
