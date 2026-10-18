package com.example.voicelock.di

import com.example.voicelock.data.repository.appLockRepository.AppLockRepository
import com.example.voicelock.data.repository.appLockRepository.AppLockRepositoryImpl
import com.example.voicelock.data.repository.appLockSessionRepository.AppLockSessionRepository
import com.example.voicelock.data.repository.appLockSessionRepository.AppLockSessionRepositoryImpl
import com.example.voicelock.data.repository.biometricRepository.BiometricRepository
import com.example.voicelock.data.repository.biometricRepository.BiometricRepositoryImpl
import com.example.voicelock.data.repository.faceProfileRepository.FaceProfileRepository
import com.example.voicelock.data.repository.faceProfileRepository.FaceProfileRepositoryImpl
import com.example.voicelock.data.repository.faceRecognitionRepository.FaceRecognitionRepository
import com.example.voicelock.data.repository.faceRecognitionRepository.FaceRecognitionRepositoryImpl
import com.example.voicelock.data.repository.installedAppsRepository.InstalledAppsRepository
import com.example.voicelock.data.repository.installedAppsRepository.InstalledAppsRepositoryImpl
import com.example.voicelock.data.repository.lockRepository.LockRepository
import com.example.voicelock.data.repository.lockRepository.LockRepositoryImpl
import com.example.voicelock.data.repository.speechToTextRepository.SpeechToTextRepository
import com.example.voicelock.data.repository.speechToTextRepository.SpeechToTextRepositoryImpl
import com.example.voicelock.data.repository.voiceCaptureRepository.VoiceCaptureRepository
import com.example.voicelock.data.repository.voiceCaptureRepository.VoiceCaptureRepositoryImpl
import com.example.voicelock.data.repository.voiceProfileRepository.VoiceProfileRepository
import com.example.voicelock.data.repository.voiceProfileRepository.VoiceProfileRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::LockRepositoryImpl) bind LockRepository::class
    singleOf(::AppLockRepositoryImpl) bind AppLockRepository::class
    singleOf(::AppLockSessionRepositoryImpl) bind AppLockSessionRepository::class
    singleOf(::VoiceProfileRepositoryImpl) bind VoiceProfileRepository::class
    singleOf(::VoiceCaptureRepositoryImpl) bind VoiceCaptureRepository::class
    singleOf(::SpeechToTextRepositoryImpl) bind SpeechToTextRepository::class
    singleOf(::BiometricRepositoryImpl) bind BiometricRepository::class
    singleOf(::FaceRecognitionRepositoryImpl) bind FaceRecognitionRepository::class
    singleOf(::FaceProfileRepositoryImpl) bind FaceProfileRepository::class
    single<InstalledAppsRepository> { InstalledAppsRepositoryImpl(context = androidContext()) }
}
