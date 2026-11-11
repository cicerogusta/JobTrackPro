package com.app.core.database

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, JobTrackDatabase::class.java).build()
        dao = database.applications()
    }
    @After fun close() { database.close() }
    private fun row(user: String = "alice", revision: String = "a") =
        JobApplication("1", user, "Example", "Android", "Remote", WorkMode.REMOTE, LocalDate.now(),
            ApplicationStatus.APPLIED, "", "", 1, 1, revision = revision).toEntity(true)
    @Test fun crudAndUserIsolation() = runBlocking {
        dao.saveLocal(row(), "")
        dao.saveLocal(row("bob"), "")
        assertEquals(1, dao.observe("alice").first().size)
        assertEquals(1, dao.observe("bob").first().size)
        dao.saveLocal(row().copy(role = "Senior", updatedAt = 2, revision = "b"), "a")
        assertEquals("Senior", dao.find("alice", "1")?.role)
        dao.saveLocal(requireNotNull(dao.find("alice", "1")).copy(deleted = true, updatedAt = 3, revision = "c"), "b")
        assertTrue(dao.observe("alice").first().isEmpty())
        assertTrue(requireNotNull(dao.find("alice", "1")).dirty)
        assertEquals(1, dao.observe("bob").first().size)
    }
    @Test fun acknowledgementDoesNotEraseNewerLocalEdit() = runBlocking {
        val first = row()
        dao.saveLocal(first, "")
        dao.saveLocal(first.copy(updatedAt = 2, revision = "b"), "a")
        dao.mergeRemote(first.copy(dirty = false))
        assertEquals("b", dao.find("alice", "1")?.revision)
        assertTrue(requireNotNull(dao.find("alice", "1")).dirty)
        dao.mergeRemote(first.copy(updatedAt = 2, revision = "b", dirty = false))
        assertFalse(requireNotNull(dao.find("alice", "1")).dirty)
    }
    @Test fun staleWritesAreRejected() = runBlocking {
        dao.saveLocal(row(), "")
        try { dao.saveLocal(row().copy(revision = "b"), "stale"); fail("Expected conflict") }
        catch (error: AppException) { assertEquals(Failure.CONFLICT, error.failure) }
    }
    @Test fun remoteTombstoneHidesRowAndSurvivesOlderRemoteData() = runBlocking {
        dao.mergeRemote(row().copy(dirty = false))
        dao.mergeRemote(row(revision = "b").copy(updatedAt = 2, deleted = true, dirty = false))
        dao.mergeRemote(row().copy(dirty = false))
        assertTrue(dao.observe("alice").first().isEmpty())
        assertTrue(requireNotNull(dao.find("alice", "1")).deleted)
    }
}
