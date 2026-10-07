package com.yeobaek.plan;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
class SecurityConfig {
    @Bean @Order(1) SecurityFilterChain searchSuggestions(HttpSecurity http) throws Exception {
        return http.securityMatcher("/p/*/research/suggestions")
            .authorizeHttpRequests(auth->auth.anyRequest().permitAll())
            .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .headers(headers->headers.frameOptions(frame->frame.sameOrigin())
                .referrerPolicy(ref->ref.policy(ReferrerPolicy.NO_REFERRER))
                .contentSecurityPolicy(csp->csp.policyDirectives("default-src 'none'; style-src 'unsafe-inline'; img-src https: data:; frame-ancestors 'self'; base-uri 'none'; form-action 'none'; sandbox allow-popups allow-popups-to-escape-sandbox")))
            .build();
    }
    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            // CSRF remains enabled. Editing is authorized by PlanController's session grant.
            .headers(headers -> headers
                .referrerPolicy(ref -> ref.policy(ReferrerPolicy.NO_REFERRER))
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'")))
            .build();
    }
}
