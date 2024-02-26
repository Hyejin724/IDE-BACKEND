package everyide.webide.chatting.domain.dto;

import lombok.Getter;

@Getter
public class MessageRequestDto {
    private Long userId;
    private String content;
}
