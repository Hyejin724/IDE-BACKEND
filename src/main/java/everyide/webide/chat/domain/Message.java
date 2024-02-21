package everyide.webide.chat.domain;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Document(collection = "message")
@Getter
@ToString
@NoArgsConstructor
public class Message{

    private String id;
    private String containerId;
    private String contentType;
    private String content;
    private Long senderId;
    private LocalDateTime sendDate;

    @Builder
    public Message(String containerId, String contentType, String content, Long senderId) {
        id = UUID.randomUUID().toString();
        this.containerId = containerId;
        this.contentType = contentType;
        this.content = content;
        this.senderId = senderId;
        sendDate = LocalDateTime.now().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }
}
