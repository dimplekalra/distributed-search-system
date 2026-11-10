# Search System Architecture

## Overview

The Search System is a distributed, production-inspired search engine built using Java 17 and Spring Boot. The system supports near real-time document indexing, distributed search execution, relevance ranking using BM25, phrase search, Boolean queries, fuzzy search, autocomplete, Redis caching, Kafka-based asynchronous ingestion, and segment-based storage.

The architecture is inspired by modern search engines such as Elasticsearch, Apache Solr, and Lucene while keeping the implementation understandable and modular.

Instead of relying on a single centralized index, documents are partitioned into shards and immutable index segments. Queries are executed in parallel across all shards, and the partial results are merged into a globally ranked response.

---

# Objectives

The primary objectives of the system are:

- Fast full-text search
- Near real-time indexing
- Horizontal scalability
- Fault tolerance
- High throughput
- Low latency
- Modular architecture
- Production-ready design principles

---

# Technology Stack

| Layer | Technology |
|--------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Build Tool | Maven |
| Database | PostgreSQL |
| Cache | Redis |
| Messaging | Apache Kafka |
| Search Index | Custom In-Memory Inverted Index |
| Ranking | BM25 + Coverage + Phrase Boost + Popularity |
| Serialization | Java Serialization |
| Concurrency | CompletableFuture + ExecutorService |
| Data Structure | Trie, HashMap, Priority Queue |

---

# High-Level Architecture

```
                      Client
                         |
                  REST Controllers
                         |
                --------------------
                |                  |
          Search API         Document API
                |                  |
                |             Kafka Producer
                |                  |
                |             Kafka Topic
                |                  |
                |            Kafka Consumer
                |                  |
                |          Indexing Worker
                |                  |
                |           Shard Router
                |                  |
      -----------------------------------------
      |              |             |           |
    Shard 1       Shard 2       Shard 3     Shard 4
      |              |             |           |
   Segments       Segments      Segments    Segments
      |              |             |           |
 Inverted Index  Inverted Index Inverted Index ...
```

---

# Major Components

The system consists of the following major components:

- REST Controllers
- Kafka Producer
- Kafka Consumer
- Indexing Worker
- Shard Router
- Shards
- Index Segments
- Inverted Index
- Query Coordinator
- Ranking Service
- Redis Cache
- Query Tracker
- Trie-based Autocomplete
- Boolean Query Engine
- Fuzzy Search Engine
- Background Segment Merge Worker

Each component has a single responsibility and communicates with the others through well-defined interfaces.

---

# End-to-End Indexing Flow

When a new document is added, the indexing pipeline is completely asynchronous.

```
POST /documents
        |
        v
DocumentController
        |
        v
Kafka Producer
        |
        v
Kafka Topic
        |
        v
Kafka Consumer
        |
        v
Indexing Worker
        |
        v
Shard Router
        |
        v
Correct Shard
        |
        v
Active Segment
        |
        v
Tokenizer
        |
        v
Inverted Index Updated
        |
        v
Trie Updated
        |
        v
Document Statistics Updated
```

### Why Kafka?

Kafka decouples document ingestion from indexing.

Instead of blocking the client while indexing, the API immediately publishes the document to Kafka. Dedicated consumers perform indexing in the background, improving throughput and allowing the system to absorb traffic spikes.

Benefits include:

- Asynchronous processing
- Back-pressure handling
- Improved scalability
- Loose coupling
- Retry capability
- Horizontal consumer scaling

---

# End-to-End Search Flow

```
Client

    |

GET /search?q=laptop

    |

SearchController

    |

QueryService

    |

Redis Cache

    |

Cache Hit ?
   /      \
 Yes      No
 |         |
Return   Query Coordinator
Cached       |
Results       |
              |
      Parallel Query Execution
              |
     --------------------------
     |      |      |         |
  Shard1 Shard2 Shard3 Shard4
     |      |      |         |
 Segment Search on each shard
     |      |      |         |
     ----------Merge----------
              |
      Top-K Aggregator
              |
       Ranking Service
              |
    Fetch Documents
              |
 Generate SearchResponse
              |
 Cache Results
              |
 Return Response
```

---

# Search Lifecycle

Every search request goes through the following stages:

