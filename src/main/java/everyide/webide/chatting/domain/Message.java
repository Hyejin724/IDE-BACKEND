package everyide.webide.chatting.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

//@Document(collection = "message")\
@Entity
@Getter
//@ToString
@NoArgsConstructor
public class Message{

    @Id
    private String id;
    private String containerId;
    private String content;
    private Long userId;
    private String userName;
    private LocalDateTime sendDate;

    @Builder
    public Message(String containerId, String content, Long userId, String userName) {
        id = UUID.randomUUID().toString();
        this.containerId = containerId;
        this.content = content;
        this.userId = userId;
        this.userName = userName;
        sendDate = LocalDateTime.now().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }
}
