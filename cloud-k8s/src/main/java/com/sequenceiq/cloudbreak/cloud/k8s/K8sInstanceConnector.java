package com.sequenceiq.cloudbreak.cloud.k8s;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

@Service
public class K8sInstanceConnector implements InstanceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(K8sInstanceConnector.class);

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        throw new CloudOperationNotSupportedException("Start instances operation is not supported on K8S");
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        throw new CloudOperationNotSupportedException("Stop instances operation is not supported on K8S");
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext authenticatedContext, List<CloudInstance> vms) {
        throw new CloudOperationNotSupportedException("Instances' states check operation is not supported on K8S");
    }

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        throw new CloudOperationNotSupportedException("Host key verification is disabled on K8S");
    }
}
