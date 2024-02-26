package everyide.webide.room;

import everyide.webide.room.domain.CreateRoomRequestDto;
import everyide.webide.room.domain.Room;
import everyide.webide.room.domain.RoomResponseDto;
import everyide.webide.room.domain.dto.EnterRoomResponseDto;
import everyide.webide.room.domain.dto.RoomFixDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/api/community")
    public ResponseEntity<?> createRoom(@RequestBody CreateRoomRequestDto requestDto) {
        Room room = roomService.create(requestDto);
        return ResponseEntity.ok(room.getId());
    }

    @GetMapping("/api/communities")
    public ResponseEntity<Slice<RoomResponseDto>> loadAllRooms(
            @RequestParam(value = "name", required = false) String name,
            Pageable pageable) {
        Slice<RoomResponseDto> responseDto = roomService.loadAllRooms(name, pageable);
        return ResponseEntity.ok().body(responseDto);
    }

    @PatchMapping("/api/community/{roomId}/settings")
    public ResponseEntity<?> updateRoom(@PathVariable("roomId") String roomId, @RequestBody RoomFixDto roomfixDto) {
        roomService.fixRoom(roomId, roomfixDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/community/{roomId}")
    public ResponseEntity<?> enterRoom(@PathVariable("roomId") String roomId,
                                       @RequestParam(value = "password", required = false) String password) {
        EnterRoomResponseDto room = roomService.enteredRoom(roomId, password);
        return ResponseEntity.ok(room);
    }
    // 방에 들어갔을때 어떤 것들을 띄워야하는지 상의하기 일단 방만 띄움

    @PatchMapping("/api/community/{roomId}/leave")
    public void communityOut(@PathVariable("roomId") String roomId) {
        roomService.leaveRoom(roomId);
    }

    @DeleteMapping("/api/community/{roomId}")
    public void communityDelete(@PathVariable("roomId") String roomId) {
        roomService.deleteRoom(roomId);
    }
}