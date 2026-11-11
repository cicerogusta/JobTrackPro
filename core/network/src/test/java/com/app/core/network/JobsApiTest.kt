package com.app.core.network

import com.app.core.model.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class JobsApiTest {
    private fun api(server: MockWebServer): JobsApi =
        Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JobsApi::class.java)

    private fun dto(mode: String? = "hybrid") =
        JobDto(
            slug = "android-sp",
            company = "Example",
            title = "Android",
            country = "BR",
            city = "São Paulo",
            region = "São Paulo",
            remote = mode,
            url = "https://www.okemprego.com/br/empregos/android-sp",
            description = "Build apps",
        )

    @Test
    fun getJobs_brazilSearch_encodesFiltersAndMapsHybridVacancy() = runTest {
        val server = MockWebServer()
        try {
            server.enqueue(
                MockResponse()
                    .setBody(
                        """{"data":[{"slug":"android-sp","company":"Example","title":"Android","country":"BR","city":"São Paulo","region":"SP","remote":"hybrid","url":"https://www.okemprego.com/br/empregos/android-sp","description":"Build apps"}],"pagination":{"page":2,"totalPages":3}}"""
                    )
            )
            val result =
                api(server).getJobs(query = "Android Kotlin", location = "São Paulo", page = 2)
            val opening = requireNotNull(result.data).single().toDomain()
            assertEquals("okemprego:android-sp", opening.id)
            assertEquals("São Paulo, SP", opening.location)
            assertEquals(WorkMode.HYBRID, opening.workMode)
            val request = requireNotNull(server.takeRequest().requestUrl)
            assertEquals("/api/v1/jobs", request.encodedPath)
            assertEquals("BR", request.queryParameter("country"))
            assertEquals("Android Kotlin", request.queryParameter("q"))
            assertEquals("São Paulo", request.queryParameter("where"))
            assertEquals("2", request.queryParameter("page"))
            assertEquals("20", request.queryParameter("perPage"))
            assertEquals("newest", request.queryParameter("sort"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun toDomain_requiredFieldsMissing_throwsMalformedDataFailure() {
        val error = assertThrows(AppException::class.java) { JobDto(slug = "1").toDomain() }
        assertEquals(Failure.MALFORMED_DATA, error.failure)
    }

    @Test
    fun toDomain_foreignCountry_rejectsVacancy() {
        val error = assertThrows(AppException::class.java) { dto().copy(country = "PT").toDomain() }
        assertEquals(Failure.MALFORMED_DATA, error.failure)
    }

    @Test
    fun toDomain_supportedModes_preservesRemoteHybridAndOnSite() {
        mapOf("full" to WorkMode.REMOTE, "hybrid" to WorkMode.HYBRID, "onsite" to WorkMode.ON_SITE)
            .forEach { (value, expected) ->
                assertEquals(expected, dto(value).toDomain().workMode)
            }
    }

    @Test
    fun toDomain_unknownMode_doesNotInventWorkArrangement() {
        assertNull(dto(null).toDomain().workMode)
        assertNull(dto("unknown").toDomain().workMode)
        assertEquals("São Paulo", dto().toDomain().location)
    }

    @Test
    fun getJobs_emptyResponse_returnsEmptyList() = runTest {
        val server = MockWebServer()
        try {
            server.enqueue(
                MockResponse().setBody("""{"data":[],"pagination":{"page":1,"totalPages":0}}""")
            )
            assertTrue(requireNotNull(api(server).getJobs().data).isEmpty())
        } finally {
            server.shutdown()
        }
    }
}