1. Request validation
2. Query normalization
3. Cache lookup
4. Tokenization
5. Query type detection
6. Parallel shard execution
7. Segment-level search
8. BM25 scoring
9. Coverage scoring
10. Phrase boosting (if applicable)
11. Popularity boosting
12. Global Top-K aggregation
13. Document retrieval
14. Search response creation
15. Redis cache update
16. Response returned to the client

---

# Query Types Supported

The search engine supports multiple query types.

### Standard Search

```
iphone charger
```

Performs tokenization followed by BM25 ranking.

---

### Phrase Search

```
"wireless mouse"
```

Documents must contain the exact sequence of terms.

Position information stored in postings is used to verify adjacency.

---

### Boolean Search

Examples:

```
apple AND iphone

```

```
apple OR samsung

```

```
apple NOT iphone
```

The Boolean parser converts the query into Boolean tokens, and the Boolean processor executes logical set operations over posting lists.

---

### Fuzzy Search

Example:

```
iphnoe
```

If no exact match exists, the fuzzy search module finds the closest indexed term and automatically re-executes the search using the corrected term.

Example:

```
iphnoe

↓

iphone
```

---

### Autocomplete

Every indexed token is inserted into a Trie.

When the user types:

```
lap
```

the Trie returns suggestions such as:

```
laptop

laptop stand

laptop charger

laptop sleeve
```

---

# Request Processing Pipeline

```
HTTP Request

↓

Validation

↓

Normalize Query

↓

Cache Lookup

↓

Tokenizer

↓

Determine Query Type

↓

Distributed Search

↓

Ranking

↓

Aggregation

↓

Document Retrieval

↓

Snippet Generation

↓

Response Serialization

↓

HTTP Response
```

---

# Architectural Principles

The implementation follows several important software engineering principles:

- Separation of concerns
- Immutable index segments
- Event-driven ingestion
- Horizontal partitioning using shards
- Read-optimized storage
- Background maintenance tasks
- Parallel query execution
- Stateless REST APIs
- Cache-first search strategy
- Modular service design

Each module has a focused responsibility, making the system easier to test, maintain, and extend.

---

# Internal Storage Architecture

The search engine stores indexed data using a hierarchical storage model inspired by modern distributed search systems. Instead of maintaining a single global index, documents are partitioned into shards, and each shard manages multiple immutable index segments.

This design minimizes lock contention during concurrent reads and writes while allowing the system to scale horizontally as the number of indexed documents grows.

The storage hierarchy is organized as follows:

```text
Search Cluster
│
├── Shard 1
│   ├── Segment A
│   ├── Segment B
│   └── Active Segment
│
├── Shard 2
│   ├── Segment A
│   ├── Segment B
│   └── Active Segment
│
├── Shard 3
│   └── ...
│
└── Shard 4
```

Each level in this hierarchy has a clearly defined responsibility:

| Component | Responsibility |
|-----------|----------------|
| Shard Router | Determines which shard owns a document |
| Shard | Owns a subset of indexed documents |
| Index Segment | Stores a portion of the shard's inverted index |
| Inverted Index | Maps terms to matching documents |
| Posting List | Stores occurrences of each term within documents |

Separating storage into these layers allows indexing, searching, and maintenance operations to execute independently without affecting the overall availability of the system.

---

# Sharding Strategy

The search system uses **hash-based sharding** to distribute documents evenly across multiple shards.

Each document is assigned to a shard using its unique document identifier:

```java
shardId = Math.abs(documentId.hashCode()) % totalShards;
```

This deterministic mapping guarantees that:

- Every document always belongs to the same shard.
- Documents are distributed uniformly across shards.
- No centralized routing table is required.
- Search execution can occur in parallel.

The current implementation initializes four shards, but the routing logic is independent of the actual shard count and can be extended to support additional shards with minimal changes.

### Why Sharding?

Without sharding, every search request would traverse a single continuously growing index.

As the dataset increases, search latency and contention also increase.

By partitioning the index:

- Multiple CPU cores can process searches simultaneously.
- Individual shards remain smaller and faster to search.
- Failures can be isolated to a single shard.
- Additional shards can be introduced to support larger datasets.

This approach closely resembles the distributed indexing strategy used in production search platforms such as Elasticsearch and Apache Solr.

---

# Index Segment Architecture

Each shard consists of multiple **Index Segments**.

Segments are immutable once sealed.

Only a single **active segment** accepts new documents.

