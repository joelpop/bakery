package org.vaadin.bakery.jpaservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.bakery.jpaclient.repository.LocationRepository;
import org.vaadin.bakery.jpaservice.mapper.LocationMapper;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.uimodel.data.LocationSummary;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of the location service.
 */
@Service
@Transactional
public class JpaLocationService implements LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    public JpaLocationService(LocationRepository locationRepository, LocationMapper locationMapper) {
        this.locationRepository = locationRepository;
        this.locationMapper = locationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationSummary> list() {
        return locationMapper.toSummaryList(locationRepository.findAllProjectedByOrderBySortOrderAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationSummary> listActive() {
        return locationMapper.toSummaryList(locationRepository.findByActiveTrueOrderBySortOrderAsc(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocationSummary> get(Long id) {
        return locationRepository.findById(id).map(locationMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocationSummary> getByCode(String code) {
        return locationRepository.findByCode(code).map(locationMapper::toSummary);
    }

    @Override
    public LocationSummary create(LocationSummary location) {
        var entity = locationMapper.toNewEntity(location);
        var saved = locationRepository.save(entity);
        return locationMapper.toSummary(saved);
    }

    @Override
    public LocationSummary update(Long id, LocationSummary location) {
        var entity = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
        locationMapper.toEntity(location, entity);
        return locationMapper.toSummary(entity);
    }

    @Override
    public void delete(Long id) {
        locationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean codeExists(String code) {
        return locationRepository.existsByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean codeExistsForOtherLocation(String code, Long locationId) {
        return locationRepository.existsByCodeAndIdNot(code, locationId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean nameExists(String name) {
        return locationRepository.existsByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean nameExistsForOtherLocation(String name, Long locationId) {
        return locationRepository.existsByNameAndIdNot(name, locationId);
    }
}
