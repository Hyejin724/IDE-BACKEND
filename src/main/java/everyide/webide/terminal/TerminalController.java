package everyide.webide.terminal;

import everyide.webide.terminal.domain.TerminalExecuteRequestDto;
import everyide.webide.terminal.domain.TerminalExecuteResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalService terminalService;

    @MessageMapping("/container/{containerId}/terminal")
    @SendToUser(value = "/queue/container/{containerId}/terminal", broadcast = false)
    public TerminalExecuteResponseDto execute(
            @DestinationVariable Long containerId,
            @Payload TerminalExecuteRequestDto requestDto
    ) throws Exception {
        log.info("웹소켓 터미널 실행, containerId={}, command={}", containerId, requestDto.getCommand());

        return terminalService.executeCommand(containerId, requestDto);
    }
}