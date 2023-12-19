package com.bmuschko.gradle.tomcat.embedded

abstract class BaseTomcat9xPlusImpl extends BaseTomcat8xPlusImpl{
    @Override
    void configureAjpConnector(int port, String uriEncoding, String protocolHandlerClassName, Boolean secretRequired, String secret) {
        def ajpConnector = createConnector(protocolHandlerClassName, uriEncoding)
        ajpConnector.port = port
        def ajpProtocol = ajpConnector.getProtocolHandler()
        ajpProtocol.secretRequired = secretRequired
        if (secretRequired) {
            ajpProtocol.secret = secret
        }
        tomcat.service.addConnector ajpConnector
    }
}
