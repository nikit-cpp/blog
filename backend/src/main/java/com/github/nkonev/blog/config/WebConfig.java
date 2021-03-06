package com.github.nkonev.blog.config;

import java.io.File;
import javax.annotation.PostConstruct;
import org.apache.catalina.Valve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebConfig.class);

    @Autowired
    private CustomConfig customConfig;

    @Autowired
    private ServerProperties serverProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

    }

    /**
     *  https://spring.io/blog/2013/05/11/content-negotiation-using-spring-mvc
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }

    @PostConstruct
    public void log(){
        LOGGER.info("Base url: {}", customConfig.getBaseUrl());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // see https://github.com/spring-projects/spring-boot/issues/14302#issuecomment-418712080 if you want to customize management tomcat
    @Bean
    public ServletWebServerFactory servletContainer(Valve... valves) {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.addContextValves(valves);

        final File baseDir = serverProperties.getTomcat().getBasedir();
        if (baseDir!=null) {
            File docRoot = new File(baseDir, "document-root");
            docRoot.mkdirs();
            tomcat.setDocumentRoot(docRoot);
        }

        tomcat.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");

        return tomcat;
    }

}
