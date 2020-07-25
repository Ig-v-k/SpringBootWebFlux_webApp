package com.react.spring.react_web_app;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootApplication
public class ReactWebAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReactWebAppApplication.class, args);
    }
}

@Repository
interface ReservationRepository extends ReactiveCrudRepository<Reservation, Integer> {
}

@RestController
@RequiredArgsConstructor
class ReservationRestController {

    private final ReservationRepository reservationRepository;

    @GetMapping("/reservations")
    Publisher<Reservation> reservationPublisher() {
        return this.reservationRepository.findAll();
    }

}

@Configuration
@EnableR2dbcRepositories
class R2dbcConfig extends AbstractR2dbcConfiguration {

    @Bean
    @Override
    public ConnectionFactory connectionFactory() {
        return new PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration
                        .builder()
                        .username("postgres")
                        .password("psqlpasswd")
                        .host("localhost")
                        .port(5432)
                        .database("reactwebapp")
                        .build()
        );
    }


}

    @Component
    @RequiredArgsConstructor
    @Log4j2
    class SampleDataInitializer {

        private final ReservationRepository reservationRepository;

        @EventListener(ApplicationReadyEvent.class)
        public void initialize() {
            Flux<Reservation> saved = Flux
                    .just("A", "B", "C", "D", "E", "F", "F", "G")
                    .map(name -> new Reservation(null, name))
                    .flatMap(this.reservationRepository::save);

            reservationRepository
                    .deleteAll()
                    .thenMany(saved)
                    .thenMany(this.reservationRepository.findAll())
                    .subscribe(log::info);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class GreetingRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class GreetingResposnse {
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class Reservation {
        @Id
        private Integer id;
        private String name;
    }