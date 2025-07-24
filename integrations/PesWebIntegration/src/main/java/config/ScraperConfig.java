package config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ScraperConfig {
    @Bean("scraperExecutor")
    public ThreadPoolTaskExecutor scraperExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(12);
        exec.setMaxPoolSize(12);
        exec.setQueueCapacity(500);            // buffer up to 500 pending tasks
        exec.setThreadNamePrefix("scraper-");
        exec.initialize();
        return exec;
    }
}
