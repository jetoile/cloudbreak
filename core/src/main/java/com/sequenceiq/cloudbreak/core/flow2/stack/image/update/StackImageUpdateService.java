package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class StackImageUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackImageUpdateService.class);

    private static final String MIN_VERSION = "2.8.0";

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackService stackService;

    public Response changeImageForStack(Stack stack, String newImageId) {
        if (!isCbVersionOk(stack)) {
            return Response.notModified("Cluster must be created with newer version of Cloudbreak then " + MIN_VERSION).build();
        }

        StatedImage image = getNewImageIfVersionsMatch(stack, newImageId);

        stackService.updateStatus(stack.getId(), StatusRequest.SYNC, false);

        storeNewImageComponent(stack, image);

        return Response.status(Status.NO_CONTENT).build();
    }

    public void storeNewImageComponent(Stack stack, StatedImage image) {
        Component imageComponent;
        try {
            imageComponent = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(image.getImage()), stack);
        } catch (JsonProcessingException e) {
            throw new CloudbreakServiceException("Failed to create json", e);
        }
        componentConfigProvider.replaceComponentWithNew(imageComponent);
    }

    public StatedImage getNewImageIfVersionsMatch(Stack stack, String newImageId) {
        try {
            Image currentImage = componentConfigProvider.getImage(stack.getId());

            StatedImage newImage = imageCatalogService.getImage(currentImage.getImageCatalogUrl(), currentImage.getImageCatalogName(), newImageId);

            if (!newImage.getImage().getOs().equals(currentImage.getOs()) || !newImage.getImage().getOsType().equals(currentImage.getOsType())) {
                String message = String.format("New image OS [%s] and OS type [%s] is different from current OS [%s] and OS type [%s]",
                        newImage.getImage().getOs(), newImage.getImage().getOsType(), currentImage.getOs(), currentImage.getOsType());
                LOGGER.warn(message);
                throw new CloudbreakApiException(message);
            }
            return newImage;

        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.error("Cloudbreak Image not found", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        } catch (CloudbreakImageCatalogException e) {
            LOGGER.error("Cloudbreak Image Catalog error", e);
            throw new CloudbreakApiException(e.getMessage(), e);
        }
    }

    public boolean isCbVersionOk(Stack stack) {
        CloudbreakDetails cloudbreakDetails = componentConfigProvider.getCloudbreakDetails(stack.getId());
        VersionComparator versionComparator = new VersionComparator();
        int compare = versionComparator.compare(cloudbreakDetails::getVersion, () -> MIN_VERSION);
        return compare >= 0;
    }

    public boolean checkPackageVersions(Stack stack, StatedImage newImage) {
        return true;
    }

}
