package com.sequenceiq.cloudbreak.api.model.stack;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("StackImageChangeRequest")
public class StackImageChangeRequest implements JsonEntity {

    @ApiModelProperty(value = "New image id", required = true)
    @NotNull
    @Size(min = 1, message = "The length of the imageId has to be greater than 1")
    private String imageId;

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
}
