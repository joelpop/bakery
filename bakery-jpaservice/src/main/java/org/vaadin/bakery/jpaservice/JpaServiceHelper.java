package org.vaadin.bakery.jpaservice;

import jakarta.persistence.OptimisticLockException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.vaadin.bakery.service.StaleDataException;

/**
 * Shared helper for JPA service operations.
 */
public final class JpaServiceHelper {

    private JpaServiceHelper() {}

    /**
     * Flushes the repository, converting optimistic lock exceptions to StaleDataException.
     */
    public static void flushOrThrowStale(JpaRepository<?, ?> repository,
                                          String entityType, Long id) {
        try {
            repository.flush();
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException _) {
            throw new StaleDataException(entityType, id);
        }
    }
}
