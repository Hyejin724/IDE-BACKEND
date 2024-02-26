package everyide.webide.config.auth.user.oauth2;

import com.amazonaws.util.StringUtils;
import everyide.webide.config.auth.exception.OAuth2AuthenticationProcessingException;
import everyide.webide.config.auth.user.CustomUserDetails;
import everyide.webide.fileSystem.DirectoryService;
import everyide.webide.fileSystem.domain.Directory;
import everyide.webide.user.UserRepository;
import everyide.webide.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {



    @Value("${file.basePath}")
    private String basePath;
    private final UserRepository userRepository;
    private final DirectoryService directoryService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        log.info("OAuth user loading...");

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(oAuth2UserRequest.getClientRegistration().getRegistrationId(), oAuth2User.getAttributes());
        log.info("Email={}", oAuth2UserInfo.getEmail());
        log.info("Id={}", oAuth2UserInfo.getId());
        log.info("Name={}", oAuth2UserInfo.getName());

        if (StringUtils.isNullOrEmpty(oAuth2UserInfo.getEmail())) {
            log.info("Email is Null or Empty");
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        log.info("Find User By Email");
        Optional<User> byEmail = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;
        if (byEmail.isPresent()) {
            user = byEmail.get();
            if (!user.getProvider().equals(AuthProvider.valueOf((oAuth2UserRequest.getClientRegistration().getRegistrationId())))) {
                log.info("provider Id is True");
                throw new OAuth2AuthenticationProcessingException("Looks like you're signed up with " +
                        user.getProvider() + " account. Please use your " + user.getProvider() +
                        " account to login.");
            }
            log.info("Is different Provider Id then Process Update User");
            user = updateExistUser(user, oAuth2UserInfo);
        } else {
            log.info("Welcome New User, Sign In");
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }
        return CustomUserDetails.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        AuthProvider provider = AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId());
        String providerId = oAuth2UserInfo.getId();
        String name = oAuth2UserInfo.getName();
        String email = oAuth2UserInfo.getEmail();
        String imageUrl = oAuth2UserInfo.getImageUrl();
        String role = "USER";
        log.info("find all info from New User");

        User newUser = User.createNewUser(provider, providerId, name, email, imageUrl, role);
        log.info("provider={}", provider);
        log.info("providerId={}", providerId);
        log.info("name={}", name);
        log.info("email={}", email);
        log.info("imageUrl={}", imageUrl);

        Directory rootDirectory = directoryService.createRootDirectory(newUser.getEmail());
        if (rootDirectory != null) {
            newUser.setRootPath(basePath + newUser.getEmail());
            log.info("회원 등록완료");
        } else {
            log.info("루트 디렉토리 생성불가");
        }
        return userRepository.save(newUser);
    }

    private User updateExistUser(User user, OAuth2UserInfo oAuth2UserInfo) {
        user.updateOAuth2UserInfo(oAuth2UserInfo);
        return userRepository.save(user);
    }
}