```text
Shard

├── Immutable Segment
├── Immutable Segment
├── Immutable Segment
└── Active Segment
```

When the active segment reaches the configured document threshold:

1. The active segment is sealed.
2. It becomes immutable.
3. A new active segment is created.
4. Future documents are indexed into the new segment.

Keeping historical segments immutable provides several advantages:

- Search operations never block writes.
- Existing posting lists never require in-place modification.
- Readers always observe a consistent view of the index.
- Background merge operations can safely combine older segments.

This append-only strategy significantly simplifies concurrency management while improving read performance.

---

# Inverted Index Design

Each index segment maintains its own inverted index.

Instead of storing documents sequentially, the inverted index maps each token to the documents that contain it.

Conceptually:

```text
Term
   ↓
Posting List
   ↓
Document IDs
```

For example:

```text
java
 ├── Doc-101
 ├── Doc-205
 └── Doc-412

spring
 ├── Doc-101
 ├── Doc-178
 └── Doc-501
```

Searching becomes efficient because the engine directly retrieves only the posting lists associated with the requested query terms instead of scanning every indexed document.

Each segment maintains its own inverted index, allowing independent indexing, searching, serialization, and background merging.

---

# Posting List Structure

Each unique term in the inverted index maps to a **posting list**, which contains information about every document in which that term appears.

Rather than storing only document identifiers, each posting maintains additional information required for ranking and phrase search.

A posting contains:

- Document Identifier
- Term Frequency
- Word Positions

The logical structure is illustrated below:

```text
Term: "spring"

↓

Posting List

├── Document: Doc-101
│   ├── Term Frequency: 3
│   └── Positions: [2, 9, 15]
│
├── Document: Doc-178
│   ├── Term Frequency: 1
│   └── Positions: [5]
│
└── Document: Doc-501
    ├── Term Frequency: 2
    └── Positions: [4, 11]
```

Each field serves a different purpose during query execution:

| Field | Purpose |
|--------|----------|
| Document ID | Identifies the matching document |
| Term Frequency | Used while computing BM25 relevance |
| Positions | Enables efficient phrase search |

Keeping positional information within the posting eliminates the need to scan entire document contents when evaluating phrase queries.

---

# Document Metadata

While the inverted index stores searchable terms, each segment also maintains lightweight metadata for every indexed document.

The metadata is stored in memory alongside the inverted index.

```text
DocumentMetadata

├── Document Length
├── Popularity Score
└── Created Timestamp
```

This metadata supports several ranking features without requiring additional database lookups during search execution.

The metadata is used for:

- BM25 document length normalization
- Popularity-based score boosting
- Future freshness-based ranking
- Performance optimization

By maintaining frequently accessed attributes inside each segment, ranking remains entirely in-memory and avoids unnecessary interaction with PostgreSQL.

---

# Tokenization Pipeline

Every document passes through a normalization pipeline before being indexed.

The tokenizer converts raw document text into searchable terms.

```text
Raw Document

↓

Convert to Lowercase

↓

Remove Punctuation

↓

Split into Individual Tokens

↓

Store Tokens in Inverted Index

↓

Insert Tokens into Trie
```

For example:

Input:

```text
Java, Spring Boot with Kafka!
```

Generated Tokens:

```text
java
spring
boot
with
kafka
```

Normalizing terms during indexing ensures that equivalent words are stored consistently, improving search accuracy while reducing duplicate entries within the index.

---

# Query Execution Inside a Segment

Once a query reaches an individual segment, the search process follows a deterministic pipeline.

```text
Receive Query

↓

Lookup Posting Lists

↓

Calculate BM25 Score

↓

Compute Query Coverage

↓

Apply Phrase Boost (if applicable)

↓

Apply Popularity Boost

↓

Generate Ranked Results
```

For each query term, the segment retrieves the corresponding posting list from the inverted index.

Matching postings contribute to the relevance score of each document.

The scores are accumulated across all query terms before the final ranking phase begins.

This design ensures that every segment independently computes relevance using only its local data, allowing searches to execute concurrently across multiple shards.

---

# BM25 Ranking Algorithm

The primary relevance algorithm used by the search engine is **BM25 (Best Matching 25)**.

BM25 is one of the most widely adopted ranking algorithms in modern search engines because it balances several important factors:

- Term frequency
- Document frequency
- Document length
- Average document length

