package com.app.core.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.app.core.database.*
import com.app.core.model.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

class SyncIntegrationTest {
    private lateinit var firebase: FirebaseProvider
    private lateinit var auth: AuthRepository
    private lateinit var firstDb: JobTrackDatabase
    private lateinit var secondDb: JobTrackDatabase
    private lateinit var first: ApplicationRepository
    private lateinit var second: ApplicationRepository
    private lateinit var firstSync: SyncRepository
    private lateinit var secondSync: SyncRepository

    @Before fun setup() = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_EMULATORS)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        firebase = FirebaseProvider(context)
        auth = FirebaseAuthRepository(firebase)
        auth.signOut()
        auth.register("sync-${UUID.randomUUID()}@example.com", "Testing123!", "Sync QA")
        firstDb = Room.inMemoryDatabaseBuilder(context, JobTrackDatabase::class.java).build()
        secondDb = Room.inMemoryDatabaseBuilder(context, JobTrackDatabase::class.java).build()
        val firstLocal = LocalApplicationDataSource(firstDb.applications())
        val secondLocal = LocalApplicationDataSource(secondDb.applications())
        first = OfflineApplicationRepository(firstLocal, auth)
        second = OfflineApplicationRepository(secondLocal, auth)
        val remote = FirestoreApplicationDataSource(firebase)
        firstSync = FirestoreSyncRepository(auth, firstLocal, remote)
        secondSync = FirestoreSyncRepository(auth, secondLocal, remote)
    }
    @After fun close() {
        if (::firstDb.isInitialized) firstDb.close()
        if (::secondDb.isInitialized) secondDb.close()
    }
    private fun application() = JobApplication(UUID.randomUUID().toString(), auth.requireUserId(), "Example", "Android",
        "Remote", WorkMode.REMOTE, LocalDate.now(), ApplicationStatus.APPLIED, "", "", 1, 1)
    @Test fun twoLocalStoresConvergeAndDeletionPropagates() = runBlocking {
        val initial = application()
        first.save(initial)
        withTimeout(30_000) { firstSync.synchronize(); secondSync.synchronize() }
        assertEquals(SyncPhase.SUCCESS, firstSync.state.value.phase)
        assertEquals(first.find(initial.id), second.find(initial.id))
        second.save(requireNotNull(second.find(initial.id)).copy(status = ApplicationStatus.INTERVIEW))
        withTimeout(30_000) { secondSync.synchronize(); firstSync.synchronize() }
        assertEquals(ApplicationStatus.INTERVIEW, first.find(initial.id)?.status)
        first.delete(initial.id)
        withTimeout(30_000) { firstSync.synchronize(); secondSync.synchronize() }
        assertNull(second.find(initial.id))
        assertTrue(requireNotNull(secondDb.applications().find(auth.requireUserId(), initial.id)).deleted)
    }
    @Test fun interruptedConnectionPreservesOutboxAndResumes() = runBlocking {
        val row = application()
        firebase.firestore.disableNetwork().await()
        try {
            first.save(row)
            assertTrue("A local offline write must enter the outbox", firstDb.applications().pending(auth.requireUserId()).isNotEmpty())
            withTimeout(30_000) { firstSync.synchronize() }
            assertEquals(SyncPhase.ERROR, firstSync.state.value.phase)
            assertTrue(firstDb.applications().pending(auth.requireUserId()).isNotEmpty())
            assertNotNull(first.find(row.id))
        } finally { firebase.firestore.enableNetwork().await() }
        withTimeout(30_000) { firstSync.synchronize(); secondSync.synchronize() }
        assertEquals(SyncPhase.SUCCESS, firstSync.state.value.phase)
        assertTrue(firstDb.applications().pending(auth.requireUserId()).isEmpty())
        assertEquals(first.find(row.id), second.find(row.id))
    }
}
