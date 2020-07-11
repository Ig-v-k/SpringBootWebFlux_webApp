package com._spring.webflux.react_web.config;

import com._spring.webflux.react_web.handlers.ResponseApplicationReactHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ResponseApplicationReactRouter {
    @Bean
    public RouterFunction<ServerResponse> route(ResponseApplicationReactHandler responseApplicationReactHandler) {
        return RouterFunctions
                .route(
                        RequestPredicates.GET("/one")
                                .and(RequestPredicates.accept(MediaType.TEXT_PLAIN)),
                        responseApplicationReactHandler::hello)
                .andRoute(
                        RequestPredicates.GET("/"),
                        responseApplicationReactHandler::index
                );

    }//method
}
