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
 *
 */

package net.krotscheck.kangaroo.test;

import net.krotscheck.kangaroo.database.entity.AbstractEntity;
import net.krotscheck.kangaroo.database.entity.Application;
import net.krotscheck.kangaroo.database.entity.ApplicationScope;
import net.krotscheck.kangaroo.database.entity.Authenticator;
import net.krotscheck.kangaroo.database.entity.AuthenticatorState;
import net.krotscheck.kangaroo.database.entity.Client;
import net.krotscheck.kangaroo.database.entity.ClientType;
import net.krotscheck.kangaroo.database.entity.OAuthToken;
import net.krotscheck.kangaroo.database.entity.OAuthTokenType;
import net.krotscheck.kangaroo.database.entity.Role;
import net.krotscheck.kangaroo.database.entity.User;
import net.krotscheck.kangaroo.database.entity.UserIdentity;
import net.krotscheck.kangaroo.util.PasswordUtil;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This class assists in the creation of a test environment, by bootstrapping
 * applications, clients, their enabled authenticators and flows, as well as
 * other miscellaneous components.
 * <p>
 * Note that this class is a bit volatile, as it makes the implicit
 * assumption that all the resources it needs will be created before they're
 * used. In other words, if you've got weird issues, then you're probably
 * using this class wrong.
 *
 * @author Michael Krotscheck
 */
@Deprecated
public final class EnvironmentBuilder {

    /**
     * Logger instance.
     */
    private static Logger logger = LoggerFactory
            .getLogger(EnvironmentBuilder.class);

    /**
     * Static timezone.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * The builder's hibernate session.
     */
    private Session session;

    /**
     * The current application context.
     */
    private Application application;

    /**
     * The most recent created scope.
     */
    private ApplicationScope scope;

    /**
     * The current application scopes.
     */
    private SortedMap<String, ApplicationScope> scopes = new TreeMap<>();

    /**
     * An authenticator state.
     */
    private AuthenticatorState authenticatorState;

    /**
     * The current role context.
     */
    private Role role;

    /**
     * The current client context.
     */
    private Client client;

    /**
     * The current authenticator context.
     */
    private Authenticator authenticator;

    /**
     * The user context.
     */
    private User user;

    /**
     * The user identity context.
     */
    private UserIdentity userIdentity;

    /**
     * The oauth token context.
     */
    private OAuthToken token;

    /**
     * The last redirect created.
     */
    private URI redirectUri;

    /**
     * The last referrer created.
     */
    private URI referrerUri;

    /**
     * The list of entities that are under management by this builder.
     */
    private final List<AbstractEntity> trackedEntities = new ArrayList<>();

    /**
     * Get the current application.
     *
     * @return The current application.
     */
    public Application getApplication() {
        return getRefreshed(application);
    }

    /**
     * Get the current role.
     *
     * @return The current role.
     */
    public Role getRole() {
        return getRefreshed(role);
    }

    /**
     * Get the list of tracked entities.
     *
     * @return The current list of tracked entities.
     */
    public List<AbstractEntity> getTrackedEntities() {
        return Collections.unmodifiableList(
                new ArrayList<>(getRefreshed(trackedEntities)));
    }

    /**
     * Return the current list of active scopes.
     *
     * @return The list of scopes.
     */
    public SortedMap<String, ApplicationScope> getScopes() {
        return Collections.unmodifiableSortedMap(getRefreshed(scopes));
    }

    /**
     * Get the current client.
     *
     * @return The current client.
     */
    public Client getClient() {
        return getRefreshed(client);
    }

    /**
     * Get the current authenticator.
     *
     * @return The current authenticator.
     */
    public Authenticator getAuthenticator() {
        return getRefreshed(authenticator);
    }

    /**
     * Get the current user.
     *
     * @return The current user.
     */
    public User getUser() {
        return getRefreshed(user);
    }

    /**
     * Get the current user identity.
     *
     * @return The current user identity.
     */
    public UserIdentity getUserIdentity() {
        return getRefreshed(userIdentity);
    }

    /**
     * Get the current token.
     *
     * @return The current token.
     */
    public OAuthToken getToken() {
        return getRefreshed(token);
    }

    /**
     * Get the current scope.
     *
     * @return The current scope.
     */
    public ApplicationScope getScope() {
        return getRefreshed(scope);
    }

