# StatefulSet Recovery Controller

The recovery controller is a spring-boot library that allows to gracefully handle the scaling down phase of a StatefulSet
by "cleaning up" the pod before termination.

If a scaling down operation is executed and the pod is not clean after termination, the previous number of replicas is restored, 
hence effectively cancelling the scaling down operation.

All pod of the statefulset require access to a shared volume that is used to store the termination status of each pod belonging to the StatefulSet.

The pod-0 of the StatefulSet periodically check the status and scale the StatefulSet to the right size if there's a mismatch.

A "edit" role on the namespace is required on Openshift in order for the pod-0 to change the number of replicas. 

## Narayana Extension for StatefulSet Recovery Controller 

The Narayana extension of the StatefulSet recovery controller checks if all pending transactions are flushed before 
allowing the StatefulSet controller to scale down correctly.

It adds a shutdown hook that waits for all processes that can generate transactions to shut down before do some recovery 
checks on the Narayana transaction system.

The Narayana extension also wraps the default JDBC configuration with a DBCP connection pool.   