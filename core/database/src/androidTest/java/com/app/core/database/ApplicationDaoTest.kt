package com.app.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.app.core.model.*
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationDaoTest {
    private lateinit var database: JobTrackDatabase
    private lateinit var dao: ApplicationDao

    @Before
    fun setup() {
        database =
            Room.inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    JobTrackDatabase::class.java,
                )
                .build()
        dao = database.applications()
    }

    @After
    fun close() {
        database.close()
    }

    private fun row(user: String = "alice", revision: String = "a") =
        JobApplication(
                "1",
                user,
                "Example",
                "Android",
                "Remote",
                WorkMode.REMOTE,
                LocalDate.now(),
                ApplicationStatus.APPLIED,
                "",
                "",
                1,
                1,
                revision = revision,
            )
            .toEntity(true)

    @Test
    fun crud_differentOwners_keepsApplicationsIsolated() = runBlocking {
        dao.saveLocal(row(), "")
        dao.saveLocal(row("bob"), "")
        assertEquals(1, dao.observe("alice").first().size)
        assertEquals(1, dao.observe("bob").first().size)
        dao.saveLocal(row().copy(role = "Senior", updatedAt = 2, revision = "b"), "a")
        assertEquals("Senior", dao.find("alice", "1")?.role)
        dao.saveLocal(
            requireNotNull(dao.find("alice", "1"))
                .copy(deleted = true, updatedAt = 3, revision = "c"),
            "b",
        )
        assertTrue(dao.observe("alice").first().isEmpty())
        assertTrue(requireNotNull(dao.find("alice", "1")).dirty)
        assertEquals(1, dao.observe("bob").first().size)
    }

    @Test
    fun mergeRemote_localEditNewer_preservesPendingRevision() = runBlocking {
        val first = row()
        dao.saveLocal(first, "")
        dao.saveLocal(first.copy(updatedAt = 2, revision = "b"), "a")
        dao.mergeRemote(first.copy(dirty = false))
        assertEquals("b", dao.find("alice", "1")?.revision)
        assertTrue(requireNotNull(dao.find("alice", "1")).dirty)
        dao.mergeRemote(first.copy(updatedAt = 2, revision = "b", dirty = false))
        assertFalse(requireNotNull(dao.find("alice", "1")).dirty)
    }

    @Test
    fun saveLocal_staleRevision_throwsConflict() = runBlocking {
        dao.saveLocal(row(), "")
        try {
            dao.saveLocal(row().copy(revision = "b"), "stale")
            fail("Expected conflict")
        } catch (error: AppException) {
            assertEquals(Failure.CONFLICT, error.failure)
        }
    }

    @Test
    fun mergeRemote_deletedRecordAndOlderSnapshot_keepsTombstone() = runBlocking {
        dao.mergeRemote(row().copy(dirty = false))
        dao.mergeRemote(row(revision = "b").copy(updatedAt = 2, deleted = true, dirty = false))
        dao.mergeRemote(row().copy(dirty = false))
        assertTrue(dao.observe("alice").first().isEmpty())
        assertTrue(requireNotNull(dao.find("alice", "1")).deleted)
    }

    @Test
    fun pending_databaseReopened_retainsUnsynchronizedWrites() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "persistence-qa-${java.util.UUID.randomUUID()}.db"
        var persisted = Room.databaseBuilder(context, JobTrackDatabase::class.java, name).build()
        try {
            persisted.applications().saveLocal(row(), "")
            persisted.close()
            persisted = Room.databaseBuilder(context, JobTrackDatabase::class.java, name).build()
            assertEquals("Android", persisted.applications().find("alice", "1")?.role)
            assertEquals(1, persisted.applications().pending("alice").size)
            assertTrue(persisted.applications().pending("bob").isEmpty())
        } finally {
            persisted.close()
            context.deleteDatabase(name)
        }
    }
}