    /**
     * Get the owner of this app.
     *
     * @return The application owner.
     */
    public User getOwner() {
        return getApplication().getOwner();
    }

    /**
     * Return the current authenticator state.
     *
     * @return The authenticator state.
     */
    public AuthenticatorState getAuthenticatorState() {
        return getRefreshed(authenticatorState);
    }

    /**
     * Create a new builder.
     *
     * @param session A Hibernate session to use.
     * @param name    The name of the application.
     */
    public EnvironmentBuilder(final Session session, final String name) {
        this.session = session;

        application = new Application();
        application.setName(name);

        persist(application);
    }

    /**
     * Create a new builder around an existing application. In this case,
     * only the application's ID is used, as the entity may be a member of a
     * different session. Note that this will try to populate some of the
     * internal fields with what's already been loaded into the database, so
     * missing fields may cause errors.
     *
     * @param session A Hibernate session to use.
     * @param app     The Application entity to wrap.
     */
    public EnvironmentBuilder(final Session session,
                              final Application app) {
        this.session = session;

        // Load this entity from the provided session.
        this.application = transactionGet(Application.class, app.getId());
        this.scopes.putAll(this.application.getScopes());
        this.scope = this.scopes.values().iterator().next();

        // If possible, find a client with a token.
        List<Client> clients = this.application.getClients().stream()
                .flatMap(c -> c.getTokens().stream())
                .map(OAuthToken::getClient)
                .collect(Collectors.toList());
        if (clients.size() > 0) {
            this.client = clients.get(0);
        } else {
            this.client = this.application.getClients().get(0);
        }

        this.authenticator = this.client.getAuthenticators().get(0);
        this.user = this.application.getUsers().get(0);
        if (this.application.getRoles().size() > 0) {
            this.role = this.application.getRoles().get(0);
        }
        if (this.user.getIdentities().size() > 0) {
            this.userIdentity = this.user.getIdentities().get(0);
        }
        if (this.client.getTokens().size() > 0) {
            this.token = this.client.getTokens().get(0);
        }
    }

    /**
     * Create a new builder.
     *
     * @param session A Hibernate session to use.
     */
    public EnvironmentBuilder(final Session session) {
        this(session, UUID.randomUUID().toString());
    }

    /**
     * Add a role to this application.
     *
     * @param name The name of the role.
     * @return This environment builder.
     */
    public EnvironmentBuilder role(final String name) {

        role = new Role();
        role.setApplication(getApplication());
        role.setName(name);

        if (scope != null) {
            role.getScopes().put(scope.getName(), scope);
        }

        persist(role);

        return this;
    }

    /**
     * Add a role to this application that is permitted a specific list of
     * scopes.
     *
     * @param name   The name of the role.
     * @param scopes The scopes to grant (must already exist).
     * @return This environment builder.
     */
    public EnvironmentBuilder role(final String name, final String[] scopes) {
        return role(name, new ArrayList<String>(Arrays.asList(scopes)));
    }

    /**
     * Add a role to this application that is permitted a specific list of
     * scopes.
     *
     * @param name   The name of the role.
     * @param scopes The scopes to grant (must already exist).
     * @return This environment builder.
     */
    public EnvironmentBuilder role(final String name,
                                   final List<String> scopes) {
        Application a = getApplication();
        role = new Role();
        role.setApplication(a);
        role.setName(name);

        SortedMap<String, ApplicationScope> appScopes = a.getScopes();
        SortedMap<String, ApplicationScope> roleScopes = new TreeMap<>();
        for (String scopeName : scopes) {
            if (appScopes.containsKey(scopeName)) {
                roleScopes.put(scopeName, appScopes.get(scopeName));
            }
        }
        role.setScopes(roleScopes);

        persist(role);

        return this;
    }

    /**
     * Add a scope to this application.
     *
     * @param name The name of the scope.
     * @return This environment builder.
     */
    public EnvironmentBuilder scope(final String name) {
        scope = new ApplicationScope();
        scope.setName(name);
        scope.setApplication(getApplication());
        scopes.put(name, scope);

        persist(scope);
        return this;
    }

    /**
     * Add a list of scopes to this application.
     *
     * @param scopes The list of scopes to add.
     * @return This builder.
     */
    public EnvironmentBuilder scopes(final List<String> scopes) {
        for (String scope : scopes) {
            scope(scope);
        }
        return this;
    }

