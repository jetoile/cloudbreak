package com.sequenceiq.cloudbreak.core.flow.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;

@Service
public class SimpleFlowFacade implements FlowFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFlowFacade.class);

    @Inject
    private ClusterFacade clusterFacade;

    @Inject
    private StackFacade stackFacade;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private StackService stackService;

    @Override
    public FlowContext collectMetadata(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Metadata collect. Context: {}", context);
        try {
            Long stackId = context.getStackId();
            Stack stack = stackService.getById(stackId);
            MDCBuilder.buildMdcContext(stack);
            metadataSetupService.collectMetadata(stack);
            LOGGER.debug("Metadata collect DONE.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during metadata collect: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleStackStatusUpdateFailure(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Handling stack start/stop failure. Context: {}", context);
        try {
            context = stackFacade.handleStatusUpdateFailure(context);
            LOGGER.debug("Stack start/stop failure is handled.");
            return context;
        } catch (Exception e) {
            LOGGER.error("Exception during handling stack start/stop failure!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext updateAllowedSubnets(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Update allowed subnet. Context: {}", context);
        try {
            context = stackFacade.updateAllowedSubnets(context);
            LOGGER.debug("Updating of allowed subnet is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the updating of allowed subnet: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleUpdateAllowedSubnetsFailure(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Handling 'update allowed subnet' failure. Context: {}", context);
        try {
            context = stackFacade.handleUpdateAllowedSubnetsFailure(context);
            LOGGER.debug("Handling of 'update allowed subnet' failure is DONE");
            return context;
        } catch (CloudbreakException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Exception during the handling of update allowed subnet failure: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext handleStackSync(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Stack sync requested. Context: {}", context);
        try {
            return stackFacade.sync(context);
        } catch (Exception e) {
            LOGGER.error("Exception during cluster start sync!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }

    @Override
    public FlowContext credentialChange(FlowContext context) throws CloudbreakException {
        LOGGER.debug("Authentication change for cluster requested. Context: {}", context);
        try {
            return clusterFacade.credentialChange(context);
        } catch (Exception e) {
            LOGGER.error("Exception during cluster authentication change!: {}", e.getMessage());
            throw new CloudbreakException(e);
        }
    }
}