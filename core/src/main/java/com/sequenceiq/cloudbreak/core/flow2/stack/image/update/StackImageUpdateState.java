package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum StackImageUpdateState implements FlowState {
    INIT_STATE,
    STACK_IMAGE_UPDATE_FAILED_STATE,
    CHECK_IMAGE_VERSIONS_STATE,
    CHECK_PACKAGE_VERSIONS_STATE,
    UPDATE_IMAGE_STATE,
    FINAL_STATE;
}
