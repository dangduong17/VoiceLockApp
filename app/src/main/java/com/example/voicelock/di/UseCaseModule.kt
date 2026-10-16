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

val useCaseModule = module {
    includes(
        homeUseCaseModule,
        settingsUseCaseModule,
        lockUseCaseModule,
        pinLockUseCaseModule,
        patternLockUseCaseModule,
        appLockUseCaseModule,
    )
}
