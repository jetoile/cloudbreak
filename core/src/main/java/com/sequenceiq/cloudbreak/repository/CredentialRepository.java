package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.BaseRepository;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Credential.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface CredentialRepository extends BaseRepository<Credential, Long> {

    Set<Credential> findAllByCloudPlatform(@Param("cloudPlatform") String cloudPlatform);

    @Query("SELECT b FROM Credential b WHERE b.name= :name AND b.account= :account AND b.archived IS FALSE")
    Credential findOneByName(@Param("name") String name, @Param("account") String account);

    @Query("SELECT c FROM Credential c WHERE c.owner= :user AND c.archived IS FALSE")
    Set<Credential> findForUser(@Param("user") String user);

    @Query("SELECT c FROM Credential c WHERE ((c.account= :account AND c.publicInAccount= true) OR c.owner= :user) AND c.archived IS FALSE")
    Set<Credential> findPublicInAccountForUser(@Param("user") String user, @Param("account") String account);

    @Query("SELECT c FROM Credential c WHERE ((c.account= :account AND c.publicInAccount= true) OR c.owner= :user) AND c.archived IS FALSE "
            + "AND cloudPlatform in (:cloudPlatforms)")
    Set<Credential> findPublicInAccountForUserFilterByPlatforms(@Param("user") String user, @Param("account") String account,
            @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @Query("SELECT c FROM Credential c WHERE c.account= :account AND c.archived IS FALSE")
    Set<Credential> findAllInAccount(@Param("account") String account);

    @Query("SELECT c FROM Credential c WHERE c.account= :account AND c.archived IS FALSE and cloudPlatform in (:cloudPlatforms)")
    Set<Credential> findAllInAccountAndFilterByPlatforms(@Param("account") String account, @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @Query("SELECT c FROM Credential c WHERE c.name= :name AND ((c.publicInAccount=true and c.account= :account) OR c.owner= :owner) AND c.archived IS FALSE")
    Credential findByNameInAccount(@Param("name") String name, @Param("account") String account, @Param("owner") String owner);

    @Query("SELECT c FROM Credential c WHERE c.id= :id AND c.account= :account AND c.archived IS FALSE")
    Credential findByIdInAccount(@Param("id") Long id, @Param("account") String account);

    @Query("SELECT c FROM Credential c WHERE c.owner= :owner and c.name= :name AND c.archived IS FALSE")
    Credential findByNameInUser(@Param("name") String name, @Param("owner") String owner);

    Long countByTopology(Topology topology);

    Set<Credential> findByTopology(Topology topology);
}