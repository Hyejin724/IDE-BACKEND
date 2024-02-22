package everyide.webide.room;

import everyide.webide.config.auth.exception.NoRoomException;
import everyide.webide.config.auth.exception.RoomDestroyException;
import everyide.webide.config.auth.exception.ValidateRoomException;
import everyide.webide.container.ContainerRepository;
import everyide.webide.container.domain.Container;
import everyide.webide.fileSystem.DirectoryService;
import everyide.webide.fileSystem.FileService;
import everyide.webide.fileSystem.domain.Directory;
import everyide.webide.room.domain.*;
import everyide.webide.room.domain.dto.EnterRoomResponseDto;
import everyide.webide.room.domain.dto.RoomFixDto;
import everyide.webide.user.UserRepository;
import everyide.webide.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    @Value("${file.basePath}")
    private String basePath;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final DirectoryService directoryService;

    public Room create(CreateRoomRequestDto requestDto) {
        User currentUser = getCurrentUser();

        // 1. 방생성
        Room room = Room.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .isLocked(requestDto.getIsLocked())
                .password(requestDto.getPassword())
                .type(RoomType.valueOf(requestDto.getRoomType()))
                .maxPeople(requestDto.getMaxPeople())
                .usersId(new ArrayList<>())
                .owner(currentUser)
                .build();


        // 2. 방 디렉토리 생성 (방 아이디 이름으로)
        Directory rootDirectory = directoryService.createRootDirectory(room.getId());
        if (rootDirectory != null) {
            room.setRootPath(basePath + room.getId());
            log.info("방 디렉토리 생성 완료");
        } else {
            log.info("루트 디렉토리 생성불가");
        }

        room.getUsersId().add(currentUser.getId());
        roomRepository.save(room);

        return room;
    }

    public List<RoomResponseDto> loadAllRooms(String name) {
        if (name == null) {
            return roomRepository.findAllByAvailableTrue()
                    .stream()
                    .map(this::toRoomResponseDto)
                    .collect(Collectors.toList());
        }
        return roomRepository.findAllByNameContaining(name)
                .stream()
                .map(this::toRoomResponseDto)
                .collect(Collectors.toList());
    }

    public EnterRoomResponseDto enteredRoom(String roomId, String password) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoRoomException("Room not found"));

        // 비밀번호가 설정된 방인 경우, 비밀번호 확인
        validateRoomAccess(room, password);

        User currentUser = getCurrentUser();

        // 현재 사용자가 방의 사용자 목록에 없다면 추가
        addUserToRoomIfNotPresent(room, currentUser);

        // 사용자 이름 리스트 생성
        List<String> usersNames = getUsersNamesFromRoom(room);

        // 응답 객체 생성 및 반환
        return buildEnterRoomResponse(room, usersNames);
    }

    private void validateRoomAccess(Room room, String password) {
        if (room.getIsLocked() && !password.equals(room.getPassword())) {
            throw new ValidateRoomException("비밀번호가 틀렸습니다.");
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void addUserToRoomIfNotPresent(Room room, User user) {
        if (!room.getUsersId().contains(user.getId())) {
            room.getUsersId().add(user.getId());
            user.getRoomsList().add(room.getId()); // 유저의 룸 리스트에 추가
            roomRepository.save(room);
        }
    }

    private List<String> getUsersNamesFromRoom(Room room) {
        return room.getUsersId().stream()
                .map(userRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(User::getName)
                .collect(Collectors.toList());
    }

    private EnterRoomResponseDto buildEnterRoomResponse(Room room, List<String> usersNames) {
        return EnterRoomResponseDto.builder()
                .room(room)
                .usersName(usersNames)
                .ownerId(room.getOwner().getId())
                .build();
    }

    public void fixRoom(String roomId, RoomFixDto roomFixDto) {
        User user = getCurrentUser();
        Optional<Room> byId = roomRepository.findById(roomId);
        Room room = byId.orElseThrow();

        if (user.equals(room.getOwner())) {
            roomRepository.findById(roomId).ifPresent(r -> {
                // name 업데이트 (null이 아닌 경우에만)
                if (roomFixDto.getName() != null) {
                    room.setName(roomFixDto.getName());
                }

                // password와 isLocked 업데이트
                if (roomFixDto.getPassword() != null) {
                    room.setPassword(roomFixDto.getPassword());
                    room.setIsLocked(true);
                } else if (roomFixDto.getIsLocked() != null && !roomFixDto.getIsLocked()) {
                    // password가 null이고, isLocked가 명시적으로 false로 설정된 경우
                    room.setIsLocked(false);
                    room.setPassword(null); // isLocked가 false일 때 password도 null로 설정
                }

                // 변경사항을 데이터베이스에 저장
                roomRepository.save(r);
            });
        } else {
            throw new RuntimeException("방장이 아닌 사람은 방을 수정 할 수 없습니다.");
        }

    }

    public void leaveRoom(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        User user = getCurrentUser();

        if (user.getId().equals(room.getOwner().getId())) {
            User user1 = userRepository.findById(room.getUsersId().get(0)).orElseThrow();
            room.setOwner(user1);
        }

        room.getUsersId().remove(user.getId());

        if (room.getUsersId().isEmpty()) {
            room.setAvailable(false);
        }
        user.getRoomsList().remove(roomId);

        userRepository.save(user);
        roomRepository.save(room);
    }

    private RoomResponseDto toRoomResponseDto(Room room) {
        User user = getCurrentUser();
        boolean join;
        join = user.getRoomsList().contains(room.getId());
        return RoomResponseDto.builder()
                .roomId(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .isLocked(room.getIsLocked())
                .type(room.getType())
                .available(room.getAvailable())
                .maxPeople(room.getMaxPeople())
                .usersCnt(room.getUsersId().size())
                .isJoined(join)
                .ownerName(Optional.ofNullable(room.getOwner())
                        .map(User::getName)
                        .orElse("Unknown"))
                .build();
    }
}
