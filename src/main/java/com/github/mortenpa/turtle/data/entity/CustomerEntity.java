package com.github.mortenpa.turtle.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

@Entity
@Table(name = "customer")
public class CustomerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Size(min=1, max = 50)
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Size(min=1, max = 50)
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Email
    @Size(min=3, max=254)
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "created_datetime", updatable = false)
    private OffsetDateTime createdDtime;

    @Column(name = "modified_datetime")
    private OffsetDateTime modifiedDtime;

    @PrePersist
    private void insertDatetimes() {
        // Set both createdDtime and modifiedDtime only if createdDtime is null (i.e., on creation)
        if (createdDtime == null) {
            OffsetDateTime now = OffsetDateTime.now();
            createdDtime = now;
            modifiedDtime = now;
        }
    }

    @PreUpdate
    private void updateModifiedDateTime() {
        modifiedDtime = OffsetDateTime.now();
    }

    public CustomerEntity() {}

    public CustomerEntity(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OffsetDateTime getCreatedDtime() {
        return createdDtime;
    }

    public void setCreatedDtime(OffsetDateTime createdDtime) {
        this.createdDtime = createdDtime;
    }

    public OffsetDateTime getModifiedDtime() {
        return modifiedDtime;
    }

    public void setModifiedDtime(OffsetDateTime modifiedDtime) {
        this.modifiedDtime = modifiedDtime;
    }

    @Override
    public String toString() {
        return "CustomerEntity [" + firstName + ", " + lastName + ", " + email + " " + createdDtime + ", " + modifiedDtime + "]";
    }
}
