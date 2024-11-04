package com.ideality.idealityproject.di

import com.ideality.idealityproject.LoginService
import com.ideality.idealityproject.LoginServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLoginService(): LoginService {
        return LoginServiceImpl() // or however you instantiate your service
    }
}