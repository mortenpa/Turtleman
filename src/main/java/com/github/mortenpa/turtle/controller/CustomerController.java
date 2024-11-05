package com.github.mortenpa.turtle.controller;

import com.github.mortenpa.turtle.data.entity.CustomerEntity;
import com.github.mortenpa.turtle.repository.CustomerRepository;
import com.github.mortenpa.turtle.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/customers")
    public ResponseEntity<CustomerEntity> addCustomer(
            @RequestBody CustomerEntity customer
    ) {

        CustomerEntity addedCustomer = customerService.addOrModify(customer);

        if (addedCustomer == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        else {
            return new ResponseEntity<>(addedCustomer, HttpStatus.CREATED);
        }
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerEntity> getCustomer(@PathVariable long id) {

        Optional<CustomerEntity> fetchedCustomer = customerService.getById(id);

        if (fetchedCustomer.isPresent()) {
            return new ResponseEntity<>(fetchedCustomer.get(), HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("customers/{id}")
    public ResponseEntity<CustomerEntity> updateCustomer(@PathVariable long id) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @DeleteMapping("customers/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable long id) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

}
