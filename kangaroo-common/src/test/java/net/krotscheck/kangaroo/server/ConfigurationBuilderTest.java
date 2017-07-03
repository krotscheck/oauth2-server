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

package net.krotscheck.kangaroo.server;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

/**
 * Unit tests for the ConfigurationBuilder.
 *
 * @author Michael Krotscheck
 */
public final class ConfigurationBuilderTest {

    /**
     * Assert that we can build with known commandline arguments.
     */
    @Test
    public void addCommandlineArgs() {
        Configuration config = new ConfigurationBuilder()
                .addCommandlineArgs(new String[]{
                        "-h=example.com",
                        "-p=9000",
                        "--kangaroo.keystore_path=/foo/bar",
                        "--kangaroo.keystore_password=keystore_password",
                        "--kangaroo.keystore_type=JKS",
                        "--kangaroo.cert_alias=cert_alias",
                        "--kangaroo.cert_key_password=key_password",
                        "--kangaroo.html_app_root=/var/www"
                })
                .build();

        Assert.assertEquals(config.getString(
                Config.HOST.getKey()), "example.com");
        Assert.assertEquals(config.getInt(
                Config.PORT.getKey()), 9000);
        Assert.assertEquals(config.getString(
                Config.KEYSTORE_PATH.getKey()), "/foo/bar");
        Assert.assertEquals(config.getString(
                Config.KEYSTORE_PASS.getKey()), "keystore_password");
        Assert.assertEquals(config.getString(
                Config.KEYSTORE_TYPE.getKey()), "JKS");
        Assert.assertEquals(config.getString(
                Config.CERT_ALIAS.getKey()), "cert_alias");
        Assert.assertEquals(config.getString(
                Config.CERT_KEY_PASS.getKey()), "key_password");
        Assert.assertEquals(config.getString(
                Config.HTML_APP_ROOT.getKey()), "/var/www");
    }

    /**
     * Assert that unknown commandline arguments are ignored.
     */
    @Test(expected = RuntimeException.class)
    public void addInvalidCommandlineArgs() {
        new ConfigurationBuilder()
                .addCommandlineArgs(new String[]{
                        "-l=invalid_field"
                })
                .build();
    }

    /**
     * Assert that lack of commandline arguments goes to defaults.
     */
    @Test
    public void addNoCommandlineArgs() {
        Configuration config = new ConfigurationBuilder()
                .addCommandlineArgs(new String[]{
                })
                .build();

        Assert.assertEquals(config.getString(
                Config.HOST.getKey()), "127.0.0.1");
        Assert.assertEquals(config.getInt(
                Config.PORT.getKey()), 8080);
        Assert.assertNull(config.getString(Config.KEYSTORE_PATH.getKey()));
        Assert.assertNull(config.getString(Config.KEYSTORE_PASS.getKey()));
        Assert.assertNull(config.getString(Config.KEYSTORE_TYPE.getKey()));
        Assert.assertNull(config.getString(Config.CERT_ALIAS.getKey()));
        Assert.assertNull(config.getString(Config.CERT_KEY_PASS.getKey()));
        Assert.assertNull(config.getString(Config.HTML_APP_ROOT.getKey()));
    }

    /**
     * Assert that we can add a properties file.
     */
    @Test
    public void addPropertiesFile() {
        URL filePath = this.getClass()
                .getResource("/config/test.properties");

        Configuration config = new ConfigurationBuilder()
                .addPropertiesFile(filePath.getPath())
                .build();

        Assert.assertEquals(config.getString(
                Config.HOST.getKey()), "example.com");
        Assert.assertEquals(config.getInt(
                Config.PORT.getKey()), 9000);
        Assert.assertEquals(config.getString(
                Config.KEYSTORE_PATH.getKey()), "/foo/bar");
        Assert.assertEquals(config.getString(
                Config.KEYSTORE_PASS.getKey()), "keystore_password");
        Assert.assertEquals(config.getString(
                Config.KEYSTORE_TYPE.getKey()), "JKS");
        Assert.assertEquals(config.getString(
                Config.CERT_ALIAS.getKey()), "cert_alias");
        Assert.assertEquals(config.getString(
                Config.CERT_KEY_PASS.getKey()), "key_password");
        Assert.assertEquals(config.getString(
                Config.HTML_APP_ROOT.getKey()), "/var/www");
    }

    /**
     * Assert that we can add a nonexistent properties file.
     */
    @Test
    public void addNonexistentPropertiesFile() {
        Configuration config = new ConfigurationBuilder()
                .addPropertiesFile("/config/nonexistent.properties")
                .build();

        Assert.assertEquals(config.getString(
                Config.HOST.getKey()), "127.0.0.1");
        Assert.assertEquals(config.getInt(
                Config.PORT.getKey()), 8080);
        Assert.assertNull(config.getString(Config.KEYSTORE_PATH.getKey()));
        Assert.assertNull(config.getString(Config.KEYSTORE_PASS.getKey()));
        Assert.assertNull(config.getString(Config.KEYSTORE_TYPE.getKey()));
        Assert.assertNull(config.getString(Config.CERT_ALIAS.getKey()));
        Assert.assertNull(config.getString(Config.CERT_KEY_PASS.getKey()));
        Assert.assertNull(config.getString(Config.HTML_APP_ROOT.getKey()));
    }

    /**
     * Assert that the system catches an invalid properties file.
     *
     * @throws Exception Should not be thrown.
     */
    @Test
    public void addMalformedPropertiesFile() throws Exception {
        File mockConfigFile = Mockito.mock(File.class);
        Mockito.doReturn(true).when(mockConfigFile).exists();

        Configuration config = new ConfigurationBuilder()
                .addPropertiesFile(mockConfigFile)
                .build();

        Assert.assertEquals(config.getString(
                Config.HOST.getKey()), "127.0.0.1");
        Assert.assertEquals(config.getInt(
                Config.PORT.getKey()), 8080);
        Assert.assertNull(config.getString(Config.KEYSTORE_PATH.getKey()));
        Assert.assertNull(config.getString(Config.KEYSTORE_PASS.getKey()));
        Assert.assertNull(config.getString(Config.KEYSTORE_TYPE.getKey()));
        Assert.assertNull(config.getString(Config.CERT_ALIAS.getKey()));
        Assert.assertNull(config.getString(Config.CERT_KEY_PASS.getKey()));
        Assert.assertNull(config.getString(Config.HTML_APP_ROOT.getKey()));
    }

    /**
     * Assert that system parameters are loaded.
     */
    @Test
    public void testSystemParameters() {
        Configuration config = new ConfigurationBuilder()
                .build();

        SystemConfiguration c = new SystemConfiguration();
        Iterator<String> iterator = c.getKeys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Assert.assertEquals(
                    config.getString(key),
                    c.getString(key)
            );
        }
    }

    /**
     * Assert that environment parameters are loaded.
     */
    @Test
    public void testEnvParameters() {
        Configuration config = new ConfigurationBuilder()
                .build();

        EnvironmentConfiguration c = new EnvironmentConfiguration();
        Iterator<String> iterator = c.getKeys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Assert.assertEquals(
                    config.getString(key),
                    c.getString(key)
            );
        }
    }
}
