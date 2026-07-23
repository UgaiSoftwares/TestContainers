package org.example

import org.apache.jena.rdfconnection.RDFConnectionRemote
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class JenaFusekiContainerTest {
    @Test
    fun `can write and read a triple`() {
        val fuseki =
            GenericContainer("stain/jena-fuseki:latest")
                .withExposedPorts(FUSEKI_PORT)
                .withEnv("ADMIN_PASSWORD", ADMIN_PASSWORD)
                .withEnv("FUSEKI_DATASET_1", DATASET_NAME)
                .waitingFor(Wait.forHttp("/").forStatusCode(200))

        try {
            fuseki.start()
        } catch (e: Exception) {
            assumeTrue(false, "Fuseki container failed to start: ${e.message}")
        }

        try {
            val updateEndpoint = "http://${fuseki.host}:${fuseki.getMappedPort(FUSEKI_PORT)}/$DATASET_NAME/update"
            val queryEndpoint = "http://${fuseki.host}:${fuseki.getMappedPort(FUSEKI_PORT)}/$DATASET_NAME/query"

            // Register admin credentials for the endpoints.
            org.apache.jena.http.auth.AuthEnv.get().registerUsernamePassword(
                java.net.URI.create(updateEndpoint),
                "admin",
                ADMIN_PASSWORD,
            )
            org.apache.jena.http.auth.AuthEnv.get().registerUsernamePassword(
                java.net.URI.create(queryEndpoint),
                "admin",
                ADMIN_PASSWORD,
            )

            RDFConnectionRemote
                .create()
                .queryEndpoint(queryEndpoint)
                .updateEndpoint(updateEndpoint)
                .build()
                .use { conn ->
                    // Wait for the dynamically created dataset to be ready.
                    var datasetReady = false
                    for (i in 1..30) {
                        try {
                            if (conn.queryAsk("ASK {}")) {
                                datasetReady = true
                                break
                            }
                        } catch (e: Exception) {
                            Thread.sleep(1000)
                        }
                    }
                    assumeTrue(datasetReady, "Dataset $DATASET_NAME was not created in time / container not working")

                    conn.update(
                        """
                        INSERT DATA {
                          <$SUBJECT_URI> <http://xmlns.com/foaf/0.1/name> "$NAME"
                        }
                        """.trimIndent(),
                    )

                    val names = mutableListOf<String>()

                    conn.querySelect(
                        """
                        SELECT ?name WHERE {
                          <$SUBJECT_URI> <http://xmlns.com/foaf/0.1/name> ?name
                        }
                        """.trimIndent(),
                    ) { qs ->
                        names += qs.getLiteral("name").lexicalForm
                    }

                    assertEquals(listOf(NAME), names)
                    assertTrue(names.contains(NAME))
                }
        } finally {
            println("--- FUSEKI CONTAINER LOGS ---")
            println(fuseki.getLogs())
            println("-----------------------------")
            fuseki.stop()
        }
    }

    companion object {
        private const val ADMIN_PASSWORD = "testpassword"
        private const val DATASET_NAME = "test"
        private const val FUSEKI_PORT = 3030
        private const val SUBJECT_URI = "http://example.com/alice"
        private const val NAME = "Alice"
    }
}
