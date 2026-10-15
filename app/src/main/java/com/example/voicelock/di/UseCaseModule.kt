package com.example.voicelock.di

import com.example.voicelock.domain.usecase.voice.EnrollVoiceUseCase
import com.example.voicelock.domain.usecase.voice.GetVoiceStatusUseCase
import com.example.voicelock.domain.usecase.voice.VerifyVoiceUseCase
import org.koin.dsl.module

val useCaseModule = module {
    factory { GetVoiceStatusUseCase(voiceProfileRepository = get(), voiceAuthEngine = get()) }
    factory { EnrollVoiceUseCase(voiceProfileRepository = get(), voiceAuthEngine = get()) }
    factory { VerifyVoiceUseCase(voiceProfileRepository = get(), voiceAuthEngine = get()) }
}
