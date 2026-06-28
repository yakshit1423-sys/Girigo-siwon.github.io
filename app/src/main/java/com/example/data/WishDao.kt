package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WishDao {
    @Query("SELECT * FROM wishes ORDER BY creationTime DESC")
    fun getAllWishes(): Flow<List<WishEntity>>

    @Query("SELECT * FROM wishes WHERE isCompleted = 0 LIMIT 1")
    fun getActiveWish(): Flow<WishEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWish(wish: WishEntity): Long

    @Update
    suspend fun updateWish(wish: WishEntity)

    @Query("DELETE FROM wishes")
    suspend fun clearAllWishes()
}
