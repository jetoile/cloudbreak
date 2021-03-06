package com.sequenceiq.cloudbreak.converter.v2;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayJsonValidator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class StackV2RequestToGatewayConverter extends AbstractConversionServiceAwareConverter<StackV2Request, Gateway> {

    @Inject
    private GatewayConvertUtil convertUtil;

    @Inject
    private GatewayJsonValidator gatewayJsonValidator;

    @Override
    public Gateway convert(StackV2Request source) {
        Gateway gateway = new Gateway();
        GatewayJson gatewayJson = source.getCluster().getAmbari().getGateway();
        ValidationResult validationResult = gatewayJsonValidator.validate(gatewayJson);
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        convertUtil.setBasicProperties(gatewayJson, gateway);
        convertUtil.setTopologies(gatewayJson, gateway);
        convertUtil.setGatewayPathAndSsoProvider(source.getGeneral().getName(), gatewayJson, gateway);
        convertUtil.generateSignKeys(gateway);
        return gateway;
    }

}
