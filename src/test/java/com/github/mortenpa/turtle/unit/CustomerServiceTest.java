package com.github.mortenpa.turtle.unit;


import com.github.mortenpa.turtle.data.entity.CustomerEntity;
import com.github.mortenpa.turtle.repository.CustomerRepository;
import com.github.mortenpa.turtle.service.CustomerService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class CustomerServiceTest {
    @InjectMocks
    private CustomerService customerService;

    @Mock
    private CustomerRepository customerRepository;

    private Validator validator;
    private AutoCloseable mocks;

    // use a simple collection to mock the database, including indexes as ids
    private List<Optional<CustomerEntity>> mockDatabaseEntries;


    @BeforeEach
    public void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        mocks = MockitoAnnotations.openMocks(this);
        mockDatabaseEntries = new ArrayList<>();

        // mock saving to repository by returning the same element with a modified ID
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> {
            CustomerEntity customer = invocation.getArgument(0);
            Set<ConstraintViolation<CustomerEntity>> customerValidationErrors = validator.validate(customer);

            if (!customerValidationErrors.isEmpty()) {
                throw new ConstraintViolationException(customerValidationErrors);
            }

            long customerId = customer.getId();
            if (mockDatabaseEntries.size() >= customerId) {
                mockDatabaseEntries.add(Optional.of(customer));
                customer.setId(mockDatabaseEntries.size() - 1);
            }
            else {
                mockDatabaseEntries.set((int) customerId, Optional.of(customer));
            }

            return customer;
        });

        // mock finding from repository via the array index
        when(customerRepository.findById(anyLong())).thenAnswer(invocationOnMock -> {
            long customerId = invocationOnMock.getArgument(0);
            if (mockDatabaseEntries.size() <= customerId) {
                return Optional.empty();
            }
            return mockDatabaseEntries.get((int) customerId);
        });

        // mock deleting from repository by setting the array element at the index to null
        doAnswer(invocationOnMock -> {
            long customerId = invocationOnMock.getArgument(0);
            // only try to modify the array if the index can be used
            if (customerId >= 0 && customerId < mockDatabaseEntries.size()) {
                mockDatabaseEntries.set((int) customerId, Optional.empty());
            }

            return null;
        }).when(customerRepository).deleteById(anyLong());
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mocks != null) {
            try {
                mocks.close();
            } catch (Exception e) {
                System.out.println("Error closing mocks: " + e);
            }
        }
    }

    @Test
    public void addOrModify_WhenInsertingValidCustomer_ReturnsCustomer() {
        // Test a simple customer
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");
        CustomerEntity addedCustomer = customerService.addOrModify(customer);

        assertNotNull(addedCustomer);
        assertEquals(addedCustomer.getFirstName(), customer.getFirstName());
        verify(customerRepository, atLeastOnce()).save(any(CustomerEntity.class));

        // Test with some more nuanced (but valid) inputs
        CustomerEntity[] specialCustomers = {
                new CustomerEntity("李", "小龍", "人@亀.海"),
                new CustomerEntity("Zoë", "O'Connor", "zoe@connor.ie"),
                new CustomerEntity("Maximiliano", "de la Cruz", "man@亀.sea"),
                new CustomerEntity("اَلْبَغْدَادِي", "كريم", "man@亀.sea"),
                new CustomerEntity("\uD83D\uDE0A", "Jones", "local@man.test"),
                new CustomerEntity("Иван", "Петрович", "ivan@письма.рф"),
                new CustomerEntity("50chars-CDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQR", "50chars-CDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQR", "its.is.a.very.long.email.address.that.is.designed.to.test.the.maximum.length.limit.of.an.email.address.in.a.validation.test.that.should.still.pass@yeah.we.are.still.going.and.we.are.not.stopping.before.we.hit254.characters.in.this.god.forsaken.email.com"),
        };

        for (CustomerEntity specialCustomer : specialCustomers) {
            CustomerEntity specialAddedCustomer = customerService.addOrModify(customer);

            assertNotNull(specialAddedCustomer);
            assertEquals(specialAddedCustomer.getFirstName(), customer.getFirstName());
        }
    }

    private void testCustomersThatThrowConstraintViolation(CustomerEntity[] customers) {
        for (CustomerEntity customer : customers) {
            System.out.println(customer.toString());
            assertThrows(ConstraintViolationException.class, () -> customerService.addOrModify(customer));
        }
    }

    @Test
    public void addOrModify_WhenInsertingInvalidCustomer_ReturnsNull() {
        // begin by testing with empty elements
        CustomerEntity[] customersWithMissingProperties = {
                new CustomerEntity(),
                new CustomerEntity("", "Turtle", "man@turtle.sea"),
                new CustomerEntity("Man", null, "man@turtle.sea"),
                new CustomerEntity("Man", "Turtle", ""),
                new CustomerEntity("Man", "Turtle", null)
        };

        testCustomersThatThrowConstraintViolation(customersWithMissingProperties);

        // these are just too long
        CustomerEntity[] customersWithTooLongProperties = {
                new CustomerEntity("51chars-CDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRO", "Turtle", "man@turtle.sea"),
                new CustomerEntity("Man", "51chars-CDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRO", "man@turtle.sea"),
                new CustomerEntity("Man", "Turtle", "at.278.characters.this.is.an.illegally.long.email.address.that.is.designed.to.test.the.maximum.length.limit.of.an.email.address.in.a.validation.test.that.should.still.pass@yeah.we.are.still.going.and.we.are.not.stopping.before.we.hit254.characters.in.this.god.forsaken.email.com"),

        };

        testCustomersThatThrowConstraintViolation(customersWithTooLongProperties);

        // wrap up with some fun illegal emails
        CustomerEntity[] CustomersWithIllegalEmails = {
                new CustomerEntity("Man", "Turtle", "turtle.man.com"),
                new CustomerEntity("Man", "Turtle", "turtleman.com"),
                new CustomerEntity("Man", "Turtle", "turtle.man.com"),
                new CustomerEntity("Man", "Turtle", "@turtles.com"),
                new CustomerEntity("Man", "Turtle", "turtle@@man.com"),
                new CustomerEntity("Man", "Turtle", "tur@tle@man.com"),
                new CustomerEntity("Man", "Turtle", "turtle@man...com"),
                new CustomerEntity("Man", "Turtle", "turtle@man.com."),
                new CustomerEntity("Man", "Turtle", "turtle@man...com"),
                new CustomerEntity("Man", "Turtle", "emailLocalPartCantExceed64CharactersSoThisIsIllegalBecause65Chars@turtle.com"),
                // TODO: this is an edge case that's recognized as valid by the Jakarta validation
                // new CustomerEntity("Man", "Turtle", "turt.le@man"),
        };

        testCustomersThatThrowConstraintViolation(CustomersWithIllegalEmails);
    }

    @Test
    public void getById_WhenExistingCustomer_ReturnsCustomer() {
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");
        customerService.addOrModify(customer);
        CustomerEntity savedCustomer = customerService.addOrModify(customer);

        Optional<CustomerEntity> retrievedCustomer = customerService.getById(savedCustomer.getId());
        assertTrue(retrievedCustomer.isPresent());
        assertEquals(retrievedCustomer.get().getFirstName(), customer.getFirstName());
        assertEquals(retrievedCustomer.get().getLastName(), customer.getLastName());
        assertEquals(retrievedCustomer.get().getEmail(), customer.getEmail());

    }

    @Test
    public void getById_WhenMissingCustomer_ReturnsNull() {
        Optional<CustomerEntity> missingCustomer = customerService.getById(1);
        assertTrue(missingCustomer.isEmpty());
    }

    @Test
    public void addOrModify_WhenValidChanges_ReturnsCustomer() {
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");

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
        verify(customerRepository, times(2)).save(any(CustomerEntity.class));

        customer.setFirstName("Also Modified");
        modifiedCustomer = customerService.addOrModify(customer);
        assertNotNull(modifiedCustomer);
        assertEquals(modifiedCustomer.getFirstName(), customer.getFirstName());
        assertEquals(modifiedCustomer.getLastName(), customer.getLastName());
        assertEquals(modifiedCustomer.getEmail(), customer.getEmail());
        verify(customerRepository, times(3)).save(any(CustomerEntity.class));
    }

    @Test
    public void addOrModify_WhenInvalidChanges_ReturnsNull() {
        // start by adding a valid customer entry
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");

        CustomerEntity addedCustomer = customerService.addOrModify(customer);
        assertNotNull(addedCustomer);
        assertEquals(addedCustomer.getLastName(), customer.getLastName());

        // then mess up the entry
        customer.setLastName("");
        CustomerEntity[] customerArr = {customer};
        testCustomersThatThrowConstraintViolation(customerArr);
        customer.setLastName("TurtleAgain");

        customer.setFirstName(null);
        testCustomersThatThrowConstraintViolation(customerArr);
        customer.setFirstName("ManAgain");

        customer.setEmail("emailLocalPartCantExceed64CharactersSoThisIsIllegalBecause65Chars@turtle.com");
        testCustomersThatThrowConstraintViolation(customerArr);
        customer.setEmail("turtle@man.again");

        // finally check that modifying the customer did not create any side effects and
        // that we can add the valid customer again
        addedCustomer = customerService.addOrModify(customer);
        assertNotNull(addedCustomer);
        assertEquals(addedCustomer.getLastName(), customer.getLastName());
    }

    @Test
    public void deleteById_WhenValidId_DeletesCustomer() {
        // start by adding some customers
        CustomerEntity[] customers = {
                new CustomerEntity("Man", "Turtle", "man@turtle.sea"),
                new CustomerEntity("Nam", "Turtle", "man2@turtle.sea"),
                new CustomerEntity("Amn", "Turtle", "man3@turtle.sea")
        };
        for (CustomerEntity customer : customers) {
            customerService.addOrModify(customer);
        }

        // check that customer 2 is present
        CustomerEntity customer = customers[1];
        Optional<CustomerEntity> fetchedCustomer = customerService.getById(customer.getId());
        assertTrue(fetchedCustomer.isPresent());
        assertEquals(fetchedCustomer.get().getFirstName(), customer.getFirstName());

        // delete the customer and check again
        customerService.delete(customer.getId());
        fetchedCustomer = customerService.getById(customer.getId());
        assertTrue(fetchedCustomer.isEmpty());

        // check other customers for no side effects
        customer = customers[0];
        fetchedCustomer = customerService.getById(customer.getId());
        assertTrue(fetchedCustomer.isPresent());
        assertEquals(fetchedCustomer.get().getFirstName(), customer.getFirstName());

        customer = customers[2];
        fetchedCustomer = customerService.getById(customer.getId());
        assertTrue(fetchedCustomer.isPresent());
        assertEquals(fetchedCustomer.get().getFirstName(), customer.getFirstName());
    }

    @Test
    public void deleteById_WhenInvalidId_HasNoSideEffects() {
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");

        CustomerEntity addedCustomer = customerService.addOrModify(customer);
        assertNotNull(addedCustomer);

        customerService.delete(-1);
        customerService.delete(addedCustomer.getId() + 1);
        verify(customerRepository, atLeastOnce()).deleteById(any(Long.class));

        Optional<CustomerEntity> fetchedCustomer = customerService.getById(customer.getId());
        assertTrue(fetchedCustomer.isPresent());
        assertEquals(fetchedCustomer.get().getFirstName(), customer.getFirstName());
    }

}
