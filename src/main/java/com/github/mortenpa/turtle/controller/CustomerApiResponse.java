package com.github.mortenpa.turtle.controller;

import com.github.mortenpa.turtle.data.entity.CustomerEntity;

public class CustomerApiResponse {
    boolean success;
    private String message;
    CustomerEntity customer;


    public CustomerApiResponse() {}

    public CustomerApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public CustomerApiResponse(boolean success, CustomerEntity customer) {
        this.success = success;
        this.customer = customer;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

}
