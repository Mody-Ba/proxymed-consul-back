package com.proxymed.configs.security;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Ne dit que deux choses : Swagger est public, et tout le reste demande un badge.
 * La Fiche 1 n'a ni connexion ni comptes : elle verifie seulement le badge delivre par le
 * service d'authentification. Les droits par role se verifient dans les controllers, avec
 * @PreAuthorize au-dessus de chaque methode (active par @EnableMethodSecurity), jamais ici.
 * Le CORS passe avant l'authentification : une pre-verification OPTIONS du navigateur
 * (sans badge) recoit sa reponse CORS sans jamais atteindre le 401.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final AuthEntryPointJwt unauthorizedHandler;
    private final AccesRefuseHandler accesRefuseHandler;
    private final AuthTokenFilter authTokenFilter;

    /**
     * AuthTokenFilter est un @Component : sans cela, Spring Boot l'enregistrerait aussi comme
     * filtre servlet global, en plus de sa place dans la chaine de securite.
     */
    @Bean
    public FilterRegistrationBean<AuthTokenFilter> authTokenFilterRegistration(AuthTokenFilter filter) {
        FilterRegistrationBean<AuthTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accesRefuseHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Rendu interne des erreurs par Spring (pas une adresse publique).
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated());

        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Origines autorisees : proxymed.cors.origines (PROXYMED_CORS_ORIGINS, separees par des virgules).
     * Pas de cookies : le badge voyage dans l'en-tete Authorization. "*" est refuse au demarrage.
     * Content-Disposition est expose pour que le front lise le nom des PDF telecharges.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${proxymed.cors.origines}") String origines) {
        List<String> originesAutorisees = Arrays.stream(origines.split(","))
                .map(String::trim)
                .filter(origine -> !origine.isEmpty())
                .toList();
        if (originesAutorisees.isEmpty() || originesAutorisees.contains(CorsConfiguration.ALL)) {
            throw new IllegalStateException("PROXYMED_CORS_ORIGINS doit lister des origines explicites (jamais \"*\")");
        }

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(originesAutorisees);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
