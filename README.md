# Neo4j sample

This project includes a small Kotlin console app that connects to Neo4j, creates a `Person` graph, and reads it back.

## Run

Set these environment variables if you are not using the defaults:

- `NEO4J_URI` - default `neo4j://localhost:7687`
- `NEO4J_USER` - default `neo4j`
- `NEO4J_PASSWORD` - default `password`
- `NEO4J_DATABASE` - default `neo4j`

Then run:

```bash
./gradlew run
```

The sample uses the official Neo4j Java driver and `driver.verifyConnectivity()` before running queries.

The Testcontainers test in `src/test/kotlin` requires Docker access to run.
