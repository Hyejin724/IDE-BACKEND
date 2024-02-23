package everyide.webide.chat.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ManyMessagesResponseDto {
    private List<MessageResponseDto> messages;
}
