package com.app.core.network

import com.app.core.model.AppException
import com.app.core.model.Failure
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class JobsApiTest {
    @Test
    fun getJobs_validResponse_mapsFieldsAndUsesDocumentedRoute() = runTest {
        val server = MockWebServer()
        try {
            server.enqueue(
                MockResponse()
                    .setBody(
                        """{"data":[{"slug":"job-1","company_name":"Example","title":"Android","location":"Berlin","remote":true,"url":"https://example.com/job","description":"Build apps"}]}"""
                    )
            )
            server.start()
            val api =
                Retrofit.Builder()
                    .baseUrl(server.url("/"))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(JobsApi::class.java)
            val opening = requireNotNull(api.getJobs().data).single().toDomain()
            assertEquals("Example", opening.company)
            assertTrue(opening.remote)
            assertEquals("/api/job-board-api", server.takeRequest().path)
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
    fun getJobs_emptyResponse_returnsEmptyList() = runTest {
        val server = MockWebServer()
        try {
            server.enqueue(MockResponse().setBody("""{"data":[]}"""))
            server.start()
            val api =
                Retrofit.Builder()
                    .baseUrl(server.url("/"))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(JobsApi::class.java)
            assertTrue(requireNotNull(api.getJobs().data).isEmpty())
        } finally {
            server.shutdown()
        }
    }
}
