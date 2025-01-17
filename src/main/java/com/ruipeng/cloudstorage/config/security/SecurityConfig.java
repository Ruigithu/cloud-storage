package com.ruipeng.cloudstorage.config.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private AppUserDetailsService userDetailsService;
    private AppAuthenticationFailureHandler failureHandler;
    private AppLogoutHandler logoutHandler;
    private  CorsFilter corsFilter;
    private  CorsConfiguration corsConfiguration;

    @Autowired
    public SecurityConfig(AppUserDetailsService userDetailsService, AppAuthenticationFailureHandler failureHandler, AppLogoutHandler logoutHandler, CorsFilter corsFilter, CorsConfiguration corsConfiguration) {
        this.userDetailsService = userDetailsService;
        this.failureHandler = failureHandler;
        this.logoutHandler = logoutHandler;
        this.corsFilter = corsFilter;
        this.corsConfiguration = corsConfiguration;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(corsFilter, SessionManagementFilter.class)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> corsConfiguration))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/signup",  //
                                "/login",     // include login page url
                                "/css/**",
                                "/js/**",
                                "/h2-console/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .failureHandler(failureHandler)
                        .defaultSuccessUrl("/home", true)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutHandler)
                        .logoutSuccessUrl("/login?logoutMessage=You have been logged out")//
                        .invalidateHttpSession(true) // invalidate session
                        .clearAuthentication(true) // clean verification information
                );

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
