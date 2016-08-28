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

package net.krotscheck.kangaroo.database.entity;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.krotscheck.kangaroo.database.deserializer.AbstractEntityReferenceDeserializer;
import net.krotscheck.kangaroo.database.filters.UUIDFilter;
import net.krotscheck.kangaroo.database.jackson.Views;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FilterCacheModeType;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.FullTextFilterDefs;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A user's identity, as provided by authenticators. Note that a user may have
 * more than one identity from an authenticator- for instance, a user may link
 * to two separate gmail accounts.
 *
 * @author Michael Krotscheck
 */
@Entity
@Indexed(index = "user_identities")
@Table(name = "user_identities")
@FullTextFilterDefs({
        @FullTextFilterDef(name = "uuid_identity_owner",
                impl = UUIDFilter.class,
                cache = FilterCacheModeType.INSTANCE_ONLY),
        @FullTextFilterDef(name = "uuid_identity_user",
                impl = UUIDFilter.class,
                cache = FilterCacheModeType.INSTANCE_ONLY),
        @FullTextFilterDef(name = "uuid_identity_authenticator",
                impl = UUIDFilter.class,
                cache = FilterCacheModeType.INSTANCE_ONLY)
})
public final class UserIdentity extends AbstractEntity {

    /**
     * The user to whom this identity record belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user", nullable = false, updatable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = User.Deserializer.class)
    @ContainedIn
    @JsonView(Views.Public.class)
    @IndexedEmbedded(includePaths = {"id", "application.owner.id"})
    private User user;

    /**
     * The authenticator that provided this identity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authenticator", nullable = false, updatable = false)
    @JsonIdentityReference(alwaysAsId = true)
    @JsonDeserialize(using = Authenticator.Deserializer.class)
    @JsonView(Views.Public.class)
    @IndexedEmbedded(includePaths = {"id"})
    private Authenticator authenticator;

    /**
     * OAuth tokens issued to this identity.
     */
    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "identity",
            cascade = {CascadeType.REMOVE, CascadeType.MERGE},
            orphanRemoval = true
    )
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ContainedIn
    private List<OAuthToken> tokens;

    /**
     * The user's remote ID- the ID by which the authenticator would recognize
     * this user.
     */
    @Basic(optional = false)
    @Column(name = "remoteId", nullable = false, updatable = false)
    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    @JsonView(Views.Public.class)
    private String remoteId;

    /**
     * The claims about the identity of this user, made by the authenticator.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_identity_claims",
            joinColumns = @JoinColumn(name = "user_identity"))
    @MapKeyColumn(name = "claimKey")
    @Column(name = "claimValue")
    @Field(name = "claims",
            index = Index.YES, analyze = Analyze.YES, store = Store.NO)
    @IndexedEmbedded
    @JsonView(Views.Public.class)
    private Map<String, String> claims = new HashMap<>();

    /**
     * The user's password salt.
     */
    @Basic
    @Column(name = "salt")
    @JsonView(Views.Secure.class)
    private String salt;

    /**
     * The user's hashed password.
     */
    @Basic
    @Column(name = "password")
    @JsonView(Views.Secure.class)
    private String password;

    /**
     * Get the user record for this identity.
     *
     * @return The user record.
     */
    public User getUser() {
        return user;
    }

    /**
     * Set the user for this identity.
     *
     * @param user This identity's user.
     */
    public void setUser(final User user) {
        this.user = user;
    }

    /**
     * The authenticator which provided this identity.
     *
     * @return The authenticator.
     */
    public Authenticator getAuthenticator() {
        return authenticator;
    }

    /**
     * Set the authenticator.
     *
     * @param authenticator A new authenticator.
     */
    public void setAuthenticator(final Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * The remote ID by which the authenticator recognizes the user.
     *
     * @return A string, representing this authenticator.
     */
    public String getRemoteId() {
        return remoteId;
    }

    /**
     * Set the remote ID by which the authenticator recognizes the user.
     *
     * @param remoteId The remote ID.
     */
    public void setRemoteId(final String remoteId) {
        this.remoteId = remoteId;
    }

    /**
     * Retrieve identity claims made by the authenticator.
     *
     * @return A set of claims, such as email and name.
     */
    public Map<String, String> getClaims() {
        return claims;
    }

    /**
     * Set the claims made by the authenticator.
     *
     * @param claims The claims.
     */
    public void setClaims(final Map<String, String> claims) {
        this.claims = new HashMap<>(claims);
    }

    /**
     * Get all the tokens issued to this identity.
     *
     * @return A list of all tokens (lazy loaded)
     */
    public List<OAuthToken> getTokens() {
        return tokens;
    }

    /**
     * Set the list of tokens for this user.
     *
     * @param tokens A new list of tokens.
     */
    public void setTokens(final List<OAuthToken> tokens) {
        this.tokens = new ArrayList<>(tokens);
    }

    /**
     * Get the salt.
     *
     * @return The current salt.
     */
    public String getSalt() {
        return salt;
    }

    /**
     * Set a new salt.
     *
     * @param salt A new salt, should be 32 characters long.
     */
    public void setSalt(final String salt) {
        this.salt = salt;
    }

    /**
     * Get the password.
     *
     * @return An encrypted password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set a new password. Make sure this is encrypted first.
     *
     * @param password The new password.
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * The owner of this entity.
     *
     * @return This entity's owner, if it exists.
     */
    @Override
    @Transient
    @JsonIgnore
    public User getOwner() {
        if (user != null) {
            return user.getOwner();
        }
        return null;
    }

    /**
     * Deserialize a reference to an User.
     *
     * @author Michael Krotschecks
     */
    public static final class Deserializer
            extends AbstractEntityReferenceDeserializer<UserIdentity> {

        /**
         * Constructor.
         */
        public Deserializer() {
            super(UserIdentity.class);
        }
    }
}
