package com._spring.webflux.react_web.handlers;

import com._spring.webflux.react_web.domain.Message;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ResponseApplicationReactHandler {

    public Mono<ServerResponse> hello(ServerRequest request) {
        Long startPag = request.queryParam("startPag")
                .map(Long::valueOf)
                .orElse(0L);
        Long count = request.queryParam("count")
                .map(Long::valueOf)
                .orElse(3L);

        Flux<Message> data = Flux
                .just(
                        "Hello, reactive!",
                        "More then one",
                        "Third post",
                        "Fourth post",
                        "Fifth post"
                )
                .skip(startPag)
                .take(count)
                .map(Message::new);

        return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(data, Message.class);

    }

    public Mono<ServerResponse> index(ServerRequest serverRequest) {
        String customUser = serverRequest.queryParam("user")
                .orElse("---");
        Map<String, Object> collectMapModel = Stream.of(new String[][]{{"customUser", customUser}})
                .collect(Collectors.toMap(data -> data[0], data -> data[1]));

        return ServerResponse
                .ok()
                .render(
                        "index",
                        collectMapModel
                );

    }//method

}
