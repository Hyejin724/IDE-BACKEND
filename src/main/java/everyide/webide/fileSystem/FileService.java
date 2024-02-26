package everyide.webide.fileSystem;

import everyide.webide.container.ContainerRepository;
import everyide.webide.container.ContainerService;
import everyide.webide.container.domain.Container;
import everyide.webide.fileSystem.domain.Directory;
import everyide.webide.fileSystem.domain.dto.*;
import everyide.webide.room.RoomRepository;
import everyide.webide.room.domain.Room;
import everyide.webide.user.UserRepository;
import everyide.webide.user.domain.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${file.basePath}")
    private String basePath;
    private final FileRepository fileRepository;
    private final DirectoryRepository directoryRepository;
    private final UserRepository userRepository;
    private final ContainerRepository containerRepository;
    private final RoomRepository roomRepository;

    public GetFileResponse getFile(Long containerId, String path) {
        Optional<Container> containerOptional = containerRepository.findById(containerId);

        if (containerOptional.isPresent()) {
            Container container = containerOptional.get();
            String filePath = container.getPath() + path;

            if (!Files.exists(Paths.get(filePath))) {
                return null;
            }

            try {
                String content = new String(Files.readAllBytes(Paths.get(filePath)));
                return new GetFileResponse(content);
            } catch (IOException e) {
                // IOException 처리
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public String createFile(CreateFileRequest createFileRequest) {
        String path = basePath + createFileRequest.getEmail() + createFileRequest.getPath();
        File file = new File(path);

        try {
            if (file.createNewFile()) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(createFileRequest.getContent());
                }
                everyide.webide.fileSystem.domain.File newFile = everyide.webide.fileSystem.domain.File.builder()
                        .path(path)
                        .content(createFileRequest.getContent())
                        .build();
                fileRepository.save(newFile);

                log.info(path + " 파일이 생성되었습니다.");
                return "ok";
            } else {
                log.info(path + " 파일은 이미 존재합니다.");
                return "already used";
            }
        } catch (IOException e) {
            log.error("파일 생성 중 오류 발생", e);
            return "cant";
        }
    }

    public String updateFile(UpdateFileRequest updateFileRequest) {
        String oldPath = basePath + updateFileRequest.getEmail() + updateFileRequest.getFromPath();
        File oldFile = new File(oldPath);

        if (updateFileRequest.getNewContent() != null && !updateFileRequest.getNewContent().isEmpty()) {
            try (FileWriter writer = new FileWriter(oldFile, false)) { // false to overwrite
                writer.write(updateFileRequest.getNewContent());

                everyide.webide.fileSystem.domain.File findFile = fileRepository.findByPath(oldPath)
                        .orElseThrow(() -> new EntityNotFoundException("File not found."));
                fileRepository.save(findFile.updateContent(updateFileRequest.getNewContent()));

                log.info(oldPath + " 파일이 업데이트되었습니다.");
                return "ok";
            } catch (IOException e) {
                log.error("파일 업데이트 중 오류 발생", e);
                return "cant";
            }
        }

        if (updateFileRequest.getToPath() != null && !updateFileRequest.getToPath().isEmpty()) {
            String newPath = basePath + updateFileRequest.getEmail() + updateFileRequest.getToPath();
            File newFile = new File(newPath);

            if (oldFile.renameTo(newFile)) {

                everyide.webide.fileSystem.domain.File findFile = fileRepository.findByPath(oldPath)
                        .orElseThrow(() -> new EntityNotFoundException("File not found."));
                fileRepository.save(findFile.updateFile(newPath));

                log.info(oldPath + " 파일이 " + newPath + "로 이름이 변경되었습니다.");
                return "ok";
            } else {
                log.error("파일 이름 변경 실패");
                return "cant";
            }
        } else {
            return "cant";
        }
    }


    public String deleteFile(DeleteFileRequest deleteFileRequest) {
        String path = basePath + deleteFileRequest.getEmail() + deleteFileRequest.getPath();
        File file = new File(path);

        if (file.delete()) {
            everyide.webide.fileSystem.domain.File findFile = fileRepository.findByPath(path)
                    .orElseThrow();
            fileRepository.delete(findFile);

            log.info(path + " 파일이 삭제되었습니다.");
            return "ok";
        } else {
            log.error("파일 삭제 실패");
            return "cant";
        }
    }

    public void createDefaultFile(String path, String language) {
        String fileName = null;
        String defaultContent = null;

        if (language.equals("java")) {
            fileName = "Main.java";
            defaultContent = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}";

        } else if (language.equals("javascript")) {
            fileName = "main.js";
            defaultContent = "console.log('Hello, World!');";
        } else {
            fileName = "main.py";
            defaultContent = "print('Hello, World!')";
        }

        File file = new File(path, fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(defaultContent);
            writer.flush();

            everyide.webide.fileSystem.domain.File defaultFile = everyide.webide.fileSystem.domain.File.builder()
                    .path(path + "/" + fileName)
                    .content(defaultContent)
                    .build();
            fileRepository.save(defaultFile);

        } catch (IOException e) {
            e.printStackTrace(); // 파일 생성 실패시 로그에 스택 트레이스 출력
        }

        File readme = new File(path, "README.md");
        try (FileWriter writer = new FileWriter(readme)) {
            String content = "# 컨테이너 이름\n" +
                    "\n" +
                    "## 개요\n" +
                    "\n" +
                    "이 컨테이너는 [간단한 설명]을 위해 만들어졌습니다. 여기에는 [기술 스택, 사용된 도구, 목적 등]에 대한 정보가 포함되어야 합니다.\n" +
                    "\n" +
                    "## 시작하기\n" +
                    "\n" +
                    "이 섹션에서는 컨테이너를 사용하기 위한 사전 요구 사항과 시작 방법을 설명합니다.\n" +
                    "\n" +
                    "### 사전 요구 사항\n" +
                    "\n" +
                    "컨테이너를 사용하기 위해 필요한 도구, 라이브러리, 환경 설정 등을 나열합니다.";
            writer.write(content);
            writer.flush();

            everyide.webide.fileSystem.domain.File readmeFile = everyide.webide.fileSystem.domain.File.builder()
                    .path(path + "/README.md")
                    .content(content)
                    .build();

            fileRepository.save(readmeFile);

        } catch (IOException e) {
            e.printStackTrace(); // 파일 생성 실패시 로그에 스택 트레이스 출력
        }

    }

    public String extractPathAfterEmail(String fullPath) {
        String[] parts = fullPath.split("/");

        int emailIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].contains("@") || parts[i].contains("-")) {
                emailIndex = i;
                break;
            }
        }

        if (emailIndex != -1 && emailIndex + 1 < parts.length) {
            StringBuilder sb = new StringBuilder();
            for (int i = emailIndex + 2; i < parts.length; i++) {
                sb.append("/");
                sb.append(parts[i]);
            }
            return sb.toString();
        }

        return "";
    }

    public FileTreeResponse listFilesAndDirectories(String id, String containerName) {
        String path = null;
        try {
            Long longId = Long.parseLong(id);
            Optional<User> findUser = userRepository.findById(longId);
            User user = findUser.get();
            path = basePath + user.getEmail() + "/" + containerName;
        } catch (NumberFormatException e) {
            path = basePath + id + "/" + containerName;
        }

        File root = new File(path);
        return listFilesAndDirectoriesRecursive(root);
    }

    private FileTreeResponse listFilesAndDirectoriesRecursive(File directory) {
        String type = directory.isDirectory() ? "directory" : "file";
        String path = extractPathAfterEmail(directory.getPath()).isEmpty() ? "/" : extractPathAfterEmail(directory.getPath());
        String id = null;

        log.info(directory.getPath());
        if (type.equals("directory")) {
            Directory findDirectory = directoryRepository.findByPath(directory.getPath())
                    .orElseThrow(() -> new EntityNotFoundException("Not found."));

            id = findDirectory.getId();
        } else {
            everyide.webide.fileSystem.domain.File findFile = fileRepository.findByPath(directory.getPath())
                    .orElseThrow(() -> new EntityNotFoundException("Not found."));
            id = findFile.getId();
        }

        FileTreeResponse fileInfo = new FileTreeResponse(id, directory.getName(), type, path, new ArrayList<>());

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {

                if (".DS_Store".equals(file.getName())) {
                    continue;
                }

                FileTreeResponse child = listFilesAndDirectoriesRecursive(file);
                System.out.println("child = " + child.getPath());
                fileInfo.getChildren().add(child);
            }
        }

        return fileInfo;
    }
}
