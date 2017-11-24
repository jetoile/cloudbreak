package com.sequenceiq.cloudbreak.cloud.yarn;

import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;
import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZones;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Regions;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;
import com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;

@Service
public class YarnPlatformParameters implements PlatformParameters {
    // There is no need to initialize the disk on ycloud
    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("nonexistent_device", 97);

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Value("${cb.yarn.regions:}")
    private String yarnRegionDefinition;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private Environment environment;

    private Map<Region, List<AvailabilityZone>> regions;

    private final Map<Region, DisplayName> regionDisplayNames = new HashMap<>();

    private Region defaultRegion;

    @PostConstruct
    public void init() {
        String zone = resourceDefinition("zone");
        regions = readRegions(zone);
        defaultRegion = getDefaultRegion();
    }

    @Override
    public ScriptParams scriptParams() {
        return SCRIPT_PARAMS;
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(Collections.emptyList(), DiskType.diskType(""), Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    public Regions regions() {
        // TODO: YCloud has dev, prod instances, which *might* be considered as regions
        // TODO: currently only have one region: "local" but it is not used
        return new Regions(regions.keySet(), defaultRegion, regionDisplayNames);
    }

    @Override
    public VmTypes vmTypes(Boolean extended) {
        return new VmTypes(virtualMachines(extended), defaultVirtualMachine());
    }

    @Override
    public Map<AvailabilityZone, VmTypes> vmTypesPerAvailabilityZones(Boolean extended) {
        Map<AvailabilityZone, VmTypes> result = new HashMap<>();
        for (Map.Entry<Region, List<AvailabilityZone>> zones : regions.entrySet()) {
            for (AvailabilityZone zone : zones.getValue()) {
                Collection<VmType> virtualMachines = new ArrayList<>();
                VmType defaultVirtualMachine = vmType("");
                result.put(zone, new VmTypes(virtualMachines, defaultVirtualMachine));
            }
        }
        return result;
    }

    @Override
    public AvailabilityZones availabilityZones() {
        return new AvailabilityZones(regions);
    }

    @Override
    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("yarn", resource);
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        List<StackParamValidation> additionalStackParameterValidations = Lists.newArrayList();
        additionalStackParameterValidations.add(new StackParamValidation(YarnConstants.YARN_LIFETIME_PARAMETER, false, Integer.class, Optional.empty()));
        additionalStackParameterValidations.add(new StackParamValidation(YarnConstants.YARN_QUEUE_PARAMETER, false, String.class, Optional.empty()));
        return additionalStackParameterValidations;
    }

    @Override
    public PlatformOrchestrator orchestratorParams() {
        return new PlatformOrchestrator(
                Collections.singletonList(orchestrator(OrchestratorConstants.SALT)),
                orchestrator(OrchestratorConstants.SALT));
    }

    @Override
    public TagSpecification tagSpecification() {
        return null;
    }

    @Override
    public VmRecommendations recommendedVms() {
        return null;
    }

    @Override
    public String getDefaultRegionsConfigString() {
        return defaultRegions;
    }

    @Override
    public String getDefaultRegionString() {
        return nthElement(regions.keySet(), 0).value();
    }

    @Override
    public String platforName() {
        return YarnConstants.YARN_PLATFORM.value();
    }

    @Override
    public SpecialParameters specialParameters() {
        Map<String, Boolean> specialParameters = Maps.newHashMap();
        specialParameters.put(PlatformParametersConsts.CUSTOM_INSTANCETYPE, Boolean.TRUE);
        return new SpecialParameters(specialParameters);
    }

    private Collection<VmType> virtualMachines(Boolean extended) {
        return new ArrayList<>();
    }

    private VmType defaultVirtualMachine() {
        return vmType("");
    }
}