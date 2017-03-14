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

package net.krotscheck.kangaroo.servlet.admin.v1.resource;

import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientRedirect;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.servlet.admin.v1.Scope;
import net.krotscheck.kangaroo.test.EnvironmentBuilder;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Test the list and filter methods of the ClientRedirect service.
 *
 * @author Michael Krotscheck
 */
@RunWith(Parameterized.class)
public final class ClientRedirectServiceBrowseTest
        extends DAbstractSubserviceBrowseTest<Client, ClientRedirect> {

    /**
     * Generic type declaration for list decoding.
     */
    private static final GenericType<List<ClientRedirect>> LIST_TYPE =
            new GenericType<List<ClientRedirect>>() {

            };

    /**
     * Create a new instance of this parameterized test.
     *
     * @param clientType The type of client.
     * @param tokenScope The client scope to issue.
     * @param createUser Whether to create a new user.
     */
    public ClientRedirectServiceBrowseTest(final ClientType clientType,
                                           final String tokenScope,
                                           final Boolean createUser) {
        super(clientType, tokenScope, createUser);
    }

    /**
     * Return the token scope required for admin access on this test.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getAdminScope() {
        return Scope.CLIENT_ADMIN;
    }

    /**
     * Return the token scope required for generic user access.
     *
     * @return The correct scope string.
     */
    @Override
    protected String getRegularScope() {
        return Scope.CLIENT;
    }

    /**
     * Return the list type used to decode browse results.
     *
     * @return The list type.
     */
    @Override
    protected GenericType<List<ClientRedirect>> getListType() {
        return LIST_TYPE;
    }

    /**
     * Return the list of entities which should be accessible given a
     * specific token.
     *
     * @param parentEntity The client to filter against.
     * @param token        The oauth token to test against.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<ClientRedirect> getAccessibleEntities(
            final Client parentEntity,
            final OAuthToken token) {
        // If you're an admin, you get to see everything. If you're not, you
        // only get to see what you own.
        if (!token.getScopes().containsKey(getAdminScope())) {
            return getOwnedEntities(parentEntity, token);
        }

        // We know you're an admin. Get all applications in the system.
        Criteria criteria = getSession().createCriteria(ClientRedirect.class)
                .add(Restrictions.eq("client", parentEntity));

        // Get all the owned clients.
        return ((List<ClientRedirect>) criteria.list());
    }

    /**
     * Return the list of entities which are owned by the given oauth token.
     *
     * @param parentEntity The client to filter against.
     * @param owner        The owner of these entities.
     * @return A list of entities (could be empty).
     */
    @Override
    protected List<ClientRedirect> getOwnedEntities(final Client parentEntity,
                                                    final User owner) {
        // Get all the owned clients.
        return owner.getApplications()
                .stream()
                .flatMap(a -> a.getClients().stream())
                .filter(c -> c.equals(parentEntity))
                .flatMap(c -> c.getRedirects().stream())
                .collect(Collectors.toList());
    }

    /**
     * Test parameters.
     *
     * @return The list of parameters.
     */
    @Parameterized.Parameters
    public static Collection parameters() {
        return Arrays.asList(
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT,
                        false
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT_ADMIN,
                        true
                },
                new Object[]{
                        ClientType.Implicit,
                        Scope.CLIENT,
                        true
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.CLIENT_ADMIN,
                        false
                },
                new Object[]{
                        ClientType.ClientCredentials,
                        Scope.CLIENT,
                        false
                });
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param id The ID to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForId(final String id) {
        String parentId = "";

        Session s = getSession();
        Transaction t = s.beginTransaction();
        try {
            ClientRedirect r = s.get(ClientRedirect.class, UUID.fromString(id));
            parentId = r.getClient().getId().toString();
        } catch (Exception e) {
            parentId = getParentEntity(getAdminContext()).getId().toString();
        } finally {
            t.rollback();
        }

        return getUrlForEntity(parentId, id);
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param entity The entity to use.
     * @return The resource URL.
     */
    @Override
    protected URI getUrlForEntity(final AbstractEntity entity) {
        String parentId = "";
        String childId = "";

        ClientRedirect referrer = (ClientRedirect) entity;
        if (referrer == null) {
            return getUrlForId(null);
        } else {
            UUID referrerId = referrer.getId();
            childId = referrerId == null ? null : referrerId.toString();
        }

        Client client = referrer.getClient();
        if (client == null) {
            return getUrlForId(null);
        } else {
            UUID clientId = client.getId();
            parentId = clientId == null ? null : clientId.toString();
        }
        return getUrlForEntity(parentId, childId);
    }

    /**
     * Return the correct parent entity type from the provided context.
     *
     * @param context The context to extract the value from.
     * @return The requested entity type under test.
     */
    @Override
    protected Client getParentEntity(final EnvironmentBuilder context) {
        return context.getReferrer().getClient();
    }

    /**
     * Construct the request URL for this test given a specific resource ID.
     *
     * @param parentId The parent ID.
     * @param childId  The Child ID.
     * @return The resource URL.
     */
    private URI getUrlForEntity(final String parentId, final String childId) {
        UriBuilder builder = UriBuilder
                .fromPath("/client")
                .path(parentId)
                .path("redirect");

        if (childId != null) {
            builder.path(childId);
        }

        return builder.build();
    }
}
