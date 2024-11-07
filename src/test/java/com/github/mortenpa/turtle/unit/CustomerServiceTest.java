package com.github.mortenpa.turtle.unit;


import com.github.mortenpa.turtle.data.entity.CustomerEntity;
import com.github.mortenpa.turtle.error.DuplicateEmailException;
import com.github.mortenpa.turtle.error.NullNotAllowedException;
import com.github.mortenpa.turtle.repository.CustomerRepository;
import com.github.mortenpa.turtle.service.CustomerService;
import jakarta.validation.*;
import net.bytebuddy.implementation.bytecode.Throw;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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

    // helper function to mock duplicate email validation
    private void checkForExistingEmail(CustomerEntity customer) {
        Optional<CustomerEntity> customerWithEmailExists = mockDatabaseEntries.stream()
                .filter(existingCustomer -> existingCustomer.isPresent() &&
                        existingCustomer.get().getId() != customer.getId() && // exclude the same customer themselves
                        existingCustomer.get().getEmail().equals(customer.getEmail()))
                .map(Optional::get)
                .findFirst();

        if (customerWithEmailExists.isPresent()) {
            CustomerEntity existingCustomer = customerWithEmailExists.get();
            // verbose message for debugging failing tests
            String message = String.format(
                    "Customer (%s, %s) Email %s is duplicated by (%d, %s, %s)",
                    customer.getFirstName(), customer.getLastName(), customer.getEmail(),
                    existingCustomer.getId(), existingCustomer.getFirstName(), existingCustomer.getLastName());
            throw new DuplicateEmailException(message);
        }
    }

    private void checkForNulls(CustomerEntity customer) {
        if (customer.getFirstName() == null) {
            throw new NullNotAllowedException("Firstname can't be null");
        } else if (customer.getLastName() == null) {
            throw new NullNotAllowedException("Lastname can't be null");
        } else if (customer.getEmail() == null) {
            throw new NullNotAllowedException("Email can't be null");
        }

    }

    @BeforeEach
    public void setUp() {
        // wrap validation set up in a try block due to AutoCloseable
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
        mocks = MockitoAnnotations.openMocks(this);
        // empty the array mocking the database
        mockDatabaseEntries = new ArrayList<>();

        // basically mocking the whole repository behaviour with an array
        mockSavingToRepository();
        mockGetFromRepository();
        mockDeletingFromRepository();
        mockExistsInRepository();
    }

    private void mockExistsInRepository() {
        when(customerRepository.existsById(anyLong())).thenAnswer(invocationOnMock -> {
            long customerId = (long) invocationOnMock.getArgument(0) - 1;
            if (customerId < 0 || mockDatabaseEntries.size() <= customerId) {
                return false;
            }
            return true;
        });
    }

    private void mockDeletingFromRepository() {
        // mock deleting from repository by setting the array element at the index to null
        doAnswer(invocationOnMock -> {
            long customerId = (long) invocationOnMock.getArgument(0) - 1;
            // only try to modify the array if the index can be used
            if (customerId >= 0 && customerId < mockDatabaseEntries.size()) {
                mockDatabaseEntries.set((int) customerId, Optional.empty());
            }

            return null;
        }).when(customerRepository).deleteById(anyLong());
    }

    private void mockGetFromRepository() {
        // mock finding from repository via the array index
        when(customerRepository.findById(anyLong())).thenAnswer(invocationOnMock -> {
            long customerId = (long) invocationOnMock.getArgument(0) - 1;
            if (customerId < 0 || mockDatabaseEntries.size() <= customerId) {
                return Optional.empty();
            }
            return mockDatabaseEntries.get((int) customerId);
        });
    }

    private void mockSavingToRepository() {
        // mock saving to repository by returning the same element with a modified ID
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> {
            CustomerEntity newOrUpdatedCustomer = invocation.getArgument(0);
            Set<ConstraintViolation<CustomerEntity>> customerValidationErrors = validator.validate(newOrUpdatedCustomer);

            if (!customerValidationErrors.isEmpty()) {
                throw new ConstraintViolationException(customerValidationErrors);
            }

            checkForNulls(newOrUpdatedCustomer);

            // for both new and updated customers we need to check the email for duplication
            checkForExistingEmail(newOrUpdatedCustomer);

            OffsetDateTime now = OffsetDateTime.now();
            // since we're using 1-based indexes, subtract 1 for the array index
            long customerId = newOrUpdatedCustomer.getId() - 1;
            // new customer entry, try to add to DB
            if (mockDatabaseEntries.size() >= customerId) {
                mockDatabaseEntries.add(Optional.of(newOrUpdatedCustomer));
                // use 1-based indexes like the database would
                newOrUpdatedCustomer.setId(mockDatabaseEntries.size());
                newOrUpdatedCustomer.setCreatedDtime(now);
                newOrUpdatedCustomer.setModifiedDtime(now);
            }
            // existing customer entry, modify DB entry
            else {
                mockDatabaseEntries.set((int) customerId, Optional.of(newOrUpdatedCustomer));
                newOrUpdatedCustomer.setModifiedDtime(now);
            }

            return newOrUpdatedCustomer;
        });
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

    // adding these customers through the service is expected to throw an error
    private <T extends Throwable> void testCustomersThatThrowException(CustomerEntity[] customers, Class<T> expectedException) {
        for (CustomerEntity customer : customers) {
            assertThrows(expectedException, () -> customerService.addOrModify(customer));
        }
    }

    /*
    ----------------------------------------TESTS START HERE---------------------------------------------------------
     */

    @Test
    public void addOrModify_WhenInsertingValidCustomer_ShouldReturnCustomer() {
        // Test creating and adding a valid customer
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

    @Test
    public void addOrModify_WhenInsertingInvalidCustomer_ShouldThrowException() {
        // begin by testing with empty elements
        CustomerEntity[] customersWithEmptyProperties = {
                new CustomerEntity("", "Turtle", "man@turtle.sea"),
                new CustomerEntity("Man", "", "man@turtle.sea"),
                new CustomerEntity("Man", "Turtle", ""),
        };

        testCustomersThatThrowException(customersWithEmptyProperties, ConstraintViolationException.class);

        CustomerEntity[] customersWithNullProperties = {
                new CustomerEntity(),
                new CustomerEntity(null, null, "manNull@turtle.sea"),
                new CustomerEntity("Man", null, "manNull@turtle.sea"),
                new CustomerEntity("Man", "Turtle", null)
        };

        testCustomersThatThrowException(customersWithNullProperties, NullNotAllowedException.class);

        // these are just too long
        CustomerEntity[] customersWithTooLongProperties = {
                new CustomerEntity("51chars-CDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRO", "Turtle", "man@turtle.sea"),
                new CustomerEntity("Man", "51chars-CDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRO", "man@turtle.sea"),
                new CustomerEntity("Man", "Turtle", "at.278.characters.this.is.an.illegally.long.email.address.that.is.designed.to.test.the.maximum.length.limit.of.an.email.address.in.a.validation.test.that.should.still.pass@yeah.we.are.still.going.and.we.are.not.stopping.before.we.hit254.characters.in.this.god.forsaken.email.com"),

        };

        testCustomersThatThrowException(customersWithTooLongProperties, ConstraintViolationException.class);

        // wrap up with some fun illegal emails
        CustomerEntity[] customersWithIllegalEmails = {
                new CustomerEntity("Man", "Turtle", "turtle.man.com"),
                new CustomerEntity("Man", "Turtle", "turtleman.com"),
                new CustomerEntity("Man", "Turtle", "turtle.man.com"),
                new CustomerEntity("Man", "Turtle", "@turtles.com"),
                new CustomerEntity("Man", "Turtle", "turtle@@man.com"),
                new CustomerEntity("Man", "Turtle", "tur@tle@man.com"),
                new CustomerEntity("Man", "Turtle", "turtle@man...com"),
                new CustomerEntity("Man", "Turtle", "turtle@man.com."),
                new CustomerEntity("Man", "Turtle", "turtle@man...com"),
                new CustomerEntity("Man", "Turtle",
                        "emailLocalPartCantExceed64CharactersSoThisIsIllegalBecause65Chars@turtle.com"),
                // missing domain (.com) is an edge case that's recognized as valid by the Jakarta validation. Emails are weird.
                // new CustomerEntity("Man", "Turtle", "turt.le@man"),
        };

        testCustomersThatThrowException(customersWithIllegalEmails, ConstraintViolationException.class);
    }

    @Test
    public void addOrModify_WhenInsertingDuplicateEmail_ThrowsDuplicateEmail() {
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");

        CustomerEntity addedCustomer = customerService.addOrModify(customer);
        assertNotNull(addedCustomer);

        CustomerEntity duplicatedCustomer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");

        // trying to add a customer with the same email should throw an error
        assertThrows(DuplicateEmailException.class, () -> customerService.addOrModify(duplicatedCustomer));
    }


    @Test
    public void getById_WhenExistingCustomer_ShouldReturnCustomer() {
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");
        CustomerEntity savedCustomer = customerService.addOrModify(customer);

        Optional<CustomerEntity> retrievedCustomer = customerService.getById(savedCustomer.getId());
        assertTrue(retrievedCustomer.isPresent());
        CustomerEntity fetchedCustomer = retrievedCustomer.get();

        assertCustomerInfoMatches(fetchedCustomer, customer);

        assertNotNull(fetchedCustomer.getCreatedDtime());
        assertNotNull(fetchedCustomer.getModifiedDtime());

        // test that timestamps are assigned and within a second
        OffsetDateTime now = OffsetDateTime.now();
        long createdTimeDifferenceInMilliseconds = Math.abs(ChronoUnit.MILLIS.between(now, fetchedCustomer.getCreatedDtime()));
        assertTrue(createdTimeDifferenceInMilliseconds < 1000);

        long modifiedTimeDifferenceInMilliseconds = Math.abs(ChronoUnit.MILLIS.between(now, fetchedCustomer.getModifiedDtime()));
        assertTrue(modifiedTimeDifferenceInMilliseconds < 1000);
    }

    @Test
    public void getById_WhenMissingCustomer_ShouldReturnNothing() {
        Optional<CustomerEntity> missingCustomer = customerService.getById(-1);
        assertTrue(missingCustomer.isEmpty());
    }

    @Test
    public void addOrModify_WhenValidChanges_ShouldModifyCustomer() {
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");

        CustomerEntity addedCustomer = customerService.addOrModify(customer);
        assertNotNull(addedCustomer);
        assertEquals(addedCustomer.getLastName(), customer.getLastName());
        verify(customerRepository, atLeastOnce()).save(any(CustomerEntity.class));

        customer.setLastName("Modified");
        customer.setEmail("turtle@turtle.pond");
        CustomerEntity modifiedCustomer = customerService.addOrModify(customer);
        assertCustomerInfoMatches(modifiedCustomer, customer);
        verify(customerRepository, times(2)).save(any(CustomerEntity.class));

        customer.setFirstName("Also Modified");
        modifiedCustomer = customerService.addOrModify(customer);
        assertCustomerInfoMatches(modifiedCustomer, customer);
        verify(customerRepository, times(3)).save(any(CustomerEntity.class));
    }

    private static void assertCustomerInfoMatches(CustomerEntity modifiedCustomer, CustomerEntity customer) {
        assertNotNull(modifiedCustomer);
        assertEquals(modifiedCustomer.getFirstName(), customer.getFirstName());
        assertEquals(modifiedCustomer.getLastName(), customer.getLastName());
        assertEquals(modifiedCustomer.getEmail(), customer.getEmail());
    }

    @Test
    public void addOrModify_WhenInvalidChanges_ShouldThrowException() {
        // start by adding a valid customer entry
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");

        CustomerEntity addedCustomer = customerService.addOrModify(customer);
        assertNotNull(addedCustomer);
        assertEquals(addedCustomer.getLastName(), customer.getLastName());

        // then mess up the entry
        customer.setLastName("");
        CustomerEntity[] customerArr = {customer};
        testCustomersThatThrowException(customerArr, ConstraintViolationException.class);
        customer.setLastName("TurtleAgain");

        customer.setFirstName(null);
        assertThrows(NullNotAllowedException.class, () -> customerService.addOrModify(customer));
        customer.setFirstName("ManAgain");

        customer.setEmail("emailLocalPartCantExceed64CharactersSoThisIsIllegalBecause65Chars@turtle.com");
        testCustomersThatThrowException(customerArr, ConstraintViolationException.class);
        customer.setEmail("turtle@man.again");

        // finally check that modifying the customer did not create any side effects and
        // that we can add the valid customer again
        addedCustomer = customerService.addOrModify(customer);
        assertNotNull(addedCustomer);
        assertEquals(addedCustomer.getLastName(), customer.getLastName());
    }

    @Test
    public void deleteById_WhenValidId_ShouldDeleteCustomer() {
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
        long customerId = customer.getId();
        Optional<CustomerEntity> fetchedCustomer = customerService.getById(customerId);
        assertTrue(fetchedCustomer.isPresent());
        assertEquals(fetchedCustomer.get().getFirstName(), customer.getFirstName());

        // delete the customer and check again
        customerService.delete(customerId);
        fetchedCustomer = customerService.getById(customerId);
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
    public void deleteById_WhenInvalidId_ShouldHaveNoSideEffects() {
        CustomerEntity customer = new CustomerEntity("Man", "Turtle", "man@turtle.sea");

        CustomerEntity addedCustomer = customerService.addOrModify(customer);
        assertNotNull(addedCustomer);

        customerService.delete(-1);
        customerService.delete(addedCustomer.getId() + 1);
        verify(customerRepository, times(0)).deleteById(any(Long.class));

        Optional<CustomerEntity> fetchedCustomer = customerService.getById(customer.getId());
        assertTrue(fetchedCustomer.isPresent());
        assertEquals(fetchedCustomer.get().getFirstName(), customer.getFirstName());
    }

}
