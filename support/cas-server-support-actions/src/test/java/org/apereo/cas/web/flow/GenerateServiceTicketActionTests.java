package org.apereo.cas.web.flow;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.AbstractCentralAuthenticationServiceTests;
import org.apereo.cas.CasProtocolConstants;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.web.config.CasSupportActionsConfiguration;
import org.apereo.cas.web.support.WebUtils;
import org.junit.Test;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.test.MockRequestContext;

import javax.servlet.http.Cookie;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Scott Battaglia
 * @since 3.0.0
 */
@Import(CasSupportActionsConfiguration.class)
@Slf4j
public class GenerateServiceTicketActionTests extends AbstractCentralAuthenticationServiceTests {

    private static final String SERVICE_PARAM = "service";

    @Autowired
    @Qualifier("generateServiceTicketAction")
    private Action action;

    private TicketGrantingTicket ticketGrantingTicket;

    @Before
    public void onSetUp() {
        final var authnResult = getAuthenticationSystemSupport()
                        .handleAndFinalizeSingleAuthenticationTransaction(CoreAuthenticationTestUtils.getService(),
                                CoreAuthenticationTestUtils.getCredentialsWithSameUsernameAndPassword());

        this.ticketGrantingTicket = getCentralAuthenticationService().createTicketGrantingTicket(authnResult);
        getTicketRegistry().addTicket(this.ticketGrantingTicket);
    }

    @Test
    public void verifyServiceTicketFromCookie() throws Exception {
        final var context = new MockRequestContext();
        context.getFlowScope().put(SERVICE_PARAM, RegisteredServiceTestUtils.getService());
        context.getFlowScope().put(WebUtils.PARAMETER_TICKET_GRANTING_TICKET_ID, this.ticketGrantingTicket.getId());
        final var request = new MockHttpServletRequest();
        context.setExternalContext(new ServletExternalContext(
                new MockServletContext(), request, new MockHttpServletResponse()));
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, SERVICE_PARAM);
        request.setCookies(new Cookie("TGT", this.ticketGrantingTicket.getId()));

        this.action.execute(context);

        assertNotNull(WebUtils.getServiceTicketFromRequestScope(context));
    }

    @Test
    public void verifyTicketGrantingTicketFromRequest() throws Exception {
        final var context = new MockRequestContext();
        context.getFlowScope().put(SERVICE_PARAM, RegisteredServiceTestUtils.getService());
        final var request = new MockHttpServletRequest();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, new MockHttpServletResponse()));
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, SERVICE_PARAM);
        WebUtils.putTicketGrantingTicketInScopes(context, this.ticketGrantingTicket);

        this.action.execute(context);

        assertNotNull(WebUtils.getServiceTicketFromRequestScope(context));
    }

    @Test
    public void verifyTicketGrantingTicketNoTgt() throws Exception {
        final var context = new MockRequestContext();
        context.getFlowScope().put(SERVICE_PARAM, RegisteredServiceTestUtils.getService());
        
        final var request = new MockHttpServletRequest();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, new MockHttpServletResponse()));
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, SERVICE_PARAM);

        final var tgt = mock(TicketGrantingTicket.class);
        when(tgt.getId()).thenReturn("bleh");
        WebUtils.putTicketGrantingTicketInScopes(context, tgt);

        assertEquals(CasWebflowConstants.TRANSITION_ID_AUTHENTICATION_FAILURE, this.action.execute(context).getId());
    }

    @Test
    public void verifyTicketGrantingTicketExpiredTgt() throws Exception {
        final var context = new MockRequestContext();
        context.getFlowScope().put(SERVICE_PARAM, RegisteredServiceTestUtils.getService());
        final var request = new MockHttpServletRequest();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, new MockHttpServletResponse()));
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, SERVICE_PARAM);
        WebUtils.putTicketGrantingTicketInScopes(context, this.ticketGrantingTicket);
        this.ticketGrantingTicket.markTicketExpired();
        getTicketRegistry().updateTicket(this.ticketGrantingTicket);
        assertEquals(CasWebflowConstants.TRANSITION_ID_AUTHENTICATION_FAILURE, this.action.execute(context).getId());
    }
    
    @Test
    public void verifyTicketGrantingTicketNotTgtButGateway() throws Exception {
        final var context = new MockRequestContext();
        context.getFlowScope().put(SERVICE_PARAM, RegisteredServiceTestUtils.getService());
        final var request = new MockHttpServletRequest();
        context.setExternalContext(new ServletExternalContext(new MockServletContext(), request, new MockHttpServletResponse()));
        request.addParameter(CasProtocolConstants.PARAMETER_SERVICE, SERVICE_PARAM);
        request.addParameter(CasProtocolConstants.PARAMETER_GATEWAY, "true");
        final var tgt = mock(TicketGrantingTicket.class);
        when(tgt.getId()).thenReturn("bleh");
        WebUtils.putTicketGrantingTicketInScopes(context, tgt);
        assertEquals(CasWebflowConstants.STATE_ID_GATEWAY, this.action.execute(context).getId());
    }
}
