package com.bookclub.book.application

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@EnableAsync
@Configuration
class BookMetadataExecutorConfig {

    @Bean("bookMetadataExecutor")
    fun bookMetadataExecutor(): ThreadPoolTaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 5
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("book-metadata-")
        executor.initialize()
        return executor
    }
}
