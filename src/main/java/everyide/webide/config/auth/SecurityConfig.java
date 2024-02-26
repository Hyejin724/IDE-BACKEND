package everyide.webide.config.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import everyide.webide.config.auth.dto.response.UserDto;
import everyide.webide.config.auth.filter.JwtAuthenticationFilter;
import everyide.webide.config.auth.filter.JwtAuthorizationFilter;
import everyide.webide.config.auth.jwt.JwtAccessDeniedHandler;
import everyide.webide.config.auth.jwt.JwtAuthenticationEntryPoint;
import everyide.webide.config.auth.handler.CustomLogoutSuccessHandler;
import everyide.webide.config.auth.jwt.JwtTokenProvider;
import everyide.webide.config.auth.user.CustomUserDetails;
import everyide.webide.config.auth.user.CustomUserDetailsService;
import everyide.webide.config.auth.user.oauth2.CustomOAuth2UserService;
import everyide.webide.user.UserRepository;
import everyide.webide.user.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    //Spring Security에서 제공하는 클래스, 비밀번호를 안전하게 해싱
    @Bean
    public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder();}

    @Bean
    protected SecurityFilterChain configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable) // 스프링 시큐리티의 HTTP 보안 설정에서 CSRF 보호기능 비활성화 (Cross-Site Request Forgery, 사이트 간 요청 위조)
                .formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 비활성화 : API 서버와 같이 폼 로그인이 필요없는 방식) 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(configurer -> configurer.configurationSource(corsConfigurationSource())) // (Cross-Origin Resource Sharing) 웹 앱의 보안을 유지하면서 다른 출처의 리소스 요청을 허용하도록 설정
                .sessionManagement(configure -> configure.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 서버가 사용자 세션을 유지하지 않음. 서버의 확장성을 높이고 client와 server 간의 결합도를 낮춘다.
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(this::onAuthenticationSuccess)
                        .failureHandler(this::onAuthenticationFailure)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(new CustomLogoutSuccessHandler(jwtTokenProvider, customUserDetailsService))
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .addFilter(new JwtAuthenticationFilter(jwtTokenProvider, userRepository, authenticationManager(customUserDetailsService), customUserDetailsService, "/api/auth"))
                .addFilterAfter(new JwtAuthorizationFilter(userRepository, jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(
//                                new AntPathRequestMatcher("/"),
//                                new AntPathRequestMatcher("/signup"),
//                                new AntPathRequestMatcher("/login"),
//                                new AntPathRequestMatcher("/auth"),
//                                new AntPathRequestMatcher("/token/**"),
//                                new AntPathRequestMatcher("/logout/**")
//                        ).permitAll()
//                        .anyRequest().authenticated())
                        .anyRequest().permitAll())
                .exceptionHandling(configurer -> configurer
                .accessDeniedHandler(new JwtAccessDeniedHandler())
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
        );
        return httpSecurity.build();
    }

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("OAuth Login Success!");
        //토큰 발급 시작
        String token = jwtTokenProvider.createToken(authentication);
        String refresh = jwtTokenProvider.createRefreshToken(authentication);
        log.info("test={}", authentication.getName());
        log.info("test={}", authentication.getAuthorities());
        log.info("test={}", authentication.getDetails());
        log.info("test={}", authentication.getClass());
        log.info("test={}", authentication.getPrincipal());

        log.info(token);
        log.info(refresh);
        ObjectMapper om = new ObjectMapper();

        response.addHeader("Authorization", "Bearer " + token);
        log.info("AccessToken in Header={}", token);
        log.info("header={}", response.getHeader("Authorization"));

        Cookie refreshTokenCookie = new Cookie("RefreshToken", refresh);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(60 * 60 * 24 * 7);
        response.addCookie(refreshTokenCookie);
        log.info("RefreshToken in Cookie={}", refresh);

        String role = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).findAny().orElse("");
        String userEmail = "";
        if(role.equals("ROLE_USER")){
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            userEmail = customUserDetails.getUsername();
        }
        User user = customUserDetailsService.selcetUser(userEmail);
        user.setRefreshToken(refresh);
        userRepository.save(user);
        UserDto userDto = new UserDto();
        userDto.setUserId(user.getId());
        userDto.setAccessToken("Bearer " + token);
        userDto.setRefreshToken(user.getRefreshToken());
        log.info("Response Body insert User");
        String result = om.registerModule(new JavaTimeModule()).writeValueAsString(userDto);
        response.getWriter().write(result);
//        response.sendRedirect("http://localhost:5173/oauth2/redirect/?token="+token);
        response.sendRedirect("https://k547f55f71a44a.user-app.krampoline.com/oauth2/redirect/?token="+token);
    }

    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authenticationProvider);
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addExposedHeader("*");
        corsConfiguration.setAllowCredentials(true);

//      corsConfiguration.setAllowedOrigins(List.of("https://ide-frontend-wheat.vercel.app/login", "https://ide-frontend-six.vercel.app", "https://ide-frontend-wheat.vercel.app"));
        corsConfiguration.setAllowedOrigins(List.of("https://ide-frontend-wheat.vercel.app/login", "krmp-d2hub-idock.9rum.cc/dev-test/repo_85a78215dc68", "https://ide-frontend-six.vercel.app", "https://ide-frontend-wheat.vercel.app", "http://localhost:3000", "https://k547f55f71a44a.user-app.krampoline.com", "http://localhost:5173"));

        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

}
