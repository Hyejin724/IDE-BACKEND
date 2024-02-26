package everyide.webide.websocket.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EnterResponseDto {
    private List<UserSession> userSessions;
}
