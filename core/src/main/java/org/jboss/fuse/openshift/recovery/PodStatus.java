package org.jboss.fuse.openshift.recovery;

public enum PodStatus {

    /**
     * The pod has been marked as RUNNING
     */
    RUNNING,

    /**
     * The pod has terminated, but there's PENDING work to be done.
     */
    PENDING,

    /**
     * The pod has been gracefully STOPPED.
     */
    STOPPED
}
