package org.apereo.cas.adaptors.ldap.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.configuration.model.support.ldap.serviceregistry.LdapServiceRegistryProperties;
import org.apereo.cas.services.AbstractServiceRegistry;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.support.events.service.CasRegisteredServiceLoadedEvent;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.LdapUtils;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.ToString;

/**
 * Implementation of the ServiceRegistry interface which stores the services in a LDAP Directory.
 *
 * @author Misagh Moayyed
 * @author Marvin S. Addison
 * @since 4.0.0
 */
@Slf4j
@ToString
public class LdapServiceRegistry extends AbstractServiceRegistry {

    private final ConnectionFactory connectionFactory;

    private final LdapRegisteredServiceMapper ldapServiceMapper;

    private final String baseDn;

    private final String searchFilter;

    private final String loadFilter;

    public LdapServiceRegistry(final ConnectionFactory connectionFactory, final String baseDn,
                               final LdapRegisteredServiceMapper ldapServiceMapper, final LdapServiceRegistryProperties ldapProperties) {
        this.connectionFactory = connectionFactory;
        this.baseDn = baseDn;
        if (ldapServiceMapper == null) {
            this.ldapServiceMapper = new DefaultLdapRegisteredServiceMapper(ldapProperties);
        } else {
            this.ldapServiceMapper = ldapServiceMapper;
        }
        this.loadFilter = ldapProperties.getLoadFilter();
        this.searchFilter = ldapProperties.getSearchFilter();
        LOGGER.debug("Configured search filter to [{}] and load filter to [{}]", this.searchFilter, this.loadFilter);
    }

    @Override
    public RegisteredService save(final RegisteredService rs) {
        try {
            if (rs.getId() != RegisteredService.INITIAL_IDENTIFIER_VALUE) {
                return update(rs);
            }
            insert(rs);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return rs;
    }

    private RegisteredService insert(final RegisteredService rs) {
        try {
            final var entry = this.ldapServiceMapper.mapFromRegisteredService(this.baseDn, rs);
            LdapUtils.executeAddOperation(this.connectionFactory, entry);
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return rs;
    }

    /**
     * Update the ldap entry with the given registered service.
     *
     * @param rs the rs
     * @return the registered service
     */
    private RegisteredService update(final RegisteredService rs) {
        String currentDn = null;
        try {
            final var response = searchForServiceById(rs.getId());
            if (LdapUtils.containsResultEntry(response)) {
                currentDn = response.getResult().getEntry().getDn();
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        if (StringUtils.isNotBlank(currentDn)) {
            LOGGER.debug("Updating registered service at [{}]", currentDn);
            final var entry = this.ldapServiceMapper.mapFromRegisteredService(this.baseDn, rs);
            LdapUtils.executeModifyOperation(currentDn, this.connectionFactory, entry);
        } else {
            LOGGER.debug("Failed to locate DN for registered service by id [{}]. Attempting to save the service anew", rs.getId());
            insert(rs);
        }
        return rs;
    }

    @Override
    public boolean delete(final RegisteredService registeredService) {
        try {
            final var response = searchForServiceById(registeredService.getId());
            if (LdapUtils.containsResultEntry(response)) {
                final var entry = response.getResult().getEntry();
                return LdapUtils.executeDeleteOperation(this.connectionFactory, entry);
            }
        } catch (final LdapException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * This may be an expensive operation.
     * In order to count the number of available definitions in LDAP,
     * this call will attempt to execute a search query to load services
     * and the results will be counted. Do NOT attempt to call this
     * operation in a loop.
     *
     * @return number of entries in the service registry
     */
    @Override
    public long size() {
        try {
            final var response = getSearchResultResponse();
            if (LdapUtils.containsResultEntry(response)) {
                return response.getResult().size();
            }
        } catch (final LdapException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<? extends RegisteredService> load() {
        final List<RegisteredService> list = new ArrayList<>();
        try {
            final var response = getSearchResultResponse();
            if (LdapUtils.containsResultEntry(response)) {
                response.getResult().getEntries()
                    .stream()
                    .map(this.ldapServiceMapper::mapToRegisteredService)
                    .filter(Objects::nonNull)
                    .forEach(s -> {
                        publishEvent(new CasRegisteredServiceLoadedEvent(this, s));
                        list.add(s);
                    });
            }
        } catch (final LdapException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return list;
    }

    private Response<SearchResult> getSearchResultResponse() throws LdapException {
        return LdapUtils.executeSearchOperation(this.connectionFactory, this.baseDn, LdapUtils.newLdaptiveSearchFilter(this.loadFilter));
    }

    @Override
    public RegisteredService findServiceById(final long id) {
        try {
            final var response = searchForServiceById(id);
            if (LdapUtils.containsResultEntry(response)) {
                return this.ldapServiceMapper.mapToRegisteredService(response.getResult().getEntry());
            }
        } catch (final LdapException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public RegisteredService findServiceById(final String id) {
        return load().stream().filter(r -> r.matches(id)).findFirst().orElse(null);
    }

    /**
     * Search for service by id.
     *
     * @param id the id
     * @return the response
     * @throws LdapException the ldap exception
     */
    private Response<SearchResult> searchForServiceById(final Long id) throws LdapException {
        final var filter = LdapUtils.newLdaptiveSearchFilter(this.searchFilter,
            LdapUtils.LDAP_SEARCH_FILTER_DEFAULT_PARAM_NAME, CollectionUtils.wrap(id.toString()));
        return LdapUtils.executeSearchOperation(this.connectionFactory, this.baseDn, filter);
    }
}
