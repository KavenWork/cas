package org.apereo.cas.support.oauth.web.response.callback;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20GrantTypes;
import org.apereo.cas.support.oauth.util.OAuth20Utils;
import org.apereo.cas.support.oauth.web.response.accesstoken.AccessTokenResponseGenerator;
import org.apereo.cas.support.oauth.web.response.accesstoken.OAuth20TokenGenerator;
import org.apereo.cas.support.oauth.web.response.accesstoken.ext.AccessTokenRequestDataHolder;
import org.apereo.cas.ticket.ExpirationPolicy;
import org.pac4j.core.context.J2EContext;
import org.springframework.web.servlet.View;

/**
 * This is {@link OAuth20ResourceOwnerCredentialsResponseBuilder}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
@AllArgsConstructor
public class OAuth20ResourceOwnerCredentialsResponseBuilder implements OAuth20AuthorizationResponseBuilder {
    private final AccessTokenResponseGenerator accessTokenResponseGenerator;
    private final OAuth20TokenGenerator accessTokenGenerator;
    private final ExpirationPolicy accessTokenExpirationPolicy;


    @Override
    public View build(final J2EContext context, final String clientId, final AccessTokenRequestDataHolder holder) {
        final var accessToken = accessTokenGenerator.generate(holder);
        accessTokenResponseGenerator.generate(context.getRequest(),
                context.getResponse(),
                holder.getRegisteredService(), holder.getService(),
                accessToken.getKey(), accessToken.getValue(),
                accessTokenExpirationPolicy.getTimeToLive(),
                OAuth20Utils.getResponseType(context));
        return null;
    }

    @Override
    public boolean supports(final J2EContext context) {
        final var grantType = context.getRequestParameter(OAuth20Constants.GRANT_TYPE);
        return OAuth20Utils.isGrantType(grantType, OAuth20GrantTypes.PASSWORD);
    }
}
