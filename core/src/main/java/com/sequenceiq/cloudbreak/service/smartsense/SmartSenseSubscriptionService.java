package com.sequenceiq.cloudbreak.service.smartsense;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Iterator;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.aspect.PermissionType;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.SmartSenseConfigurationNotFoundException;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.repository.SmartSenseSubscriptionRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;

@Service
public class SmartSenseSubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartSenseSubscriptionService.class);

    @Inject
    private SmartSenseSubscriptionRepository smartSenseSubscriptionRepository;

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private AuthorizationService authorizationService;

    @Value("${cb.smartsense.id:}")
    private String defaultSmartsenseId;

    @PostConstruct
    public void init() {
        if (!defaultSmartsenseId.isEmpty() && !Pattern.matches(SmartSenseSubscriptionJson.ID_PATTERN, defaultSmartsenseId)) {
            throw new IllegalArgumentException(SmartSenseSubscriptionJson.ID_FORMAT);
        }
    }

    public SmartSenseSubscription create(SmartSenseSubscription subscription) {
        long count = smartSenseSubscriptionRepository.count();
        if (count != 0L) {
            throw new BadRequestException("Only one SmartSense subscription is allowed by deployment.");
        }
        try {
            subscription = smartSenseSubscriptionRepository.save(subscription);
            LOGGER.info("SmartSense subscription has been created: {}", subscription);
            return subscription;
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.SMARTSENSE_SUBSCRIPTION, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public SmartSenseSubscription update(SmartSenseSubscription subscription) {
        return smartSenseSubscriptionRepository.save(subscription);
    }

    public void delete(SmartSenseSubscription subscription) {
        if (!flexSubscriptionService.hasBySmartSenseSubscription(subscription)) {
            throw new BadRequestException("Subscription could not be deleted, because it is assigned to Flex subscription(s).");
        }
        smartSenseSubscriptionRepository.delete(subscription);
        LOGGER.info("SmartSense subscription has been deleted: {}", subscription);
    }

    public void delete(Long id) {
        SmartSenseSubscription subscription = smartSenseSubscriptionRepository.findById(id).orElseThrow(notFound("SmartSense subscription", id));
        delete(subscription);
    }

    public void delete(String subscriptionId, IdentityUser cbUser) {
        SmartSenseSubscription subscription = smartSenseSubscriptionRepository.findBySubscriptionIdAndAccount(subscriptionId, cbUser.getAccount());
        delete(subscription);
    }

    public SmartSenseSubscription findById(Long id) {
        LOGGER.debug("Looking for SmartSense subscription with id: {}", id);
        return smartSenseSubscriptionRepository.findById(id).orElseThrow(notFound("SmartSense subscription", id));
    }

    public Optional<SmartSenseSubscription> getDefault() {
        LOGGER.debug("Get the SmartSense subscription");
        Iterator<SmartSenseSubscription> subscriptions = smartSenseSubscriptionRepository.findAll().iterator();
        return subscriptions.hasNext() ? Optional.of(subscriptions.next()) : Optional.empty();
    }

    public SmartSenseSubscription getDefaultForUser(@Nonnull IdentityUser cbUser) {
        Optional<SmartSenseSubscription> subscription = obtainSmartSenseSubscription(cbUser);
        checkSmartSenseSubscriptionAuthorization(subscription);
        return subscription.get();
    }

    private Optional<SmartSenseSubscription> obtainSmartSenseSubscription(IdentityUser cbUser) {
        Optional<SmartSenseSubscription> subscription = getByAccountAndOwnerSilently(cbUser);
        if (subscription.isPresent()) {
            upgradeDefaultSmartSenseSubscription(subscription.get());
        } else {
            LOGGER.info("Unable to find subscription for user.");
            subscription = Optional.ofNullable(createSubscriptionFromIdentityUser(cbUser));
        }
        return subscription;
    }

    private Optional<SmartSenseSubscription> getByAccountAndOwnerSilently(IdentityUser cbUser) {
        try {
            return Optional.ofNullable(smartSenseSubscriptionRepository.findByAccountAndOwner(cbUser.getAccount(), cbUser.getUserId()));
        } catch (AccessDeniedException ignore) {
            return Optional.empty();
        }

    }

    private void upgradeDefaultSmartSenseSubscription(SmartSenseSubscription subscription) {
        if (!StringUtils.isEmpty(defaultSmartsenseId) && !defaultSmartsenseId.equals(subscription.getSubscriptionId())) {
            LOGGER.info("Upgrading default SmartSense subscription");
            subscription.setSubscriptionId(defaultSmartsenseId);
            smartSenseSubscriptionRepository.save(subscription);
        }
    }

    private void checkSmartSenseSubscriptionAuthorization(Optional<SmartSenseSubscription> subscription) {
        try {
            authorizationService.hasPermission(subscription.orElseThrow(SmartSenseConfigurationNotFoundException::new), PermissionType.READ.name());
        } catch (AccessDeniedException | SmartSenseConfigurationNotFoundException issue) {
            String message = "Unable to identify SmartSense subscription for the user.";
            LOGGER.warn(message);
            throw new SmartSenseSubscriptionAccessDeniedException(message, issue);
        }
    }

    @Nullable
    private SmartSenseSubscription createSubscriptionFromIdentityUser(IdentityUser cbUser) {
        SmartSenseSubscription newSubscription = null;
        if (!StringUtils.isEmpty(defaultSmartsenseId)) {
            LOGGER.info("Generating default SmartSense subscription");
            newSubscription = new SmartSenseSubscription();
            newSubscription.setSubscriptionId(defaultSmartsenseId);
            newSubscription.setAccount(cbUser.getAccount());
            newSubscription.setOwner(cbUser.getUserId());
            newSubscription.setPublicInAccount(true);
            smartSenseSubscriptionRepository.save(newSubscription);
        }
        return newSubscription;
    }

}
