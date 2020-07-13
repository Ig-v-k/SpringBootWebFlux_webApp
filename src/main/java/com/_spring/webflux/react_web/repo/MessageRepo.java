package com._spring.webflux.react_web.repo;

import com._spring.webflux.react_web.domain.Message;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepo extends ReactiveCrudRepository<Message, Long> {

}