    /**
     * Add a client to this application.
     *
     * @param type The client type.
     * @return This builder.
     */
    public EnvironmentBuilder client(final ClientType type) {
        return client(type, false);
    }

    /**
     * Add a client to this application.
     *
     * @param type The client type.
     * @param name An explicit client name.
     * @return This builder.
     */
    public EnvironmentBuilder client(final ClientType type,
                                     final String name) {
        return client(type, name, false);
    }

    /**
     * Add a client, with a secret, to this application.
     *
     * @param isPrivate Is this a private client or not?
     * @param type      The client type.
     * @return This builder.
     */
    public EnvironmentBuilder client(final ClientType type,
                                     final Boolean isPrivate) {
        return client(type, "Test Client", isPrivate);
    }

    /**
     * Add a client, with a name, to this application.
     *
     * @param isPrivate Is this a private client or not?
     * @param name      An explicit client name.
     * @param type      The client type.
     * @return This builder.
     */
    public EnvironmentBuilder client(final ClientType type,
                                     final String name,
                                     final Boolean isPrivate) {

        client = new Client();
        client.setName(name);
        client.setType(type);
        client.setApplication(getApplication());

        if (isPrivate) {
            client.setClientSecret(UUID.randomUUID().toString());
        }

        persist(client);

        return this;
    }

    /**
     * Add a redirect to the current client context.
     *
     * @param redirect The Redirect URI for the client.
     * @return This builder.
     */
    public EnvironmentBuilder redirect(final String redirect) {
        Client c = getClient();

        redirectUri = UriBuilder.fromUri(redirect).build();
        c.getRedirects().add(redirectUri);

        persist(c);

        return this;
    }

    /**
     * Add a referrer to the current client context.
     *
     * @param referrer The Referral URI for the client.
     * @return This builder.
     */
    public EnvironmentBuilder referrer(final String referrer) {
        Client c = getClient();

        referrerUri = UriBuilder.fromUri(referrer).build();
        c.getReferrers().add(referrerUri);

        persist(c);

        return this;
    }

    /**
     * Enable an authenticator for the current client context.
     *
     * @param name The authenticator to enable.
     * @return This builder.
     */
    public EnvironmentBuilder authenticator(final String name) {

        authenticator = new Authenticator();
        authenticator.setClient(getClient());
        authenticator.setType(name);

        persist(authenticator);

        return this;
    }

    /**
     * Create a new user for this application.
     *
     * @return This builder.
     */
    public EnvironmentBuilder user() {
        return user(role);
    }

    /**
     * Create a new user with a specific role.
     *
     * @param role The role.
     * @return This builder.
     */
    public EnvironmentBuilder user(final Role role) {

        user = new User();
        user.setApplication(getApplication());
        user.setRole(role);

        persist(user);

        return this;
    }

    /**
     * Persist and track an entity.
     *
     * @param e The entity to persist.
     */
    public void persist(final AbstractEntity e) {

        // Set created/updated dates for all entities.
        if (e.getCreatedDate() == null) {
            e.setCreatedDate(Calendar.getInstance(UTC));
        }
        e.setModifiedDate(Calendar.getInstance(UTC));

        Transaction t = null;
        try {
            t = session.beginTransaction();
            session.saveOrUpdate(e);
            t.commit();
        } catch (Exception ex) {
            logger.error("Cannot persist", ex);
            if (t != null) {
                t.rollback();
            }
        }

        if (!trackedEntities.contains(e)) {
            trackedEntities.add(e);
        }

        // Persist all changes.
        session.flush();
    }

    /**
     * Add a login for the current user context.
     *
     * @param login    The user login.
     * @param password The user password.
     * @return This builder.
     */
    public EnvironmentBuilder login(final String login, final String password) {
        userIdentity = new UserIdentity();
        userIdentity.setUser(getUser());
        userIdentity.setRemoteId(login);
        userIdentity.setSalt(PasswordUtil.createSalt());
        userIdentity.setPassword(PasswordUtil.hash(password,
                userIdentity.getSalt()));
        userIdentity.setAuthenticator(getAuthenticator());
        persist(userIdentity);

        return this;
    }

    /**
     * Add an identity with a specific name to the current user context.
     *
     * @param remoteIdentity The unique identity.
     * @return This builder.
     */
    public EnvironmentBuilder identity(final String remoteIdentity) {
        userIdentity = new UserIdentity();
        userIdentity.setUser(getUser());
        userIdentity.setRemoteId(remoteIdentity);
        userIdentity.setAuthenticator(getAuthenticator());
        persist(userIdentity);
        return this;
    }

