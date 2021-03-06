package com.react.spring.react_web_app;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.context.webflux.IReactiveDataDriverContextVariable;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Stream;

@SpringBootApplication
public class ReactWebAppApplication {
  public static void main(String[] args) {
	SpringApplication.run(ReactWebAppApplication.class, args);
  }
}

@Log4j2
@Controller
@RequiredArgsConstructor
class ReservationRestController {

  private final ReservationRepository reservationRepository;
  private final ReservationService reservationService;
  private final IntervalMessageProducer intervalMessageProducer;

  @GetMapping("/main.p")
  String mainPage(Model model) {
	IReactiveDataDriverContextVariable driverContextVariable = new ReactiveDataDriverContextVariable(reservationService.listAllReact(), 3);
	model.addAttribute("cdata", driverContextVariable);
	return "app";
  }

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/sec/{n}")
  Publisher<GreetingResponse> stringPublisher(@PathVariable String n) {
	return this.intervalMessageProducer.produceGreeting(new GreetingRequest(n));
  }

  @ModelAttribute("allSeedStarters")
  public Flux<Reservation> populateSeedStarters() {
	return this.reservationService.listAllReact();
  }
}

@Configuration
class WebEndpointConfiguration {

  Mono<ServerResponse> userHello(ServerRequest request) {
	Mono<String> principalPublisher = request.principal().map(p -> "Hello, " + p.getName() + "!");
	return ServerResponse.ok().body(principalPublisher, String.class);
  }

//  Mono<ServerResponse> userOne(ServerRequest request) {
//	Mono<UserDetails> detailsMono = request.principal()
//		  .map(p -> UserDetails.class.cast(Authentication.class.cast(p).getPrincipal()));
//	return ServerResponse.ok().body(detailsMono, UserDetails.class);
//  }

//  @Bean
//  RouterFunction<?> routes() {
//	return RouterFunctions.route(GET("/wel"), this::userHello)
//		  .andRoute(GET("/usr/nam"), this::userOne);
//  }

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

@Service
@RequiredArgsConstructor
class ReservationService {
  private final ReservationRepository reservationRepository;
  Flux<Reservation> listAllReact() {
	return reservationRepository.findAll().delayElements(Duration.ofSeconds(1));
  }
  public Reservation listByName(String name) {
	return reservationRepository.findByName(name).block();
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
@EnableWebMvc
@ComponentScan
@RequiredArgsConstructor
class SpringWebConfig implements ApplicationContextAware, WebMvcConfigurer {
  private ApplicationContext applicationContext;
  public void setApplicationContext(final ApplicationContext applicationContext)
		throws BeansException {
	this.applicationContext = applicationContext;
  }
  @Override
  public void addResourceHandlers(final ResourceHandlerRegistry registry) {
	registry.addResourceHandler("/images/**").addResourceLocations("/images/");
	registry.addResourceHandler("/css/**").addResourceLocations("/css/");
	registry.addResourceHandler("/js/**").addResourceLocations("/js/");
  }
  @Bean
  public ResourceBundleMessageSource messageSource() {
	ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
	messageSource.setBasename("Messages_es.properties");
	return messageSource;
  }
  @Override
  public void addFormatters(final FormatterRegistry registry) {
	registry.addFormatter(varietyFormatter());
	registry.addFormatter(dateFormatter());
  }
  @Bean
  public VarietyFormatter varietyFormatter() {
	return new VarietyFormatter();
  }
  @Bean
  public DateFormatter dateFormatter() {
	return new DateFormatter();
  }
  @Bean
  public SpringResourceTemplateResolver templateResolver() {
	SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
	templateResolver.setApplicationContext(this.applicationContext);
	templateResolver.setPrefix("/WEB-INF/templates/");
	templateResolver.setSuffix(".html");
	templateResolver.setCacheable(true);
	return templateResolver;
  }
  @Bean
  public SpringTemplateEngine templateEngine() {
	SpringTemplateEngine templateEngine = new SpringTemplateEngine();
	templateEngine.setTemplateResolver(templateResolver());
	templateEngine.setEnableSpringELCompiler(true);
	return templateEngine;
  }
  @Bean
  public ThymeleafViewResolver viewResolver() {
	ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
	viewResolver.setTemplateEngine(templateEngine());
	return viewResolver;
  }
}

class DateFormatter implements Formatter<Date> {
  @Autowired
  private MessageSource messageSource;
  public DateFormatter() {
	super();
  }
  public Date parse(final String text, final Locale locale) throws ParseException {
	final SimpleDateFormat dateFormat = createDateFormat(locale);
	return dateFormat.parse(text);
  }
  public String print(final Date object, final Locale locale) {
	final SimpleDateFormat dateFormat = createDateFormat(locale);
	return dateFormat.format(object);
  }
  private SimpleDateFormat createDateFormat(final Locale locale) {
	final String format = this.messageSource.getMessage("date.format", null, locale);
	final SimpleDateFormat dateFormat = new SimpleDateFormat(format);
	dateFormat.setLenient(false);
	return dateFormat;
  }
}

class VarietyFormatter implements Formatter<Reservation> {
  @Autowired
  private ReservationService reservationService;
  public VarietyFormatter() {
	super();
  }
  public Reservation parse(final String text, final Locale locale) throws ParseException {
	return this.reservationService.listByName(text);
  }
  @Override
  public String print(final Reservation object, final Locale locale) {
	return object.getId().toString();
  }
}

@Configuration
class WebConfig implements WebMvcConfigurer {
  @Bean
  @Description("Thymeleaf template resolver serving HTML")
  public ClassLoaderTemplateResolver templateResolver() {
	ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
	templateResolver.setPrefix("templates/");
	templateResolver.setCacheable(false);
	templateResolver.setSuffix(".html");
	templateResolver.setTemplateMode("HTML");
	templateResolver.setCharacterEncoding("UTF-8");

	return templateResolver;
  }
  @Bean
  @Description("Thymeleaf template engine with Spring integration")
  public SpringTemplateEngine templateEngine() {
	SpringTemplateEngine templateEngine = new SpringTemplateEngine();
	templateEngine.setTemplateResolver(templateResolver());
	return templateEngine;
  }
  @Bean
  @Description("Thymeleaf view resolver")
  public ViewResolver viewResolver() {
	ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
	viewResolver.setTemplateEngine(templateEngine());
	viewResolver.setCharacterEncoding("UTF-8");
	return viewResolver;
  }
  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
	registry.addViewController("/main.p").setViewName("app");
  }
}

//@Configuration
//@EnableWebFluxSecurity
//class UserSecurityConfiguration {
//
//  @Bean
//  MapReactiveUserDetailsService userDetailsService() {
//	UserDetails user = User.withUsername("user").password("password").roles("USER").build();
//	UserDetails admin = User.withUsername("admin").password("jk").roles("USER", "ADMIN").build();
//	return new MapReactiveUserDetailsService(user, admin);
//  }
//
//  @Bean
//  SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity security) {
//	return security
//		  .securityContextRepository(new WebSessionServerSecurityContextRepository())
//		  .authorizeExchange()
//		  .pathMatchers("/usr/nam/{username}").access((mono, authorizationContext) -> mono
//				.map(authentication -> authentication.getName().equals(authorizationContext.getVariables().get("username")))
//				.map(AuthorizationDecision::new))
//		  .anyExchange().authenticated()
//		  .and()
//		  .build();
//  }
//}

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
		  .just("A", "B", "C", "D", "E", "F", "G", "H")
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