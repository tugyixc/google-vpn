package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warp_configs")
data class WarpConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val privateKey: String,
    val publicKey: String,
    val ipv4Address: String,
    val ipv6Address: String,
    val endpoint: String,
    val configText: String,
    val timestamp: Long = System.currentTimeMillis()
)
