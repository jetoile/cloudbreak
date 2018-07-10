package com.sequenceiq.cloudbreak.domain.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "organization_properties")
public class OrganizationProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "organization_properties_generator")
    @SequenceGenerator(name = "organization_properties_generator", sequenceName = "organization_properties_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "property_name")
    private String propertyName;

    @Column(name = "property_value")
    private String propertyValue;

    @ManyToOne
    private Organization organization;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}
