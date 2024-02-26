package everyide.webide.container;

import everyide.webide.container.domain.*;
import everyide.webide.fileSystem.DirectoryRepository;
import everyide.webide.fileSystem.DirectoryService;
import everyide.webide.fileSystem.FileRepository;
import everyide.webide.fileSystem.FileService;
import everyide.webide.fileSystem.domain.Directory;
import everyide.webide.fileSystem.domain.dto.DeleteDirectoryRequest;
import everyide.webide.fileSystem.domain.dto.UpdateDirectoryRequest;
import everyide.webide.room.RoomRepository;
import everyide.webide.room.domain.Room;
import everyide.webide.user.UserRepository;
import everyide.webide.user.domain.User;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerService {

    @Value("${file.basePath}")
    private String basePath;
    private final ContainerRepository containerRepository;
    private final DirectoryRepository directoryRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final DirectoryService directoryService;
    private final RoomRepository roomRepository;
    private final FileRepository fileRepository;

    public List<ContainerDetailResponse> getContainer(String id) {
        try {
            Long longId = Long.parseLong(id);
            return userRepository.findById(longId)
                    .map(user -> user.getContainers().stream()
                            .map(this::toContainerDetailResponse)
                            .collect(Collectors.toList()))
                    .orElse(Collections.emptyList());
        } catch (NumberFormatException e) {
            try {
                return roomRepository.findById(id)
                        .map(room -> room.getContainers().stream()
                                .map(this::toContainerDetailResponse)
                                .collect(Collectors.toList()))
                        .orElse(Collections.emptyList());
            } catch (IllegalArgumentException illegalArgumentException) {
                log.error("Invalid ID format: " + id);
                return Collections.emptyList();
            }
        }
    }

    private ContainerDetailResponse toContainerDetailResponse(Container container) {
        return ContainerDetailResponse.builder()
                .id(container.getId())
                .name(container.getName())
                .description(container.getDescription())
                .language(container.getLanguage())
                .active(container.isActive())
                .shared(container.getShared())
                .createDate(container.getCreateDate())
                .lastModifiedDate(container.getLastModifiedDate())
                .build();
    }


    public ContainerDetailResponse createContainer(CreateContainerRequest createContainerRequest) {
        Optional<User> findUser = userRepository.findByEmail(createContainerRequest.getEmail());
        Optional<Room> findRoom = roomRepository.findById(createContainerRequest.getEmail());

        if (findUser.isEmpty() && findRoom.isEmpty()) {
            return ContainerDetailResponse.builder()
                    .id(-200L)
                    .build();
        }

        String path = basePath + createContainerRequest.getEmail() + "/" + createContainerRequest.getName();
        File container = new File(path);

        if (container.exists()) {
            return ContainerDetailResponse.builder()
                    .id(-300L)
                    .build();
        }

        boolean isCreated = container.mkdir();
        if (!isCreated) {
            return ContainerDetailResponse.builder()
                    .id(-200L)
                    .build();
        }

        try {
            Container newContainer = Container.builder()
                    .name(createContainerRequest.getName())
                    .description(createContainerRequest.getDescription())
                    .path(path)
                    .language(createContainerRequest.getLanguage())
                    .build();

            findUser.ifPresent(newContainer::setUser);
            findRoom.ifPresent(newContainer::setRoom);

            containerRepository.save(newContainer);
            Directory directory = Directory.builder()
                    .path(path)
                    .build();
            directoryRepository.save(directory);
            fileService.createDefaultFile(path, createContainerRequest.getLanguage());

            return ContainerDetailResponse.builder()
                    .id(newContainer.getId())
                    .name(newContainer.getName())
                    .description(newContainer.getDescription())
                    .language(newContainer.getLanguage())
                    .active(newContainer.isActive())
                    .shared(newContainer.getShared())
                    .createDate(newContainer.getCreateDate())
                    .lastModifiedDate(newContainer.getLastModifiedDate())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return ContainerDetailResponse.builder()
                    .id(-200L)
                    .build();
        }
    }



    @Transactional
    public boolean deleteContainer(DeleteContainerRequest deleteContainerRequest) {
        String path = basePath + deleteContainerRequest.getEmail() + "/" + deleteContainerRequest.getName();
        File container = new File(path);

        if (container.exists()) {
            Container findContainer = containerRepository.findByPath(path)
                    .orElseThrow(() -> new EntityNotFoundException("Container not found."));
            if (deleteContainerRequest.getEmail().contains("@")) {
                findContainer.getUser().removeContainer(findContainer);
            } else {
                findContainer.getRoom().removeContainer(findContainer);
            }

            if (findContainer.getSourceContainer() != null) {
                Optional<Container> optionalContainer = containerRepository.findById(findContainer.getSourceContainer());

                if (optionalContainer.isPresent()) {
                    Container sourceContainer = optionalContainer.get();
                    sourceContainer.unshare();
                    containerRepository.save(sourceContainer);
                }
            }

            containerRepository.delete(findContainer);
            directoryService.deleteDirectory(new DeleteDirectoryRequest(deleteContainerRequest.getEmail(), "/" + deleteContainerRequest.getName()));

            return true;
        } else {
            return false;
        }
    }

    /*
     * 컨테이너 수정
     * */
    @Transactional
    public String updateContainer(UpdateContainerRequest updateContainerRequest) {
        String path = basePath + updateContainerRequest.getEmail();
        String oldPath = path + "/" + updateContainerRequest.getOldName();
        String newPath = path + "/" + updateContainerRequest.getNewName();

        File oldContainer = new File(oldPath);
        File newContainer = new File(newPath);

        if (oldContainer.exists()) {
            if (!oldPath.equals(newPath) && newContainer.exists()) {
                return "already used";
            }

            Container container = containerRepository.findByPath(oldPath)
                    .orElseThrow(() -> new EntityNotFoundException("Container not found."));

            containerRepository.save(container.updateContainer(updateContainerRequest.getNewName(), newPath, updateContainerRequest.getNewDescription(), updateContainerRequest.isActive()));
//            oldContainer.renameTo(newContainer);

            directoryService.updateDirectory(new UpdateDirectoryRequest(updateContainerRequest.getEmail(), "/" + updateContainerRequest.getOldName(), "/" + updateContainerRequest.getNewName()));

            return "ok";
        } else {
            return "not found";
        }
    }

    @Transactional
    public ContainerDetailResponse copyContainer(Long id, CopyContainerRequest copyContainerRequest) {
        Container sourceContainer = containerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Source container not found."));

        String newPath = basePath + copyContainerRequest.getRoomId() + "/" + sourceContainer.getName();
        File newContainerDir = new File(newPath);
        if (newContainerDir.exists()) {
//            throw new IllegalStateException("Target container already exists.");
            return ContainerDetailResponse.builder()
                    .id(-400L)
                    .build();
        }

        try {
            FileUtils.copyDirectory(new File(sourceContainer.getPath()), newContainerDir);
        } catch (IOException e) {
            log.error("Failed to copy container directory", e);
            throw new RuntimeException("Failed to copy container.");
        }

        Container newContainer = Container.builder()
                .name(sourceContainer.getName())
                .description(sourceContainer.getDescription())
                .path(newPath)
                .language(sourceContainer.getLanguage())
                .build();
//        newContainer.setUser(userRepository.findByEmail(copyContainerRequest.getRoomId())
//                .orElseThrow(() -> new EntityNotFoundException("not found")));
        newContainer.setRoom(roomRepository.findById(copyContainerRequest.getRoomId())
                .orElseThrow(()-> new EntityNotFoundException("Room not found.")));
        newContainer.setSourceContainer(sourceContainer.getId());

        containerRepository.save(newContainer);
        containerRepository.save(sourceContainer.share());

        Directory newDirectory = Directory.builder()
                .path(newPath)
                .build();
        directoryRepository.save(newDirectory);

        fileService.createDefaultFile(newPath, newContainer.getLanguage());

        return ContainerDetailResponse.builder()
                .id(newContainer.getId())
                .name(newContainer.getName())
                .description(newContainer.getDescription())
                .language(newContainer.getLanguage())
                .active(newContainer.isActive())
                .shared(newContainer.getShared())
                .createDate(newContainer.getCreateDate())
                .lastModifiedDate(newContainer.getLastModifiedDate())
                .build();
    }

    private void copyDirectoryContents(String sourcePath, String targetPath, User user) {
        File sourceDir = new File(sourcePath);
        File[] files = sourceDir.listFiles();

        if (files != null) {
            for (File file : files) {
                String newPath = targetPath + "/" + file.getName();
                File newFile = new File(newPath);

                if (file.isDirectory()) {
                    newFile.mkdir();
                    Directory newDirectory = Directory.builder()
                            .path(newPath)
                            .build();
                    directoryRepository.save(newDirectory);
                    copyDirectoryContents(file.getAbsolutePath(), newPath, user);
                } else {
                    try {
                        Files.copy(file.toPath(), newFile.toPath());
                        everyide.webide.fileSystem.domain.File dbFile = everyide.webide.fileSystem.domain.File.builder()
                                .path(newPath)
                                .content(Files.readString(file.toPath()))
                                .build();
                        fileRepository.save(dbFile);
                    } catch (IOException e) {
                        log.error("Failed to copy file: " + file.getAbsolutePath(), e);
                        throw new RuntimeException("Failed to copy file.");
                    }
                }
            }
        }
    }

}