Unlike simple keyword counting, BM25 assigns greater importance to rare terms while preventing excessively long documents from receiving unfairly high scores.

Within each segment, BM25 is calculated for every matching posting before any application-specific ranking adjustments are applied.

The resulting BM25 score serves as the foundation for all subsequent ranking operations.

---

# Final Ranking Pipeline

After computing the BM25 relevance score, additional ranking signals are applied to improve result quality.

The final score is composed of multiple independent components.

```text
Final Score

=

BM25 Score

+

Coverage Boost

+

Phrase Match Boost

+

Popularity Boost
```

### Query Coverage

Documents matching a larger proportion of the user's query receive additional weight.

For example, a document matching all query terms is ranked higher than one matching only a subset.

### Phrase Match

If the query is identified as a phrase search, documents containing the exact sequence of words receive an additional boost.

### Popularity

Frequently accessed documents accumulate a popularity score.

A logarithmic boost is applied during ranking to promote popular documents without allowing popularity to dominate textual relevance.

Combining these signals produces search results that are both relevant and aligned with user behavior.

---

# Distributed Top-K Aggregation

Each shard independently computes and ranks its local search results.

Instead of returning every matching document, only the highest-ranked results are returned to the Query Coordinator.

```text
Shard 1 → Top 10

Shard 2 → Top 10

Shard 3 → Top 10

Shard 4 → Top 10

↓

Top-K Result Aggregator

↓

Globally Ranked Results
```

The coordinator merges these partial rankings using a priority queue and selects the highest scoring documents across all shards.

This significantly reduces:

- Memory consumption
- Network overhead
- Sorting cost
- Overall response latency

The aggregation strategy scales efficiently even as the number of shards increases because each shard contributes only a limited number of candidate results.

---

# Background Segment Merge

As documents continue to be indexed, each shard gradually accumulates multiple immutable segments.

Searching a large number of small segments introduces unnecessary overhead.

To address this, a background merge worker periodically combines smaller immutable segments into a larger optimized segment.

```text
Segment A

+

Segment B

+

Segment C

↓

Merged Segment
```

The merge process:

1. Reads immutable segments.
2. Combines their inverted indexes.
3. Merges associated document metadata.
4. Creates a new optimized segment.
5. Replaces the original segments atomically.

Because only immutable segments participate in merging, search requests continue uninterrupted throughout the process.

This strategy improves long-term query performance while minimizing fragmentation.

---

# Concurrency Model

The search system is designed to support multiple concurrent indexing and search operations.

Several concurrency mechanisms are used throughout the implementation.

| Mechanism | Purpose |
|-----------|----------|
| ReadWriteLock | Allows concurrent searches while protecting structural updates |
| CompletableFuture | Executes shard searches in parallel |
| ExecutorService | Manages the search thread pool |
| Immutable Segments | Eliminates synchronization during reads |
| Background Workers | Performs indexing and segment merging asynchronously |

Search requests primarily acquire read locks, allowing multiple queries to execute simultaneously.

Operations that modify shard structure, such as replacing merged segments, acquire write locks to ensure consistency.

This design minimizes contention while maintaining thread safety.

---

# Time Complexity Analysis

The major search operations have the following computational complexity.

| Operation | Time Complexity |
|-----------|-----------------|
| Document Routing | O(1) |
| Tokenization | O(n) |
| Inverted Index Insertion | O(t) |
| Posting Lookup | O(1) average |
| BM25 Score Calculation | O(p) |
| Phrase Search | O(p × positions) |
| Boolean Query Processing | O(p) |
| Trie Autocomplete | O(prefix + results) |
| Top-K Aggregation | O(k log s) |

Where:

- **n** = document length
- **t** = number of unique tokens
- **p** = number of matching postings
- **k** = number of requested results
- **s** = number of shards

These characteristics allow the system to maintain efficient search performance while scaling across increasing document volumes.

---

# Fault Tolerance and Recovery

The search system is designed to tolerate partial failures during distributed query execution. Since every search request is executed independently across multiple shards, the failure of an individual shard does not prevent the entire query from completing.

The `QueryCoordinatorService` dispatches search requests to every shard using `CompletableFuture`. Each shard executes independently with a configurable timeout. If a shard becomes unavailable, exceeds the configured timeout, or throws an exception during execution, the coordinator gracefully ignores the failed shard and continues processing results from the remaining healthy shards.

