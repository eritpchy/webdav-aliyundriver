package com.github.zxbu.webdavteambition.config;

import org.apache.catalina.CredentialHandler;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.filters.CorsFilter;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.MessageDigestCredentialHandler;
import org.apache.catalina.realm.RealmBase;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;

@Component
@ConditionalOnProperty(prefix = "aliyundrive.auth", name = "enable", matchIfMissing = true)
public class EmbdTomcatConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>, Ordered {

    @Autowired
    private AliyunDriveProperties mAliyunDriveProperties;

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {

        TomcatServletWebServerFactory tomcatServletWebServerFactory = (TomcatServletWebServerFactory) factory;

        tomcatServletWebServerFactory.addContextCustomizers(context -> {

            RealmBase realm = new RealmBase() {
                @Override
                protected String getPassword(String username) {
                    if (mAliyunDriveProperties.getAuth().getUserName().equals(username)) {
                        return mAliyunDriveProperties.getAuth().getPassword();
                    }
                    return null;
                }

                @Override
                protected Principal getPrincipal(String username) {
                    return new GenericPrincipal(username, mAliyunDriveProperties.getAuth().getPassword(), Collections.singletonList("**"));
                }
            };

            CredentialHandler credentialHandler = new MessageDigestCredentialHandler();
            realm.setCredentialHandler(credentialHandler);
            context.setRealm(realm);

            AuthenticatorBase digestAuthenticator = new BasicAuthenticator();
            SecurityConstraint securityConstraint = new SecurityConstraint();
            securityConstraint.setAuthConstraint(true);
            securityConstraint.addAuthRole("**");
            SecurityCollection collection = new SecurityCollection();
            collection.addPattern("/*");
            collection.addOmittedMethod("OPTIONS");
            securityConstraint.addCollection(collection);
            context.addConstraint(securityConstraint);
            context.getPipeline().addValve(digestAuthenticator);

            FilterDef corsFilterDef = new FilterDef();
            corsFilterDef.setFilterClass(CorsFilter.class.getName());
            corsFilterDef.setFilterName(CorsFilter.class.getSimpleName());
            corsFilterDef.addInitParameter("cors.allowed.origins", "*");
            corsFilterDef.addInitParameter("cors.allowed.methods", "OPTIONS,GET,HEAD,POST,DELETE,TRACE,PROPPATCH,COPY,MOVE,LOCK,UNLOCK,PROPFIND,PUT,MKCOL");
            corsFilterDef.addInitParameter("cors.allowed.headers", "origin,content-type,accept,authorization,access-control-allow-origin,depth,destination,If-None-Match");
            corsFilterDef.addInitParameter("cors.exposed.headers", "Access-Control-Allow-Origin,Access-Control-Allow-Credentials");
            context.addFilterDef(corsFilterDef);

            FilterMap corsFilterMap = new FilterMap();
            corsFilterMap.setFilterName(CorsFilter.class.getSimpleName());
            corsFilterMap.addURLPattern("/*");
            context.addFilterMap(corsFilterMap);
        });

    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }
}
