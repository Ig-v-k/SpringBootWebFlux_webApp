package com.react.spring.react_web_app;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@SpringBootApplication
public class ReactWebAppApplication {
  public static void main(String[] args) {
	SpringApplication.run(ReactWebAppApplication.class, args);
  }
}

@Log4j2
@RestController
@RequiredArgsConstructor
class ReservationRestController {

  private final ReservationRepository reservationRepository;
  private final IntervalMessageProducer intervalMessageProducer;

  @GetMapping("/reservation")
  Flux<Reservation> reservationPublisher(Model model) {
	return reservationRepository.findAll();
  }

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/sec/{n}")
  Publisher<GreetingResponse> stringPublisher(@PathVariable String n) {
	return this.intervalMessageProducer.produceGreeting(new GreetingRequest(n));
  }
}

@Configuration
class WebEndpointConfiguration {

  Mono<ServerResponse> userHello(ServerRequest request) {
    Mono<String> principalPublisher = request.principal().map(p -> "Hello, " + p.getName() + "!");
    return ServerResponse.ok().body(principalPublisher, String.class);
  }

  Mono<ServerResponse> userOne(ServerRequest request) {
    Mono<UserDetails> detailsMono = request.principal()
		  .map(p -> UserDetails.class.cast(Authentication.class.cast(p).getPrincipal()));
    return ServerResponse.ok().body(detailsMono, UserDetails.class);
  }

  @Bean
  RouterFunction<?> routes() {
    return RouterFunctions.route(GET("/wel"), this::userHello)
		  .andRoute(GET("/use/nam"), this::userOne);
  }

  @Bean
  RouterFunction<ServerResponse> route(ReservationRepository reservationRepository) {
	return RouterFunctions
		  .route()
		  .GET("/h/rsn", serverRequest -> ServerResponse.ok()
				.body(
					  reservationRepository.findAll(),
					  Reservation.class))
		  .GET("/h/rsn/{n}", serverRequest -> ServerResponse.ok()
				.body(
					  reservationRepository.findByName(
							serverRequest
								  .pathVariable("n")
								  .toUpperCase()
					  ),
					  Reservation.class))
		  .DELETE("/h/rsn/{n}", serverRequest -> ServerResponse.ok()
				.body(
					  reservationRepository.deleteAllByName(
							serverRequest
								  .pathVariable("n")
								  .toUpperCase()
					  ),
					  Reservation.class))
		  .PUT("/h/rsn/{n}", serverRequest -> ServerResponse.ok()
				.body(
					  reservationRepository
							.findByName(serverRequest
								  .pathVariable("n")
								  .toUpperCase()
							)
							.map(reservation -> new Reservation(reservation.getId(), serverRequest.queryParam("nn").get()))
							.flatMap(reservationRepository::save)
							.switchIfEmpty(Mono.error(new Exception())),
					  Reservation.class
				))
		  .build();
  }
}


@Component
class IntervalMessageProducer {
  Flux<GreetingResponse> produceGreeting(GreetingRequest request) {
	return Flux
		  .fromStream(Stream.generate(() -> "Hello " + request.getText() + " @ " + Instant.now()))
		  .map(GreetingResponse::new)
		  .delayElements(Duration.ofSeconds(1));
  }
}

@Configuration
@EnableWebFluxSecurity
class UserSecurityConfiguration {
  @Bean
  MapReactiveUserDetailsService userDetailsService() {
	UserDetails user = User.withUsername("user").password("password").roles("USER").build();
	UserDetails admin = User.withUsername("admin").password("password").roles("USER", "ADMIN").build();
	return new MapReactiveUserDetailsService(user, admin);
  }

  @Bean
  SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {
	return httpSecurity
		  .authorizeExchange().anyExchange().authenticated()
		  .and()
		  .httpBasic()
		  .and()
		  .build();
  }

  @Bean
  AuthenticationProvider daoAuthenticationProvider() {
	DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
	authenticationProvider.setPasswordEncoder(null);
	return authenticationProvider;
  }
}

@Configuration
@EnableR2dbcRepositories
@RequiredArgsConstructor
class R2dbcConfiguration extends AbstractR2dbcConfiguration {

  private final ConnectionFactory connectionFactory;

  @Override
  public ConnectionFactory connectionFactory() {
	return this.connectionFactory;
  }
}

@Configuration
class DatabaseConfiguration {

  @Value("${custom.spring.database.password}")
  String password;
  @Value("${custom.spring.database.username}")
  String username;

  @Bean
  PostgresqlConnectionFactory connectionFactory() {
	return new PostgresqlConnectionFactory(
		  PostgresqlConnectionConfiguration.builder()
				.host("localhost")
				.database("reactwebapp")
				.username(username)
				.password(password)
				.build()
	);
  }
}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, Integer> {
  Mono<Reservation> findByName(String name);
  Flux<Reservation> deleteAllByName(String name);
}

@Component
@Log4j2
@RequiredArgsConstructor
class SampleDataInitializer {

  private final ReservationRepository reservationRepository;

  @EventListener(ApplicationReadyEvent.class)
  public void initialize() {
	Flux<Reservation> saved = Flux
		  .just("A", "B", "C", "D", "E", "F", "G")
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
class Reservation {
  @Id
  private Integer id;
  @NonNull
  private String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingRequest {
  private String text;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingResponse {
  private String text;
}