```
Client Query
      |
      |
      v
-------------------------------
| Shard1 | Shard2 | Shard3 | Shard4 |
-------------------------------
     |        X         |        |
     |     Timeout      |        |
     |                  |        |
     +--------+---------+--------+
              |
      Top-K Aggregator
              |
       Partial Results
              |
        Return Response
```

This approach provides several advantages:

- Partial search availability during failures
- Isolation of shard failures
- Reduced cascading failures
- Faster response through timeout protection
- Better user experience under degraded conditions

Although the current implementation focuses on graceful degradation, the architecture is designed so that replica shards, automatic failover, retry policies, and health-aware routing can be incorporated in future versions.

---

# Caching Architecture

Executing identical distributed searches repeatedly is computationally expensive. To reduce latency and improve throughput, the search system uses Redis as a distributed cache.

Every incoming query is normalized before cache lookup.

```
Incoming Query

↓

Normalize Query

↓

Redis Lookup

↓

Cache Hit?

 ┌─────────────┐
 │             │
Yes           No
 │             │
 │             │
Return     Distributed
Cached       Search
Result         │
               │
               ↓
        Cache Response
```

The cache stores the complete search response, including document identifiers, relevance scores, generated snippets, and pagination information.

Using Redis provides several benefits:

- Lower query latency
- Reduced CPU utilization
- Lower shard workload
- Reduced database access
- Improved throughput for repeated searches

Frequently executed queries are therefore served directly from Redis without invoking the distributed search pipeline.

---

# Snapshot and Recovery

The search index primarily resides in memory for fast query execution. To avoid rebuilding the complete index after every application restart, each shard can export its current state as a snapshot.

A snapshot contains:

- Immutable index segments
- Active segment
- Inverted indexes
- Posting lists
- Document metadata

```
Shard

↓

Export Snapshot

↓

SnapshotData

↓

Persistent Storage
```

During application startup, snapshots can be restored into memory.

```
Persistent Storage

↓

Load Snapshot

↓

Restore Segments

↓

Initialize Services

↓

Ready for Search
```

Snapshot restoration significantly reduces startup time because the system can reload the serialized index instead of reconstructing it from the relational database.

Future improvements may include:

- Incremental snapshots
- Compressed snapshots
- Versioned snapshots
- Remote snapshot storage
- Automatic snapshot scheduling

---

# Scalability Characteristics

The architecture has been designed to scale both horizontally and vertically.

## Horizontal Scaling

Documents are partitioned across multiple shards using deterministic hash-based routing.

Adding additional shards increases indexing throughput while reducing the amount of data stored within each individual shard.

```
Before

Shard 1
Shard 2
Shard 3
Shard 4

↓

After

Shard 1
Shard 2
Shard 3
Shard 4
Shard 5
Shard 6
Shard 7
Shard 8
```

Horizontal scaling provides:

- Increased indexing throughput
- Reduced query latency
- Better workload distribution
- Improved fault isolation

## Vertical Scaling

Each shard executes search requests concurrently using a thread pool managed by `ExecutorService`.

Additional CPU cores allow more shard searches to execute simultaneously, improving overall throughput.

Vertical scaling improves:

- Concurrent search execution
- Background merge throughput
- Kafka consumer throughput
- Parallel indexing performance

---

# Security Considerations

The primary objective of this project is to demonstrate distributed search engine architecture. Therefore, security-related concerns have intentionally been kept minimal.

For production deployments, the following security mechanisms should be incorporated:

- JWT Authentication
- OAuth2 Authorization
- Role-Based Access Control (RBAC)
- HTTPS/TLS encryption
- API rate limiting
- Input validation
- Request size limits
- Secure Kafka communication
- Redis authentication
- Secret management
- Audit logging

These additions would protect both the indexing pipeline and the search APIs from unauthorized access while improving overall system security.

---

# Monitoring and Observability

Large-scale search systems require continuous monitoring to maintain performance and quickly detect failures.

The following metrics should be collected.

## Search Metrics

- Search latency
- Queries per second
- Cache hit ratio
- Average shard response time
- Ranking execution time

## Indexing Metrics

- Documents indexed per second
- Kafka consumer lag
- Segment rotation frequency
- Segment merge duration

## Infrastructure Metrics

- CPU utilization
- JVM heap usage
- Memory utilization
- Garbage collection pauses
- Redis memory usage
- PostgreSQL connection pool usage

