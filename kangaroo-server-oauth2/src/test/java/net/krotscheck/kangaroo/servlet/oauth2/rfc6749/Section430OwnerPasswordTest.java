/*
 * Copyright (c) 2016 Michael Krotscheck
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
 */

package net.krotscheck.kangaroo.servlet.oauth2.rfc6749;

import net.krotscheck.kangaroo.common.exception.ErrorResponseBuilder.ErrorResponse;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientConfig;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.servlet.oauth2.resource.TokenResponseEntity;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import net.krotscheck.kangaroo.test.HttpUtil;
import org.apache.http.HttpStatus;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * These tests run through the Owner Password Flow. In this context, the
 * owner must be manually assigned a userid or password.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.3">https://tools.ietf.org/html/rfc6749#section-4.3</a>
 */
public final class Section430OwnerPasswordTest
        extends AbstractRFC6749Test {

    /**
     * User name used for valid requests.
     */
    private String username = "valid_user";

    /**
     * The password used for valid requests.
     */
    private String password = "valid_password";

    /**
     * The environment builder for the regular client.
     */
    private EnvironmentBuilder builder;

    /**
     * The environment builder for the authentication client.
     */
    private EnvironmentBuilder authBuilder;

    /**
     * The auth header string for each test.
     */
    private String authHeader;

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     * @throws Exception Should not be thrown.
     */
    @Override
    public List<EnvironmentBuilder> fixtures() throws Exception {
        builder = new EnvironmentBuilder(getSession())
                .scope("debug")
                .role("debug", new String[]{"debug"})
                .client(ClientType.OwnerCredentials)
                .authenticator("password")
                .user()
                .login(username, password);
        authBuilder = new EnvironmentBuilder(getSession())
                .scope("debug")
                .role("debug", new String[]{"debug"})
                .client(ClientType.OwnerCredentials, true)
                .authenticator("password")
                .user()
                .login(username, password);
        authHeader = HttpUtil.authHeaderBasic(
                authBuilder.getClient().getId(),
                authBuilder.getClient().getClientSecret());

        List<EnvironmentBuilder> fixtures = new ArrayList<>();
        fixtures.add(builder);
        fixtures.add(authBuilder);
        return fixtures;
    }

    /**
     * Assert that a simple token request works.
     */
    @Test
    public void testTokenSimpleRequest() {
        // Build the entity.
        Form f = new Form();
        f.param("client_id", builder.getClient().getId().toString());
        f.param("grant_type", "password");
        f.param("username", username);
        f.param("password", password);

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_OK, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertNotNull(entity.getAccessToken());
        assertNotNull(entity.getRefreshToken());
        assertEquals((int) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                entity.getExpiresIn().intValue());
        assertNull(entity.getScope());
        assertEquals(OAuthTokenType.Bearer, entity.getTokenType());
    }

    /**
     * Assert that using the wrong password fails.
     */
    @Test
    public void testTokenBadAuth() {
        // Build the entity.
        Form f = new Form();
        f.param("client_id", builder.getClient().getId().toString());
        f.param("grant_type", "password");
        f.param("username", username);
        f.param("password", "wrong_password");

        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_UNAUTHORIZED, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("unauthorized", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that missing a client id errors.
     */
    @Test
    public void testTokenNoClientId() {
        // Build the entity.
        Form f = new Form();
        f.param("grant_type", "password");
        f.param("username", username);
        f.param("password", password);
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);

        // Make the request
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_client", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that missing a grant type errors.
     */
    @Test
    public void testTokenNoGrant() {
        // Build the entity.
        Form f = new Form();
        f.param("client_id", builder.getClient().getId().toString());
        f.param("username", username);
        f.param("password", password);
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_grant", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Test that, if the client provides a token password, that
     * authentication using that password via the Authorization header works.
     */
    @Test
    public void testTokenAuthHeaderValid() {
        // Build the entity.
        Form f = new Form();
        f.param("client_id", authBuilder.getClient().getId().toString());
        f.param("grant_type", "password");
        f.param("scope", "debug");
        f.param("username", username);
        f.param("password", password);
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_OK, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertNotNull(entity.getAccessToken());
        assertNotNull(entity.getRefreshToken());
        assertEquals((int) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                entity.getExpiresIn().intValue());
        assertEquals("debug", entity.getScope());
        assertEquals(OAuthTokenType.Bearer, entity.getTokenType());
    }

    /**
     * Test that a user that provides a mismatched client_id in the request
     * body and the Authorization header fails.
     */
    @Test
    public void testTokenAuthHeaderMismatchClientId() {
        // Build the entity.
        Form f = new Form();
        f.param("client_id", builder.getClient().getId().toString());
        f.param("grant_type", "password");
        f.param("username", username);
        f.param("password", password);
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_client", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Test that a user may not identify themselves solely via the
     * Authorization header.
     */
    @Test
    public void testTokenAuthHeaderValidNoExplicitClientId() {
        // Build the entity.
        Form f = new Form();
        f.param("grant_type", "password");
        f.param("username", username);
        f.param("password", password);
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_client", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Test that authorization via the header with a bad password fails.
     */
    @Test
    public void testTokenAuthHeaderInvalid() {
        String badHeader = HttpUtil.authHeaderBasic(
                authBuilder.getClient().getId(),
                "badsecret");

        // Build the entity.
        Form f = new Form();
        f.param("client_id", authBuilder.getClient().getId().toString());
        f.param("grant_type", "password");
        f.param("username", username);
        f.param("password", password);
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", badHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_UNAUTHORIZED, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("access_denied", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Test that a client may also authenticate by putting the client_secret
     * in the post body.
     */
    @Test
    public void testTokenAuthSecretInBody() {
        Client c = authBuilder.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("client_secret", c.getClientSecret());
        f.param("grant_type", "password");
        f.param("scope", "debug");
        f.param("username", username);
        f.param("password", password);
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request()
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_OK, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        TokenResponseEntity entity = r.readEntity(TokenResponseEntity.class);
        assertNotNull(entity.getAccessToken());
        assertNotNull(entity.getRefreshToken());
        assertEquals((int) ClientConfig.ACCESS_TOKEN_EXPIRES_DEFAULT,
                entity.getExpiresIn().intValue());
        assertEquals("debug", entity.getScope());
        assertEquals(OAuthTokenType.Bearer, entity.getTokenType());
    }

    /**
     * Assert that only one authentication method may be used.
     */
    @Test
    public void testTokenAuthBothMethods() {
        Client c = authBuilder.getClient();

        // Build the entity.
        Form f = new Form();
        f.param("client_id", c.getId().toString());
        f.param("client_secret", c.getClientSecret());
        f.param("grant_type", "password");
        f.param("username", username);
        f.param("password", password);
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token")
                .request()
                .header("Authorization", authHeader)
                .post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_client", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an invalid grant type errors.
     */
    @Test
    public void testTokenInvalidGrantTypeRefreshToken() {
        // Build the entity.
        Form f = new Form();
        f.param("client_id", builder.getClient().getId().toString());
        f.param("grant_type", "refresh_token");
        f.param("username", username);
        f.param("password", password);
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_grant", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an invalid grant type errors.
     */
    @Test
    public void testTokenInvalidGrantTypeClientCredentials() {
        // Build the entity.
        Form f = new Form();
        f.param("client_id", builder.getClient().getId().toString());
        f.param("grant_type", "client_credentials");
        f.param("username", username);
        f.param("password", password);
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_grant", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }

    /**
     * Assert that an unknown grant type errors.
     */
    @Test
    public void testTokenUnknownGrantType() {
        // Build the entity.
        Form f = new Form();
        f.param("client_id", builder.getClient().getId().toString());
        f.param("grant_type", "unknown_grant_type");
        f.param("username", username);
        f.param("password", password);
        Entity postEntity = Entity.entity(f,
                MediaType.APPLICATION_FORM_URLENCODED_TYPE);
        Response r = target("/token").request().post(postEntity);

        // Assert various response-specific parameters.
        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());

        // Validate the query parameters received.
        ErrorResponse entity = r.readEntity(ErrorResponse.class);
        assertEquals("invalid_grant", entity.getError());
        assertNotNull(entity.getErrorDescription());
    }
}
