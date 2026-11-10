# Distributed Search Engine

A production-inspired distributed search engine built with Java 17, Spring Boot 3, Kafka, Redis, and PostgreSQL. This project demonstrates the core ideas behind modern search platforms: sharding, inverted indexes, query coordination, relevance ranking, asynchronous indexing, and autocomplete.

It is designed to be both practical and easy to explain, making it a strong example for backend engineering, system design, and interview discussions.

## Why This Project Matters

This repository models how modern search systems handle:

- document ingestion
- full-text indexing
- shard-based partitioning
- ranking and relevance scoring
- fuzzy and phrase search
- autocomplete and suggestions
- Redis caching and Kafka-based async processing

## Architecture at a Glance

```text
Client / API Consumer
        │
        v
┌──────────────────────┐
│ REST Controllers     │
│ - DocumentController │
│ - SearchController   │
│ - Autocomplete       │
└──────────┬───────────┘
           │
           v
┌──────────────────────┐
│ Query Orchestration  │
│ - QueryService       │
│ - QueryCoordinator   │
│ - ShardRouter        │
└──────────┬───────────┘
           │
           v
┌──────────────────────┐
│ Search Core          │
│ - Tokenizer         │
│ - Trie              │
│ - Shard             │
│ - IndexSegment      │
│ - InvertedIndex     │
└──────────┬───────────┘
           │
           v
┌──────────────────────┐
│ Infrastructure       │
│ - PostgreSQL        │
│ - Redis             │
│ - Kafka             │
└──────────────────────┘
```

## Key Features

- Full-text search with relevance ranking
- Phrase and boolean query support
- Fuzzy query correction
- Trie-based autocomplete and suggestions
- Sharded index execution
- Immutable segment rotation
- Redis-backed query caching
- Kafka-based async ingestion
- Retry and DLQ-style error handling

## Repository Structure

```text
search-system/
├── ARCHITECTURE.md
├── README.md
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── mvnw
├── mvnw.cmd
├── src/
│   ├── main/
│   │   ├── java/com/searchSystem/
│   │   │   ├── autoComplete/
│   │   │   ├── cache/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── index/
│   │   │   ├── kafka/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   │   ├── indexing/
│   │   │   │   ├── persistence/
│   │   │   │   ├── search/
│   │   │   │   │   ├── booleanquery/
│   │   │   │   │   └── fuzzy/
│   │   │   │   └── wal/
│   │   │   ├── storage/
│   │   │   ├── tokenizer/
│   │   │   └── SearchSystemApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── static/
│   └── test/
│       └── java/com/searchSystem/search_system/
└── target/
```

The main packages are organized by responsibility:

| Package | Responsibility |
|---|---|
| `controller` | Exposes document ingestion, search, autocomplete, and suggestion REST endpoints. |
| `service` | Coordinates ingestion, query handling, and shard routing. |
| `service.indexing` | Processes indexed documents, retries failed work, and merges segments. |
| `service.search` | Coordinates shard searches, ranking, snippets, autocomplete, and Top-K aggregation. |
| `service.search.booleanquery` / `service.search.fuzzy` | Parses and executes Boolean queries and provides fuzzy term matching. |
| `service.persistence` / `service.wal` | Handles index snapshots and write-ahead logging. |
| `index` | Contains shard, segment, and inverted-index data structures. |
| `autoComplete` / `tokenizer` | Implements trie-based prefix lookup and text tokenization. |
| `kafka` | Publishes and consumes document-ingestion events. |
| `cache` | Provides Redis-backed query caching. |
| `model` | Defines documents, postings, search results, metadata, and related data objects. |
| `repository` / `storage` | Provides document persistence and document storage abstractions. |
| `config` | Defines application configuration and Spring bean wiring. |

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Build Tool | Maven |
| Persistence | PostgreSQL |
| Cache | Redis |
| Messaging | Kafka |
| Indexing Model | Inverted index + shards + segments |
| Ranking | BM25-inspired relevance scoring |
| Concurrency | ExecutorService, CompletableFuture |

## How It Works

### 1. Documents are ingested asynchronously

```text
POST /documents
    │
    v
DocumentController
    │
    v
DocumentProducer
    │
    v
Kafka Topic
    │
    v
DocumentConsumer
    │
    v
IndexingWorkerService
    │
    v
ShardRouter -> Shard -> Active Segment
```

### 2. Search is executed across shards

