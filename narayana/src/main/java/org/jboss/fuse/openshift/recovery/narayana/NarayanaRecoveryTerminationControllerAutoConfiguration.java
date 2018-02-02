package org.jboss.fuse.openshift.recovery.narayana;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.jboss.fuse.openshift.recovery.PodStatusManager;
import org.jboss.fuse.openshift.recovery.StatefulsetRecoveryControllerAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.transaction.jta.NarayanaJtaConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@AutoConfigureAfter({StatefulsetRecoveryControllerAutoConfiguration.class, CamelAutoConfiguration.class, NarayanaJtaConfiguration.class})
@ConditionalOnBean({PodStatusManager.class, CamelContext.class})
public class NarayanaRecoveryTerminationControllerAutoConfiguration {


    @Bean(initMethod = "start", destroyMethod = "stop")
    @DependsOn("narayanaRecoveryManagerService")
    @ConditionalOnMissingBean(NarayanaRecoveryTerminationController.class)
    public NarayanaRecoveryTerminationController narayanaRecoveryTerminationController(PodStatusManager podStatusManager, CamelContext context) {
        return new NarayanaRecoveryTerminationController(podStatusManager, context);
    }

}
