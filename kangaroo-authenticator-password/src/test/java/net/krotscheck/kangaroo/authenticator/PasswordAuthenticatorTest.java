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

package net.krotscheck.kangaroo.authenticator;

import net.krotscheck.kangaroo.authenticator.PasswordAuthenticator.Binder;
import net.krotscheck.kangaroo.common.exception.rfc6749.Rfc6749Exception.InvalidRequestException;
import net.krotscheck.kangaroo.database.entity.Authenticator;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import net.krotscheck.kangaroo.test.DatabaseTest;
import net.krotscheck.kangaroo.test.IFixture;
import net.krotscheck.util.ResourceUtil;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Unit tests for the password authenticator.
 *
 * @author Michael Krotscheck
 */
public final class PasswordAuthenticatorTest extends DatabaseTest {

    /**
     * The tests's backing data.
     */
    private static final File TEST_DATA = ResourceUtil.getFileForResource(
            "PasswordAuthenticatorData.xml");

    /**
     * Load data fixtures for each test.
     *
     * @return A list of fixtures, which will be cleared after the test.
     */
    @Override
    public List<IFixture> fixtures() {
        return null;
    }

    /**
     * Load the test data.
     *
     * @return The test data.
     */
    @Override
    public File testData() {
        return TEST_DATA;
    }

    /**
     * Assert that the test delegate does nothing.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testDelegate() throws Exception {
        IAuthenticator a = new PasswordAuthenticator(getSession());

        Authenticator config = new Authenticator();
        URI callback = UriBuilder.fromPath("http://example.com").build();

        Response r = a.delegate(config, callback);
        Assert.assertNull(r);
    }

    /**
     * Test that a valid authentication works.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testAuthenticateValid() throws Exception {
        Authenticator config = getSession().get(Authenticator.class,
                UUID.fromString("00000000-0000-0000-0000-000000000001"));

        IAuthenticator a = new PasswordAuthenticator(getSession());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("username", "login");
        params.add("password", "password");
        UserIdentity i = a.authenticate(config, params);
        Assert.assertEquals("login", i.getRemoteId());
    }

    /**
     * Assert that trying to authenticate with a null input fails.
     *
     * @throws Exception An authenticator exception.
     */
    @Test(expected = InvalidRequestException.class)
    public void testAuthenticateNullConfig() throws Exception {
        IAuthenticator a = new PasswordAuthenticator(getSession());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        a.authenticate(null, params);
    }

    /**
     * Assert that trying to authenticate with a null input fails.
     *
     * @throws Exception An authenticator exception.
     */
    @Test(expected = InvalidRequestException.class)
    public void testAuthenticateNullParams() throws Exception {
        IAuthenticator a = new PasswordAuthenticator(getSession());
        Authenticator config = new Authenticator();
        a.authenticate(config, null);
    }

    /**
     * Assert that trying to authenticate with no matching identity fails.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testAuthenticateNoIdentity() throws Exception {
        Authenticator config = getSession().get(Authenticator.class,
                UUID.fromString("00000000-0000-0000-0000-000000000001"));

        IAuthenticator a = new PasswordAuthenticator(getSession());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("username", "wrongIdentity");
        params.add("password", "password");
        UserIdentity i = a.authenticate(config, params);
        Assert.assertNull(i);
    }

    /**
     * Assert that trying to authenticate with a wrong password fails.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testAuthenticateWrongPassword() throws Exception {
        Authenticator config = getSession().get(Authenticator.class,
                UUID.fromString("00000000-0000-0000-0000-000000000001"));

        IAuthenticator a = new PasswordAuthenticator(getSession());
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("username", "login");
        params.add("password", "wrongpassword");
        UserIdentity i = a.authenticate(config, params);
        Assert.assertNull(i);
    }

    /**
     * Assert that we can invoke the binder.
     *
     * @throws Exception An authenticator exception.
     */
    @Test
    public void testBinder() throws Exception {
        ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        ServiceLocator locator = factory.create("PasswordAuthenticatorTest");

        Binder b = new PasswordAuthenticator.Binder();
        ServiceLocatorUtilities.bind(locator, b);

        List<ActiveDescriptor<?>> descriptors =
                locator.getDescriptors(
                        BuilderHelper.createContractFilter(
                                PasswordAuthenticator.class.getName()));
        Assert.assertEquals(1, descriptors.size());

        ActiveDescriptor descriptor = descriptors.get(0);
        Assert.assertNotNull(descriptor);
        // Request scoped...
        Assert.assertEquals(RequestScoped.class.getCanonicalName(),
                descriptor.getScope());

        // ... with no name.
        Assert.assertNull(descriptor.getName());
    }
}