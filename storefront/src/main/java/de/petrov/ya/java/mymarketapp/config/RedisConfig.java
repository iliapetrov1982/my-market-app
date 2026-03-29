package de.petrov.ya.java.mymarketapp.config;


import de.petrov.ya.java.mymarketapp.dto.page.ItemDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, ItemDto> itemRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        JacksonJsonRedisSerializer<ItemDto> valueSerializer =
                new JacksonJsonRedisSerializer<>(objectMapper, ItemDto.class);

        RedisSerializationContext<String, ItemDto> context =
                RedisSerializationContext.<String, ItemDto>newSerializationContext(keySerializer)
                        .key(keySerializer)
                        .value(valueSerializer)
                        .hashKey(keySerializer)
                        .hashValue(valueSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}