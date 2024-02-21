package everyide.webide.websocket.userstate;

import everyide.webide.chat.MessageRepository;
import everyide.webide.chat.domain.MessageResponseDto;
import everyide.webide.config.auth.jwt.JwtTokenProvider;
import everyide.webide.user.UserRepository;
import everyide.webide.user.domain.User;
import everyide.webide.websocket.WebSocketRoomUserSessionMapper;
import everyide.webide.websocket.domain.EnterResponseDto;
import everyide.webide.websocket.domain.UserSession;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserStateController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebSocketRoomUserSessionMapper webSocketRoomUserSessionMapper;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @SubscribeMapping("/container/{containerId}/enter")
    public void enter(SimpMessageHeaderAccessor headerAccessor, @DestinationVariable String containerId) {
        String sessionId = headerAccessor.getUser().getName();
        webSocketRoomUserSessionMapper.putSession(
                containerId, sessionId, createUserSession(headerAccessor, containerId)
        );

        sendUserState(containerId);
        log.info("입장 세션={}", sessionId);
    }

    // 유저가 입장이나 퇴장할 때 수정된 유저들의 정보를 브로드캐스팅
    public void sendUserState(String containerId) {
        messagingTemplate.convertAndSend(
                "/topic/container/" + containerId + "/state",
                new EnterResponseDto(
                        webSocketRoomUserSessionMapper.getAllSessionsInContainer(containerId),
                        messageRepository.findTop10ByContainerIdOrderBySendDateDesc(containerId)
                                .stream()
                                .map((message -> MessageResponseDto.builder()
                                        .userId(message.getUserId())
                                        .name(message.getUserName())
                                        .content(message.getContent())
                                        .build()))
                                .collect(Collectors.toList())
                        )
                );
    }

    private UserSession createUserSession(SimpMessageHeaderAccessor headerAccessor, String containerId) {
        String token = getHeaderValue(headerAccessor, "Authorization").substring(7);
        User user = getUserFromToken(token);
        return new UserSession(user.getId(), user.getName(), user.getEmail(), containerId);
    }

    private String getHeaderValue(SimpMessageHeaderAccessor headerAccessor, String headerKey) {
        String value = headerAccessor.getFirstNativeHeader(headerKey);
        if (value == null || value.isEmpty()) {
            log.error("Header {} not found", headerKey);
            throw new IllegalArgumentException("Required header not found: " + headerKey);
        }
        return value;
    }

    private User getUserFromToken(String token) {
        if (!jwtTokenProvider.validateToken(token).equals("Success")) {
            System.out.println(jwtTokenProvider.validateToken(token));
            log.error("token 오류");
            throw new SecurityException("Invalid token");
        }
        String userEmail = jwtTokenProvider.getClaims(token).getSubject();
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found for email: " + userEmail));
    }
}
