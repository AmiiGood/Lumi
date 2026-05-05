package io.github.amiigood.lumi.lumi.data

import android.content.Context
import io.github.amiigood.lumi.lumi.data.local.LumiDatabase
import io.github.amiigood.lumi.lumi.data.local.MediaItemDao
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
        LumiDatabase.Companion.build(context)

    @Provides
    fun provideMediaItemDao(db: LumiDatabase): MediaItemDao = db.mediaItemDao()
}