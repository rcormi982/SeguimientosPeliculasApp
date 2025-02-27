package com.example.seguimientopeliculas.di

import android.content.Context
import androidx.work.WorkManager
import com.example.seguimientopeliculas.workers.WorkManagerHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideWorkManagerHelper(
        @ApplicationContext context: Context,
        workManager: WorkManager
    ): WorkManagerHelper {
        return WorkManagerHelper(context, workManager)
    }
}