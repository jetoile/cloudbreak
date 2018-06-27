package com.sequenceiq.it.cloudbreak;

import static com.sequenceiq.it.cloudbreak.newway.cloud.AzureCloudProvider.AZURE;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Database.Ranger;

public class SharedServiceAzureWasbTest extends SharedServiceTestRoot {

    public SharedServiceAzureWasbTest() {
        this(LoggerFactory.getLogger(SharedServiceAzureWasbTest.class), AZURE, Hive.CONFIG_NAME, Ranger.CONFIG_NAME, "wasb");
    }

    private SharedServiceAzureWasbTest(@Nonnull Logger logger, @Nonnull String implementation, String hiveConfigKey, String rangerConfigKey,
                                       String optionalClusterPostfix) {
        super(logger, implementation, hiveConfigKey, rangerConfigKey, optionalClusterPostfix);
    }
}
