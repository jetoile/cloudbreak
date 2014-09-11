package com.sequenceiq.cloudbreak.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;

import com.sequenceiq.cloudbreak.domain.AwsTemplate;

public interface AwsTemplateRepository extends CrudRepository<AwsTemplate, Long> {

    @PostAuthorize("returnObject?.user == principal")
    AwsTemplate findOne(@Param("id") Long id);
}