package org.jboss.fuse.openshift.recovery;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recovery.controller")
public class StatefulsetRecoveryControllerProperties {

    /**
     * Enables the recovery controller.
     * The recovery controller runs only on pod 0 of the Statefulset by default.
     */
    private boolean enabled = true;

    /**
     * Enables the recovery controller on all pods, not only on pod 0 of the Statefulset.
     */
    private boolean enabledOnAllPods = false;

    /**
     * The delay in milliseconds between two runs of the recovery controller.
     */
    private Long period;

    /**
     * The target statefulset to monitor
     */
    private String statefulset;

    /**
     * The name of the current pod, to be filled using Kubernetes downward API.
     */
    private String currentPodName;


    /**
     * Path of the pod directory where pods will save their status.
     */
    private String statusDir;


    public Long getPeriod() {
        return period;
    }

    public void setPeriod(Long period) {
        this.period = period;
    }

    public String getStatefulset() {
        return statefulset;
    }

    public void setStatefulset(String statefulset) {
        this.statefulset = statefulset;
    }

    public String getCurrentPodName() {
        return currentPodName;
    }

    public void setCurrentPodName(String currentPodName) {
        this.currentPodName = currentPodName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabledOnAllPods() {
        return enabledOnAllPods;
    }

    public void setEnabledOnAllPods(boolean enabledOnAllPods) {
        this.enabledOnAllPods = enabledOnAllPods;
    }

    public String getStatusDir() {
        return statusDir;
    }

    public void setStatusDir(String statusDir) {
        this.statusDir = statusDir;
    }
}
