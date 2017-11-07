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

package net.krotscheck.kangaroo.authz.common.database.entity;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import net.krotscheck.kangaroo.authz.common.database.entity.User.Deserializer;
import net.krotscheck.kangaroo.common.hibernate.id.IdUtil;
import net.krotscheck.kangaroo.common.jackson.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Test the user entity.
 *
 * @author Michael Krotscheck
 */
public final class UserTest {

    /**
     * Assert that we can get and set the application to which this user
     * belongs.
     */
    @Test
    public void testGetSetApplication() {
        User u = new User();
        Application a = new Application();

        Assert.assertNull(u.getApplication());
        u.setApplication(a);
        Assert.assertEquals(a, u.getApplication());
    }

    /**
     * Assert that we can get and set the applications which this user owns.
     */
    @Test
    public void testGetSetApplications() {
        User u = new User();
        List<Application> applications = new ArrayList<>();
        applications.add(new Application());
        applications.add(new Application());

        Assert.assertEquals(0, u.getIdentities().size());
        u.setApplications(applications);
        Assert.assertEquals(applications, u.getApplications());
        Assert.assertNotSame(applications, u.getApplications());
    }

    /**
     * Assert that we can get and set the user's role.
     */
    @Test
    public void testGetSetRole() {
        User user = new User();
        Role role = new Role();

        Assert.assertNull(user.getRole());
        user.setRole(role);
        Assert.assertEquals(role, user.getRole());
    }

    /**
     * Assert that we can get and set the identities.
     */
    @Test
    public void testGetSetIdentities() {
        List<UserIdentity> identities = new ArrayList<>();
        identities.add(new UserIdentity());
        User user = new User();

        Assert.assertEquals(0, user.getIdentities().size());
        user.setIdentities(identities);
        Assert.assertEquals(identities, user.getIdentities());
        Assert.assertNotSame(identities, user.getIdentities());
    }

    /**
     * Assert that we retrieve the owner from the parent client.
     */
    @Test
    public void testGetOwner() {
        User user = new User();
        Application spy = spy(new Application());

        // Null check
        Assert.assertNull(user.getOwner());

        user.setApplication(spy);
        user.getOwner();

        Mockito.verify(spy).getOwner();
    }

    /**
     * Assert that this entity can be serialized into a JSON object, and
     * doesn't
     * carry an unexpected payload.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testJacksonSerializable() throws Exception {
        List<UserIdentity> identities = new ArrayList<>();
        UserIdentity identity = new UserIdentity();
        identity.setId(IdUtil.next());
        identities.add(identity);

        Role role = new Role();
        role.setId(IdUtil.next());

        Application application = new Application();
        application.setId(IdUtil.next());

        User user = new User();
        user.setId(IdUtil.next());
        user.setCreatedDate(Calendar.getInstance());
        user.setModifiedDate(Calendar.getInstance());
        user.setApplication(application);
        user.setRole(role);
        user.setIdentities(identities);

        // De/serialize to json.
        ObjectMapper m = new ObjectMapperFactory().get();
        DateFormat format = new ISO8601DateFormat();
        String output = m.writeValueAsString(user);
        JsonNode node = m.readTree(output);

        Assert.assertEquals(
                IdUtil.toString(user.getId()),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(user.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(user.getModifiedDate().getTime()),
                node.get("modifiedDate").asText());

        Assert.assertEquals(
                IdUtil.toString(user.getRole().getId()),
                node.get("role").asText());
        Assert.assertEquals(
                IdUtil.toString(user.getApplication().getId()),
                node.get("application").asText());

        // Should not be serialized.
        Assert.assertFalse(node.has("identities"));

        // Enforce a given number of items.
        List<String> names = new ArrayList<>();
        Iterator<String> nameIterator = node.fieldNames();
        while (nameIterator.hasNext()) {
            names.add(nameIterator.next());
        }
        Assert.assertEquals(5, names.size());
    }

    /**
     * Assert that this entity can be deserialized from a JSON object.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testJacksonDeserializable() throws Exception {
        ObjectMapper m = new ObjectMapperFactory().get();
        DateFormat format = new ISO8601DateFormat();
        ObjectNode node = m.createObjectNode();
        node.put("id", IdUtil.toString(IdUtil.next()));
        node.put("createdDate",
                format.format(Calendar.getInstance().getTime()));
        node.put("modifiedDate",
                format.format(Calendar.getInstance().getTime()));

        String output = m.writeValueAsString(node);
        User user = m.readValue(output, User.class);

        Assert.assertEquals(
                IdUtil.toString(user.getId()),
                node.get("id").asText());
        Assert.assertEquals(
                format.format(user.getCreatedDate().getTime()),
                node.get("createdDate").asText());
        Assert.assertEquals(
                format.format(user.getModifiedDate().getTime()),
                node.get("modifiedDate").asText());
    }


    /**
     * Test the user deserializer.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testDeserializeSimple() throws Exception {
        BigInteger newId = IdUtil.next();
        String id = String.format("\"%s\"", IdUtil.toString(newId));
        JsonFactory f = new JsonFactory();
        JsonParser preloadedParser = f.createParser(id);
        preloadedParser.nextToken(); // Advance to the first value.

        Deserializer deserializer = new Deserializer();
        User u = deserializer.deserialize(preloadedParser,
                mock(DeserializationContext.class));

        Assert.assertEquals(newId, u.getId());
    }
}
