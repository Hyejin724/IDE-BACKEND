package everyide.webide.chatting.message;

import everyide.webide.chatting.domain.Message;
import everyide.webide.chatting.domain.dto.MessageRequestDto;
import everyide.webide.chatting.domain.dto.MessageResponseDto;
import everyide.webide.user.UserRepository;
import everyide.webide.user.domain.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Controller
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    @MessageMapping("/container/{containerId}/chat")
    @SendTo("/topic/container/{containerId}/chat")
    public MessageResponseDto message(MessageRequestDto messageRequestDto, @DestinationVariable String containerId) {
        User user = userRepository.findById(messageRequestDto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("없는 유저"));

        Message message = Message.builder()
                .containerId(containerId)
                .content(messageRequestDto.getContent())
                .userId(messageRequestDto.getUserId())
                .userName(user.getName())
                .build();
        messageRepository.save(message);

        return new MessageResponseDto(message.getId(), message.getUserId(), user.getName(), messageRequestDto.getContent());
    }
}
