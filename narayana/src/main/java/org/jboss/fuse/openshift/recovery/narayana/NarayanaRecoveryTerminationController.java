package org.jboss.fuse.openshift.recovery.narayana;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.objectstore.StoreManager;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.state.InputObjectState;
import com.arjuna.ats.internal.arjuna.common.UidHelper;
import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.jboss.fuse.openshift.recovery.PodStatus;
import org.jboss.fuse.openshift.recovery.PodStatusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NarayanaRecoveryTerminationController {

    private static final Logger LOG = LoggerFactory.getLogger(NarayanaRecoveryTerminationController.class);

    private PodStatusManager podStatusManager;

    private CamelContext camelContext;

    public NarayanaRecoveryTerminationController(PodStatusManager podStatusManager, CamelContext camelContext) {
        this.podStatusManager = Objects.requireNonNull(podStatusManager, "podStatusManager cannot be null");
        this.camelContext = Objects.requireNonNull(camelContext, "camelContext cannot be null");
    }

    public void start() {
        LOG.info("Narayana recovery termination controller started");
        podStatusManager.setStatus(PodStatus.RUNNING);
    }

    public void stop() {
        try {
            // Stop all services that may use transactions
            waitForCamelContextToStop();

            LOG.info("Performing transaction recovery scan...");
            RecoveryManager.manager().scan();
            LOG.info("Performing second run of transaction recovery scan...");
            RecoveryManager.manager().scan();

            List<Uid> pendingUids = getPendingUids();
            if (pendingUids.isEmpty()) {
                LOG.info("There are no pending transactions left");
                podStatusManager.setStatus(PodStatus.STOPPED);
            } else {
                LOG.warn("There are pending transactions: {}", pendingUids);
                podStatusManager.setStatus(PodStatus.PENDING);
            }

        } catch (Exception ex) {
            LOG.error("Error while cleaning transaction subsystem", ex);
        }
    }

    private void waitForCamelContextToStop() throws InterruptedException {
        LOG.info("Waiting for Camel context to stop...");
        int attempts = 200;
        ServiceStatus camelStatus = camelContext.getStatus();
        for (int i=0; i < attempts && !camelStatus.isStopped(); i++) {
            LOG.debug("Camel context still running, waiting 1 second more...");
            Thread.sleep(1000);
            camelStatus = camelContext.getStatus();
        }

        if (!camelStatus.isStopped()) {
            throw new IllegalStateException("Camel context not stopped after " + attempts + " seconds");
        }
        LOG.info("Camel context stopped");
    }

    private List<Uid> getPendingUids() throws Exception {
        InputObjectState types = new InputObjectState();
        StoreManager.getRecoveryStore().allTypes(types);

        List<Uid> allUIDs = new ArrayList<>();
        for (String typeName = types.unpackString(); typeName != null && typeName.compareTo("") != 0; typeName = types.unpackString()) {
            List<Uid> uids = getPendingUids(typeName);

            if (uids.isEmpty()) {
                LOG.debug("Found {} UIDs for action type {}", 0, typeName);
            } else {
                LOG.warn("Found {} UIDs for action type {}", uids.size(), typeName);
            }
            allUIDs.addAll(uids);
        }

        return allUIDs;
    }

    private List<Uid> getPendingUids(String type) throws Exception {
        List<Uid> uidList = new ArrayList<>();
        InputObjectState uids = new InputObjectState();
        if (!StoreManager.getRecoveryStore().allObjUids(type, uids)) {
            throw new RuntimeException("Cannot obtain pending Uids");
        }

        if (uids.notempty()) {
            Uid u;
            do {
                u = UidHelper.unpackFrom(uids);

                if (Uid.nullUid().notEquals(u))
                {
                    uidList.add(u);
                }
            } while (Uid.nullUid().notEquals(u));
        }

        return uidList;
    }

}
