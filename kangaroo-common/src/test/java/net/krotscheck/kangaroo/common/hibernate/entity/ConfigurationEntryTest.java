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

package net.krotscheck.kangaroo.common.hibernate.entity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test the configuration entry entity.
 *
 * @author Michael Krotscheck
 */
public final class ConfigurationEntryTest {

    /**
     * Test getting/setting the section.
     */
    @Test
    public void testGetSetSection() {
        ConfigurationEntry entry = new ConfigurationEntry();

        assertNull(entry.getSection());
        entry.setSection("section");
        assertEquals("section", entry.getSection());
    }

    /**
     * Test getting/setting the key.
     */
    @Test
    public void testGetSetKey() {
        ConfigurationEntry entry = new ConfigurationEntry();

        assertNull(entry.getKey());
        entry.setKey("key");
        assertEquals("key", entry.getKey());
    }

    /**
     * Test getting/setting the value.
     */
    @Test
    public void testGetSetValue() {
        ConfigurationEntry entry = new ConfigurationEntry();

        assertNull(entry.getValue());
        entry.setValue("value");
        assertEquals("value", entry.getValue());
    }
}
