package com.example.voicelock.di

import com.example.voicelock.feature.home.HomeViewModel
import com.example.voicelock.feature.setupPermission.SetupPermissionViewModel
import com.example.voicelock.feature.setupPin.SetupPinViewModel
import com.example.voicelock.feature.setupVoice.SetupVoiceViewModel
import com.example.voicelock.feature.welcome.WelcomeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { WelcomeViewModel(onboardingRepository = get()) }
    viewModel { SetupPermissionViewModel() }
    viewModel {
        SetupVoiceViewModel(
            voiceAuthEngine = get(),
            getVoiceStatusUseCase = get(),
            enrollVoiceUseCase = get(),
            voiceRecorder = get(),
        )
    }
    viewModel { SetupPinViewModel(pinRepository = get(), onboardingRepository = get()) }
    viewModel {
        HomeViewModel(
            getVoiceStatusUseCase = get(),
            verifyVoiceUseCase = get(),
            voiceProfileRepository = get(),
            pinRepository = get(),
            voiceRecorder = get(),
        )
    }
}