    /**
     * Add an identity to the current user context.
     *
     * @return This builder.
     */
    public EnvironmentBuilder identity() {
        return identity(UUID.randomUUID().toString());
    }

    /**
     * Add an authorization code to the current client/redirect scope.
     *
     * @return This builder.
     */
    public EnvironmentBuilder authToken() {
        return token(OAuthTokenType.Authorization, false, null,
                redirectUri.toString(),
                null);
    }

    /**
     * Add a bearer token to this user.
     *
     * @return This builder.
     */
    public EnvironmentBuilder bearerToken() {
        return bearerToken((String[]) null);
    }

    /**
     * Add a scoped bearer token to this user.
     *
     * @param scopes The scopes to assign to this token.
     * @return This builder.
     */
    public EnvironmentBuilder bearerToken(final String scopes) {
        return token(OAuthTokenType.Bearer, false, scopes, null, null);
    }

    /**
     * Add a scoped bearer token for a specific user..
     *
     * @param client   The client to apply this to.
     * @param identity The identity to apply this to.
     * @param scopes   The scopes to assign to this token.
     * @return This builder.
     */
    public EnvironmentBuilder bearerToken(final Client client,
                                          final UserIdentity identity,
                                          final String scopes) {
        return token(client, identity, OAuthTokenType.Bearer, false,
                scopes, null, null);
    }

    /**
     * Add a scoped bearer token to this user.
     *
     * @param scopes The scopes to assign to this token.
     * @return This builder.
     */
    public EnvironmentBuilder bearerToken(final String... scopes) {
        return token(OAuthTokenType.Bearer,
                false,
                scopes != null
                        ? String.join(" ", (CharSequence[]) scopes)
                        : null,
                null, null);
    }

    /**
     * Add a scoped bearer token to this user.
     *
     * @param client The client for which to create this token.
     * @param scopes The scopes to assign to this token.
     * @return This builder.
     */
    public EnvironmentBuilder bearerToken(final Client client,
                                          final String... scopes) {
        return token(client, getUserIdentity(),
                OAuthTokenType.Bearer,
                false,
                String.join(" ", (CharSequence[]) scopes),
                null, null);
    }

    /**
     * Add a refresh token.
     *
     * @return This builder.
     */
    public EnvironmentBuilder refreshToken() {
        String scopes = String.join(" ", token.getScopes().keySet());
        return token(OAuthTokenType.Refresh, false, scopes,
                null, token);
    }

    /**
     * Customize a token.
     *
     * @param type        The token type.
     * @param expired     Whether it's expired.
     * @param scopeString The requested scope.
     * @param redirect    The redirect URL.
     * @param authToken   An optional auth token.
     * @return This builder.
     */
    public EnvironmentBuilder token(final OAuthTokenType type,
                                    final Boolean expired,
                                    final String scopeString,
                                    final String redirect,
                                    final OAuthToken authToken) {
        return token(getClient(), getUserIdentity(), type, expired,
                scopeString, redirect, authToken);
    }

    /**
     * Customize a token.
     *
     * @param client      The client for which this token should be created.
     * @param identity    The identity for this token.
     * @param type        The token type.
     * @param expired     Whether it's expired.
     * @param scopeString The requested scope.
     * @param redirect    The redirect URL.
     * @param authToken   An optional auth token.
     * @return This builder.
     */
    public EnvironmentBuilder token(final Client client,
                                    final UserIdentity identity,
                                    final OAuthTokenType type,
                                    final Boolean expired,
                                    final String scopeString,
                                    final String redirect,
                                    final OAuthToken authToken) {
        token = new OAuthToken();
        token.setTokenType(type);
        token.setClient(client);
        token.setAuthToken(getRefreshed(authToken));

        // Only non-client-credentials clients are associated with users.
        if (!token.getClient().getType().equals(ClientType.ClientCredentials)) {
            token.setIdentity(getUserIdentity());
        }

        if (!StringUtils.isEmpty(redirect)) {
            URI redirectUri = UriBuilder.fromUri(redirect).build();
            token.setRedirect(redirectUri);
        }

        // If expired, else use defaults.
        if (expired) {
            token.setExpiresIn(-100);
        } else {
            token.setExpiresIn(100);
        }

        // Split and attach the scopes.
        SortedMap<String, ApplicationScope> newScopes = new TreeMap<>();
        SortedMap<String, ApplicationScope> currentScopes = getScopes();
        if (!StringUtils.isEmpty(scopeString)) {
            for (String scope : scopeString.split(" ")) {
                newScopes.put(scope, currentScopes.get(scope));
            }
        }
        token.setScopes(newScopes);

        persist(token);
        return this;
    }