These metrics can be exported to Prometheus and visualized through Grafana dashboards for real-time operational monitoring.

---

# Future Enhancements

Although the current implementation already supports distributed indexing and search, several production-grade capabilities can be added in future iterations.

## Search Improvements

- Synonym expansion
- Stop-word filtering
- Stemming
- Lemmatization
- Semantic search
- Vector search
- Hybrid ranking
- Learning-to-Rank (LTR)
- Personalized search
- Query expansion

## Distributed System Improvements

- Dynamic shard allocation
- Shard replication
- Automatic failover
- Leader election
- Cluster discovery
- Consistent hashing

## Performance Improvements

- Posting list compression
- Skip pointers
- Delta encoding
- Memory-mapped indexes
- Incremental indexing
- Adaptive cache eviction
- Query prefetching

## Storage Improvements

- Persistent segment storage
- Incremental snapshots
- Snapshot compression
- Write-Ahead Logging (WAL)
- Segment versioning

---

# Design Decisions

Several important architectural decisions were made while designing the search system.

| Decision | Reason |
|----------|--------|
| Immutable Segments | Eliminate synchronization during reads |
| Hash-Based Sharding | Uniform document distribution |
| Inverted Index | Fast keyword lookup |
| Trie | Efficient autocomplete |
| Kafka | Asynchronous indexing |
| Redis | Low-latency caching |
| CompletableFuture | Parallel shard execution |
| BM25 | High-quality relevance ranking |
| Top-K Aggregation | Efficient distributed ranking |
| Background Segment Merge | Reduce search overhead |
| ReadWriteLock | Thread-safe concurrent access |
| Document Metadata | In-memory ranking without database lookups |

These design decisions prioritize scalability, maintainability, modularity, and efficient query execution.

---

# Conclusion

The Search System demonstrates the core architectural concepts behind modern distributed search engines while remaining compact, modular, and easy to understand.

The implementation combines asynchronous document ingestion, shard-based partitioning, immutable index segments, inverted indexes, BM25 relevance ranking, distributed query execution, Redis caching, Kafka messaging, Boolean search, phrase search, fuzzy search, autocomplete, background segment merging, and concurrent query processing into a cohesive architecture.

Rather than relying on external search frameworks such as Elasticsearch or Apache Solr, the project implements these concepts from first principles, providing a deeper understanding of how production search engines are designed internally.

Although simplified compared to enterprise-grade search platforms, the architecture establishes a strong foundation that can be extended with replication, distributed consensus, semantic search, advanced ranking models, and additional fault-tolerance mechanisms.

Overall, the project demonstrates important software engineering practices including modular design, separation of concerns, asynchronous processing, concurrency control, scalable storage structures, distributed systems design, and production-inspired search engine architecture.

---

# End-to-End Lifecycle Summary

The following condensed view summarizes the document ingestion and query paths through the system.

## Document Ingestion

```text
Client
  ↓
REST API
  ↓
Kafka
  ↓
Indexing Worker
  ↓
Shard Router
  ↓
Index Segment
  ↓
Inverted Index
```

## Search Query

```text
Client Query
  ↓
Search API
  ↓
Redis Cache
  ↓ (cache miss)
Distributed Search
  ↓
Ranking
  ↓
Top-K Merge
  ↓
Response
```

On a cache hit, the cached response is returned without running distributed search.

---

# Why Build a Custom Search Engine?

This project implements search-engine fundamentals directly rather than using Elasticsearch or Apache Solr as its search backend. The goal is educational: to make the data structures and architectural trade-offs behind a search platform visible and understandable, not to replace mature production search products.

Building the core path in this repository provides hands-on experience with:

- Inverted indexes and posting lists for efficient term lookup
- BM25 and additional ranking signals for relevance scoring
- Distributed search, shard routing, and parallel query execution
- Segment architecture for organizing indexed data
- Concurrency across indexing and search operations

The implementation is intentionally smaller than a production search engine and should be viewed as a learning and portfolio project.

---

# References

The design is informed by the following systems and resources. They are references and sources of inspiration, not implementation dependencies:

- Apache Lucene
- Elasticsearch
- Apache Solr
- _Designing Data-Intensive Applications_ by Martin Kleppmann
- _Introduction to Information Retrieval_ by Christopher D. Manning, Prabhakar Raghavan, and Hinrich Schütze