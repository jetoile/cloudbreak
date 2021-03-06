package com.sequenceiq.cloudbreak.repository;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisablePermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = HostGroup.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisablePermission
public interface HostGroupRepository extends DisabledBaseRepository<HostGroup, Long> {

    @EntityGraph(value = "HostGroup.constraint.instanceGroup.instanceMetaData", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT h FROM HostGroup h LEFT JOIN FETCH h.hostMetadata LEFT JOIN FETCH h.recipes WHERE h.cluster.id= :clusterId")
    Set<HostGroup> findHostGroupsInCluster(@Param("clusterId") Long clusterId);

    @EntityGraph(value = "HostGroup.constraint.instanceGroup.instanceMetaData", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT h FROM HostGroup h LEFT JOIN FETCH h.hostMetadata LEFT JOIN FETCH h.recipes WHERE h.cluster.id= :clusterId AND h.name= :hostGroupName")
    HostGroup findHostGroupInClusterByName(@Param("clusterId") Long clusterId, @Param("hostGroupName") String hostGroupName);

    @EntityGraph(value = "HostGroup.constraint.instanceGroup.instanceMetaData", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT h FROM HostGroup h JOIN h.recipes r WHERE r.id= :recipeId")
    Set<HostGroup> findAllHostGroupsByRecipe(@Param("recipeId") Long recipeId);

    @EntityGraph(value = "HostGroup.constraint.instanceGroup.instanceMetaData", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT h FROM HostGroup h WHERE h.cluster.id= :clusterId AND h.constraint.instanceGroup.groupName= :instanceGroupName")
    HostGroup findHostGroupsByInstanceGroupName(@Param("clusterId") Long clusterId, @Param("instanceGroupName") String instanceGroupName);

}
