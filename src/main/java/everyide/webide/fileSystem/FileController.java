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

    @GetMapping("/api/{userId}/filetree/{containerName}")
    public  ResponseEntity<FileTreeResponse> getFileTree(@PathVariable("userId") Long userId, @PathVariable("containerName") String containerName) {
        return ResponseEntity.ok(fileService.listFilesAndDirectories(userId, containerName));
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

    @PostMapping("api/files")
    public ResponseEntity<?> createFile(@RequestBody CreateFileRequest createFileRequest) {
        fileService.createFile(createFileRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("파일 생성완료.");
    }

    @PatchMapping("api/files")
    public ResponseEntity<?> updateFile(@RequestBody UpdateFileRequest updateFileRequest) {
        fileService.updateFile(updateFileRequest);
        return ResponseEntity.status(HttpStatus.OK).body("파일 수정완료.");
    }

    @DeleteMapping("api/files")
    public ResponseEntity<?> deleteFile(@RequestBody DeleteFileRequest deleteFileRequest) {
        fileService.deleteFile(deleteFileRequest);
        return ResponseEntity.status(HttpStatus.OK).body("파일 삭제완료.");
    }
}
