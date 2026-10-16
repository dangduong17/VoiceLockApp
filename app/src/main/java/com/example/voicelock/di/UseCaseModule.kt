package com.example.voicelock.di

import com.example.voicelock.domain.usecase.home.GetHomeStatusUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val homeUseCaseModule = module {
    factoryOf(::GetHomeStatusUseCase)
}

val useCaseModule = module {
    includes(homeUseCaseModule)
}
