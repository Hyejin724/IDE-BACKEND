package everyide.webide.chatting.domain.dto;

import everyide.webide.chatting.domain.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SearchMessageResponseDto {
    private List<MessageResponseDto> prevMessageList;
    private Message targetMessage;
    private List<MessageResponseDto> nextMessageList;
}
