package everyide.webide.fileSystem;

import everyide.webide.fileSystem.domain.dto.CreateDirectoryRequest;
import everyide.webide.fileSystem.domain.dto.DeleteDirectoryRequest;
import everyide.webide.fileSystem.domain.dto.UpdateDirectoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DirectoryController {

    private final DirectoryService directoryService;

    @GetMapping("/api/directories")
    public void getDirectory() {

    }

    @PostMapping("/api/directories")
    public ResponseEntity<?> createDirectory(@RequestBody CreateDirectoryRequest createDirectoryRequest) {
        String status = directoryService.createDirectory(createDirectoryRequest);
        if (status.equals("ok")) {
            return ResponseEntity.status(HttpStatus.OK).body("디렉토리 생성완료.");
        } else if (status.equals("already")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 사용중인 이름입니다.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("디렉토리 생성불가.");
        }
    }

    @PatchMapping("/api/directories")
    public ResponseEntity<?> updateDirectory(@RequestBody UpdateDirectoryRequest updateDirectoryRequest) {
        String status = directoryService.updateDirectory(updateDirectoryRequest);
        if (status.equals("ok")) {
            return ResponseEntity.status(HttpStatus.OK).body("디렉토리 업데이트 완료.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("디렉토리 업데이트 실패.");
        }
    }

    @DeleteMapping("/api/directories")
    public ResponseEntity<?> deleteDirectory(@RequestBody DeleteDirectoryRequest deleteDirectoryRequest) {
        String status = directoryService.deleteDirectory(deleteDirectoryRequest);
        if (status.equals("ok")) {
            return ResponseEntity.status(HttpStatus.OK).body("디렉토리 삭제완료.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("디렉토리 삭제실패.");
        }
    }
}
