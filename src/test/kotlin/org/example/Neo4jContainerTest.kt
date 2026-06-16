package org.example

import org.junit.jupiter.api.Test
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.QueryConfig
import org.testcontainers.containers.Neo4jContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals

@Testcontainers
class Neo4jContainerTest {
    companion object {
        private const val ADMIN_PASSWORD = "testpassword"

        @Container
        @JvmStatic
        val neo4j =
            Neo4jContainer("neo4j:5.26")
                .withAdminPassword(ADMIN_PASSWORD)
    }

    @Test
    fun `can write and read data`() {
        GraphDatabase.driver(neo4j.boltUrl, AuthTokens.basic("neo4j", ADMIN_PASSWORD)).use { driver ->
            driver.verifyConnectivity()

            driver
                .executableQuery(
                    """
                    MERGE (alice:Person {name: ${'$'}name})
                    MERGE (bob:Person {name: ${'$'}friendName})
                    MERGE (alice)-[:KNOWS]->(bob)
                    """.trimIndent(),
                ).withParameters(
                    mapOf(
                        "name" to "Alice",
                        "friendName" to "Bob",
                    ),
                ).withConfig(QueryConfig.builder().withDatabase("neo4j").build())
                .execute()

            val names =
                driver
                    .executableQuery(
                        """
                        MATCH (p:Person)-[:KNOWS]->(friend:Person)
                        RETURN p.name AS person, friend.name AS friend
                        ORDER BY person, friend
                        """.trimIndent(),
                    ).withConfig(QueryConfig.builder().withDatabase("neo4j").build())
                    .execute()
                    .records()
                    .map { record ->
                        "${record.get("person").asString()}->${record.get("friend").asString()}"
                    }

            assertEquals(listOf("Alice->Bob"), names)
        }
    }
}
