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

package net.krotscheck.kangaroo.util;

import net.krotscheck.kangaroo.common.exception.exception.HttpInvalidFieldException;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Unit tests for our ParamUtil.
 *
 * @author Michael Krotscheck
 */
public final class ParamUtilTest {

    /**
     * Assert that the constructor is private.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor c = ParamUtil.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(c.getModifiers()));

        // Create a new instance for coverage.
        c.setAccessible(true);
        c.newInstance();
    }

    /**
     * Assert that testOne will always return one value.
     */
    @Test
    public void testGetOne() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("foo", "bar");

        String result = ParamUtil.getOne(params, "foo");
        Assert.assertEquals("bar", result);
    }

    /**
     * Assert that testOne throws an exception if no value exists to retrieve.
     */
    @Test(expected = HttpInvalidFieldException.class)
    public void testGetOneNoValue() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        ParamUtil.getOne(params, "does_not_exist");
    }

    /**
     * Assert that testOne throws an exception if more than one value exists.
     */
    @Test(expected = HttpInvalidFieldException.class)
    public void testGetOneMultiResult() {
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.add("foo", "one");
        params.add("foo", "two");

        ParamUtil.getOne(params, "foo");
    }
}