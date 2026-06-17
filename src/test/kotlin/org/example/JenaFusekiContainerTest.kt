package org.example

import org.apache.jena.sparql.exec.http.QueryExecHTTP
import org.apache.jena.sparql.exec.http.UpdateExecHTTP
import org.apache.jena.sparql.exec.http.UpdateSendMode
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JenaFusekiContainerTest {
    @Test
    fun `can write and read a triple`() {
        val fuseki =
            GenericContainer("stain/jena-fuseki:latest")
                .withExposedPorts(FUSEKI_PORT)
                .withEnv("ADMIN_PASSWORD", ADMIN_PASSWORD)
                .withEnv("FUSEKI_DATASET_1", DATASET_NAME)
                .waitingFor(Wait.forHttp("/").forStatusCode(200))

        fuseki.start()

        try {
            val updateEndpoint = "http://${fuseki.host}:${fuseki.getMappedPort(FUSEKI_PORT)}/$DATASET_NAME/update"
            val queryEndpoint = "http://${fuseki.host}:${fuseki.getMappedPort(FUSEKI_PORT)}/$DATASET_NAME/query"

            // Register admin credentials for the endpoints
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

            // Wait for the dynamically created dataset to be ready
            var datasetReady = false
            for (i in 1..30) {
                try {
                    QueryExecHTTP
                        .service(queryEndpoint)
                        .useGet()
                        .query("ASK {}")
                        .build()
                        .use { it.ask() }
                    datasetReady = true
                    break
                } catch (e: Exception) {
                    Thread.sleep(1000)
                }
            }
            if (!datasetReady) {
                throw AssertionError("Dataset $DATASET_NAME was not created in time")
            }

            UpdateExecHTTP
                .service(updateEndpoint)
                .sendMode(UpdateSendMode.asPost)
                .update(
                    """
                    INSERT DATA {
                      <$SUBJECT_URI> <http://xmlns.com/foaf/0.1/name> "$NAME"
                    }
                    """.trimIndent(),
                ).build()
                .execute()

            val names = mutableListOf<String>()

            QueryExecHTTP
                .service(queryEndpoint)
                .useGet()
                .query(
                    """
                    SELECT ?name WHERE {
                      <$SUBJECT_URI> <http://xmlns.com/foaf/0.1/name> ?name
                    }
                    """.trimIndent(),
                ).build()
                .use { queryExec ->
                    val rowSet = queryExec.select()
                    try {
                        while (rowSet.hasNext()) {
                            names += rowSet.next().get("name").getLiteralLexicalForm()
                        }
                    } finally {
                        rowSet.close()
                    }
                }

            assertEquals(listOf(NAME), names)
            assertTrue(names.contains(NAME))
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
