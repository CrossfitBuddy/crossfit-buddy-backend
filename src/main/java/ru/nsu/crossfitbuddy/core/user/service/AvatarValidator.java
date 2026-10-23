package ru.nsu.crossfitbuddy.core.user.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

@Component
public class AvatarValidator {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000;

        public String validate(byte[] content) {
        if (content.length > MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "AVATAR_TOO_LARGE",
                "Avatar must not exceed 5 MB.");
        }
        if (content.length == 0) {
            throw invalid();
        }
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalid();
            }
            var reader = readers.next();
            try {
                String type = switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpeg", "jpg" -> "image/jpeg";
                    case "png" -> "image/png";
                    case "webp" -> "image/webp";
                    default -> throw invalid();
                };
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "AVATAR_DIMENSIONS",
                        "Avatar must contain at most 16 million pixels.");
                }
                reader.read(0);
                return type;
            } finally {
                reader.dispose();
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw invalid();
        }
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AVATAR",
            "Upload a valid JPEG, PNG or WEBP image.");
    }
}
