package com.sequenceiq.cloudbreak.domain.security;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "user_to_organization")
public class UserToOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "user_to_organization")
    @SequenceGenerator(name = "user_to_organization", sequenceName = "user_to_organization_id_seq", allocationSize = 1)
    private Long id;

    private String role;

    @ManyToOne
    private User user;

    @ManyToOne
    private Organization organization;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}
