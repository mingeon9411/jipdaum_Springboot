package com.jipdaum_spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 캐시 대상(ProductDetailResponse 등)이 record라 JDK 기본 직렬화(Serializable) 대상이 아니므로
     * JSON 직렬화로 바꾼다. GenericJackson2JsonRedisSerializer의 기본 ObjectMapper는 JSR-310
     * 모듈이 없어 LocalDateTime 필드에서 InvalidDefinitionException을 던지므로 직접 등록해야 한다
     * (실제로 붙여서 호출해보고 나서 확인된 문제). WRITE_DATES_AS_TIMESTAMPS도 꺼야 하는데,
     * 안 끄면 LocalDateTime이 ISO 문자열이 아니라 [2026,7,27,...] 배열로 직렬화돼 캐시 히트 때와
     * DB 조회 때(MVC 기본 ObjectMapper 기준) 응답 JSON 모양이 달라지는 회귀가 생긴다(이것도 실측).
     * TTL 30초는 Django Admin이 같은 DB를 직접 써서(공유 DB, HANDOFF 참고) Spring 캐시가 그
     * 변경을 못 보는 문제에 대한 최종 일관성 타협이다 — 무효화 이벤트 연동은 안 함.
     */
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        ObjectMapper redisObjectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        GenericJackson2JsonRedisSerializer serializer = GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(redisObjectMapper)
                .build();
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
