package com.app.core.database

import androidx.room.Entity
import com.app.core.model.ApplicationStatus
import com.app.core.model.JobApplication
import com.app.core.model.WorkMode
import java.time.LocalDate

@Entity(tableName = "applications", primaryKeys = ["userId", "id"])
data class ApplicationEntity(
    val id: String,
    val userId: String,
    val company: String,
    val role: String,
    val location: String,
    val workMode: String,
    val appliedOn: String,
    val status: String,
    val jobUrl: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean,
    val revision: String,
    val dirty: Boolean,
)
fun ApplicationEntity.toDomain() = JobApplication(
    id, userId, company, role, location, WorkMode.valueOf(workMode), LocalDate.parse(appliedOn),
    ApplicationStatus.valueOf(status), jobUrl, notes, createdAt, updatedAt, deleted, revision,
)
fun JobApplication.toEntity(dirty: Boolean) = ApplicationEntity(
    id, userId, company, role, location, workMode.name, appliedOn.toString(),
    status.name, jobUrl, notes, createdAt, updatedAt, deleted, revision, dirty,
)
