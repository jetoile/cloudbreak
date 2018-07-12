package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class StackImageUpdateTriggerEvent extends StackEvent {

    private String newImageId;

    public StackImageUpdateTriggerEvent(String selector, Long stackId, String newImageId) {
        super(selector, stackId);
        this.newImageId = newImageId;
    }

    public StackImageUpdateTriggerEvent(String selector, Long stackId, Promise<Boolean> accepted, String newImageId) {
        super(selector, stackId, accepted);
        this.newImageId = newImageId;
    }

    public String getNewImageId() {
        return newImageId;
    }
}
