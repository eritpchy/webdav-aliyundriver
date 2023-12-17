package com.github.zxbu.webdavteambition.config;

import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import com.github.zxbu.webdavteambition.util.AliyunDriveClientServiceHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.File;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.ServerResponse.ok;

@Configuration
@EnableConfigurationProperties(AliyunDrivePropertiesSpring.class)
public class AliyunDriveAutoConfig implements WebMvcConfigurer {

    @Autowired
    private AliyunDriveClientServiceHolder mAliyunDriveClientServiceHolder;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        AliyunDriveClientService service = mAliyunDriveClientServiceHolder.getAliyunDriveClientService();
        AliyunDriveProperties properties = service.getProperties();
        File frontendDir = properties.getFrontendDir();
        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + frontendDir.getAbsolutePath() + File.separator);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
//        registry.addViewController("/error").setViewName("forward:/index.html");
    }

    @Bean
    public RouterFunction<ServerResponse> vueHistoryModeCatchAllRoute() {
        HandlerFunction<ServerResponse> serveIndexHtmlFunction = request -> ok().contentType(MediaType.TEXT_HTML)
                .render("forward:/index.html");
        String firstApiSegmentExcludes = "favicon\\.ico|index.html|assets|dav";
        return route(GET("/{path:^(?!" + firstApiSegmentExcludes + ").*}"), serveIndexHtmlFunction)
                .and(route(GET("/{path:^(?!" + firstApiSegmentExcludes + ").*}/**"),
                        serveIndexHtmlFunction));
    }
}
