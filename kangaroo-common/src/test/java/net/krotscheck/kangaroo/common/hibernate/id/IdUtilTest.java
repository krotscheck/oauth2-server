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

package net.krotscheck.kangaroo.common.hibernate.id;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import java.security.Security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * ID utility tests.
 *
 * @author Michael Krotscheck
 */
public class IdUtilTest {

    /**
     * Assert that the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = IdUtil.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isProtected(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert that a security exception is rethrown.
     *
     * @throws Exception Should not be thrown.
     */
    @Test(expected = RuntimeException.class)
    public void testSecureRandomThrows() throws Exception {
        String key = "securerandom.strongAlgorithms";
        String propValue = Security.getProperty(key);
        Security.setProperty(key, "");

        try {
            new IdUtil();
        } finally {
            Security.setProperty(key, propValue);
        }
    }

    /**
     * Assert that generated ID's are 16 characters long.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testIdCorrectLength() throws Exception {
        byte[] id = IdUtil.next();
        assertEquals(16, id.length);
    }

    /**
     * Assert that we can convert an ID to a string and back.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testIdStringConvert() throws Exception {
        byte[] id = IdUtil.next();
        String idString = IdUtil.toString(id);
        assertArrayEquals(id, IdUtil.fromString(idString));
    }

    /**
     * Assert that null values are passed straight back out.
     */
    @Test
    public void testNullSafeStringConvert() {
        assertNull(IdUtil.toString(null));
        assertNull(IdUtil.fromString(null));
    }

    /**
     * Assert that a malformed string cannot be converted back to a byte[].
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMalformedIdFromString() {
        IdUtil.fromString("notBase16String");
    }

    /**
     * Assert that a too short string cannot be converted back to a byte[].
     */
    @Test(expected = IllegalArgumentException.class)
    public void testTooShortIdFromString() {
        IdUtil.fromString("4214");
    }

    /**
     * Assert that a too long string cannot be converted back to a byte[].
     */
    @Test(expected = IllegalArgumentException.class)
    public void testTooLongIdFromString() {
        IdUtil.fromString("0123456789012345678901234567890123456789");
    }

    /**
     * Assert that a byte array that's too short cannot be converted.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testTooShortIdToString() {
        byte[] shortId = new byte[2];
        new SecureRandom().nextBytes(shortId);
        IdUtil.toString(shortId);
    }

    /**
     * Assert that a byte array that's too long cannot be converted.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testTooLongIdToString() {
        byte[] shortId = new byte[1000];
        new SecureRandom().nextBytes(shortId);
        IdUtil.toString(shortId);
    }
}