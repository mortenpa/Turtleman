package com.github.mortenpa.turtle.controller.util;

import com.github.mortenpa.turtle.controller.CustomerApiResponse;
import com.github.mortenpa.turtle.data.entity.CustomerEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ApiResponseHandler {

    static public ResponseEntity<CustomerApiResponse> buildApiResponse(boolean success, String message, HttpStatus status) {
        CustomerApiResponse response = new CustomerApiResponse(success, message);
        return ResponseEntity.status(status).body(response);
    }

    static public ResponseEntity<CustomerApiResponse> buildApiResponse(boolean success, CustomerEntity customer , HttpStatus status) {
        CustomerApiResponse response = new CustomerApiResponse(success, customer);
        return ResponseEntity.status(status).body(response);
    }
}
