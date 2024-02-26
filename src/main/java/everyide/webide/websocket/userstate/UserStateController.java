package everyide.webide.websocket.userstate;

import everyide.webide.websocket.WebSocketRoomUserSessionMapper;
import everyide.webide.websocket.domain.EnterResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserStateController {

    private final WebSocketRoomUserSessionMapper webSocketRoomUserSessionMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @SubscribeMapping("/container/{containerId}/enter")
    public void enter( @DestinationVariable String containerId) {
        sendUserState(containerId);
    }

    // 유저가 입장이나 퇴장할 때 수정된 유저들의 정보를 브로드캐스팅
    public void sendUserState(String containerId) {
        messagingTemplate.convertAndSend(
                "/topic/container/" + containerId + "/state",
                EnterResponseDto.builder()
                        .userSessions(webSocketRoomUserSessionMapper.getAllSessionsInContainer(containerId))
                        .build()
        );
    }
}
