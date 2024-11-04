package com.github.mortenpa.turtle.unit;


import com.github.mortenpa.turtle.data.entity.CustomerEntity;
import com.github.mortenpa.turtle.repository.CustomerRepository;
import com.github.mortenpa.turtle.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class CustomerServiceTest {
    @InjectMocks
    private CustomerService customerService;

    @Mock
    private CustomerRepository customerRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void addOrModify_WhenInsertingValidCustomer_ReturnsCustomer() {
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(customer);

        CustomerEntity addedCustomer = customerService.addOrModify(customer);

        assertNotNull(addedCustomer);
        assertEquals(addedCustomer.getFirstName(), customer.getFirstName());
        verify(customerRepository, atLeastOnce()).save(any(CustomerEntity.class));
    }

    @Test
    public void addOrModify_WhenInsertingInvalidCustomer_ReturnsNull() {}

    @Test
    public void getById_WhenExistingCustomer_ReturnsCustomer() {
        // existing customer
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");
        CustomerEntity savedCustomer = customerService.addOrModify(customer);
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(savedCustomer);


        Optional<CustomerEntity> retrievedCustomer = customerService.getById(savedCustomer.getId());
        assertTrue(retrievedCustomer.isPresent());
        assertEquals(retrievedCustomer.get().getFirstName(), customer.getFirstName());




    }

    @Test
    public void getById_WhenMissingCustomer_ReturnsNull() {
        Optional<CustomerEntity> missingCustomer = customerService.getById(1);
        assertTrue(missingCustomer.isEmpty());
    }

    @Test
    public void addOrModify_WhenValidChanges_ReturnsCustomer() {
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(customer);

        CustomerEntity addedCustomer = customerService.addOrModify(customer);
        assertNotNull(addedCustomer);
        assertEquals(addedCustomer.getLastName(), customer.getLastName());
        verify(customerRepository, atLeastOnce()).save(any(CustomerEntity.class));

        customer.setLastName("Modified");
        customer.setEmail("turtle@turtle.pond");
        CustomerEntity modifiedCustomer = customerService.addOrModify(customer);
        assertNotNull(modifiedCustomer);
        assertEquals(modifiedCustomer.getFirstName(), customer.getFirstName());
        assertEquals(modifiedCustomer.getLastName(), customer.getLastName());
        assertEquals(modifiedCustomer.getEmail(), customer.getEmail());
        verify(customerRepository, atLeastOnce()).save(any(CustomerEntity.class));

    }

    @Test
    public void addOrModify_WhenInvalidChanges_ReturnsNull() {

    }

    @Test
    public void deleteById_WhenInvalidId_HasNoSideEffects() {
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(customer);

        CustomerEntity addedCustomer = customerService.addOrModify(customer);
        assertNotNull(addedCustomer);

        long customerId = addedCustomer.getId();
    }

    @Test
    public void deleteById_WhenValidId_DeletesCustomer() {

    }

}
