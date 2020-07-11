package com._spring.webflux.react_web.controller;

import com._spring.webflux.react_web.domain.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/controller")
public class MainController {
    @GetMapping
    public Flux<Message> list(
            @RequestParam(defaultValue = "0") Long startPaging,
            @RequestParam(defaultValue = "3") Long count) {
        return Flux
                .just(
                        "Hello, reactive!",
                        "More then one",
                        "Third post",
                        "Fourth post",
                        "Fifth post"
                )
                .skip(startPaging)
                .take(count)
                .map(Message::new);
    }
}
