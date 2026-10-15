package com.jstudy.inout.common.util;

import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class FileStorageUtil {

    public static final Set<String> DEFAULT_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    public static final Set<String> DEFAULT_ATTACHMENT_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "pdf", "txt");

    private FileStorageUtil() {
    }


    public static String extractAllowedExtension(String originalFilename, Set<String> allowedExtensions) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InoutException(ErrorCode.INVALID_FILE_NAME);
        }

        
        if (originalFilename.contains("/") || originalFilename.contains("\\")
                || originalFilename.contains("..") || originalFilename.contains("\u0000")) {
            throw new InoutException(ErrorCode.INVALID_FILE_NAME, "허용되지 않는 파일 이름입니다.");
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            throw new InoutException(ErrorCode.INVALID_FILE_EXTENSION, "확장자가 없는 파일은 업로드할 수 없습니다.");
        }

        String extension = originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!allowedExtensions.contains(extension)) {
            throw new InoutException(ErrorCode.INVALID_FILE_EXTENSION, "허용되지 않는 파일 형식입니다. 허용된 확장자: " + allowedExtensions);
        }
        return extension;
    }

    public static Path resolveSafeTargetPath(Path uploadRoot, String safeFileName) {
        Path normalizedRoot = uploadRoot.toAbsolutePath().normalize();
        Path target = normalizedRoot.resolve(safeFileName).normalize();

        if (!target.startsWith(normalizedRoot)) {
            throw new InoutException(ErrorCode.INVALID_FILE_PATH, "잘못된 업로드 경로입니다.");
        }
        return target;
    }
}
