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

package net.krotscheck.kangaroo.authz.common.authenticator.test;

import net.krotscheck.kangaroo.authz.common.authenticator.AuthenticatorType;
import net.krotscheck.kangaroo.authz.common.authenticator.IAuthenticator;
import net.krotscheck.kangaroo.authz.common.authenticator.test.TestAuthenticator.Binder;
import net.krotscheck.kangaroo.authz.common.database.entity.Authenticator;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.UserIdentity;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder;
import net.krotscheck.kangaroo.authz.test.ApplicationBuilder.ApplicationContext;
import net.krotscheck.kangaroo.test.jersey.DatabaseTest;
import net.krotscheck.kangaroo.test.rule.TestDataResource;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mockito;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Test the test authenticator.
 *
 * @author Michael Krotscheck
 */
public final class TestAuthenticatorTest extends DatabaseTest {

    /**
     * DB Context, constructed for testing.
     */
    private static ApplicationContext context;
    /**
     * Test data loading for this test.
     */
    @ClassRule
    public static final TestRule TEST_DATA_RULE =
            new TestDataResource(HIBERNATE_RESOURCE) {
                /**
                 * Initialize the test data.
                 */
                @Override
                protected void loadTestData(final Session session) {
                    context = ApplicationBuilder.newApplication(session)
                            .client(ClientType.AuthorizationGrant)
                            .role("not_test_role")
                            .authenticator(AuthenticatorType.Test)
                            .build();
                }
            };

    /**
     * Assert that delegate simply redirects to the authentication endpoint.
     */
    @Test
    public void delegate() {
        Session session = Mockito.mock(Session.class);
        IAuthenticator a = new TestAuthenticator(session);
        Authenticator config = new Authenticator();
        URI testUri = UriBuilder.fromUri("http://example.com/redirect").build();

        Response r = a.delegate(config, testUri);

        Assert.assertEquals(testUri, r.getLocation());
        Mockito.verifyNoMoreInteractions(session);
    }

    /**
     * Assert that authenticating without the demo user creates it.
     */
    @Test
    public void authenticateNoTestUser() {
        IAuthenticator a = new TestAuthenticator(getSession());
        Authenticator config = context.getAuthenticator();
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        UserIdentity i = a.authenticate(config, params);

        assertNotNull(i);
        assertNotNull(i.getId());
        Assert.assertTrue(getSession().contains(i));
        Assert.assertEquals("dev_user", i.getRemoteId());
    }

    /**
     * Assert that authenticating without the demo user creates it.
     */
    @Test
    public void authenticateTestUserWithMatchingRoles() {
        assertNotNull(context.getApplication().getDefaultRole());

        IAuthenticator a = new TestAuthenticator(getSession());
        Authenticator config = context.getAuthenticator();
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        UserIdentity i = a.authenticate(config, params);

        assertNotNull(i);
        assertNotNull(i.getId());
        Assert.assertTrue(getSession().contains(i));
        Assert.assertEquals("dev_user", i.getRemoteId());
        Assert.assertEquals(context.getApplication().getDefaultRole(),
                i.getUser().getRole());
    }

    /**
     * Assert that authenticating with an existing demo user returns it.
     */
    @Test
    public void authenticateWithTestUser() {
        ApplicationContext testContext = context.getBuilder()
                .user()
                .identity("dev_user")
                .build();

        UserIdentity persistedIdentity = testContext.getUserIdentity();

        IAuthenticator a = new TestAuthenticator(getSession());
        Authenticator config = testContext.getAuthenticator();
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();

        UserIdentity i = a.authenticate(config, params);

        Assert.assertEquals(persistedIdentity, i);
    }
}
