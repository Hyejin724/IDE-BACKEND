package everyide.webide.chatting.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MessageResponseDto {
    private String id;
    private Long userId;
    private String name;
    private String content;
}
