package com.app.core.data

import com.app.core.model.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject

class FirestoreApplicationDataSource @Inject constructor(private val firebase: FirebaseProvider) {
    suspend fun merge(application: JobApplication): JobApplication {
        val reference = firebase.firestore.collection("users").document(application.userId)
            .collection("applications").document(application.id)
        val merged = firebase.firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            val remote = if (snapshot.exists()) snapshot.toApplication(application.userId) else null
            val winner = remote?.let { ConflictResolver.winner(application, it).copy(createdAt = it.createdAt) } ?: application
            if (remote != winner) transaction.set(reference, winner.toDocument())
            winner
        }.await()
        // A server read confirms delivery before the durable local outbox is acknowledged.
        val confirmed = reference.get(Source.SERVER).await().toApplication(application.userId)
        if (ConflictResolver.winner(merged, confirmed) != confirmed) throw AppException(Failure.CONFLICT)
        return confirmed
    }
    suspend fun fetch(userId: String): List<JobApplication> =
        firebase.firestore.collection("users").document(userId).collection("applications")
            .get(Source.SERVER).await().documents.map { it.toApplication(userId) }

    private fun DocumentSnapshot.toApplication(userId: String): JobApplication = try {
        fun text(key: String) = getString(key) ?: throw AppException(Failure.MALFORMED_DATA)
        JobApplication(id, userId, text("company"), text("role"), text("location"),
            WorkMode.valueOf(text("workMode")), LocalDate.parse(text("appliedOn")),
            ApplicationStatus.valueOf(text("status")), text("jobUrl"), text("notes"),
            getLong("createdAt") ?: throw AppException(Failure.MALFORMED_DATA),
            getLong("updatedAt") ?: throw AppException(Failure.MALFORMED_DATA),
            getBoolean("deleted") ?: false, text("revision")).also { ApplicationValidator.validate(it) }
    } catch (error: IllegalArgumentException) { throw AppException(Failure.MALFORMED_DATA, error) }
      catch (error: java.time.format.DateTimeParseException) { throw AppException(Failure.MALFORMED_DATA, error) }

    private fun JobApplication.toDocument(): Map<String, Any> = mapOf(
        "userId" to userId, "company" to company, "role" to role, "location" to location,
        "workMode" to workMode.name, "appliedOn" to appliedOn.toString(), "status" to status.name,
        "jobUrl" to jobUrl, "notes" to notes, "createdAt" to createdAt, "updatedAt" to updatedAt,
        "deleted" to deleted, "revision" to revision,
    )
}
