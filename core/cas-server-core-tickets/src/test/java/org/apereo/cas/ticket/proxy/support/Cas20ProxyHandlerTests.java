package org.apereo.cas.ticket.proxy.support;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.HttpBasedServiceCredential;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.DefaultUniqueTicketIdGenerator;
import org.apereo.cas.util.http.SimpleHttpClientFactoryBean;
import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Scott Battaglia
 * @since 3.0.0
 */
@Slf4j
public class Cas20ProxyHandlerTests {

    private Cas20ProxyHandler handler;

    @Mock
    private TicketGrantingTicket proxyGrantingTicket;

    public Cas20ProxyHandlerTests() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void initialize() {
        final var factory = new SimpleHttpClientFactoryBean();
        factory.setConnectionTimeout(10000);
        factory.setReadTimeout(10000);
        this.handler = new Cas20ProxyHandler(factory.getObject(), new DefaultUniqueTicketIdGenerator());
        when(this.proxyGrantingTicket.getId()).thenReturn("proxyGrantingTicket");
    }

    @Test
    public void verifyValidProxyTicketWithoutQueryString() throws Exception {
        assertNotNull(this.handler.handle(new HttpBasedServiceCredential(new URL("https://www.google.com/"),
            CoreAuthenticationTestUtils.getRegisteredService("https://some.app.edu")), proxyGrantingTicket));
    }

    @Test
    public void verifyValidProxyTicketWithQueryString() throws Exception {
        assertNotNull(this.handler.handle(new HttpBasedServiceCredential(new URL("https://www.google.com/?test=test"),
            CoreAuthenticationTestUtils.getRegisteredService("https://some.app.edu")), proxyGrantingTicket));
    }

    @Test
    public void verifyNonValidProxyTicket() throws Exception {
        final var clientFactory = new SimpleHttpClientFactoryBean();
        clientFactory.setAcceptableCodes(CollectionUtils.wrapList(900));

        this.handler = new Cas20ProxyHandler(clientFactory.getObject(), new DefaultUniqueTicketIdGenerator());

        assertNull(this.handler.handle(new HttpBasedServiceCredential(new URL("http://www.rutgers.edu"),
            CoreAuthenticationTestUtils.getRegisteredService("https://some.app.edu")), proxyGrantingTicket));
    }
}
