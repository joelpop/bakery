package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.CustomerSummary;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for customer management operations.
 */
public interface CustomerService {

    /**
     * Searches for customers matching the given query string.
     */
    List<CustomerSummary> search(String query);

    /**
     * Search customers by partial phone number match.
     * Non-digit characters are ignored in the comparison.
     *
     * @param phoneDigits partial phone number (digits only)
     * @return matching customers
     */
    List<CustomerSummary> searchByPhone(String phoneDigits);

    /**
     * Returns the customer with the given exact phone number, if found.
     */
    Optional<CustomerSummary> getByPhoneNumber(String phoneNumber);

    /**
     * Creates a new customer and returns the saved result.
     */
    CustomerSummary create(CustomerSummary customer);

    /**
     * Updates an existing customer identified by the given ID.
     */
    CustomerSummary update(Long id, CustomerSummary customer);

    /**
     * Soft delete a customer (marks as inactive).
     * Blocks if customer has in-progress orders.
     * Cancels pre-production orders.
     *
     * @param id customer ID
     * @throws IllegalStateException if customer has in-progress orders
     */
    void delete(Long id);

    /**
     * Checks if a customer can be deleted.
     *
     * @param id customer ID
     * @return deletion eligibility result with affected order information
     */
    CustomerDeletionResult canDelete(Long id);

    /**
     * Checks whether the given phone number is already in use by any customer.
     */
    boolean phoneNumberExists(String phoneNumber);

    /**
     * Checks whether the given phone number is in use by a customer other than the specified one.
     */
    boolean phoneNumberExistsForOtherCustomer(String phoneNumber, Long customerId);

    /**
     * Result of checking if a customer can be deleted.
     */
    record CustomerDeletionResult(
            boolean canDelete,
            int inProgressOrderCount,
            int preProductionOrderCount,
            String blockingReason
    ) {
        public static CustomerDeletionResult canDelete(int preProductionOrderCount) {
            return new CustomerDeletionResult(true, 0, preProductionOrderCount, null);
        }

        public static CustomerDeletionResult blocked(int inProgressOrderCount, String reason) {
            return new CustomerDeletionResult(false, inProgressOrderCount, 0, reason);
        }
    }
}
