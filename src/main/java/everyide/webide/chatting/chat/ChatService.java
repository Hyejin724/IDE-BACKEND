package everyide.webide.chatting.chat;

import everyide.webide.chatting.domain.Message;
import everyide.webide.chatting.domain.dto.ManyMessagesResponseDto;
import everyide.webide.chatting.domain.dto.MessageResponseDto;
import everyide.webide.chatting.domain.dto.SearchMessageResponseDto;
import everyide.webide.chatting.message.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final MessageRepository messageRepository;

    public ManyMessagesResponseDto getMessages(String containerId) {
        List<Message> messageList = messageRepository.findTop10ByContainerIdOrderBySendDateDesc(containerId);

        return new ManyMessagesResponseDto(makeResponseDto(messageList));
    }

    public ManyMessagesResponseDto getPrevMessages(String containerId, String messageId) {
        List<Message> messageList = messageRepository.findTop10ByContainerIdAndIdLessThanOrderBySendDateDesc(containerId, messageId);

        return new ManyMessagesResponseDto(makeResponseDto(messageList));
    }

    public ManyMessagesResponseDto getNextMessages(String containerId, String messageId) {
        List<Message> messageList = messageRepository.findTop10ByContainerIdAndIdGreaterThanOrderBySendDateAsc(containerId, messageId);

        return new ManyMessagesResponseDto(makeResponseDto(messageList));
    }

    public SearchMessageResponseDto searchMessage(String containerId, String keyword, int seq) {
        List<Message> keywordMessages = messageRepository.findByContainerIdAndContentContaining(containerId, keyword);

        if (seq < 1 || seq > keywordMessages.size()) {
            throw new IllegalArgumentException("Invalid seq number");
        }
        Message targetMessage = keywordMessages.get(seq - 1);

        List<Message> prevMessageList = messageRepository.findTop10ByContainerIdAndIdLessThanOrderBySendDateDesc(containerId, targetMessage.getId());
        List<Message> nextMessageList = messageRepository.findTop10ByContainerIdAndIdGreaterThanOrderBySendDateAsc(containerId, targetMessage.getId());

        return new SearchMessageResponseDto(makeResponseDto(prevMessageList), targetMessage, makeResponseDto(nextMessageList));
    }

    private List<MessageResponseDto> makeResponseDto(List<Message> messageList) {
        List<MessageResponseDto> responseDtoList = messageList
                .stream()
                .map(message -> MessageResponseDto.builder()
                        .id(message.getId())
                        .userId(message.getUserId())
                        .name(message.getUserName())
                        .content(message.getContent())
                        .build())
                .collect(Collectors.toList());

        Collections.reverse(responseDtoList);
        return responseDtoList;
    }
}
