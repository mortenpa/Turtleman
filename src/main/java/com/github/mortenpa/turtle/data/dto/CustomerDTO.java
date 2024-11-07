package com.github.mortenpa.turtle.data.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class CustomerDTO {
    @Size(min=1, max = 50)
    private String firstName;

    @Size(min=1, max = 50)
    private String lastName;

    @Email
    @Size(min=3, max=254)
    private String email;

    public CustomerDTO() {}

    public CustomerDTO(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
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

    public String toString() {
        return "CustomerDTO [firstName=" + firstName + ", lastName=" + lastName + ", email=" + email + "]";
    }
}
