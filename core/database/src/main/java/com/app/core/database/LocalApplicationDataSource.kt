package com.app.core.database

import javax.inject.Inject

class LocalApplicationDataSource @Inject constructor(private val dao: ApplicationDao) {
    fun observe(userId: String) = dao.observe(userId)
    suspend fun find(userId: String, id: String) = dao.find(userId, id)
    suspend fun save(entity: ApplicationEntity, expectedRevision: String) = dao.saveLocal(entity, expectedRevision)
    suspend fun pending(userId: String) = dao.pending(userId)
    suspend fun mergeRemote(entity: ApplicationEntity) = dao.mergeRemote(entity)
}
