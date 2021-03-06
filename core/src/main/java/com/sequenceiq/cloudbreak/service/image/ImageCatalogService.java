package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.service.image.StatedImage.statedImage;
import static com.sequenceiq.cloudbreak.service.image.StatedImages.statedImages;
import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakVersion;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.UserProfile;
import com.sequenceiq.cloudbreak.repository.ImageCatalogRepository;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.account.AccountPreferencesService;
import com.sequenceiq.cloudbreak.service.user.UserProfileService;
import com.sequenceiq.cloudbreak.util.SemanticVersionComparator;

@Component
public class ImageCatalogService {

    public static final String UNDEFINED = "";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogService.class);

    private static final String CLOUDBREAK_DEFAULT_CATALOG_NAME = "cloudbreak-default";

    @Value("${info.app.version:}")
    private String cbVersion;

    @Value("${cb.image.catalog.url}")
    private String defaultCatalogUrl;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    @Inject
    private ImageCatalogRepository imageCatalogRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private ImageCatalogVersionFilter versionFilter;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private UserProfileService userProfileService;

    @Inject
    private AccountPreferencesService accountPreferencesService;

    public StatedImages getImagesOsFiltered(String provider, String os) throws CloudbreakImageCatalogException {
        StatedImages images = getImages(getDefaultImageCatalog(), provider, cbVersion);
        if (!StringUtils.isEmpty(os)) {
            Images rawImages = images.getImages();
            List<Image> baseImages = filterImagesByOs(rawImages.getBaseImages(), os);
            List<Image> hdpImages = filterImagesByOs(rawImages.getHdpImages(), os);
            List<Image> hdfImages = filterImagesByOs(rawImages.getHdfImages(), os);
            images = statedImages(new Images(baseImages, hdpImages, hdfImages, rawImages.getSuppertedVersions()),
                    images.getImageCatalogUrl(), images.getImageCatalogName());
        }
        return images;
    }

    public StatedImage getLatestBaseImageDefaultPreferred(String platform, String os) throws CloudbreakImageCatalogException, CloudbreakImageNotFoundException {
        StatedImages statedImages = getImagesOsFiltered(platform, os);
        Optional<Image> defaultBaseImage = getLatestBaseImageDefaultPreferred(statedImages, os);
        if (defaultBaseImage.isPresent()) {
            return statedImage(defaultBaseImage.get(), statedImages.getImageCatalogUrl(), statedImages.getImageCatalogName());
        } else {
            throw new CloudbreakImageNotFoundException(imageNotFoundErrorMessage(platform));
        }
    }

    public Optional<Image> getLatestBaseImageDefaultPreferred(StatedImages statedImages, String os) {
        List<Image> baseImages = statedImages.getImages().getBaseImages();
        if (!StringUtils.isEmpty(os)) {
            baseImages = filterImagesByOs(baseImages, os);
        }
        return getLatestImageDefaultPreferred(baseImages);
    }

    public StatedImage getPrewarmImageDefaultPreferred(String platform, String clusterType, String clusterVersion, String os)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImages statedImages = getImagesOsFiltered(platform, os);
        List<Image> images = getImagesForClusterType(statedImages, clusterType);
        Optional<Image> selectedImage = Optional.empty();
        if (!CollectionUtils.isEmpty(images)) {
            List<Image> matchingVersionImages = images.stream().filter(i -> {
                String[] repoIdParts = i.getStackDetails().getRepo().getStack().get("repoid").split("-");
                return repoIdParts.length > 1 && repoIdParts[1].equals(clusterVersion);
            }).collect(Collectors.toList());
            selectedImage = getLatestImageDefaultPreferred(matchingVersionImages);
        }
        if (!selectedImage.isPresent()) {
            selectedImage = getLatestBaseImageDefaultPreferred(statedImages, os);
        }
        if (!selectedImage.isPresent()) {
            throw new CloudbreakImageNotFoundException(imageNotFoundErrorMessage(platform));
        }
        return statedImage(selectedImage.get(), statedImages.getImageCatalogUrl(), statedImages.getImageCatalogName());
    }

    private String imageNotFoundErrorMessage(String platform) {
        return String.format("Could not find any image for platform '%s' and Cloudbreak version '%s'.", platform, cbVersion);
    }

    public StatedImages getImages(String name, String provider) throws CloudbreakImageCatalogException {
        return getImages(name, ImmutableSet.of(provider));
    }

    public StatedImages getImages(String name, Set<String> providers) throws CloudbreakImageCatalogException {
        try {
            ImageCatalog imageCatalog = get(name);
            return getImages(imageCatalog, providers, cbVersion);
        } catch (AccessDeniedException | NotFoundException ignore) {
            throw new CloudbreakImageCatalogException(String.format("The %s catalog does not exist or does not belongs to your account.", name));
        }
    }

    public StatedImage getImage(String imageId) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return getImage(defaultCatalogUrl, CLOUDBREAK_DEFAULT_CATALOG_NAME, imageId);
    }

    public StatedImage getImage(String catalogUrl, String catalogName, String imageId) throws CloudbreakImageNotFoundException,
            CloudbreakImageCatalogException {
        Images images = imageCatalogProvider.getImageCatalogV2(catalogUrl).getImages();
        Optional<? extends Image> image = getImage(imageId, images);
        if (!image.isPresent()) {
            images = imageCatalogProvider.getImageCatalogV2(catalogUrl, true).getImages();
            image = getImage(imageId, images);
        }
        if (!image.isPresent()) {
            throw new CloudbreakImageNotFoundException(String.format("Could not find any image with id: '%s'.", imageId));
        }
        return statedImage(image.get(), catalogUrl, catalogName);
    }

    public StatedImage getImageByCatalogName(String imageId, String catalogName) throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        StatedImage image;
        if (StringUtils.isEmpty(catalogName)) {
            image = getImage(imageId);
        } else {
            ImageCatalog imageCatalog = get(catalogName);
            if (imageCatalog != null) {
                image = getImage(imageCatalog.getImageCatalogUrl(), imageCatalog.getImageCatalogName(), imageId);
            } else {
                String msg = String.format("The specified image catalog '%s' could not be found.", catalogName);
                LOGGER.error(msg);
                throw new CloudbreakImageNotFoundException(msg);
            }
        }
        return image;
    }

    public ImageCatalog create(ImageCatalog imageCatalog) {
        try {
            if (isEnvDefault(imageCatalog.getImageCatalogName())) {
                throw new BadRequestException(String
                        .format("%s cannot be created because it is an environment default image catalog.", imageCatalog.getImageCatalogName()));
            }
            return imageCatalogRepository.save(imageCatalog);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.IMAGE_CATALOG, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public void delete(String name) {
        if (isEnvDefault(name)) {
            throw new BadRequestException(String.format("%s cannot be deleted because it is an environment default image catalog.", name));
        }
        ImageCatalog imageCatalog = get(name);
        imageCatalog.setArchived(true);
        setImageCatalogAsDefault(null);
        imageCatalog.setImageCatalogName(generateArchiveName(name));
        imageCatalogRepository.save(imageCatalog);
        LOGGER.info("Image catalog has been archived: {}", imageCatalog);
    }

    public ImageCatalog get(String name) {
        ImageCatalog imageCatalog;
        if (isEnvDefault(name)) {
            imageCatalog = getCloudbreakDefaultImageCatalog();
        } else {
            IdentityUser user = authenticatedUserService.getCbUser();
            imageCatalog = imageCatalogRepository.findByName(name, user.getUserId(), user.getAccount());
        }
        return imageCatalog;
    }

    public ImageCatalog get(Long id) {
        ImageCatalog imageCatalog;
        IdentityUser user = authenticatedUserService.getCbUser();
        imageCatalog = imageCatalogRepository.findAllPublicInAccount(user.getAccount(), user.getUserId()).isEmpty() ? getCloudbreakDefaultImageCatalog()
                : findImageCatalog(id);
        return imageCatalog;
    }

    public ImageCatalog setAsDefault(String name) {

        removeDefaultFlag();

        if (!isEnvDefault(name)) {
            ImageCatalog imageCatalog = get(name);
            checkImageCatalog(imageCatalog, name);

            setImageCatalogAsDefault(imageCatalog);

            return imageCatalog;
        }
        return getCloudbreakDefaultImageCatalog();
    }

    public boolean isEnvDefault(String name) {
        return CLOUDBREAK_DEFAULT_CATALOG_NAME.equals(name);
    }

    private void setImageCatalogAsDefault(ImageCatalog imageCatalog) {
        UserProfile userProfile = getUserProfile();
        userProfile.setImageCatalog(imageCatalog);
        userProfileService.save(userProfile);
    }

    public ImageCatalog update(ImageCatalog source) {
        ImageCatalog imageCatalog = findImageCatalog(source.getId());
        checkImageCatalog(imageCatalog, source.getId());
        imageCatalog.setImageCatalogName(source.getImageCatalogName());
        imageCatalog.setImageCatalogUrl(source.getImageCatalogUrl());
        return create(imageCatalog);
    }

    private ImageCatalog findImageCatalog(Long id) {
        return imageCatalogRepository.findById(id).orElseThrow(notFound("Image catalog", id));
    }

    private void checkImageCatalog(ImageCatalog imageCatalog, Object filter) {
        if (imageCatalog == null) {
            throw new NotFoundException(String.format("Resource not found with filter [%s]", filter));
        }
    }

    public Iterable<ImageCatalog> getAllPublicInAccount() {
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        List<ImageCatalog> allPublicInAccount = imageCatalogRepository.findAllPublicInAccount(cbUser.getUserId(), cbUser.getAccount());
        allPublicInAccount.add(getCloudbreakDefaultImageCatalog());
        return allPublicInAccount;
    }

    private ImageCatalog getCloudbreakDefaultImageCatalog() {
        ImageCatalog imageCatalog = new ImageCatalog();
        imageCatalog.setImageCatalogName(CLOUDBREAK_DEFAULT_CATALOG_NAME);
        imageCatalog.setImageCatalogUrl(defaultCatalogUrl);
        imageCatalog.setPublicInAccount(true);
        return imageCatalog;
    }

    private Images emptyImages() {
        return new Images(emptyList(), emptyList(), emptyList(), emptySet());
    }

    private Optional<? extends Image> getImage(String imageId, Images images) {
        Optional<? extends Image> image = findFirstWithImageId(imageId, images.getBaseImages());
        if (!image.isPresent()) {
            image = findFirstWithImageId(imageId, images.getHdpImages());
        }
        if (!image.isPresent()) {
            image = findFirstWithImageId(imageId, images.getHdfImages());
        }
        return image;
    }

    public StatedImages getImages(ImageCatalog imageCatalog, String platform, String cbVersion) throws CloudbreakImageCatalogException {
        return getImages(imageCatalog, ImmutableSet.of(platform), cbVersion);
    }

    public StatedImages getImages(ImageCatalog imageCatalog, Set<String> platforms, String cbVersion) throws CloudbreakImageCatalogException {
        LOGGER.info("Determine images for imageCatalogUrl: '{}', platforms: '{}' and Cloudbreak version: '{}'.",
                imageCatalog.getImageCatalogUrl(), platforms, cbVersion);
        StatedImages images;
        validateRequestPlatforms(platforms);
        CloudbreakImageCatalogV2 imageCatalogV2 = imageCatalogProvider.getImageCatalogV2(imageCatalog.getImageCatalogUrl());
        Set<String> suppertedVersions;
        if (imageCatalogV2 != null) {
            Set<String> vMImageUUIDs = new HashSet<>();
            Set<String> defaultVMImageUUIDs = new HashSet<>();
            List<CloudbreakVersion> cloudbreakVersions = imageCatalogV2.getVersions().getCloudbreakVersions();
            String cbv = versionFilter.isVersionUnspecified(cbVersion) ? versionFilter.latestCloudbreakVersion(cloudbreakVersions) : cbVersion;
            List<CloudbreakVersion> exactMatchedImgs = cloudbreakVersions.stream()
                    .filter(cloudbreakVersion -> cloudbreakVersion.getVersions().contains(cbv)).collect(Collectors.toList());

            if (!exactMatchedImgs.isEmpty()) {
                for (CloudbreakVersion exactMatchedImg : exactMatchedImgs) {
                    vMImageUUIDs.addAll(exactMatchedImg.getImageIds());
                    defaultVMImageUUIDs.addAll(exactMatchedImg.getDefaults());
                }
                suppertedVersions = Collections.singleton(cbv);
            } else {
                LOGGER.info("No image found with exact match for version {} Trying prefix matching", cbv);
                PrefixMatchImages prefixMatchImages = prefixMatchForCBVersion(cbVersion, cloudbreakVersions);
                vMImageUUIDs.addAll(prefixMatchImages.vMImageUUIDs);
                defaultVMImageUUIDs.addAll(prefixMatchImages.defaultVMImageUUIDs);
                suppertedVersions = prefixMatchImages.supportedVersions;
            }

            List<Image> baseImages = filterImagesByPlatforms(platforms, imageCatalogV2.getImages().getBaseImages(), vMImageUUIDs);
            List<Image> hdpImages = filterImagesByPlatforms(platforms, imageCatalogV2.getImages().getHdpImages(), vMImageUUIDs);
            List<Image> hdfImages = filterImagesByPlatforms(platforms, imageCatalogV2.getImages().getHdfImages(), vMImageUUIDs);
            Stream.concat(Stream.concat(baseImages.stream(), hdpImages.stream()), hdfImages.stream()).collect(Collectors.toList())
                    .forEach(img -> img.setDefaultImage(defaultVMImageUUIDs.contains(img.getUuid())));

            images = statedImages(new Images(baseImages, hdpImages, hdfImages, suppertedVersions),
                    imageCatalog.getImageCatalogUrl(),
                    imageCatalog.getImageCatalogName());
        } else {
            images = statedImages(emptyImages(),
                    imageCatalog.getImageCatalogUrl(),
                    imageCatalog.getImageCatalogName());
        }
        return images;
    }

    public void validateRequestPlatforms(Set<String> platforms) throws CloudbreakImageCatalogException {
        Set<String> collect = platforms.stream()
                .filter(requestedPlatform -> accountPreferencesService.enabledPlatforms().stream()
                        .filter(enabledPlatform -> enabledPlatform.equalsIgnoreCase(requestedPlatform))
                        .collect(Collectors.toSet()).isEmpty())
                .collect(Collectors.toSet());
        if (!collect.isEmpty()) {
            throw new CloudbreakImageCatalogException(String.format("Platform(s) %s are not supported by the current catalog",
                    org.apache.commons.lang3.StringUtils.join(collect, ",")));
        }
    }

    public Images propagateImagesIfRequested(String name, boolean withImages) {
        if (withImages) {
            Set<String> platforms = accountPreferencesService.enabledPlatforms();
            try {
                return getImages(name, platforms).getImages();
            } catch (CloudbreakImageCatalogException e) {
                LOGGER.error("No images was found: ", e);
            }
        }
        return null;
    }

    private Optional<? extends Image> findFirstWithImageId(String imageId, Collection<? extends Image> images) {
        return images.stream()
                .filter(img -> img.getUuid().equals(imageId))
                .findFirst();
    }

    private List<Image> filterImagesByPlatforms(Collection<String> platforms, Collection<Image> images, Collection<String> vMImageUUIDs) {
        return images.stream()
                .filter(isPlatformMatching(platforms, vMImageUUIDs))
                .collect(Collectors.toList());
    }

    private static Predicate<Image> isPlatformMatching(Collection<String> platforms, Collection<String> vMImageUUIDs) {
        return img -> vMImageUUIDs.contains(img.getUuid())
                && img.getImageSetsByProvider().keySet().stream().anyMatch(p -> platforms.stream().anyMatch(platform -> platform.equalsIgnoreCase(p)));
    }

    private List<Image> filterImagesByOs(List<Image> images, String os) {
        Map<Boolean, List<Image>> partitionedImages = images.stream().collect(Collectors.partitioningBy(isMatchingOs(os)));
        if (!partitionedImages.get(false).isEmpty()) {
            LOGGER.debug("Used filter OS: | {} | Images filtered: {}", os,
                    partitionedImages.get(false).stream().map(a -> a.shortOsDescriptionFormat()).collect(Collectors.joining(", ")));
        }
        return partitionedImages.get(true);
    }

    private PrefixMatchImages prefixMatchForCBVersion(String cbVersion, Collection<CloudbreakVersion> cloudbreakVersions) {
        Set<String> supportedVersions = new HashSet<>();
        Set<String> vMImageUUIDs = new HashSet<>();
        Set<String> defaultVMImageUUIDs = new HashSet<>();
        String unReleasedVersion = versionFilter.extractUnreleasedVersion(cbVersion);
        boolean versionIsReleased = unReleasedVersion.equals(cbVersion);

        if (!versionIsReleased) {
            Set<CloudbreakVersion> unReleasedCbVersions = versionFilter.filterUnreleasedVersions(cloudbreakVersions, unReleasedVersion);
            for (CloudbreakVersion unReleasedCbVersion : unReleasedCbVersions) {
                vMImageUUIDs.addAll(unReleasedCbVersion.getImageIds());
                defaultVMImageUUIDs.addAll(unReleasedCbVersion.getDefaults());
            }
            supportedVersions = unReleasedCbVersions.stream().map(CloudbreakVersion::getVersions).flatMap(List::stream).collect(Collectors.toSet());
        }

        if (versionIsReleased || vMImageUUIDs.isEmpty()) {
            String releasedVersion = versionFilter.extractReleasedVersion(cbVersion);
            Set<CloudbreakVersion> releasedCbVersions = versionFilter.filterReleasedVersions(cloudbreakVersions, releasedVersion);

            Integer accumulatedImageCount = accumulateImageCount(releasedCbVersions);
            if (releasedCbVersions.isEmpty() || accumulatedImageCount == 0) {
                releasedCbVersions = previousCbVersion(releasedVersion, cloudbreakVersions);
            }

            for (CloudbreakVersion releasedCbVersion : releasedCbVersions) {
                vMImageUUIDs.addAll(releasedCbVersion.getImageIds());
                defaultVMImageUUIDs.addAll(releasedCbVersion.getDefaults());
            }
            supportedVersions = releasedCbVersions.stream().map(CloudbreakVersion::getVersions).flatMap(List::stream).collect(Collectors.toSet());
        }
        return new PrefixMatchImages(vMImageUUIDs, defaultVMImageUUIDs, supportedVersions);
    }

    private Set<CloudbreakVersion> previousCbVersion(String releasedVersion, Collection<CloudbreakVersion> cloudbreakVersions) {
        List<String> versions = cloudbreakVersions.stream()
                .map(CloudbreakVersion::getVersions)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        try {
            versions = versions.stream().sorted((o1, o2) -> new SemanticVersionComparator().compare(o2, o1)).collect(Collectors.toList());

            Predicate<String> ealierVersionPredicate = ver -> new SemanticVersionComparator().compare(ver, releasedVersion) == -1;
            Predicate<String> releaseVersionPredicate = ver -> versionFilter.extractExtendedUnreleasedVersion(cbVersion).equals(ver);
            Predicate<String> versionHasImagesPredicate = ver -> accumulateImageCount(cloudbreakVersions) > 0;
            Optional<String> applicableVersion = versions.stream()
                    .filter(ealierVersionPredicate)
                    .filter(releaseVersionPredicate)
                    .filter(versionHasImagesPredicate)
                    .findAny();

            return applicableVersion
                    .map(ver -> versionFilter.filterReleasedVersions(cloudbreakVersions, ver))
                    .orElse(emptySet());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Image catalog versions are not supported by this Cloudbreak version.");
        }
    }

    private Integer accumulateImageCount(Collection<CloudbreakVersion> cloudbreakVersions) {
        return cloudbreakVersions.stream()
                .map(CloudbreakVersion::getImageIds)
                .map(List::size)
                .reduce((x, y) -> x + y)
                .orElse(0);
    }

    private void removeDefaultFlag() {
        ImageCatalog imageCatalog = getDefaultImageCatalog();
        if (imageCatalog.getImageCatalogName() != null) {
            setImageCatalogAsDefault(null);
            imageCatalogRepository.save(imageCatalog);
        }
    }

    @Nonnull
    private ImageCatalog getDefaultImageCatalog() {
        IdentityUser user = authenticatedUserService.getCbUser();
        ImageCatalog imageCatalog = userProfileService.getOrCreate(user.getAccount(), user.getUserId()).getImageCatalog();
        if (imageCatalog == null) {
            imageCatalog = new ImageCatalog();
            imageCatalog.setImageCatalogUrl(defaultCatalogUrl);
            imageCatalog.setImageCatalogName(null);
        }
        return imageCatalog;
    }

    public String getDefaultImageCatalogName() {
        return getDefaultImageCatalog().getImageCatalogName();
    }

    public String getImageDefaultCatalogUrl() {
        return getDefaultImageCatalog().getImageCatalogUrl();
    }

    private UserProfile getUserProfile() {
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        return userProfileService.getOrCreate(cbUser.getAccount(), cbUser.getUserId(), cbUser.getUsername());
    }

    private static Predicate<Image> isMatchingOs(String os) {
        return img -> img.getOs().equalsIgnoreCase(os);
    }

    private Optional<Image> getLatestImageDefaultPreferred(List<Image> images) {
        List<Image> defaultImages = images.stream().filter(Image::isDefaultImage).collect(Collectors.toList());
        return defaultImages.isEmpty() ? images.stream().max(Comparator.comparing(Image::getDate))
                : defaultImages.stream().max(Comparator.comparing(Image::getDate));
    }

    private List<Image> getImagesForClusterType(StatedImages statedImages, String clusterType) {
        switch (clusterType) {
            case "HDP":
                return statedImages.getImages().getHdpImages();
            case "HDF":
                return statedImages.getImages().getHdfImages();
            default:
                return emptyList();
        }
    }

    private static class PrefixMatchImages {
        private final Set<String> vMImageUUIDs;

        private final Set<String> defaultVMImageUUIDs;

        private final Set<String> supportedVersions;

        private PrefixMatchImages(Set<String> vMImageUUIDs, Set<String> defaultVMImageUUIDs, Set<String> supportedVersions) {
            this.vMImageUUIDs = vMImageUUIDs;
            this.defaultVMImageUUIDs = defaultVMImageUUIDs;
            this.supportedVersions = supportedVersions;
        }
    }
}
