package com.ruipeng.cloudstorage.config.security;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final AppUserDetailsService userDetailsService;
    private final AppAuthenticationFailureHandler failureHandler;
    private final CorsFilter corsFilter;
    private final CorsConfiguration corsConfiguration;
    private final JWTService jwtService;
    private final JWTFilter jwtFilter;


    @Autowired
    public SecurityConfig(AppUserDetailsService userDetailsService, AppAuthenticationFailureHandler failureHandler, CorsFilter corsFilter, CorsConfiguration corsConfiguration, JWTService jwtService, JWTFilter jwtFilter) {
        this.userDetailsService = userDetailsService;
        this.failureHandler = failureHandler;
        this.corsFilter = corsFilter;
        this.corsConfiguration = corsConfiguration;
        this.jwtService = jwtService;
        this.jwtFilter = jwtFilter;
    }




    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(corsFilter, SessionManagementFilter.class)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> corsConfiguration))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/signup",
                                "/login",
                                "/share/**",
                                "/download",
                                "/css/**",
                                "/js/**",
                                "/h2-console/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            boolean isApiRequest = request.getHeader("Accept") != null &&
                                    request.getHeader("Accept").contains("application/json") ||
                                    request.getRequestURI().startsWith("/share/");

                            if (isApiRequest) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Unauthorized\"}");
                            } else {
                                response.sendRedirect("/login");
                            }
                        })
                ).sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            if (request.getHeader("Accept") != null &&
                                    request.getHeader("Accept").contains("application/json")) {
                                String username = authentication.getName();
                                String token = jwtService.generateToken(username);

                                response.setStatus(HttpServletResponse.SC_OK);
                                response.setContentType("application/json");
                                response.getWriter().write(
                                        //format not safe
                                        String.format("{\"success\":true,\"token\":\"%s\",\"username\":\"%s\"}",
                                                token, username));
                            } else {
                                response.sendRedirect("/home");
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            if (request.getHeader("Accept") != null &&
                                    request.getHeader("Accept").contains("application/json")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Invalid credentials\"}");
                            } else {
                                failureHandler.onAuthenticationFailure(request, response, exception);
                            }
                        })
                        .permitAll()
                );

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    //AuthenticationConfiguration will collect Provider automatically,included the one we registered
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider providerManager() throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(getPasswordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder(16);
    }

}
