package com.example.voicelock.di

import com.example.voicelock.feature.applock.AppLockViewModel
import com.example.voicelock.feature.applock.LockOverlayViewModel
import com.example.voicelock.feature.home.HomeViewModel
import com.example.voicelock.feature.patternlock.PatternLockViewModel
import com.example.voicelock.feature.permissions.PermissionsViewModel
import com.example.voicelock.feature.pinlock.PinLockViewModel
import com.example.voicelock.feature.settings.SettingsViewModel
import com.example.voicelock.feature.voice.auth.TwoStageAuthViewModel
import com.example.voicelock.feature.voice.enrollment.EnrollmentViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::PinLockViewModel)
    viewModelOf(::PatternLockViewModel)
    viewModelOf(::AppLockViewModel)
    viewModelOf(::LockOverlayViewModel)
    viewModelOf(::PermissionsViewModel)
    viewModelOf(::EnrollmentViewModel)
    viewModelOf(::TwoStageAuthViewModel)
}
