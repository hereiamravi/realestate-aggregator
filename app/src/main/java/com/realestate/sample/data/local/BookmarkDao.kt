package com.realestate.sample.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY savedAt DESC")
    suspend fun getAll(): List<BookmarkEntity>

    @Query("SELECT postId FROM bookmarks")
    suspend fun getBookmarkedPostIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE postId = :postId")
    suspend fun deleteByPostId(postId: String)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE postId = :postId")
    suspend fun countByPostId(postId: String): Int
}
