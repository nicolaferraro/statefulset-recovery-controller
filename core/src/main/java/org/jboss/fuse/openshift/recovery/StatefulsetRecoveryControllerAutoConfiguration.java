package org.jboss.fuse.openshift.recovery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(StatefulsetRecoveryControllerProperties.class)
@ConditionalOnProperty(value = "recovery.controller.enabled", matchIfMissing = true)
public class StatefulsetRecoveryControllerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PodStatusManager.class)
    public PodStatusManager podStatusManager(StatefulsetRecoveryControllerProperties properties) {
        return new PodStatusManager(properties);
    }

    @Bean
    @ConditionalOnBean(PodStatusManager.class)
    @ConditionalOnMissingBean(StatefulsetRecoveryController.class)
    public StatefulsetRecoveryController statefulsetRecoveryController(StatefulsetRecoveryControllerProperties properties, PodStatusManager podStatusManager) {
        return new StatefulsetRecoveryController(properties, podStatusManager);
    }

}
