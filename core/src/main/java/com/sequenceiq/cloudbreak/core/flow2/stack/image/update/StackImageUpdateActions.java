package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageUpdateEvent;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

@Configuration
public class StackImageUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackImageUpdateActions.class);

    @Inject
    private StackImageUpdateService stackImageUpdateService;

    @Bean(name = "CHECK_IMAGE_VERSIONS_STATE")
    public Action<?, ?> checkImageVersion() {
        return new AbastractStackImageUpdateAction<>(StackImageUpdateTriggerEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackImageUpdateTriggerEvent payload, Map<Object, Object> variables) {
                stackImageUpdateService.isCbVersionOk(context.getStack());
                StatedImage newImage = stackImageUpdateService.getNewImageIfVersionsMatch(context.getStack(), payload.getNewImageId());
                sendEvent(context.getFlowId(), new ImageUpdateEvent(StackImageUpdateEvent.CHECK_IMAGE_VERESIONS_FINISHED_EVENT.event(),
                        context.getStack().getId(), newImage));
            }
        };
    }

    @Bean(name = "CHECK_PACKAGE_VERSIONS_STATE")
    public Action<?, ?> checkPackageVersions() {
        return new AbastractStackImageUpdateAction<>(ImageUpdateEvent.class) {
            @Override
            protected void doExecute(StackContext context, ImageUpdateEvent payload, Map<Object, Object> variables) {
                stackImageUpdateService.checkPackageVersions(context.getStack(), payload.getImage());
                sendEvent(context.getFlowId(), new ImageUpdateEvent(StackImageUpdateEvent.CHECK_PACKAGE_VERSIONS_FINISHED_EVENT.event(),
                        context.getStack().getId(), payload.getImage()));
            }
        };
    }

    @Bean(name = "UPDATE_IMAGE_STATE")
    public Action<?, ?> updateImage() {
        return new AbastractStackImageUpdateAction<>(ImageUpdateEvent.class) {
            @Override
            protected void doExecute(StackContext context, ImageUpdateEvent payload, Map<Object, Object> variables) {
                stackImageUpdateService.storeNewImageComponent(context.getStack(), payload.getImage());
                sendEvent(context.getFlowId(), new StackEvent(StackImageUpdateEvent.UPDATE_IMAGE_FINESHED_EVENT.event(), context.getStack().getId()));
            }
        };
    }

}
