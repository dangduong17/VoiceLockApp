package com.example.voicelock.di

import com.example.voicelock.domain.usecase.applock.GetInstalledAppsUseCase
import com.example.voicelock.domain.usecase.applock.GetLockedPackagesUseCase
import com.example.voicelock.domain.usecase.applock.IsAppUnlockedUseCase
import com.example.voicelock.domain.usecase.applock.MarkAppUnlockedUseCase
import com.example.voicelock.domain.usecase.applock.SetAppLockedUseCase
import com.example.voicelock.domain.usecase.home.GetHomeStatusUseCase
import com.example.voicelock.domain.usecase.lock.GetLockoutStateUseCase
import com.example.voicelock.domain.usecase.patternlock.GetPatternInfoUseCase
import com.example.voicelock.domain.usecase.patternlock.SetPatternUseCase
import com.example.voicelock.domain.usecase.patternlock.VerifyPatternUseCase
import com.example.voicelock.domain.usecase.pinlock.GetPinInfoUseCase
import com.example.voicelock.domain.usecase.pinlock.SetPinUseCase
import com.example.voicelock.domain.usecase.pinlock.VerifyPinUseCase
import com.example.voicelock.domain.usecase.settings.GetSettingsUseCase
import com.example.voicelock.domain.usecase.voice.EnrollOwnerVoiceUseCase
import com.example.voicelock.domain.usecase.voice.GetVoiceSettingsUseCase
import com.example.voicelock.domain.usecase.voice.IsVoiceEnrolledUseCase
import com.example.voicelock.domain.usecase.voice.MatchVoiceKeywordUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceLockEnabledUseCase
import com.example.voicelock.domain.usecase.voice.SetVoiceUnlockEnabledUseCase
import com.example.voicelock.domain.usecase.voice.VerifyOwnerVoiceUseCase
import com.example.voicelock.domain.usecase.voice.WarmUpVoiceModelUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val homeUseCaseModule = module {
    factoryOf(::GetHomeStatusUseCase)
}

val settingsUseCaseModule = module {
    factoryOf(::GetSettingsUseCase)
}

val lockUseCaseModule = module {
    factoryOf(::GetLockoutStateUseCase)
}

val pinLockUseCaseModule = module {
    factoryOf(::GetPinInfoUseCase)
    factoryOf(::SetPinUseCase)
    factoryOf(::VerifyPinUseCase)
}

val patternLockUseCaseModule = module {
    factoryOf(::GetPatternInfoUseCase)
    factoryOf(::SetPatternUseCase)
    factoryOf(::VerifyPatternUseCase)
}

val appLockUseCaseModule = module {
    factoryOf(::GetInstalledAppsUseCase)
    factoryOf(::GetLockedPackagesUseCase)
    factoryOf(::SetAppLockedUseCase)
    factoryOf(::MarkAppUnlockedUseCase)
    factoryOf(::IsAppUnlockedUseCase)
}

val voiceUseCaseModule = module {
    factoryOf(::IsVoiceEnrolledUseCase)
    factoryOf(::WarmUpVoiceModelUseCase)
    factoryOf(::EnrollOwnerVoiceUseCase)
    factoryOf(::VerifyOwnerVoiceUseCase)
    factoryOf(::MatchVoiceKeywordUseCase)
    factoryOf(::GetVoiceSettingsUseCase)
    factoryOf(::SetVoiceUnlockEnabledUseCase)
    factoryOf(::SetVoiceLockEnabledUseCase)
}

val useCaseModule = module {
    includes(
        homeUseCaseModule,
        settingsUseCaseModule,
        lockUseCaseModule,
        pinLockUseCaseModule,
        patternLockUseCaseModule,
        appLockUseCaseModule,
        voiceUseCaseModule,
    )
}
