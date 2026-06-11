package xyz.atsumeru.web.security.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import xyz.atsumeru.web.security.filter.JwtAuthenticationFilter;
import xyz.atsumeru.web.security.filter.SharingAuthenticationFilter;

@Profile("!dev")
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SharingAuthenticationFilter sharingAuthenticationFilter;

    public WebSecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter,
                                    SharingAuthenticationFilter sharingAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.sharingAuthenticationFilter = sharingAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers("/api/v1/share/**").permitAll()
                                .requestMatchers("/api/**").authenticated()
                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").authenticated()
                                .anyRequest().permitAll()
                )
                .httpBasic(basic -> basic.authenticationEntryPoint(basicEntryPoint()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(sharingAuthenticationFilter, JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    public BasicAuthenticationEntryPoint basicEntryPoint() {
        return new BasicAuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException authException) throws java.io.IOException {
                String requestedWith = request.getHeader("X-Requested-With");
                boolean isAjaxRequest = "XMLHttpRequest".equals(requestedWith);

                if (isAjaxRequest) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                } else {
                    super.commence(request, response, authException);
                }
            }

            @Override
            public void afterPropertiesSet() {
                setRealmName("Atsumeru");
                super.afterPropertiesSet();
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}