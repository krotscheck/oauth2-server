/*
 * Copyright (c) 2017 Michael Krotscheck
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.krotscheck.kangaroo.authz.common.authenticator.facebook;

import com.google.common.base.Strings;
import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.exception.ThirdPartyErrorException;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.AbstractOAuth2Authenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2IdPToken;
import net.krotscheck.kangaroo.authz.common.authenticator.oauth2.OAuth2User;
import net.krotscheck.kangaroo.util.HttpUtil;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import java.util.Map;

/**
 * This authentication helper permits using facebook as an IdP. It's not a
 * true facebook app: it immediately discards any issued token, and
 * does no subsequent lookups on the user. In other words, this will not
 * provide you with fancy amazing facebook features, though we may choose to
 * enable this in the future.
 *
 * @author Michael Krotscheck
 */
public final class FacebookAuthenticator
        extends AbstractOAuth2Authenticator {

    /**
     * Facebook's base user endpoint.
     */
    private static final String USER_ENDPOINT =
            "https://graph.facebook.com/v2.10/me";

    /**
     * Facebook's auth endpoint.
     *
     * @return The absolute URL to facebook's auth endpoint.
     */
    @Override
    protected String getAuthEndpoint() {
        return "https://www.facebook.com/v2.10/dialog/oauth";
    }

    /**
     * The token endpoint.
     *
     * @return Facebook's token endpoint.
     */
    @Override
    protected String getTokenEndpoint() {
        return "https://graph.facebook.com/v2.10/oauth/access_token";
    }

    /**
     * List of scopes that we need from facebook.
     *
     * @return A static list of scopes.
     */
    @Override
    protected String getScopes() {
        return "public_profile,email";
    }

    /**
     * Load the user's identity from facebook, and wrap it into a common format.
     *
     * @param token The OAuth token.
     * @return The user identity.
     */
    @Override
    protected OAuth2User loadUserIdentity(final OAuth2IdPToken token) {
        Response r = getClient()
                .target(USER_ENDPOINT)
                .request()
                .header(HttpHeaders.AUTHORIZATION,
                        HttpUtil.authHeaderBearer(token.getAccessToken()))
                .get();

        try {
            // If this is an error...
            if (r.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
                FacebookUserEntity fbUser =
                        r.readEntity(FacebookUserEntity.class);
                if (Strings.isNullOrEmpty(fbUser.getId())) {
                    throw new ThirdPartyErrorException();
                }
                return fbUser.asGenericUser();
            } else {
                Map<String, String> params = r.readEntity(MAP_TYPE);
                throw new ThirdPartyErrorException(params);
            }
        } catch (ProcessingException e) {
            throw new ThirdPartyErrorException();
        } finally {
            r.close();
        }
    }

    /**
     * HK2 Binder for our injector context.
     */
    public static final class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(FacebookAuthenticator.class)
                    .to(IAuthenticator.class)
                    .named(AuthenticatorType.Facebook.name())
                    .in(RequestScoped.class);
        }
    }
}
