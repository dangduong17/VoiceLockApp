package com.example.voicelock.di

import com.example.voicelock.domain.usecase.home.GetHomeStatusUseCase
import com.example.voicelock.domain.usecase.settings.GetSettingsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val homeUseCaseModule = module {
    factoryOf(::GetHomeStatusUseCase)
}

val settingsUseCaseModule = module {
    factoryOf(::GetSettingsUseCase)
}

val useCaseModule = module {
    includes(
        homeUseCaseModule,
        settingsUseCaseModule,
    )
}
