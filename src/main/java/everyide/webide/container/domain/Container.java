package everyide.webide.container.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import everyide.webide.BaseEntity;
import everyide.webide.fileSystem.domain.Directory;
import everyide.webide.fileSystem.domain.File;
import everyide.webide.room.domain.Room;
import everyide.webide.user.domain.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "containers")
public class Container extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String path;
    private String description;
    private String language;
    private boolean active;
    private int shared;
    private Long sourceContainer;

    @OneToMany(mappedBy = "container")
    private List<Directory> directories = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Room room;

    @Builder
    public Container(String name, String path, String description, String language) {
        this.name = name;
        this.path = path;
        this.description = description;
        this.language = language;
        this.active = true;
        this.shared = 0;
    }


    //== 연관관계 메서드 ==//
    public void setUser(User user) {
        this.user = user;
        user.addContainer(this);
    }

    public void setRoom(Room room) {
        this.room = room;
        room.addContainer(this);
    }

    public void setSourceContainer(Long sourceContainerId) {
        this.sourceContainer = sourceContainerId;
    }

    public void addDirectories(Directory directory) {
        this.directories.add(directory);
    }

    public void onContainer() {
        this.active = true;
    }

    public void offContainer() {
        this.active = false;
    }

    public Container share() {
        this.shared += 1;
        return this;
    }

    public Container unshare() {
        this.shared -= 1;
        return this;
    }

    public Container updateContainer(String newName, String newPath, String newDescription, boolean active) {
        this.name = newName;
        this.path = newPath;
        this.description = newDescription;
        if (active) {
            onContainer();
        } else {
            offContainer();
        }

        return this;
    }

    public Container updateContainer(String newDescription, boolean active) {
        this.description = newDescription;
        if (active) {
            onContainer();
        } else {
            offContainer();
        }
        return this;
    }
}
