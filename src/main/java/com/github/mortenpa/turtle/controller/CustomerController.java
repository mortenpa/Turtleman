package com.github.mortenpa.turtle.controller;

import com.github.mortenpa.turtle.data.dto.CustomerDTO;
import com.github.mortenpa.turtle.data.entity.CustomerEntity;
import com.github.mortenpa.turtle.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.github.mortenpa.turtle.controller.util.ApiResponseHandler.buildApiResponse;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    public CustomerEntity createCustomerEntityFromDTO(CustomerDTO customerDTO) {
        return new CustomerEntity(
                customerDTO.getFirstName(),
                customerDTO.getLastName(),
                customerDTO.getEmail()
        );
    }

    @PostMapping
    public ResponseEntity<CustomerApiResponse> addCustomer(
            @RequestBody @Valid CustomerDTO customerDTO
    ) {
        CustomerEntity customer = createCustomerEntityFromDTO(customerDTO);
        CustomerEntity addedCustomer = customerService.addOrModify(customer);

        if (addedCustomer == null) {
            return buildApiResponse(false,"failed to add a new customer", HttpStatus.NOT_FOUND);
        }
        else {
            return buildApiResponse(true, addedCustomer, HttpStatus.CREATED);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerApiResponse> getCustomer(
            @PathVariable long id
    ) {
        Optional<CustomerEntity> fetchedCustomer = customerService.getById(id);

        if (fetchedCustomer.isPresent()) {
            return buildApiResponse(true, fetchedCustomer.get(), HttpStatus.OK);
        }
        else {
            return buildApiResponse(false, "Customer not found", HttpStatus.NOT_FOUND);
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<CustomerApiResponse> updateCustomer(
            @PathVariable long id,
            @RequestBody @Valid CustomerDTO customerDTO
    ) {

        // start by checking for customer existence
        // PUT will only allow modification, not addition to db
        if (customerService.getById(id).isEmpty()) {
            return buildApiResponse(false, "Customer with the ID does not exist", HttpStatus.NOT_FOUND);
        }

        CustomerEntity customer = createCustomerEntityFromDTO(customerDTO);
        customer.setId(id); // set the customer id from the parameter

        CustomerEntity addedCustomer = customerService.addOrModify(customer);

        if (addedCustomer == null) {
            return buildApiResponse(false, "Customer modification failed", HttpStatus.NOT_FOUND);
        }
        else {
            return buildApiResponse(true, addedCustomer, HttpStatus.OK);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CustomerApiResponse> deleteCustomer(@PathVariable long id) {
        boolean successfulDelete = customerService.delete(id);

        if (successfulDelete) {
            return buildApiResponse(true, "Customer deleted successfully", HttpStatus.OK);
        } else {
            return buildApiResponse(false, "Customer deletion failed", HttpStatus.NOT_FOUND);
        }
    }

}
