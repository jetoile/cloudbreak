package com.sequenceiq.cloudbreak.domain.security;

import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "users_generator")
    @SequenceGenerator(name = "users_generator", sequenceName = "users_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Basic(optional = false)
    private String email;

    private String company;

    @Column(name = "is_cb_admin")
    private Boolean isCbAdmin;

    @Basic(optional = false)
    @Column(name = "tenant_role")
    private String tenantRole;

    @ManyToOne
    private Tenant tenant;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserToOrganization> organizations;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Boolean getCbAdmin() {
        return isCbAdmin;
    }

    public void setCbAdmin(Boolean cbAdmin) {
        isCbAdmin = cbAdmin;
    }

    public String getTenantRole() {
        return tenantRole;
    }

    public void setTenantRole(String tenantRole) {
        this.tenantRole = tenantRole;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Set<UserToOrganization> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(Set<UserToOrganization> organizations) {
        this.organizations = organizations;
    }
}
