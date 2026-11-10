package com.searchSystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.searchSystem.service.indexing.IndexingWorkerService;
import com.searchSystem.service.search.QueryCoordinatorService;
import com.searchSystem.service.search.RankingService;
import com.searchSystem.service.search.SegmentSearchService;
import com.searchSystem.service.search.SnippetService;
import com.searchSystem.service.IngestionService;
import com.searchSystem.service.QueryService;
import com.searchSystem.service.QueryTracker;
import com.searchSystem.service.ShardRouter;
import com.searchSystem.service.search.DocumentStatisticsService;
import com.searchSystem.service.search.TopKResultAggregator;
import com.searchSystem.service.search.booleanquery.BooleanQueryParser;
import com.searchSystem.service.search.booleanquery.BooleanQueryProcessor;
import com.searchSystem.service.search.fuzzy.FuzzySearchService;
import com.searchSystem.autoComplete.Trie;
import com.searchSystem.cache.RedisCacheService;
import com.searchSystem.repository.DocumentRepository;

@Configuration
public class AppConfig {

    @Bean
    public ShardRouter shardRouter(Trie trie, DocumentStatisticsService statsService,
            SegmentSearchService segmentSearchService, RankingService rankingService) {
        return new ShardRouter(trie, statsService, segmentSearchService, rankingService);
    }

    @Bean
    public Trie trie() {
        return new Trie();
    }

    @Bean
    public IngestionService ingestionService(IndexingWorkerService indexingWorkerService) {
        return new IngestionService(indexingWorkerService);
    }

    @Bean
    public QueryService queryService(QueryTracker queryTracker, RedisCacheService redisCache,
            QueryCoordinatorService queryCoordinatorService, DocumentRepository documentRepository,
            BooleanQueryParser booleanQueryParser, BooleanQueryProcessor booleanQueryProcessor,
            FuzzySearchService fuzzySearchService, SnippetService snippetService) {
        return new QueryService(queryTracker, redisCache, queryCoordinatorService, documentRepository,
                booleanQueryParser, booleanQueryProcessor, fuzzySearchService, snippetService);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public QueryTracker queryTracker() {
        return new QueryTracker();
    }

    @Bean
    public QueryCoordinatorService queryCoordinatorService(ShardRouter shardRouter, TopKResultAggregator aggregator,
            Trie trie) {
        return new QueryCoordinatorService(shardRouter, aggregator, trie);
    }
}
