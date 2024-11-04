package com.github.mortenpa.turtle.service;

import com.github.mortenpa.turtle.data.entity.CustomerEntity;
import com.github.mortenpa.turtle.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Optional<CustomerEntity> getById(long customerId) {
        return customerRepository.findById(customerId);
    }

    public List<CustomerEntity> getAll() {
        return customerRepository.findAll();
    }

    public CustomerEntity addOrModify(CustomerEntity customerEntity) {
        return customerRepository.save(customerEntity);
    }

    public void delete(long customerId) {
        customerRepository.deleteById(customerId);
    }

}
