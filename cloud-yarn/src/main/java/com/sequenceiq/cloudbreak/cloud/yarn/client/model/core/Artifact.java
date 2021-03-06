package com.sequenceiq.cloudbreak.cloud.yarn.client.model.core;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;

public class Artifact implements JsonEntity {

    private String id;

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
