package com.example.voicelock.di

import com.example.voicelock.data.repository.lockRepository.LockRepository
import com.example.voicelock.data.repository.lockRepository.LockRepositoryImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::LockRepositoryImpl) bind LockRepository::class
}
