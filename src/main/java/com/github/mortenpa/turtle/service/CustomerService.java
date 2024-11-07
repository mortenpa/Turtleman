package com.github.mortenpa.turtle.service;

import com.github.mortenpa.turtle.data.entity.CustomerEntity;
import com.github.mortenpa.turtle.error.DuplicateEmailException;
import com.github.mortenpa.turtle.error.NullNotAllowedException;
import com.github.mortenpa.turtle.repository.CustomerRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
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

    public CustomerEntity addOrModify(@Valid CustomerEntity customerEntity) {
        try {
            customerRepository.save(customerEntity);

            // if the save was successful, we fetch the element from the repository
            // since in some cases the input object is not complete (missing datetimes on modify operations)
            Optional<CustomerEntity> savedCustomer = customerRepository.findById(customerEntity.getId());

            return savedCustomer.orElse(null);
        // let's deal with any data/constraint violations we might encounter
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateEmailException(exception)) {
                throw new DuplicateEmailException("Email is not unique!");
            }
            else if (isNullNotAllowedException(exception)) {
                throw new NullNotAllowedException("null is not allowed for properties");
            }
            else {
                throw exception;
            }
        } catch (ConstraintViolationException exception) {
            throw new ConstraintViolationException(exception.getConstraintViolations());
        }
    }

    public boolean delete(long customerId) {
        // start by checking if the customer exists in the repository
        // since we want to return information about whether the operation was successful
        if (customerRepository.existsById(customerId)) {
            customerRepository.deleteById(customerId);
            return true;
        }
        return false;
    }

    // this is kinda hacky but it works so ¯\_(ツ)_/¯
    // using error codes could be an improvement
    private boolean isDuplicateEmailException(DataIntegrityViolationException exception) {
        String message = exception.getMessage().toLowerCase();
        return message.contains("unique index or primary key violation") &&
               message.contains("public.customer(email nulls first)");
    }

    private boolean isNullNotAllowedException(DataIntegrityViolationException exception) {
        return exception.getMessage().toLowerCase().contains("null not allowed for column");
    }

}
