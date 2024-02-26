package everyide.webide.config.auth.user.oauth2;


import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

@Slf4j
public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if(registrationId.equalsIgnoreCase(AuthProvider.google.name())) {
            log.info("login With={}", registrationId);
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.github.name())) {
            log.info("login With={}", registrationId);
            return new GithubOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.kakao.name())) {
            log.info("login With={}", registrationId);
            return new KakaoOAuth2UserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationException(registrationId + " is not support");
        }
    }

}