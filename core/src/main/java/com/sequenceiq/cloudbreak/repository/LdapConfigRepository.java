package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.BaseRepository;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = LdapConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
@HasPermission
public interface LdapConfigRepository extends BaseRepository<LdapConfig, Long> {

    @Query("SELECT c FROM LdapConfig c WHERE c.name= :name and c.account= :account")
    LdapConfig findByNameInAccount(@Param("name") String name, @Param("account") String account);

    @Query("SELECT c FROM LdapConfig c WHERE (c.account= :account AND c.publicInAccount= true)  OR c.owner= :owner")
    Set<LdapConfig> findPublicInAccountForUser(@Param("owner") String userId, @Param("account") String account);

    @Query("SELECT c FROM LdapConfig c WHERE c.account= :account")
    Set<LdapConfig> findAllInAccount(@Param("account") String account);

    @Query("SELECT c FROM LdapConfig c WHERE c.owner= :owner")
    Set<LdapConfig> findForUser(@Param("owner") String userId);

    @Query("SELECT c FROM LdapConfig c WHERE c.name= :name and c.owner= :owner")
    LdapConfig findByNameForUser(@Param("name") String name, @Param("owner") String userId);
}
