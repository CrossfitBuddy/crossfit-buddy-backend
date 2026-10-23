package ru.nsu.crossfitbuddy.core.user.service;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

class AvatarValidatorTest {
    private final AvatarValidator validator = new AvatarValidator();

    @ParameterizedTest
    @ValueSource(strings = {"png", "jpeg"})
    @DisplayName("Проверка формата аватара")
    void acceptsSupportedImages(String format) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, bytes);
        assertThat(validator.validate(bytes.toByteArray())).isEqualTo("image/" + format);
    }

    @Test
    @DisplayName("Загрузка WEBP")
    void acceptsWebp() {
        byte[] bytes = Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
        assertThat(validator.validate(bytes)).isEqualTo("image/webp");
    }

    @Test
    @DisplayName("Некорректные файлы отклоняются")
    void rejectsInvalidFiles() throws Exception {
        for (byte[] content : new byte[][] {new byte[0], "not an image".getBytes(),
            new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 13, 10, 26, 10}}) {
            assertThatThrownBy(() -> validator.validate(content)).isInstanceOf(ApiException.class);
        }
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "gif", bytes);
        assertThatThrownBy(() -> validator.validate(bytes.toByteArray())).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("Файл больше 5 МБ отклоняется")
    void rejectsOversizedFiles() {
        assertThatThrownBy(() -> validator.validate(new byte[AvatarValidator.MAX_BYTES + 1]))
            .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.status().value()).isEqualTo(413));
    }
}
