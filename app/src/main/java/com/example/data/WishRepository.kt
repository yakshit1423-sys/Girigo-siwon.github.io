package com.example.data

import kotlinx.coroutines.flow.Flow

class WishRepository(private val wishDao: WishDao) {
    val allWishes: Flow<List<WishEntity>> = wishDao.getAllWishes()
    val activeWish: Flow<WishEntity?> = wishDao.getActiveWish()

    suspend fun insertWish(wish: WishEntity): Long {
        return wishDao.insertWish(wish)
    }

    suspend fun updateWish(wish: WishEntity) {
        wishDao.updateWish(wish)
    }

    suspend fun clearAll() {
        wishDao.clearAllWishes()
    }
}
