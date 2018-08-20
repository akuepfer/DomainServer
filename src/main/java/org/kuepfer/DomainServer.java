package org.kuepfer;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.RequestDumpingHandler;
import io.undertow.server.handlers.encoding.ContentEncodedResourceManager;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.*;

import javax.net.ssl.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;

import static io.undertow.Handlers.resource;
import static io.undertow.servlet.Servlets.defaultContainer;

/**
 * A simple web server based on undertow.io.
 * - Redirect from http to https.
 * - serve / for Let's encrypt certificate renewal.
 * - serve /portfolio for personal stuff
 * - serve /berge-versetzen for test of web pages for the business of Brigitte Roellin
 * - resource /berge-versetzen utilizes content compression and caching of 8 days
 *   to have only relevant issues reported by google page speed.
 *
 */
public class DomainServer {

    public static final String ROOT_DIRECTORY   = "/home/armin/ws/DomainServer/root/";
    public static final String MY_WORKSPACE     = "/home/armin/ws/MyWorkspace/src/";
    public static final String MOVE_MOUNTAINTS  = "/home/armin/ws/MoveMountains/app/";
    private static final char[] STORE_PASSWORD  = "password".toCharArray();

    /**
     * @param args string of java properties
     *  -Dserver.keystore='keystore-path'
     *  -Dserver.keystore.password='password'
     *  -Dserver.truststore=truststore-path
     *  -Dserver.truststore.password='password'
     *  -Dkey.password=password
     */
    public static void main(final String[] args) throws Exception {

        int httpPort = 8080;
        int httpsPort = 8443;
        if ("root".equals(System.getProperty("user.name"))) {
            httpPort = 80;
            httpsPort = 443;
        }

        /*
         * Serve local top directory '/' for Let's encrypt certificate setup.
         */
        ResourceHandler rootFileHandler = resource(new PathResourceManager(Paths.get(ROOT_DIRECTORY), 100))
                .setDirectoryListingEnabled(false);


        /*
         * Serve directory /portfolio
         */
        ResourceHandler workspaceHandler = resource(new PathResourceManager(Paths.get(MY_WORKSPACE), 100))
                .setDirectoryListingEnabled(false);


        /**
         * Server for the optimized page under /berge-versetzen/
         */
        File fileRoot = new File(MOVE_MOUNTAINTS);
        FileResourceManager resourceManager = new FileResourceManager(fileRoot, 10485760);
        CachingResourceManager cachingResourceManager = new CachingResourceManager(100, 10000, null, resourceManager, -1);



        /*Predicates.parse("max-content-size[5]")*/

        Predicate allContent = Predicates.truePredicate();

        Predicate compressedAndCachedPredicate = Predicates.or(
                Predicates.suffix(".css"),
                Predicates.suffix(".woff2"),
                Predicates.suffix(".jpg"),
                Predicates.suffix(".jpeg"),
                Predicates.suffix(".png"),
                Predicates.suffix(".js"),
                Predicates.suffix(".pdf"));

        /**
         * Enable content encoding (compression)
         */


        Path pathRoot = Paths.get(MOVE_MOUNTAINTS);
        ContentEncodingRepository contentEncodingRepository =
                new ContentEncodingRepository().addEncodingHandler("gzip", new GzipEncodingProvider(), 50,
                        compressedAndCachedPredicate);

        int minResourceSize = 0;
        int maxResourceSize = 100000;
        Predicate encodingAllowed = Predicates.truePredicate();

        ContentEncodedResourceManager contentEncodedResourceManager
                = new ContentEncodedResourceManager(pathRoot, cachingResourceManager, contentEncodingRepository,
                minResourceSize, maxResourceSize, encodingAllowed);

        /**
         * Enable caching of 8 days, for css/ fonts/ img/ js/ pdf/
         * --> reduce time for development to 1 minute
         */
        ResourceHandler resourceHandler = new ResourceHandler(resourceManager)
                .setDirectoryListingEnabled(false)
                .setCachable(compressedAndCachedPredicate)
                .setContentEncodedResourceManager(contentEncodedResourceManager)
                .setCacheTime(/*8 * 24 * 60 * */ 60);

        PathHandler pathHandler = Handlers.path()
                .addPrefixPath("/", rootFileHandler)
                .addPrefixPath("/berge-versetzen", resourceHandler)
                .addPrefixPath("/workspace", workspaceHandler);

        SSLContext sslContext = createSSLContext(loadKeyStore("server.keystore"), loadKeyStore("server.truststore"));


        /**
         * To dump header use this request handler.
         */
        RequestDumpingHandler requestDumpingHandler = new RequestDumpingHandler(pathHandler);

        Undertow optimizedServer = Undertow.builder()
                .addHttpsListener(httpsPort, "0.0.0.0", sslContext)
                .setHandler(pathHandler)
                //.setHandler(requestDumpingHandler)
                .build();
        optimizedServer.start();


        /**
         * Redirect server - redirect HTTP to HTTPS
         */
        int finalHttpsPort = httpsPort;
        DeploymentInfo servletDeploymentInfo = Servlets.deployment()
                .addSecurityConstraint(new SecurityConstraint()
                        .addWebResourceCollection(new WebResourceCollection().addUrlPattern("/*"))
                        .setTransportGuaranteeType(TransportGuaranteeType.CONFIDENTIAL)
                        .setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.PERMIT))
                .setConfidentialPortManager((exchange) -> {
                    return finalHttpsPort;
                })
                .setDeploymentName("DeploymentName")
                .setContextPath("/")
                .setClassLoader(DomainServer.class.getClassLoader());

        DeploymentManager manager = defaultContainer().addDeployment(servletDeploymentInfo);
        manager.deploy();

        HttpHandler servletHandler = manager.start();
        Undertow redirectServer = Undertow.builder()
                .addHttpListener(httpPort, "0.0.0.0")
                .setHandler(servletHandler)
                .build();
        redirectServer.start();
    }


    /**
     * Load the keystore files
     * @param name either a java property of a directory path
     * @return the key store.
     * @throws Exception
     */
    private static KeyStore loadKeyStore(String name) throws Exception {
        String storeLoc = System.getProperty(name);
        final InputStream stream;
        if (storeLoc == null) {
            stream = DomainServer.class.getResourceAsStream(name);
        } else {
            stream = Files.newInputStream(Paths.get(storeLoc));
        }

        if (stream == null) {
            throw new RuntimeException("Could not load keystore");
        }
        try (InputStream is = stream) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(is, password(name));
            return loadedKeystore;
        }
    }


    /**
     * Lookup the java propery 'name'.password and return it as char array
     */
    static char[] password(String name) {
        String pw = System.getProperty(name + ".password");
        return pw != null ? pw.toCharArray() : STORE_PASSWORD;
    }


    /**
     * Initializes the SSL context.
     */
    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) throws Exception {
        KeyManager[] keyManagers;
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password("key"));
        keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers;
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        trustManagers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        return sslContext;
    }

}
