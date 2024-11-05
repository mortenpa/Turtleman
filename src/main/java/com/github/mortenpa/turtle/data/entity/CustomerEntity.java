package com.github.mortenpa.turtle.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

@Entity
@Table(name = "customer")
public class CustomerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotEmpty
    @Size(max = 50)
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotEmpty
    @Size(max = 50)
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotEmpty
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "created_datetime", updatable = false)
    private OffsetDateTime createdDtime;

    @Column(name = "modified_datetime", updatable = false)
    private OffsetDateTime modifiedDtime;

    public CustomerEntity() {
        this.createdDtime = OffsetDateTime.now();
        this.modifiedDtime = OffsetDateTime.now();
    }

    public CustomerEntity(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.createdDtime = OffsetDateTime.now();
        this.modifiedDtime = OffsetDateTime.now();
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

    public OffsetDateTime getModifiedDtime() {
        return modifiedDtime;
    }

    @Override
    public String toString() {
        return "[" + firstName + ", " + lastName + ", " + email + "]";
    }
}
