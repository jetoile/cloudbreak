package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.filesystem.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.api.model.filesystem.GcsFileSystem;

@Component
public class GcsCloudStorageParametersToGcsFileSystemConverter
        extends AbstractConversionServiceAwareConverter<GcsCloudStorageParameters, GcsFileSystem> {

    @Override
    public GcsFileSystem convert(GcsCloudStorageParameters source) {
        GcsFileSystem fileSystemConfigurations = new GcsFileSystem();
        fileSystemConfigurations.setServiceAccountEmail(source.getServiceAccountEmail());
        return fileSystemConfigurations;
    }
}
