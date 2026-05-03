package com.sweetcode.lumi.data

import android.content.Context
import com.sweetcode.lumi.data.local.LumiDatabase
import com.sweetcode.lumi.data.local.MediaItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LumiDatabase =
        LumiDatabase.build(context)

    @Provides
    fun provideMediaItemDao(db: LumiDatabase): MediaItemDao = db.mediaItemDao()
}