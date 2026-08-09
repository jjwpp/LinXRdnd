package com.jjwpp.linxr;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
    RedisEmbeddingStoreAutoConfiguration.class
})
public class LinXrApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinXrApplication.class, args);
    }

}
