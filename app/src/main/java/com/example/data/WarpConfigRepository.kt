package com.example.data

import kotlinx.coroutines.flow.Flow

class WarpConfigRepository(private val warpConfigDao: WarpConfigDao) {
    val allConfigs: Flow<List<WarpConfigEntity>> = warpConfigDao.getAllConfigs()
    val latestConfig: Flow<WarpConfigEntity?> = warpConfigDao.getLatestConfig()

    suspend fun insert(config: WarpConfigEntity): Long {
        return warpConfigDao.insertConfig(config)
    }

    suspend fun deleteById(id: Int) {
        warpConfigDao.deleteConfigById(id)
    }

    suspend fun deleteAll() {
        warpConfigDao.deleteAllConfigs()
    }
}
