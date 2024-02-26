package everyide.webide.config.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import everyide.webide.config.auth.jwt.JwtTokenProvider;
import everyide.webide.config.auth.user.CustomUserDetails;
import everyide.webide.user.UserRepository;
import everyide.webide.user.domain.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthorizationFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String accessToken = request.getHeader("Authorization");
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("RefreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (accessToken != null) {
            String tokenValidationResult = jwtTokenProvider.validateToken(accessToken.replace("Bearer ", ""));
            if ("token expired.".equals(tokenValidationResult)) {
                log.info("AccessToken is expired");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } else if (!"Success".equals(tokenValidationResult)) {
                log.info("is not validate token");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // 토큰이 유효한 경우 사용자 인증 처리
            Authentication authentication = getUsernamePasswordAuthentication(request);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (refreshToken != null) {
            // 리프레시 토큰으로 새 엑세스 토큰 발급
            log.info("Use Refresh Token And Make New Access Token");
            String tokenValidationResult = jwtTokenProvider.validateToken(refreshToken.replace("Bearer ", ""));
            if ("token expired.".equals(tokenValidationResult)) {
                log.info("AccessToken is expired");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            String newAccessToken = jwtTokenProvider.generateAccessTokenFromRefreshToken(refreshToken);
            response.setHeader("NewAccessToken","Bearer " + newAccessToken); // 새 토큰을 헤더에 추가
            response.setHeader("expTime", "3600");

                //공통 에러 클래스 필요한 이유
                // 프론트가 정해진 형식을 받아야 된다 (우리만의 오류코드 오류 디스크립션, 다른 메타데이터 등이 들어가야 프론트에서 특정에러를 받았을때 정형화된 형식으로 response를 파싱하고 적절 조치를 취할수 있음)
                // 리프레시토큰 만료같은 경우는 -> 우리가 내려주는 리스폰스를 우리만의 공통에러 클래스로 지정해서 그 케이스를 별도로 처리 가능
                // 에러 명세를 만드는 것 ex(6000~7000 기능에러~, 이런식으로)
        }
        chain.doFilter(request, response);
    }

    private Authentication getUsernamePasswordAuthentication(HttpServletRequest request) {
        String token = request.getHeader("Authorization")
                .replace("Bearer ", "");

        Claims claims = jwtTokenProvider.getClaims(token);
        String email = claims.getSubject();

        log.info(email);

        if(email != null) {
            Optional<User> oUser = userRepository.findByEmail(email);
            User user = oUser.get();
            CustomUserDetails customUserDetails = CustomUserDetails.create(user);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(customUserDetails.getUsername(), null, customUserDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication); // 세션에 넣기
            return authentication;
        }
        return null;
    }
}
