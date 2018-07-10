package com.sequenceiq.cloudbreak.domain.security;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Entity
@Table(name = "organization")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "organization_generator")
    @SequenceGenerator(name = "organization_generator", sequenceName = "organization_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @ManyToOne
    private Tenant tenant;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<OrganizationProperty> properties = new HashSet<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserToOrganization> users = new HashSet<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Stack> stacks = new HashSet<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Recipe> recipes = new HashSet<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Blueprint> blueprints = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Set<OrganizationProperty> getProperties() {
        return properties;
    }

    public void setProperties(Set<OrganizationProperty> properties) {
        this.properties = properties;
    }

    public Set<UserToOrganization> getUsers() {
        return users;
    }

    public void setUsers(Set<UserToOrganization> users) {
        this.users = users;
    }

    public Set<Stack> getStacks() {
        return stacks;
    }

    public void setStacks(Set<Stack> stacks) {
        this.stacks = stacks;
    }
}
