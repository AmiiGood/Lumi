package com.sweetcode.lumi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MediaItemDao {

    @Query("SELECT * FROM media_items")
    suspend fun getAll(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: String): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>)

    @Query("DELETE FROM media_items")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(items: List<MediaItemEntity>) {
        deleteAll()
        insertAll(items)
    }

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun count(): Int
}