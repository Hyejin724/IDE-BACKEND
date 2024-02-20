package everyide.webide.room;

import everyide.webide.room.domain.CreateRoomRequestDto;
import everyide.webide.room.domain.Room;
import everyide.webide.room.domain.RoomType;
import everyide.webide.room.domain.dto.RoomFixDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> loadAllRooms() {
        return ResponseEntity.ok().body(roomService.loadAllRooms());
    }

    @PatchMapping("/api/community/{roomId}/settings")
    public void updateRoom(@PathVariable("roomId") String roomId, @RequestBody RoomFixDto roomfixDto) {
        roomService.fixRoom(roomId, roomfixDto);
    }

    @GetMapping("/api/community/{roomId}")
    public ResponseEntity<?> enterRoom(@PathVariable("roomId") String roomId) {
        Room room = roomService.enteredRoom(roomId);
        return ResponseEntity.ok(room);
    }
    // 방에 들어갔을때 어떤 것들을 띄워야하는지 상의하기 일단 방만 띄움

    @GetMapping("/api/community/{roomId}/leave")
    public void communityOut(@PathVariable("roomId") String roomId) {
        roomService.leaveRoom(roomId);
    }

    @GetMapping("/api/communities/search")
    public ResponseEntity<?> searchRooms(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "type", required = false) RoomType type) {
        return ResponseEntity.ok(roomService.searchRooms(name, type));
    }
}