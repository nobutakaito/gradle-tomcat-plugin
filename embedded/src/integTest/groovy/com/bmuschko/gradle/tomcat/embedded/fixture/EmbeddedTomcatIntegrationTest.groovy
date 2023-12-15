package com.bmuschko.gradle.tomcat.embedded.fixture

import com.bmuschko.gradle.tomcat.embedded.TomcatServer
import com.bmuschko.gradle.tomcat.embedded.TomcatUser
import com.bmuschko.gradle.tomcat.embedded.TomcatVersion
import com.bmuschko.gradle.tomcat.fixture.AvailablePortFinder
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

abstract class EmbeddedTomcatIntegrationTest extends Specification {
    @TempDir
    Path tempDir;

    TomcatServer tomcatServer
    Integer port

    def setup() {
        tomcatServer = createTomcatServer()
        tomcatServer.home = getTomcatHomeDir().canonicalPath
        port = reservePort()
    }

    private Integer reservePort() {
        AvailablePortFinder availablePortFinder = AvailablePortFinder.createPrivate()
        availablePortFinder.nextAvailable
    }

    void verifyCreatedSocket() {
        new Socket(InetAddress.getByName('localhost'), port)
    }

    protected abstract TomcatServer createTomcatServer()
    protected abstract void configureTomcatServer()

    private File getTomcatHomeDir() {
        return tempDir.resolve(tomcatServer.version.description).toFile();
    }

    private TomcatVersion getTomcatVersion() {
        tomcatServer.version
    }

    def "Indicates correct version"() {
        expect:
        tomcatServer.version == getTomcatVersion()
    }

    def "Can start server"() {
        when:
        configureTomcatServer()
        tomcatServer.start()

        then:
        verifyCreatedSocket()

        cleanup:
        tomcatServer.stop()
    }

    def "Can start server with authentication user"() {
        when:
        configureTomcatServer()
        addUsers()
        tomcatServer.start()

        then:
        verifyCreatedSocket()

        cleanup:
        tomcatServer.stop()
    }

    private void addUsers() {
        tomcatServer.configureUser(new TomcatUser(username: 'nykolaslima', password: '123456', roles: ['developer', 'admin']))
        tomcatServer.configureUser(new TomcatUser(username: 'bmuschko', password: 'abcdef', roles: ['manager']))
        tomcatServer.configureUser(new TomcatUser(username: 'unprivileged', password: 'pwd'))
        tomcatServer.configureUser(new TomcatUser(username: 'unprivileged', password: 'pwd', roles: null))
    }
}
