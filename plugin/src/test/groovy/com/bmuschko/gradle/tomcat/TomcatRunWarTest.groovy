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

import org.gradle.api.Project
import com.bmuschko.gradle.tomcat.embedded.Tomcat6xServer
import com.bmuschko.gradle.tomcat.embedded.TomcatServer
import com.bmuschko.gradle.tomcat.extension.TomcatPluginExtension
import com.bmuschko.gradle.tomcat.tasks.TomcatRunWar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test case for TomcatRunWar task.
 */
class TomcatRunWarTest {
    private final File testDir = new File("build/tmp/tests")
    private Project project
    private TomcatRunWar tomcatRunWar

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().withProjectDir(testDir).build()
        tomcatRunWar = project.tasks.create(TomcatPlugin.TOMCAT_RUN_WAR_TASK_NAME, TomcatRunWar.class)
    }

    @AfterEach
    void tearDown() {
        tomcatRunWar = null

        if(testDir.exists()) {
            testDir.deleteDir()
        }
    }

    @Test
    public void testValidateConfigurationForExistentWebApp() {
        File webAppDir = createWebAppDir()
        File war = createWar(webAppDir)
        tomcatRunWar.setWebApp war
        tomcatRunWar.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRunWar.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRunWar.validateConfiguration()
        assert tomcatRunWar.getWebDefaultXml() == null
        assert tomcatRunWar.getConfigFile() == null
        assert tomcatRunWar.getWebApp() == war
    }

    @Test
    public void testValidateConfigurationForExistentWebDefaultXml() {
        File webAppDir = createWebAppDir()
        File war = createWar(webAppDir)
        File webDefaultXml = createWebDefaultXml()
        tomcatRunWar.setWebApp war
        tomcatRunWar.setWebDefaultXml webDefaultXml
        tomcatRunWar.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRunWar.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRunWar.validateConfiguration()
        assert tomcatRunWar.getWebDefaultXml() == webDefaultXml
        assert tomcatRunWar.getConfigFile() == null
        assert tomcatRunWar.getWebApp() == war
    }

    @Test
    public void testValidateConfigurationForExistentConfigFile() {
        File webAppDir = createWebAppDir()
        File war = createWar(webAppDir)
        File configFile = createConfigFile()
        tomcatRunWar.setWebApp war
        tomcatRunWar.setConfigFile configFile
        tomcatRunWar.setHttpProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRunWar.setHttpsProtocol TomcatPluginExtension.DEFAULT_PROTOCOL_HANDLER
        tomcatRunWar.validateConfiguration()
        assert tomcatRunWar.getWebDefaultXml() == null
        assert tomcatRunWar.getConfigFile() == configFile
        assert tomcatRunWar.getWebApp() == war
    }

    @Test
    public void testSetWebApplicationContextForFullContextPath() {
        File webAppDir = createWebAppDir()
        File configFile = createConfigFile()
        String contextPath = "/app"
        TomcatServer server = new Tomcat6xServer()
        tomcatRunWar.setServer server
        tomcatRunWar.setContextPath contextPath
        tomcatRunWar.setWebApp webAppDir
        tomcatRunWar.setConfigFile configFile
        tomcatRunWar.setWebApplicationContext()
        assert tomcatRunWar.getServer() == server
        assert tomcatRunWar.getServer().getContext().getDocBase() == webAppDir.getCanonicalPath()
        assert tomcatRunWar.getServer().getContext().getPath() == contextPath
    }

    @Test
    public void testSetWebApplicationContextForContextPathWithoutLeadingSlash() {
        File webAppDir = createWebAppDir()
        File configFile = createConfigFile()
        String contextPath = "app"
        TomcatServer server = new Tomcat6xServer()
        tomcatRunWar.setServer server
        tomcatRunWar.setContextPath contextPath
        tomcatRunWar.setWebApp webAppDir
        tomcatRunWar.setConfigFile configFile
        tomcatRunWar.setWebApplicationContext()
        assert tomcatRunWar.getServer() == server
        assert tomcatRunWar.getServer().getContext().getDocBase() == webAppDir.getCanonicalPath()
        assert tomcatRunWar.getServer().getContext().getPath() == "/" + contextPath
    }

    @Test
    public void testSetWebApplicationContextForRootContextUrl() {
        String contextPath = '/'
        tomcatRunWar.setContextPath contextPath
        assert tomcatRunWar.getFullContextPath() == ''
    }

    @Test
    public void testSetWebApplicationContextForBlankContextUrl() {
        String contextPath = ''
        tomcatRunWar.setContextPath contextPath
        assert tomcatRunWar.getFullContextPath() == ''
    }

    @Test
    public void testSetWebApplicationContextForContextUrlWithLeadingSlash() {
        String contextPath = '/app'
        tomcatRunWar.setContextPath contextPath
        assert tomcatRunWar.getFullContextPath() == '/app'
    }

    @Test
    public void testSetWebApplicationContextForContextUrlWithoutLeadingSlash() {
        String contextPath = 'app'
        tomcatRunWar.setContextPath contextPath
        assert tomcatRunWar.getFullContextPath() == '/app'
    }

    private File createWebAppDir() {
        File webAppDir = new File(testDir, "webApp")
        if(webAppDir.exists()) {
            return webAppDir
        }
        boolean success = webAppDir.mkdirs()

        if(!success) {
            fail("Unable to create web app directory");
        }

        webAppDir
    }

    private File createWar(File webAppDir) {
        File war = new File(webAppDir, "test.war")
        File zippedFile = new File(webAppDir, "entry.txt")
        boolean success = zippedFile.createNewFile()

        if(!success) {
            fail "Unable to create test file for WAR"
        }

        ZipOutputStream out = null

        try {
            out = new ZipOutputStream(new FileOutputStream(war))
            out.putNextEntry(new ZipEntry(zippedFile.canonicalPath))
        }
        catch(IOException e) {
            fail "Unable to create WAR"
        }
        finally {
            if(out) {
                out.close()
            }
        }

        war
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
}
