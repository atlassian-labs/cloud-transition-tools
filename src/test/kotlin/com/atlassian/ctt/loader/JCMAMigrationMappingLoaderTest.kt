import com.atlassian.ctt.data.loader.LoaderStatusCode
import com.atlassian.ctt.data.loader.MigrationScope
import com.atlassian.ctt.data.loader.jcma.JCMAMigrationMappingLoader
import com.atlassian.ctt.data.store.MigrationStore
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JCMAMigrationMappingLoaderTest {
    private lateinit var mockWebServer: MockWebServer

    private val mappingDataCSV =
        """
        entityType,serverId,cloudId
        jira/classic:issueType,10000,10000
        jira:issue,17499,10542
        jira:issue,17589,10452
        jira:projectVersion,10011,10045
        jira:comment,16412,11782
        jira/classic:project,10200,10037
        jira/classic:project,10102,10041
        """.trimIndent()
    private val mappingCount = 7

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `load -  invalid server URL`() =
        runBlocking {
            val scope = MigrationScope("cloudSiteURL", "not a valid url")
            val authHeader = "authHeader"
            val dataStore = mockk<MigrationStore>(relaxed = true)
            val loader = JCMAMigrationMappingLoader(scope, authHeader)

            val status = loader.load(dataStore, reload = true)
            assertEquals(LoaderStatusCode.FAILED, status.code)
        }

    @Test
    fun `load -  Server down`() =
        runBlocking {
            val scope = MigrationScope("cloudSiteURL", "http://localhost/invalid")
            val authHeader = "authHeader"
            val dataStore = mockk<MigrationStore>(relaxed = true)
            val loader = JCMAMigrationMappingLoader(scope, authHeader)

            val status = loader.load(dataStore, reload = true)
            assertEquals(LoaderStatusCode.FAILED, status.code)
        }

    @Test
    fun `load -  Server Timeout`() =
        runBlocking {
            val scope = MigrationScope("cloudSiteURL", mockWebServer.url("/serverA").toString())
            val authHeader = "authHeader"
            val dataStore = mockk<MigrationStore>(relaxed = true)
            val loader = JCMAMigrationMappingLoader(scope, authHeader)

            val status = loader.load(dataStore, reload = true)
            assertEquals(LoaderStatusCode.FAILED, status.code)
        }

    @Test
    fun `load - invalid cloud URL`() =
        runBlocking {
            val scope = MigrationScope("cloudSiteURL", mockWebServer.url("/serverA").toString())
            val authHeader = "authHeader"
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(400)
                    .setBody("No migrationScopeId found for the cloudSiteUrl $scope.cloudSiteURL"),
            )
            val dataStore = mockk<MigrationStore>(relaxed = true)
            val loader = JCMAMigrationMappingLoader(scope, authHeader)

            val status = loader.load(dataStore, reload = true)
            assertEquals(LoaderStatusCode.FAILED, status.code)
        }

    @Test
    fun `load - Request accepted followed by failure`() =
        runBlocking {
            val scope = MigrationScope("cloudSiteURL", mockWebServer.url("/serverA").toString())
            val authHeader = "authHeader"
            mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody(""))
            val dataStore = mockk<MigrationStore>(relaxed = true)
            val loader = JCMAMigrationMappingLoader(scope, authHeader)
            val status = loader.load(dataStore, reload = true)
            assertEquals(LoaderStatusCode.FAILED, status.code)
        }

    @Test
    fun `load - No Reload`() =
        runBlocking {
            val scope = MigrationScope("cloudSiteURL", mockWebServer.url("/serverA").toString())
            val authHeader = "authHeader"
            val dataStore = mockk<MigrationStore>(relaxed = true)
            val loader = JCMAMigrationMappingLoader(scope, authHeader)

            val status = loader.load(dataStore, reload = false)
            assertEquals(LoaderStatusCode.LOADED, status.code)
        }

    @Test
    fun `test load`() =
        runBlocking {
            val scope = MigrationScope("cloudSiteURL", mockWebServer.url("/serverA").toString())
            val authHeader = "authHeader"
            mockWebServer.enqueue(
                MockResponse().setResponseCode(200).setBody(mappingDataCSV).addHeader("Content-Type", "text/csv"),
            )
            val dataStore = mockk<MigrationStore>(relaxed = true)
            val loader = JCMAMigrationMappingLoader(scope, authHeader)

            var mappedEntities = 0
            coEvery { dataStore.store(any()) } answers { mappedEntities++ }
            val status = loader.load(dataStore, reload = true)
            assertEquals(LoaderStatusCode.LOADED, status.code)
            assertEquals(mappingCount, mappedEntities)
        }

    @Test
    fun `test load with unknown ARI`() =
        runBlocking {
            val scope = MigrationScope("cloudSiteURL", mockWebServer.url("/serverA").toString())
            val authHeader = "authHeader"
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("$mappingDataCSV\nunknown,10,10")
                    .addHeader("Content-Type", "text/csv"),
            )
            val dataStore = mockk<MigrationStore>(relaxed = true)
            val loader = JCMAMigrationMappingLoader(scope, authHeader)

            var mappedEntities = 0
            coEvery { dataStore.store(any()) } answers { mappedEntities++ }
            val status = loader.load(dataStore, reload = true)
            assertEquals(LoaderStatusCode.LOADED, status.code)
            assertEquals(mappingCount + 1, mappedEntities)
        }

    @Test
    fun `test load with invalid data`() =
        runBlocking {
            val scope = MigrationScope("cloudSiteURL", mockWebServer.url("/serverA").toString())
            val authHeader = "authHeader"
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("$mappingDataCSV\nunknown,\"string\",10\"")
                    .addHeader("Content-Type", "text/csv"),
            )
            val dataStore = mockk<MigrationStore>(relaxed = true)
            val loader = JCMAMigrationMappingLoader(scope, authHeader)

            var mappedEntities = 0
            coEvery { dataStore.store(any()) } answers { mappedEntities++ }
            val status = loader.load(dataStore, reload = true)
            assertEquals(LoaderStatusCode.LOADED, status.code)
            assertEquals(mappingCount, mappedEntities)
        }

    @Test
    fun `Request accepted followed by successful load`() =
        runBlocking {
            val scope = MigrationScope("cloudSiteURL", mockWebServer.url("/serverA").toString())
            val authHeader = "authHeader"
            mockWebServer.enqueue(MockResponse().setResponseCode(202).setBody(""))
            mockWebServer.enqueue(
                MockResponse().setResponseCode(200).setBody(mappingDataCSV).addHeader("Content-Type", "text/csv"),
            )
            val dataStore = mockk<MigrationStore>(relaxed = true)
            val loader = JCMAMigrationMappingLoader(scope, authHeader)

            var mappedEntities = 0
            coEvery { dataStore.store(any()) } answers { mappedEntities++ }
            val status = loader.load(dataStore, reload = true)
            assertEquals(LoaderStatusCode.LOADED, status.code)
            assertEquals(mappingCount, mappedEntities)
        }

    @Test
    fun `load - invalid response`() =
        runBlocking {
            val scope = MigrationScope("cloudSiteURL", mockWebServer.url("/serverA").toString())
            val authHeader = "authHeader"
            mockWebServer.enqueue(
                MockResponse().setResponseCode(200).setBody("").addHeader("Content-Type", "application/json"),
            )
            val dataStore = mockk<MigrationStore>(relaxed = true)
            val loader = JCMAMigrationMappingLoader(scope, authHeader)

            coEvery { dataStore.store(any()) } returns Unit
            val status = loader.load(dataStore, reload = true)
            assertEquals(LoaderStatusCode.FAILED, status.code)
        }

    @Test
    fun `load - invalid header`() =
        runBlocking {
            val scope = MigrationScope("cloudSiteURL", mockWebServer.url("/serverA").toString())
            val authHeader = "authHeader"
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        entity,serverId,cloudId
                        """.trimIndent(),
                    ).addHeader("Content-Type", "text/csv"),
            )
            val dataStore = mockk<MigrationStore>(relaxed = true)
            val loader = JCMAMigrationMappingLoader(scope, authHeader)

            coEvery { dataStore.store(any()) } returns Unit
            val status = loader.load(dataStore, reload = true)
            assertEquals(LoaderStatusCode.FAILED, status.code)
        }
}
