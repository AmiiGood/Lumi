package com.sweetcode.lumi.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MediaItemEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(MediaConverters::class)
abstract class LumiDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao

    companion object {
        fun build(context: Context): LumiDatabase = Room.databaseBuilder(
            context,
            LumiDatabase::class.java,
            "lumi.db"
        ).build()
    }
}