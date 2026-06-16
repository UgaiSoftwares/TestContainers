package org.example

import org.neo4j.driver.AuthTokens
import org.neo4j.driver.GraphDatabase
import org.neo4j.driver.QueryConfig

private const val DEFAULT_URI = "neo4j://localhost:7687"
private const val DEFAULT_USER = "neo4j"
private const val DEFAULT_PASSWORD = "password"
private const val DEFAULT_DATABASE = "neo4j"

fun main() {
    val uri = env("NEO4J_URI", DEFAULT_URI)
    val user = env("NEO4J_USER", DEFAULT_USER)
    val password = env("NEO4J_PASSWORD", DEFAULT_PASSWORD)
    val database = env("NEO4J_DATABASE", DEFAULT_DATABASE)

    GraphDatabase.driver(uri, AuthTokens.basic(user, password)).use { driver ->
        driver.verifyConnectivity()
        println("Connected to $uri")

        val createResult =
            driver
                .executableQuery(
                    """
                    MERGE (alice:Person {name: ${'$'}name})
                    MERGE (bob:Person {name: ${'$'}friendName})
                    MERGE (alice)-[:KNOWS]->(bob)
                    RETURN alice.name AS person, bob.name AS friend
                    """.trimIndent(),
                ).withParameters(
                    mapOf(
                        "name" to "Alice",
                        "friendName" to "Bob",
                    ),
                ).withConfig(QueryConfig.builder().withDatabase(database).build())
                .execute()

        createResult.records().forEach { record ->
            println("Created: ${record.get("person").asString()} -> ${record.get("friend").asString()}")
        }

        val queryResult =
            driver
                .executableQuery(
                    """
                    MATCH (person:Person)-[:KNOWS]->(friend:Person)
                    RETURN person.name AS person, friend.name AS friend
                    ORDER BY person, friend
                    """.trimIndent(),
                ).withConfig(QueryConfig.builder().withDatabase(database).build())
                .execute()

        println("Known relationships:")
        queryResult.records().forEach { record ->
            println("${record.get("person").asString()} knows ${record.get("friend").asString()}")
        }
    }
}

private fun env(
    name: String,
    defaultValue: String,
): String = System.getenv(name)?.takeIf { it.isNotBlank() } ?: defaultValue
