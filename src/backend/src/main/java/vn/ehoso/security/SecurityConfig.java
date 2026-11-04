package vn.ehoso.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneId;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** Role rules of the SRS section 3.7 matrix (FR-05); ownership is checked in the services. */
@Configuration
class SecurityConfig {
  private static final String CITIZEN = "CITIZEN";
  private static final String CBMC = "CBMC";
  private static final String CCTP = "CCTP";
  private static final String LEADER = "LEADER";
  private static final String ADMIN = "ADMIN";
  private static final String STAFF_DOSSIER = "/api/v1/staff/dossiers/*/";

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, ActorAuthenticationConverter converter)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/register",
                        "/api/v1/auth/verify-email",
                        "/api/v1/auth/resend-verification",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/password-reset",
                        "/api/v1/auth/password-reset/confirm",
                        "/api/v1/public/lookup")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/procedures", "/api/v1/procedures/*")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers("/swagger-ui.html")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/dossiers/*/attachments")
                    .hasAnyRole(CITIZEN, CCTP)
                    .requestMatchers("/api/v1/dossiers", "/api/v1/dossiers/**")
                    .hasRole(CITIZEN)
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/attachments/*")
                    .hasAnyRole(CITIZEN, CCTP)
                    .requestMatchers("/api/v1/attachments/*/download-url")
                    .hasAnyRole(CITIZEN, CBMC, CCTP, LEADER)
                    .requestMatchers("/api/v1/staff/officers")
                    .hasRole(CBMC)
                    .requestMatchers(
                        STAFF_DOSSIER + "accept",
                        STAFF_DOSSIER + "request-supplement",
                        STAFF_DOSSIER + "reject",
                        STAFF_DOSSIER + "assign",
                        STAFF_DOSSIER + "return-result")
                    .hasRole(CBMC)
                    .requestMatchers(STAFF_DOSSIER + "verification", STAFF_DOSSIER + "submit-approval")
                    .hasRole(CCTP)
                    .requestMatchers(STAFF_DOSSIER + "approve", STAFF_DOSSIER + "send-back")
                    .hasRole(LEADER)
                    .requestMatchers(STAFF_DOSSIER + "logs")
                    .hasAnyRole(CBMC, CCTP, LEADER, ADMIN)
                    .requestMatchers("/api/v1/staff/dossiers", "/api/v1/staff/dossiers/*")
                    .hasAnyRole(CBMC, CCTP, LEADER)
                    .requestMatchers("/api/v1/dashboard", "/api/v1/dashboard/**")
                    .hasRole(LEADER)
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole(ADMIN)
                    .requestMatchers("/api/v1/auth/logout")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)));
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  Clock clock() {
    return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
  }

  @Bean
  JwtEncoder jwtEncoder(@Value("${ehoso.jwt.secret}") String secret) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key(secret)));
  }

  @Bean
  JwtDecoder jwtDecoder(@Value("${ehoso.jwt.secret}") String secret) {
    return NimbusJwtDecoder.withSecretKey(key(secret)).macAlgorithm(MacAlgorithm.HS256).build();
  }

  private SecretKey key(String secret) {
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }
}
