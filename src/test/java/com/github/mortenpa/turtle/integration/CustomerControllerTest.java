package com.github.mortenpa.turtle.integration;

import com.github.mortenpa.turtle.controller.CustomerApiResponse;
import com.github.mortenpa.turtle.data.dto.CustomerDTO;
import com.github.mortenpa.turtle.data.entity.CustomerEntity;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;


import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CustomerControllerTest {
    public static final String API_ENDPOINT = "/api/customers";

    @Autowired
    private TestRestTemplate restTemplate;

    CustomerDTO[] invalidCustomerDTOs =  new CustomerDTO[] {
            new CustomerDTO(),
            new CustomerDTO("", "Turtle", "man@turtle.sea"),
            new CustomerDTO("Man", null, "man@turtle.sea"),
            new CustomerDTO("Man", "Turtle", ""),
            new CustomerDTO("Man", "Turtle", "this.email.is.wrong"),
            new CustomerDTO("51chars-is.Too.Much...QRSTUVWXYZABCDEFGHIJKLMNOPQRO", "Turtle", "man@turtle.sea"),
            new CustomerDTO("Man", "51chars-is.Too.Much...QRSTUVWXYZABCDEFGHIJKLMNOPQRO", "man@turtle.sea"),
    };

    private CustomerEntity createCustomerWithRandomEmail() {
        String randomEmail = UUID.randomUUID().toString().substring(0, 5) + "@turtle.sea";
        return new CustomerEntity("Man", "Turtle", randomEmail);
    }

    private CustomerDTO createCustomerDTOFromCustomer(CustomerEntity customer) {
        return new CustomerDTO(customer.getFirstName(), customer.getLastName(), customer.getEmail());
    }

    private String apiEndPointWithId(Long id) {
        return API_ENDPOINT + "/" + id;
    }

    private CustomerEntity addAndCheckCustomer(CustomerEntity customer) {
        CustomerDTO customerDTO = createCustomerDTOFromCustomer(customer);
        ResponseEntity<CustomerApiResponse> response = restTemplate.postForEntity(
                API_ENDPOINT, customerDTO, CustomerApiResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        CustomerApiResponse body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());

        CustomerEntity responseCustomer = body.getCustomer();
        assertNotNull(responseCustomer);
        return responseCustomer;
    }

    private void checkResponse(
            ResponseEntity<CustomerApiResponse> response,
            CustomerEntity comparisonCustomer,
            HttpStatus expectedStatus,
            boolean expectedSuccess
    ) {
        assertNotNull(response);
        assertEquals(expectedStatus, response.getStatusCode());

        CustomerApiResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(body.isSuccess(), expectedSuccess);
        CustomerEntity fetchedCustomer = body.getCustomer();

        if (comparisonCustomer != null) {
            assertEquals(comparisonCustomer.getFirstName(), fetchedCustomer.getFirstName());
            assertEquals(comparisonCustomer.getLastName(), fetchedCustomer.getLastName());
            assertEquals(comparisonCustomer.getEmail(), fetchedCustomer.getEmail());
        }
    }

    ResponseEntity<CustomerApiResponse> putRequest(long customerId, CustomerDTO properties) {
        return restTemplate.exchange(
                apiEndPointWithId(customerId),
                HttpMethod.PUT,
                new HttpEntity<>(properties),
                CustomerApiResponse.class
        );
    }

    ResponseEntity<CustomerApiResponse> getRequest(long customerId) {
        return restTemplate.exchange(
                apiEndPointWithId(customerId),
                HttpMethod.GET,
                null,
                CustomerApiResponse.class
        );
    }

    ResponseEntity<CustomerApiResponse> deleteRequest(long customerId) {
        return restTemplate.exchange(
                apiEndPointWithId(customerId),
                HttpMethod.DELETE,
                null,
                CustomerApiResponse.class
        );
    }

    // tests start from here on out

    @Test
    public void addCustomer_WhenCustomerIsValid_ShouldAddCustomer() {
        OffsetDateTime now = OffsetDateTime.now(); //will be used later to test datetimes

        // start by creating a customer and adding it through a POST request
        CustomerEntity customer = createCustomerWithRandomEmail();
        CustomerEntity addedCustomer = addAndCheckCustomer(customer);

        // check that all provided properties match the original input
        assertEquals(customer.getFirstName(), addedCustomer.getFirstName());
        assertEquals(customer.getLastName(), addedCustomer.getLastName());
        assertEquals(customer.getEmail(), addedCustomer.getEmail());

        // test that timestamps are assigned and within a second
        assertDatetimeIsWithinSecond(now, addedCustomer.getCreatedDtime());
        assertDatetimeIsWithinSecond(now, addedCustomer.getModifiedDtime());
    }

    private static void assertDatetimeIsWithinSecond(OffsetDateTime now, OffsetDateTime target) {
        long createdTimeDifferenceInMilliseconds = Math.abs(ChronoUnit.MILLIS.between(now, target));
        assertTrue(createdTimeDifferenceInMilliseconds < 1000);
    }

    @Test
    public void addCustomer_WhenCustomerIsInvalid_ShouldFail() {

        for (CustomerDTO invalidCustomer : invalidCustomerDTOs) {
            ResponseEntity<CustomerApiResponse> response = restTemplate.postForEntity(
                    API_ENDPOINT, invalidCustomer, CustomerApiResponse.class);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            CustomerApiResponse body = response.getBody();
            assertNotNull(body);
            assertFalse(body.isSuccess());
            assertFalse(body.getMessage().isEmpty());
        }
    }

    @Test
    public void modifyCustomer_WhenCustomerIsValid_ShouldModifyCustomer() {
        CustomerEntity customer = createCustomerWithRandomEmail();
        CustomerEntity addedCustomer = addAndCheckCustomer(customer);
        long customerId = addedCustomer.getId();
        assert(customerId > 0);

        // proceed to modify the customer
        CustomerDTO modifiedProperties = new CustomerDTO("wow", "really", "even@new.email");
        ResponseEntity<CustomerApiResponse> response = putRequest(customerId, modifiedProperties);

        CustomerEntity expectedCustomer = new CustomerEntity(modifiedProperties.getFirstName(), modifiedProperties.getLastName(), modifiedProperties.getEmail());

        checkResponse(response, expectedCustomer, HttpStatus.OK, true);
    }

    @Test
    public void modifyCustomer_WhenPropertiesAreInvalid_ShouldFail() {
        CustomerEntity customer = createCustomerWithRandomEmail();
        CustomerEntity addedCustomer = addAndCheckCustomer(customer);
        long customerId = addedCustomer.getId();
        assertTrue(customerId > 0);

        // bombard the added customer with invalid changes
        for (CustomerDTO invalidCustomer : invalidCustomerDTOs) {
            ResponseEntity<CustomerApiResponse> response = putRequest(customerId, invalidCustomer);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        // make sure that the customer retains its properties
        ResponseEntity<CustomerApiResponse> response = getRequest(customerId);
        checkResponse(response, customer, HttpStatus.OK, true);
    }


    @Test
    public void getCustomer_WhenCustomerExists_ShouldReturnCustomer() {
        CustomerEntity customer = createCustomerWithRandomEmail();
        CustomerEntity addedCustomer = addAndCheckCustomer(customer);
        long customerId = addedCustomer.getId();
        assertTrue(customerId > 0);

        // make sure that the customer has its properties
        ResponseEntity<CustomerApiResponse> response = getRequest(customerId);
        checkResponse(response, customer, HttpStatus.OK, true);
    }

    @Test
    public void getCustomer_WhenCustomerDoesNotExist_ShouldReturnNull() {
        long customerId = -1;

        // query a non-existing customer
        ResponseEntity<CustomerApiResponse> response = getRequest(customerId);
        checkResponse(response, null, HttpStatus.NOT_FOUND, false);

        CustomerApiResponse body = response.getBody();
        assertNotNull(body);
        assertFalse(body.getMessage().isEmpty()); // we should still have a message about the customer not found
        assertNull(body.getCustomer());
    }

    @Test
    public void deleteCustomer_WhenCustomerIsValid_ShouldDeleteCustomer() {
        CustomerEntity customer = createCustomerWithRandomEmail();
        CustomerEntity addedCustomer = addAndCheckCustomer(customer);

        long customerId = addedCustomer.getId();
        assertTrue(customerId > 0);

        // delete and check results
        ResponseEntity<CustomerApiResponse> response = deleteRequest(customerId);
        checkResponse(response, null, HttpStatus.OK, true);
    }

    @Test
    public void deleteCustomer_WhenCustomerIsInvalid_ShouldFail() {
        long customerId = 12;

        ResponseEntity<CustomerApiResponse> response = deleteRequest(customerId);
        checkResponse(response, null, HttpStatus.NOT_FOUND, false);
    }

    /*
        This final test combines everything into one flow
        1. add multiple customers
        2. modify a customer
        3. check modifications with get
        4. delete a customer
     */
    @Test
    public void addModifyGetDelete_WhenCustomerIsValid_ShouldDoEverything() {
        // 1. add multiple customers
        CustomerEntity[] customers = new CustomerEntity[] {
                createCustomerWithRandomEmail(),
                createCustomerWithRandomEmail(),
                createCustomerWithRandomEmail()
        };

        ArrayList<CustomerEntity> addedCustomers = new ArrayList<>();

        for (CustomerEntity customer : customers) {
            CustomerEntity addedCustomer = addAndCheckCustomer(customer);
            addedCustomers.add(addedCustomer);
        }

        // 2. let's modify the second customer
        long customerId = addedCustomers.get(1).getId();
        System.out.println(customerId);
        CustomerDTO customerDTO = new CustomerDTO("brand", "new", "customer@and.email");

        ResponseEntity<CustomerApiResponse> putResponse = putRequest(customerId, customerDTO);
        CustomerEntity expectedCustomer = new CustomerEntity(customerDTO.getFirstName(), customerDTO.getLastName(), customerDTO.getEmail());
        checkResponse(putResponse, expectedCustomer, HttpStatus.OK, true);

        // 3. let's check the modified customer and the other two customers as well with get
        ResponseEntity<CustomerApiResponse> getResponse = getRequest(customerId);
        checkResponse(getResponse, expectedCustomer, HttpStatus.OK, true);

        getResponse = getRequest(addedCustomers.get(0).getId());
        checkResponse(getResponse, addedCustomers.get(0), HttpStatus.OK, true);

        getResponse = getRequest(addedCustomers.get(2).getId());
        checkResponse(getResponse, addedCustomers.get(2), HttpStatus.OK, true);

        // 4. finally let's delete the second customer and check everything again
        ResponseEntity<CustomerApiResponse> deleteResponse = deleteRequest(customerId);
        checkResponse(deleteResponse, null, HttpStatus.OK, true);

        // let's try the same deletion again
        deleteResponse = deleteRequest(customerId);
        checkResponse(deleteResponse, null, HttpStatus.NOT_FOUND, false);

        // we expect the other 2 customers to be unaffected
        getResponse = getRequest(addedCustomers.get(0).getId());
        checkResponse(getResponse, addedCustomers.get(0), HttpStatus.OK, true);

        getResponse = getRequest(addedCustomers.get(2).getId());
        checkResponse(getResponse, addedCustomers.get(2), HttpStatus.OK, true);
    }

}
