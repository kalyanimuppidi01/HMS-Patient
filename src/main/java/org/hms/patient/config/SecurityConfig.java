package org.hms.patient.config;

import org.hms.patient.security.RoleBasedAccessFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final RoleBasedAccessFilter roleBasedAccessFilter;

    public SecurityConfig(RoleBasedAccessFilter roleBasedAccessFilter) {
        this.roleBasedAccessFilter = roleBasedAccessFilter;
    }

    /** In-memory users for local testing */
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails admin = User.withUsername("admin")
                .password("{noop}password")   // {noop} ⇒ no encoder (dev only)
                .roles("ADMIN")
                .build();

        UserDetails doctor = User.withUsername("doctor")
                .password("{noop}password")
                .roles("DOCTOR")
                .build();

        UserDetails reception = User.withUsername("reception")
                .password("{noop}password")
                .roles("RECEPTION")
                .build();

        return new InMemoryUserDetailsManager(admin, doctor, reception);
    }

    /** Security chain + RBAC filter registration */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Open endpoints
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/actuator/**").permitAll()
                        // Everything else must authenticate
                        .anyRequest().authenticated()
                )
                .httpBasic()   // Basic auth for Swagger / curl
                .and()
                // Plug in your role-based filter after authentication
                .addFilterAfter(roleBasedAccessFilter, BasicAuthenticationFilter.class);

        return http.build();
    }
}
