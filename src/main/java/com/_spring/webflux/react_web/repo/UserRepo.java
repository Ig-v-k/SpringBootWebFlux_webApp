package com._spring.webflux.react_web.repo;

import com._spring.webflux.react_web.domain.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepo extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByUsername(String name);
}
