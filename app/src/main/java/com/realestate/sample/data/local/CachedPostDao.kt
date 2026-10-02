package com.realestate.sample.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedPostDao {
    @Query("SELECT * FROM cached_posts ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<CachedPostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(posts: List<CachedPostEntity>)

    @Query("DELETE FROM cached_posts")
    suspend fun clear()
}
