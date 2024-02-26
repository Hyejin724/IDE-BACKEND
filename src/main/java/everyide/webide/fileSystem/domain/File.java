package everyide.webide.fileSystem.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import everyide.webide.BaseEntity;
import everyide.webide.container.domain.Container;
import everyide.webide.user.domain.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "files")
public class File extends BaseEntity {
    @Id
    private String id;

    private String path;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Container container;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Directory directory;

    @Builder
    public File(String path, String content) {
        this.id = UUID.randomUUID().toString();
        this.path = path;
        this.content = content;
    }

    public void setDirectory(Directory directory) {
        this.directory = directory;
        directory.addFiles(this);
    }

    public File updateFile(String path) {
        this.path = path;

        return this;
    }

    public File updateContent(String newContent) {
        this.content = newContent;
        return this;
    }

    @PrePersist
    private void prePersist() {
        if (this.id == null) { // ID가 설정되지 않은 경우에만 새 UUID 생성
            this.id = UUID.randomUUID().toString();
        }
    }
}
