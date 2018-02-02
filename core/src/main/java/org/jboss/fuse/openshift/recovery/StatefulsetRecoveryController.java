package org.jboss.fuse.openshift.recovery;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class StatefulsetRecoveryController {

    private static final Logger LOG = LoggerFactory.getLogger(StatefulsetRecoveryController.class);

    private StatefulsetRecoveryControllerProperties properties;

    private PodStatusManager podStatusManager;

    public StatefulsetRecoveryController(StatefulsetRecoveryControllerProperties properties, PodStatusManager podStatusManager) {
        this.properties = Objects.requireNonNull(properties, "No properties set");
        this.podStatusManager = Objects.requireNonNull(podStatusManager, "No podStatusManager set");

        Objects.requireNonNull(properties.getStatefulset(), "statefulset property missing in recovery controller configuration");
        Objects.requireNonNull(properties.getCurrentPodName(), "current-pod-name property missing in recovery controller configuration");
        Objects.requireNonNull(properties.getPeriod(), "period property missing in recovery controller configuration");
    }

    @Scheduled(fixedDelayString = "${recovery.controller.period}", initialDelayString = "${recovery.controller.period}")
    public void periodicCheck() throws Exception {
        if (properties.isEnabledOnAllPods() || isMainStatefulsetPod()) {
            // Run this on the first pod only if not configured differently

            try (OpenShiftClient client = new DefaultOpenShiftClient()) {

                Set<String> pendingPods = podStatusManager.getAllPodsStatus().entrySet().stream()
                        .filter(e -> !Optional.of(PodStatus.STOPPED).equals(e.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());

                LOG.debug("Found {} pods not stopped: {}", pendingPods.size(), pendingPods);
                int minReplicas = 0;
                for (String podName : pendingPods) {
                    LOG.debug("Retrieving pod {} from Openshift", podName);
                    Pod pod = client.pods().withName(podName).get();
                    if (pod == null) {
                        // pod has completely been removed from the cluster
                        LOG.debug("Pod {} not found in Openshift", podName);
                        OptionalInt prg = getProgressiveNumber(podName);
                        if (!prg.isPresent()) {
                            LOG.warn("Status manager contains pods not belonging to the Statefulset: {}", podName);
                        } else {
                            minReplicas = Math.max(minReplicas, prg.getAsInt() + 1);
                        }
                    } else {
                        LOG.debug("Pod {} is running on Openshift", podName);
                    }
                }

                LOG.debug("StatefulSet requires a minimum of {} replicas", minReplicas);

                if (minReplicas > 1) {
                    // One pod is running, this one
                    StatefulSet statefulSet = client.apps().statefulSets().withName(properties.getStatefulset()).get();
                    if (statefulSet == null) {
                        LOG.warn("Cannot find StatefulSet named {} in namespace", properties.getStatefulset());
                    } else {
                        int replicas = statefulSet.getSpec().getReplicas();
                        if (replicas > 0 && replicas < minReplicas) {
                            LOG.warn("Pod {}-{} has pending work and must be restored again", properties.getStatefulset(), minReplicas - 1);

                            LOG.debug("Scaling the statefulset back to {} replicas", minReplicas);
                            client.apps().statefulSets().withName(properties.getStatefulset()).scale(minReplicas);
                            LOG.info("Statefulset {} successfully scaled to {} replicas", properties.getStatefulset(), minReplicas);
                        } else if (replicas == 0) {
                            LOG.debug("StatefulSet {} is going to be shut down. Controller will not interfere", properties.getStatefulset(), replicas);
                        } else {
                            LOG.debug("StatefulSet {} has a sufficient number of replicas: {} >= {}", properties.getStatefulset(), replicas, minReplicas);
                        }
                    }
                }
            }

        }
    }

    private boolean isMainStatefulsetPod() {
        return getProgressiveNumber(properties.getCurrentPodName()).equals(OptionalInt.of(0));
    }

    private OptionalInt getProgressiveNumber(String podName) {
        try {
            return OptionalInt.of(Integer.parseInt(podName.substring(properties.getStatefulset().length() + 1), 10));
        } catch (Exception e) {
            LOG.warn("Cannot extract progressive number from pod name: " + podName);
            return OptionalInt.empty();
        }
    }

}
