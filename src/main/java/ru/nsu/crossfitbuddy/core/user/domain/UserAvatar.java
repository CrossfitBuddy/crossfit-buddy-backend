package ru.nsu.crossfitbuddy.core.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "user_avatar")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAvatar {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] content;

    @Version
    @Column(nullable = false)
    private Long version;

    public static UserAvatar create(UUID userId, String contentType, byte[] content) {
        var avatar = new UserAvatar();
        avatar.id = userId;
        avatar.replace(contentType, content);
        return avatar;
    }

    public void replace(String contentType, byte[] content) {
        this.contentType = contentType;
        this.content = content.clone();
    }

    public byte[] getContent() {
        return content.clone();
    }
}
