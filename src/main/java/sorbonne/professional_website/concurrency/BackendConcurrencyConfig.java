package sorbonne.professional_website.concurrency;

import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(BackendConcurrencyProperties.class)
public class BackendConcurrencyConfig {

    @Bean
    TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                try {
                    if (context == null) MDC.clear();
                    else MDC.setContextMap(context);
                    runnable.run();
                } finally {
                    if (previous == null) MDC.clear();
                    else MDC.setContextMap(previous);
                }
            };
        };
    }

    @Bean(name = "virtualIoExecutor", destroyMethod = "close")
    BoundedVirtualThreadExecutor virtualIoExecutor(BackendConcurrencyProperties properties) {
        return new BoundedVirtualThreadExecutor(properties.virtualIoMaxConcurrency());
    }

    @Bean(name = "maintenanceExecutor")
    ThreadPoolTaskExecutor maintenanceExecutor(BackendConcurrencyProperties properties, TaskDecorator taskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("portfolio-maint-");
        executor.setCorePoolSize(properties.maintenanceCoreSize());
        executor.setMaxPoolSize(properties.maintenanceMaxSize());
        executor.setQueueCapacity(properties.maintenanceQueueCapacity());
        executor.setKeepAliveSeconds(45);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(8);
        executor.setTaskDecorator(taskDecorator);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}