```text
GET /search?q=smartphone
    │
    v
SearchController
    │
    v
QueryService
    │
    ├── normalize query
    ├── check Redis cache
    ├── tokenization
    └── QueryCoordinatorService
           │
           v
     Parallel shard execution
           │
           v
     Merge + rank + Top-K selection
           │
           v
     Final response
```

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/documents` | Accepts a document and publishes it for asynchronous indexing. |
| `GET` | `/search?q={query}&page={page}&size={size}` | Searches indexed documents and returns paginated results. |
| `GET` | `/autocomplete?prefix={prefix}` | Returns autocomplete suggestions for a prefix. |
| `GET` | `/suggest?prefix={prefix}` | Returns matching prefix terms from the trie. |

### Add a document

```http
POST /documents
Content-Type: application/json
```

Example:

```bash
curl -X POST http://localhost:8080/documents \
  -H "Content-Type: application/json" \
  -d "{\"id\": \"doc-001\", \"content\": \"Spring Boot microservices and distributed search systems\"}"
```

### Search documents

```http
GET /search?q={query}&page={page}&size={size}
```

Example:

```bash
curl "http://localhost:8080/search?q=spring%20boot&page=0&size=10"
```

### Autocomplete

```http
GET /autocomplete?prefix=spr
```

### Prefix suggestions

```http
GET /suggest?prefix=spr
```

### Top queries

```http
GET /search/top-queries
```

## Main Modules

### Controller layer

- DocumentController — document ingestion API
- SearchController — query execution API
- AutocompleteController — autocomplete endpoints
- SuggestionController — trie-based prefix suggestions

### Service layer

- IngestionService — starts asynchronous indexing flow
- QueryService — orchestrates search logic and result assembly
- QueryCoordinatorService — executes queries across shards
- ShardRouter — assigns documents and queries to shards
- QueryTracker — monitors popular search terms
- RankingService — computes relevance and scoring
- AutoCompleteService — builds suggestions
- FuzzySearchService — handles typo correction

### Core search components

- Tokenizer — splits text into searchable tokens
- Trie and TrieNode — prefix candidate lookup
- Shard — shard-local index partition
- IndexSegment — immutable segment abstraction
- InvertedIndex — term-to-document mapping

### Infrastructure

- RedisCacheService — query result caching
- DocumentProducer and DocumentConsumer — Kafka ingestion flow
- DocumentRepository — persistence layer for documents
- AppConfig — bean wiring and service configuration

## Local Setup

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker + Docker Compose

### Start dependencies

```bash
docker-compose up -d
```

This starts:

- PostgreSQL on localhost:5432
- Redis on localhost:6379
- Kafka on localhost:9092
- Zookeeper for Kafka coordination

### Build the app

```bash
./mvnw clean package
```

### Run the app

```bash
./mvnw spring-boot:run
```

The app runs at:

```text
http://localhost:8080
```

## Architecture Reference

For a deeper breakdown of the system design, see [ARCHITECTURE.md](./ARCHITECTURE.md).

## Interview-Friendly Summary

This project demonstrates several high-value engineering concepts:

- distributed indexing and query partitioning
- asynchronous processing using Kafka
- cache-aware search architecture
- relevance-based ranking
- sharding and segment lifecycle management
- practical backend design patterns used in real search services

## Screenshots

_Screenshots can be added here when representative application or API screenshots are available._

## Future Enhancements

Potential areas for future development include:

- Shard replication and automatic failover
- Synonym expansion and improved linguistic analysis
- Posting-list compression and incremental snapshots
- Semantic or vector search

These are planned directions, not features currently claimed by this project.

## Learning Outcomes

Working through this project provides practical experience with:

- Building an inverted index and positional posting lists
- Applying BM25-based ranking and query-specific scoring signals
- Partitioning search work across shards and merging Top-K results
- Designing asynchronous ingestion with Kafka and caching with Redis
- Managing concurrent reads, indexing, and immutable index segments

## Contributing

Contributions and suggestions are welcome. Before opening a pull request, describe the change and its rationale, keep documentation consistent with implemented behavior, and run the relevant Maven tests.

## License

No license file is currently included in this repository. Unless a license is added, the project should be treated as having no granted reuse or distribution permissions.

## Final Note

This project is intentionally production-inspired rather than fully production-ready. It is best viewed as a strong learning and portfolio project for understanding how large-scale search systems are architected and why the design decisions matter.
