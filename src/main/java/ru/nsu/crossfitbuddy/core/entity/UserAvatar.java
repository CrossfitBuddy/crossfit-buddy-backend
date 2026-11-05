package ru.nsu.crossfitbuddy.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "user_avatar")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAvatar {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "object_key", nullable = false, length = 255)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Version
    @Column(nullable = false)
    private Long version;

    public static UserAvatar create(UUID userId, String objectKey, String contentType, Long fileSize) {
        var avatar = new UserAvatar();
        avatar.id = userId;
        avatar.objectKey = objectKey;
        avatar.contentType = contentType;
        avatar.fileSize = fileSize;
        return avatar;
    }

    public void replace(String objectKey, String contentType, Long fileSize) {
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }
}
