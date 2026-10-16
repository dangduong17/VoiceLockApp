package com.example.voicelock.di

import com.example.voicelock.data.repository.appLockRepository.AppLockRepository
import com.example.voicelock.data.repository.appLockRepository.AppLockRepositoryImpl
import com.example.voicelock.data.repository.appLockSessionRepository.AppLockSessionRepository
import com.example.voicelock.data.repository.appLockSessionRepository.AppLockSessionRepositoryImpl
import com.example.voicelock.data.repository.installedAppsRepository.InstalledAppsRepository
import com.example.voicelock.data.repository.installedAppsRepository.InstalledAppsRepositoryImpl
import com.example.voicelock.data.repository.lockRepository.LockRepository
import com.example.voicelock.data.repository.lockRepository.LockRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::LockRepositoryImpl) bind LockRepository::class
    singleOf(::AppLockRepositoryImpl) bind AppLockRepository::class
    singleOf(::AppLockSessionRepositoryImpl) bind AppLockSessionRepository::class
    single<InstalledAppsRepository> { InstalledAppsRepositoryImpl(context = androidContext()) }
}
