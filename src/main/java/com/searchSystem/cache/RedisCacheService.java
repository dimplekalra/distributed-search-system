package com.searchSystem.cache;

import com.searchSystem.model.SearchResponse;
import com.searchSystem.model.SearchResult;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCacheService(
            RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<SearchResponse> get(String query) {

        return (List<SearchResponse>) redisTemplate.opsForValue()
                .get(query);
    }

    public void put(
            String query,
            List<SearchResponse> results) {

        redisTemplate.opsForValue()
                .set(
                        query,
                        results,
                        10,
                        TimeUnit.MINUTES);
    }

    public void clearCache() {
        redisTemplate.keys("*")
                .forEach(redisTemplate::delete);
    }
}
