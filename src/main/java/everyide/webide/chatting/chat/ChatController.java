package everyide.webide.chatting.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 입장시 채팅기록 전송 or 위로 스크롤하면 이전 기록 전송
    @GetMapping("/api/container/{containerId}/chat/prev")
    public ResponseEntity<?> getPrevMessages(@PathVariable String containerId, @RequestParam(required = false) String cursor) {
        if (cursor == null) {
            return ResponseEntity.ok(chatService.getMessages(containerId));
        } else {
            return ResponseEntity.ok((chatService.getPrevMessages(containerId, cursor)));
        }
    }

    // 아래로 스크롤하면 다음 기록 전송
    @GetMapping("/api/container/{containerId}/chat/next")
    public ResponseEntity<?> getNextMessages(@PathVariable String containerId, @RequestParam String cursor) {
        return ResponseEntity.ok(chatService.getNextMessages(containerId, cursor));
    }

    // 메세지 검색
    @GetMapping("/api/container/{containerId}/chat/{keyword}/{seq}")
    public ResponseEntity<?> searchMessage(@PathVariable String containerId, @PathVariable String keyword, @PathVariable int seq) {
        return ResponseEntity.ok(chatService.searchMessage(containerId, keyword, seq));
    }
}