    /**
     * Add an identity claim.
     *
     * @param name  Name of the field.
     * @param value Value of the field.
     * @return This builder.
     */
    public EnvironmentBuilder claim(final String name, final String value) {
        // User identity has to be separately hydrated and persisted, in
        // order to make sure we get the latest entity.
        UserIdentity i = getUserIdentity();
        i.getClaims().putIfAbsent(name, value);
        persist(i);
        return this;
    }

    /**
     * Create an authenticator state on the present client.
     *
     * @return This builder.
     */
    public EnvironmentBuilder authenticatorState() {
        authenticatorState = new AuthenticatorState();
        authenticatorState.setAuthenticator(getAuthenticator());
        authenticatorState.setClientRedirect(redirectUri);
        persist(authenticatorState);
        return this;
    }

    /**
     * Set the owner for the current application.
     *
     * @param user The new owner.
     * @return This builder.
     */
    public EnvironmentBuilder owner(final User user) {
        Application a = getApplication();
        // Reload the owner from the current session.
        User sessionUser = transactionGet(User.class, user.getId());
        a.setOwner(sessionUser);

        // Since the session user may not be tracked by this environment
        // builder, we manually persist it.
        persist(a);

        return this;
    }

    /**
     * Clear all created entities from the database.
     */
    public void clear() {

        // Delete the entities in reverse order.
        for (int i = trackedEntities.size() - 1; i >= 0; i--) {
            AbstractEntity e = trackedEntities.get(i);

            // First, evict the entity.
            session.evict(e);

            // Now, reload it.
            e = session.get(e.getClass(), e.getId());

            // Is it still in the database?
            if (e != null) {
                Transaction t = null;
                try {
                    t = session.beginTransaction();
                    session.delete(e);
                    t.commit();
                } catch (Exception exception) {
                    logger.error("Cannot clean", exception);
                    if (t != null) {
                        t.rollback();
                    }
                }
            }
        }
        trackedEntities.clear();

        application = null;
        scopes.clear();
        scope = null;
        role = null;
        client = null;
        authenticator = null;
        user = null;
        userIdentity = null;
        token = null;
        redirectUri = null;
        referrerUri = null;
        authenticatorState = null;
    }

    /**
     * Null-safe refresh of the passed entity.
     *
     * @param e   Entity to refresh.
     * @param <T> An AbstractEntity.
     * @return The entity, refreshed, or null.
     */
    private <T extends AbstractEntity> T getRefreshed(final T e) {
        if (e != null) {
            List<T> entities = new ArrayList<T>();
            entities.add(e);
            getRefreshed(entities);
        }
        return e;
    }

    /**
     * Null-safe refresh of the passed entity list.
     *
     * @param entities Entity list to refresh.
     * @param <T>      An AbstractEntity.
     * @return The entity, refreshed, or null.
     */
    private <T extends AbstractEntity> Collection<T> getRefreshed(
            final Collection<T> entities) {

        Transaction t = session.beginTransaction();
        for (AbstractEntity e : entities) {
            session.refresh(e);
        }
        t.commit();

        return entities;
    }

    /**
     * Null-safe refresh of the passed Map.
     *
     * @param entities Entity list to refresh.
     * @param <T>      An AbstractEntity.
     * @return The entity, refreshed, or null.
     */
    private <T extends AbstractEntity> SortedMap<String, T> getRefreshed(
            final SortedMap<String, T> entities) {
        getRefreshed(entities.values());
        return entities;
    }

    /**
     * Transaction based get() of a specific type and ID.
     *
     * @param type The type to retrieve.
     * @param id   The Unique DB ID.
     * @param <K>  The class of entity to retrieve.
     * @return The entity, refreshed, or null.
     */
    private <K extends AbstractEntity> K transactionGet(
            final Class<K> type, final UUID id) {

        Transaction t = session.beginTransaction();
        K entity = session.get(type, id);
        t.commit();
        return entity;
    }
}
