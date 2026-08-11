package sorbonne.professional_website.concurrency;

import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
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

    @Bean(name = "cpuExecutor")
    ThreadPoolTaskExecutor cpuExecutor(BackendConcurrencyProperties properties, TaskDecorator taskDecorator) {
        return executor(
                "portfolio-cpu-",
                properties.cpuCoreSize(),
                properties.cpuMaxSize(),
                properties.cpuQueueCapacity(),
                taskDecorator,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "ioExecutor")
    ThreadPoolTaskExecutor ioExecutor(BackendConcurrencyProperties properties, TaskDecorator taskDecorator) {
        return executor(
                "portfolio-io-",
                properties.ioCoreSize(),
                properties.ioMaxSize(),
                properties.ioQueueCapacity(),
                taskDecorator,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "virtualIoExecutor", destroyMethod = "close")
    BoundedVirtualThreadExecutor virtualIoExecutor(BackendConcurrencyProperties properties) {
        return new BoundedVirtualThreadExecutor(properties.virtualIoMaxConcurrency());
    }

    @Bean(name = "maintenanceExecutor")
    ThreadPoolTaskExecutor maintenanceExecutor(BackendConcurrencyProperties properties, TaskDecorator taskDecorator) {
        return executor(
                "portfolio-maint-",
                properties.maintenanceCoreSize(),
                properties.maintenanceMaxSize(),
                properties.maintenanceQueueCapacity(),
                taskDecorator,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    private static ThreadPoolTaskExecutor executor(
            String threadPrefix,
            int coreSize,
            int maxSize,
            int queueCapacity,
            TaskDecorator taskDecorator,
            java.util.concurrent.RejectedExecutionHandler rejectionHandler
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(threadPrefix);
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(45);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(8);
        executor.setTaskDecorator(taskDecorator);
        executor.setRejectedExecutionHandler(rejectionHandler);
        return executor;
    }
}
