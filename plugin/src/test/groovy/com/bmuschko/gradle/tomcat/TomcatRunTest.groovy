/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmuschko.gradle.tomcat

import com.bmuschko.gradle.tomcat.embedded.Tomcat6xServer
import com.bmuschko.gradle.tomcat.embedded.TomcatServer
import com.bmuschko.gradle.tomcat.extension.TomcatPluginExtension
import com.bmuschko.gradle.tomcat.tasks.TomcatRun
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.util.GFileUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test case for TomcatRun task.
 */
class TomcatRunTest {
    private final File testDir = new File("build/tmp/tests")
    private Project project
    private TomcatRun tomcatRun

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().withProjectDir(testDir).build()
        tomcatRun = project.tasks.create(TomcatPlugin.TOMCAT_RUN_TASK_NAME, TomcatRun.class)
    }

    @AfterEach
    void tearDown() {
        tomcatRun = null

        if(testDir.exists()) {
            testDir.deleteDir()
        }
    }

    @Test
    public void testValidateConfigurationForExistentWebAppSourceDirectory() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.validateConfiguration()
        assert tomcatRun.getWebDefaultXml() == null
        assert tomcatRun.getConfigFile() == null
        assert tomcatRun.getWebAppSourceDirectory() == webAppSourceDir
        assert tomcatRun.additionalRuntimeResources == []
    }

    @Test
    public void testValidateConfigurationForExistentWebDefaultXml() {
        File webAppSourceDir = createWebAppSourceDirectory()
        File webDefaultXml = createWebDefaultXml()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setWebDefaultXml webDefaultXml
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.validateConfiguration()
        assert tomcatRun.getWebDefaultXml() == webDefaultXml
        assert tomcatRun.getConfigFile() == null
        assert tomcatRun.getWebAppSourceDirectory() == webAppSourceDir
        assert tomcatRun.additionalRuntimeResources == []
    }

    @Test
    public void testValidateConfigurationForExistentConfigFile() {
        File webAppSourceDir = createWebAppSourceDirectory()
        File configFile = createConfigFile()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setConfigFile configFile
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.validateConfiguration()
        assert tomcatRun.getWebDefaultXml() == null
        assert tomcatRun.getConfigFile() == configFile
        assert tomcatRun.getWebAppSourceDirectory() == webAppSourceDir
        assert tomcatRun.additionalRuntimeResources == []
    }

    @Test
    public void testValidateConfigurationForExistentDefaultConfigFile() {
        File webAppSourceDir = createWebAppSourceDirectory()
        File defaultConfigFile = createDefaultConfigFile(webAppSourceDir)
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.validateConfiguration()
        assert tomcatRun.getWebDefaultXml() == null
        assert tomcatRun.getResolvedConfigFile().path == defaultConfigFile.toURI().toURL().path
        assert tomcatRun.getWebAppSourceDirectory() == webAppSourceDir
        assert tomcatRun.additionalRuntimeResources == []
    }

    @Test
    void testValidateConfigurationForEnabledSSLButNoKeystoreConfigured() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.enableSSL = true
        tomcatRun.validateConfiguration()
        assert tomcatRun.getEnableSSL() == true
        assert tomcatRun.getKeystoreFile() == null
        assert tomcatRun.getKeystorePass() == null
    }

    @Test
    void testValidateConfigurationForEnabledSSLButOnlyKeystoreFileConfigured() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setKeystoreFile createFile(testDir, "keystore.jks")
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.enableSSL = true
        assertThrows(InvalidUserDataException.class, () -> tomcatRun.validateConfiguration());
    }

    @Test
    void testValidateConfigurationForEnabledSSLButOnlyKeystorePasswordConfigured() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setKeystorePass 'pwd'
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.enableSSL = true
        assertThrows(InvalidUserDataException.class, () -> tomcatRun.validateConfiguration());
    }

    @Test
    void testValidateConfigurationForEnabledSSLForKeystoreFileAndPasswordConfigured() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setKeystoreFile createFile(testDir, "keystore.jks")
        tomcatRun.setKeystorePass 'pwd'
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.enableSSL = true
        tomcatRun.validateConfiguration()
        assert tomcatRun.getEnableSSL() == true
        assert tomcatRun.getKeystoreFile() == new File(testDir, "keystore.jks")
        assert tomcatRun.getKeystorePass() == 'pwd'
    }
    
    @Test
    void testValidateConfigurationForEnabledSSLButNoTruststoreConfigured() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.enableSSL = true
        tomcatRun.validateConfiguration()
        assert tomcatRun.getEnableSSL() == true
        assert tomcatRun.getTruststoreFile() == null
        assert tomcatRun.getTruststorePass() == null
    }

    @Test
    void testValidateConfigurationForEnabledSSLButOnlyTruststoreFileConfigured() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setTruststoreFile createFile(testDir, "truststore.jks")
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.enableSSL = true
        assertThrows(InvalidUserDataException.class, () -> tomcatRun.validateConfiguration());
    }

    @Test
    void testValidateConfigurationForEnabledSSLButOnlyTruststorePasswordConfigured() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setTruststorePass 'pwd'
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.enableSSL = true
        assertThrows(InvalidUserDataException.class, () -> tomcatRun.validateConfiguration());
    }

    @Test
    void testValidateConfigurationForEnabledSSLForTruststoreFileAndPasswordConfigured() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setTruststoreFile createFile(testDir, "truststore.jks")
        tomcatRun.setTruststorePass 'pwd'
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.enableSSL = true
        tomcatRun.validateConfiguration()
        assert tomcatRun.getEnableSSL() == true
        assert tomcatRun.getTruststoreFile() == new File(testDir, "truststore.jks")
        assert tomcatRun.getTruststorePass() == 'pwd'
    }
    
    @Test
    void testValidateConfigurationForEnabledSSLButIllegalClientAuthValue() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setTruststoreFile createFile(testDir, "truststore.jks")
        tomcatRun.setTruststorePass 'pwd'
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.enableSSL = true
        tomcatRun.clientAuth = "This is not a valid clientAuth value"
        assertThrows(InvalidUserDataException.class, () -> tomcatRun.validateConfiguration());
    }
    
    @Test
    void testValidateConfigurationForEnabledSSLAndClientAuthValueFalse() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setTruststoreFile createFile(testDir, "truststore.jks")
        tomcatRun.setTruststorePass 'pwd'
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.enableSSL = true
        tomcatRun.clientAuth = "false"
        tomcatRun.validateConfiguration()
        assert tomcatRun.getEnableSSL() == true
        assert tomcatRun.getTruststoreFile() == new File(testDir, "truststore.jks")
        assert tomcatRun.getTruststorePass() == 'pwd'
        assert tomcatRun.clientAuth == "false"
    }
    
    @Test
    void testValidateConfigurationForEnabledSSLAndClientAuthValueTrue() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setTruststoreFile createFile(testDir, "truststore.jks")
        tomcatRun.setTruststorePass 'pwd'
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.enableSSL = true
        tomcatRun.clientAuth = "true"
        tomcatRun.validateConfiguration()
        assert tomcatRun.getEnableSSL() == true
        assert tomcatRun.getTruststoreFile() == new File(testDir, "truststore.jks")
        assert tomcatRun.getTruststorePass() == 'pwd'
        assert tomcatRun.clientAuth == "true"
    }
    
    @Test
    void testValidateConfigurationForEnabledSSLAndClientAuthValueWant() {
        File webAppSourceDir = createWebAppSourceDirectory()
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setTruststoreFile createFile(testDir, "truststore.jks")
        tomcatRun.setTruststorePass 'pwd'
        tomcatRun.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRun.enableSSL = true
        tomcatRun.clientAuth = "want"
        tomcatRun.validateConfiguration()
        assert tomcatRun.getEnableSSL() == true
        assert tomcatRun.getTruststoreFile() == new File(testDir, "truststore.jks")
        assert tomcatRun.getTruststorePass() == 'pwd'
        assert tomcatRun.clientAuth == "want"
    }

    @Test
    public void testSetWebApplicationContextForFullContextPath() {
        File webAppSourceDir = createWebAppSourceDirectory()
        String contextPath = "/app"
        TomcatServer server = new Tomcat6xServer()
        tomcatRun.setServer server
        tomcatRun.setContextPath contextPath
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setWebApplicationContext()
        assert tomcatRun.getServer() == server
        assert tomcatRun.getServer().getContext().getDocBase() == webAppSourceDir.getCanonicalPath()
        assert tomcatRun.getServer().getContext().getPath() == contextPath
    }

    @Test
    public void testSetWebApplicationContextForContextPathWithoutLeadingSlash() {
        File webAppSourceDir = createWebAppSourceDirectory()
        String contextPath = "app"
        TomcatServer server = new Tomcat6xServer()
        tomcatRun.setServer server
        tomcatRun.setContextPath contextPath
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setWebApplicationContext()
        assert tomcatRun.getServer() == server
        assert tomcatRun.getServer().getContext().getDocBase() == webAppSourceDir.getCanonicalPath()
        assert tomcatRun.getServer().getContext().getPath() == "/" + contextPath
    }

    @Test
    public void testConfigureWebApplication() {
        File webAppSourceDir = createWebAppSourceDirectory()
        String contextPath = "app"
        TomcatServer server = new Tomcat6xServer()
        tomcatRun.setServer server
        tomcatRun.setContextPath contextPath
        tomcatRun.setWebAppSourceDirectory webAppSourceDir
        tomcatRun.setWebAppClasspath project.files("jars")
        tomcatRun.reloadable = true
        tomcatRun.configureWebApplication()
        assert tomcatRun.getServer() == server
        assert tomcatRun.getServer().getContext().getDocBase() == webAppSourceDir.getCanonicalPath()
        assert tomcatRun.getServer().getContext().getPath() == "/" + contextPath
        assert tomcatRun.getServer().getContext().getReloadable() == true
        assert tomcatRun.getServer().getContext().getLoader().getRepositories().size() == 0
    }

    @Test
    public void testConfigureAdditionalClasspathForExistingDirectoryAndJarFile() {
        File propsDir = new File(testDir, 'tmp/props')
        GFileUtils.mkdirs(propsDir)
        File jarFile = new File(testDir, 'tmp/my.jar')
        GFileUtils.touch(jarFile)
        createBasicTomcatServer()
        tomcatRun.additionalRuntimeResources = [propsDir, jarFile]
        tomcatRun.configureWebApplication()
        assert tomcatRun.getServer().getContext().getLoader().getRepositories().size() == 2
    }

    @Test
    public void testConfigureAdditionalClasspathForNonExistingDirectoryAndJarFile() {
        File propsDir = new File(testDir, 'tmp/props')
        File jarFile = new File(testDir, 'tmp/my.jar')
        createBasicTomcatServer()
        tomcatRun.additionalRuntimeResources = [propsDir, jarFile]
        tomcatRun.configureWebApplication()
        assert tomcatRun.getServer().getContext().getLoader().getRepositories().size() == 0
    }

    @Test
    public void testSetWebApplicationContextForRootContextUrl() {
        String contextPath = '/'
        tomcatRun.setContextPath contextPath
        assert tomcatRun.getFullContextPath() == ''
    }

    @Test
    public void testSetWebApplicationContextForBlankContextUrl() {
        String contextPath = ''
        tomcatRun.setContextPath contextPath
        assert tomcatRun.getFullContextPath() == ''
    }

    @Test
    public void testSetWebApplicationContextForContextUrlWithLeadingSlash() {
        String contextPath = '/app'
        tomcatRun.setContextPath contextPath
        assert tomcatRun.getFullContextPath() == '/app'
    }

    @Test
    public void testSetWebApplicationContextForContextUrlWithoutLeadingSlash() {
        String contextPath = 'app'
        tomcatRun.setContextPath contextPath
        assert tomcatRun.getFullContextPath() == '/app'
    }

    private File createWebAppSourceDirectory() {
        File webAppSourceDir = new File(testDir, "webapp")
        boolean success = webAppSourceDir.mkdirs()

        if(!success) {
            fail "Unable to create web app source directory"
        }

        webAppSourceDir
    }

    private File createWebDefaultXml() {
        File webDefaultXml = new File(testDir, "web.xml")
        boolean success = webDefaultXml.createNewFile()

        if(!success) {
            fail "Unable to create web default XML"
        }

        webDefaultXml
    }

    private File createConfigFile() {
        File configFile = new File(testDir, "context.xml")
        boolean success = configFile.createNewFile()

        if(!success) {
            fail "Unable to create config file"
        }

        configFile
    }

    private File createDefaultConfigFile(File webAppSourceDir) {
        File metaInfDir = new File(webAppSourceDir, "META-INF")
        metaInfDir.mkdir()
        File defaultConfigFile = new File(metaInfDir, "context.xml")
        boolean success = defaultConfigFile.createNewFile()

        if(!success) {
            fail "Unable to create default config file"
        }

        defaultConfigFile
    }

    private File createFile(File dir, String filename) {
        File file = new File(dir, filename)
        boolean success = file.createNewFile()

        if(!success) {
            fail "Unable to create file: ${filename} in directory ${dir.canonicalPath}"
        }

        file
    }

    private TomcatServer createBasicTomcatServer() {
        TomcatServer server = new Tomcat6xServer()
        tomcatRun.server = server
        tomcatRun.contextPath = '/'
        tomcatRun.webAppClasspath = project.files()
        server
    }
}
