package com.example.voicelock.di

import org.koin.core.module.Module

val appModules: List<Module> =
    listOf(
        coreModule,
        networkModule,
        databaseModule,
        repositoryModule,
        useCaseModule,
        viewModelModule,
    )
