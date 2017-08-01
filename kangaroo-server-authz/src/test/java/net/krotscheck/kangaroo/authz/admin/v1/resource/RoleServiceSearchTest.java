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

package net.krotscheck.kangaroo.authz.admin.v1.resource;

import net.krotscheck.kangaroo.authz.common.database.entity.AbstractAuthzEntity;
import net.krotscheck.kangaroo.authz.common.database.entity.Application;
import net.krotscheck.kangaroo.authz.common.database.entity.ClientType;
import net.krotscheck.kangaroo.authz.common.database.entity.OAuthToken;
import net.krotscheck.kangaroo.authz.common.database.entity.Role;
import net.krotscheck.kangaroo.authz.common.database.entity.User;
import net.krotscheck.kangaroo.authz.admin.Scope;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Test the search endpoint of the scope service.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class RoleServiceSearchTest
        extends AbstractServiceSearchTest<Role> {

    /**
     * Convenience generic type for response decoding.
     */
    private static final GenericType<List<Role>> LIST_TYPE =
            new GenericType<List<Role>>() {

            };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of  client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public RoleServiceSearchTest(final ClientType clientType,
                                 final String tokenScope,
                                 final Boolean createUser) {
        super(Role.class, clientType, tokenScope, createUser);
    }

    /**
     * Return the appropriate list type for this test suite.
     *
     * @return The list type, used for test decoding.
     */
    @Override
    protected GenericType<List<Role>> getListType() {
        return LIST_TYPE;
    }

    /**
     * Return the list of entities which are owned by the given user.
     * Includes scope checks.
     *
     * @param owner The owner of the entities.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<Role> getOwnedEntities(final User owner) {
        // Get all the owned clients.
        return getAttached(owner).getApplications()
                .stream()
                .flatMap(a -> a.getRoles().stream())
                .collect(Collectors.toList());
    }

    /**
     * Return the list of field names on which this particular entity type
     * has build a search index.
     *
     * @return An array of field names.
     */
    protected String[] getSearchIndexFields() {
        return new String[]{"name"};
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.ROLE_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.ROLE;
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        return UriBuilder.fromPath("/role/")
                .path(id)
                .build();
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param entity The entity to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForEntity(final AbstractAuthzEntity entity) {
        return getUrlForId(entity.getId().toString());
    }

    /**
     * Test parameters.
     *
     * @return The parameters passed to this test during every run.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(
                new Object[]{
                        ClientType.Implicit,
                        Scope.ROLE_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.ROLE,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.ROLE_ADMIN,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.ROLE,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.ROLE_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.ROLE,
                        false
                });
    }

    /**
     * Test that we can filter a search by an application ID.
     */
    @Test
    public void testSearchByApplication() {
        String query = "many";
        Application a = getSecondaryContext()
                .getApplication();

        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", query);
        params.put("application", a.getId().toString());
        Response r = search(params, token);

        // Determine result set.
        List<Role> searchResults = getSearchResults(query);
        List<Role> accessibleEntities = getAccessibleEntities(token);

        List<Role> expectedResults = searchResults
                .stream()
                .filter((item) -> accessibleEntities.indexOf(item) > -1)
                .filter((item) -> item.getApplication().equals(a))
                .collect(Collectors.toList());

        Integer expectedTotal = expectedResults.size();
        int expectedResultSize = Math.min(10, expectedTotal);
        Integer expectedOffset = 0;
        Integer expectedLimit = 10;

        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else if (!isAccessible(a, token)) {
            assertErrorResponse(r, Status.BAD_REQUEST);
        } else {
            Assert.assertTrue(expectedTotal > 1);

            List<Role> results = r.readEntity(getListType());
            Assert.assertEquals(expectedOffset.toString(),
                    r.getHeaderString("Offset"));
            Assert.assertEquals(expectedLimit.toString(),
                    r.getHeaderString("Limit"));
            Assert.assertEquals(expectedTotal.toString(),
                    r.getHeaderString("Total"));
            Assert.assertEquals(expectedResultSize, results.size());
        }
    }

    /**
     * Test that an invalid application throws an error.
     */
    @Test
    public void testSearchByInvalidApplication() {
        OAuthToken token = getAdminToken();
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("application", UUID.randomUUID().toString());

        Response r = search(params, token);
        if (isLimitedByClientCredentials()) {
            assertErrorResponse(r, Status.BAD_REQUEST.getStatusCode(),
                    "invalid_scope");
        } else {
            assertErrorResponse(r, Status.BAD_REQUEST);
        }
    }

    /**
     * Test that an malformed application throws an error.
     */
    // TODO(krotscheck): This should return a 400.
    @Test
    public void testSearchByMalformedApplication() {
        Map<String, String> params = new HashMap<>();
        params.put("q", "many");
        params.put("application", "malformed");

        Response r = search(params, getAdminToken());
        assertErrorResponse(r, Status.NOT_FOUND);
    }
}