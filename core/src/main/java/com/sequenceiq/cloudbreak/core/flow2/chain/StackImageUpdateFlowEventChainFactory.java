package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent;

@Component
public class StackImageUpdateFlowEventChainFactory implements FlowEventChainFactory<StackImageUpdateTriggerEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(StackImageUpdateTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackSyncTriggerEvent(StackSyncEvent.STACK_SYNC_EVENT.event(), event.getStackId(), true, event.accepted()));
        flowEventChain.add(new StackImageUpdateTriggerEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_EVENT.event(), event.getStackId(), event.getNewImageId()));
        return flowEventChain;
    }
}
