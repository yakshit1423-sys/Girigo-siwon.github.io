package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishes")
data class WishEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val wishText: String,
    val creationTime: Long,
    val expirationTime: Long,
    val isGranted: Boolean = true,
    val isCompleted: Boolean = false
)
