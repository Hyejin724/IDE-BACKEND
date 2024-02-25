package everyide.webide.config.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import everyide.webide.config.auth.dto.response.UserDto;
import everyide.webide.config.auth.jwt.JwtTokenProvider;
import everyide.webide.config.auth.user.CustomUserDetails;
import everyide.webide.config.auth.user.CustomUserDetailsService;
import everyide.webide.user.UserRepository;
import everyide.webide.user.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserRepository userRepository;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String token = jwtTokenProvider.createToken(authentication);
        String refresh = jwtTokenProvider.createRefreshToken(authentication);
        log.info(token);
        log.info(refresh);
        ObjectMapper om = new ObjectMapper();

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

        String url = makeRedirectUrl(token);
        System.out.println("url: " + url);

        if (response.isCommitted()) {
            logger.debug("응답이 이미 커밋된 상태입니다. " + url + "로 리다이렉트하도록 바꿀 수 없습니다.");
            return;
        }
        getRedirectStrategy().sendRedirect(request, response, url);
    }

    private String makeRedirectUrl(String token) {
        return UriComponentsBuilder.fromUriString("https://k547f55f71a44a.user-app.krampoline.com/oauth2/redirect/?token="+token)
//        return UriComponentsBuilder.fromUriString("https://ide-frontend-six.vercel.app/oauth2/redirect/?token="+token)
                .build().toUriString();
    }
}
