package everyide.webide.fileSystem;

import everyide.webide.fileSystem.domain.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping("/api/{id}/filetree/{containerName}")
    public  ResponseEntity<FileTreeResponse> getFileTree(@PathVariable("id") String id, @PathVariable("containerName") String containerName) {
        return ResponseEntity.ok(fileService.listFilesAndDirectories(id, containerName));
    }

    @GetMapping("/api/containers/{containerId}/files")
    public ResponseEntity<?> getFile(@PathVariable("containerId") Long id, @RequestParam("path") String path) {
        GetFileResponse response = fileService.getFile(id, path);
        if (response != null) {
            return ResponseEntity.ok().body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("파일을 찾을 수 없습니다.");
        }

    }

    @PostMapping("/api/files")
    public ResponseEntity<?> createFile(@RequestBody CreateFileRequest createFileRequest) {
        String status = fileService.createFile(createFileRequest);
        if (status.equals("ok")) {
            return ResponseEntity.status(HttpStatus.OK).body("파일 생성완료.");
        } else if (status.equals("already used")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 사용중인 이름입니다.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("존재하지 않는 경로입니다.");
        }
    }

    @PatchMapping("/api/files")
    public ResponseEntity<?> updateFile(@RequestBody UpdateFileRequest updateFileRequest) {
        String status = fileService.updateFile(updateFileRequest);
        if (status.equals("ok")) {
            return ResponseEntity.status(HttpStatus.OK).body("파일 업데이트 완료.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("파일 업데이트 실패.");
        }
    }

    @DeleteMapping("/api/files")
    public ResponseEntity<?> deleteFile(@RequestBody DeleteFileRequest deleteFileRequest) {
        String status = fileService.deleteFile(deleteFileRequest);
        if (status.equals("ok")) {
            return ResponseEntity.status(HttpStatus.OK).body("파일 삭제 완료.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("파일 삭제 실패.");
        }
    }
}
