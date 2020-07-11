package com._spring.webflux.react_web.config;

import com._spring.webflux.react_web.handlers.ResponseApplicationReactHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class ResponseApplicationReactRouter {
    @Bean
    public RouterFunction<ServerResponse> route(ResponseApplicationReactHandler responseApplicationReactHandler) {
        return RouterFunctions
                .route(RequestPredicates.GET("/one")
                        .and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), responseApplicationReactHandler::hello)
                .andRoute(
                        RequestPredicates.GET("/"),
                        serverRequest -> {
                            return ServerResponse
                                    .ok()
                                    .render(
                                            "index",
                                            Stream.of(new String[][] {{ "user", "AAAAAAAAAAAAAAAAAAAAAAA" }})
                                                    .collect(Collectors.toMap(data -> data[0], data -> data[1]))
                                    );
                        }
                );
    }
}
