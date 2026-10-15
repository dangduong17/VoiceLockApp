package com.example.voicelock.di

import com.example.voicelock.data.repository.onboardingRepository.OnboardingRepository
import com.example.voicelock.data.repository.onboardingRepository.OnboardingRepositoryImpl
import com.example.voicelock.data.repository.pinRepository.PinRepository
import com.example.voicelock.data.repository.pinRepository.PinRepositoryImpl
import com.example.voicelock.data.repository.voiceProfileRepository.VoiceProfileRepository
import com.example.voicelock.data.repository.voiceProfileRepository.VoiceProfileRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single<VoiceProfileRepository> { VoiceProfileRepositoryImpl(context = androidContext(), gson = get()) }
    single<PinRepository> { PinRepositoryImpl(context = androidContext()) }
    single<OnboardingRepository> { OnboardingRepositoryImpl(context = androidContext()) }
}
