package org.vaadin.bakery.jpaservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.bakery.jpamodel.code.OrderStatusCode;
import org.vaadin.bakery.jpaclient.repository.CustomerRepository;
import org.vaadin.bakery.jpaclient.repository.OrderRepository;
import org.vaadin.bakery.jpaservice.mapper.CustomerMapper;
import org.vaadin.bakery.service.CustomerService;
import org.vaadin.bakery.uimodel.data.CustomerSummary;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of the customer service.
 */
@Service
@Transactional
public class JpaCustomerService implements CustomerService {

    private static final List<OrderStatusCode> IN_PROGRESS_STATUSES = List.of(
            OrderStatusCode.IN_PROGRESS,
            OrderStatusCode.BAKED,
            OrderStatusCode.PACKAGED,
            OrderStatusCode.READY_FOR_PICK_UP
    );

    private static final List<OrderStatusCode> PRE_PRODUCTION_STATUSES = List.of(
            OrderStatusCode.NEW,
            OrderStatusCode.VERIFIED,
            OrderStatusCode.NOT_OK
    );

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final CustomerMapper customerMapper;

    public JpaCustomerService(CustomerRepository customerRepository, OrderRepository orderRepository,
                              CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerSummary> search(String query) {
        if (query == null || query.isBlank()) {
            return customerMapper.toSummaryList(customerRepository.findByActiveTrueOrderByName());
        }
        return customerMapper.toSummaryList(
                customerRepository.findByNameContainingIgnoreCaseAndActiveTrueOrderByName(query, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerSummary> getByPhoneNumber(String phoneNumber) {
        return customerRepository.findByPhoneNumberAndActiveTrue(phoneNumber)
                .map(customerMapper::toSummary);
    }

    @Override
    public CustomerSummary create(CustomerSummary customer) {
        var entity = customerMapper.toNewEntity(customer);
        var saved = customerRepository.save(entity);
        return customerMapper.toSummary(saved);
    }

    @Override
    public CustomerSummary update(Long id, CustomerSummary customer) {
        var entity = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        customerMapper.toEntity(customer, entity);
        return customerMapper.toSummary(entity);
    }

    @Override
    public void delete(Long id) {
        var result = canDelete(id);
        if (!result.canDelete()) {
            throw new IllegalStateException(result.blockingReason());
        }

        // Cancel pre-production orders
        var preProductionOrders = orderRepository.findByCustomerIdAndStatusIn(id, PRE_PRODUCTION_STATUSES);
        for (var order : preProductionOrders) {
            order.setStatus(OrderStatusCode.CANCELLED);
        }

        // Soft delete customer
        var customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        customer.setActive(false);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDeletionResult canDelete(Long id) {
        if (orderRepository.existsByCustomerIdAndStatusIn(id, IN_PROGRESS_STATUSES)) {
            var count = orderRepository.findByCustomerIdAndStatusIn(id, IN_PROGRESS_STATUSES).size();
            return CustomerDeletionResult.blocked(count,
                    "Cannot delete customer with " + count + " in-progress order(s)");
        }

        var preProductionOrders = orderRepository.findByCustomerIdAndStatusIn(id, PRE_PRODUCTION_STATUSES);
        return CustomerDeletionResult.canDelete(preProductionOrders.size());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean phoneNumberExists(String phoneNumber) {
        return customerRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean phoneNumberExistsForOtherCustomer(String phoneNumber, Long customerId) {
        return customerRepository.existsByPhoneNumberAndIdNot(phoneNumber, customerId);
    }
}
