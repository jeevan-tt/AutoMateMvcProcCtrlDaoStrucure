package util;

import java.util.Map;
import java.util.Set;

public final class FileValidationUtil {

    private FileValidationUtil() {
        // Utility class
    }

    /*
     * Magic numbers / file signatures.
     *
     * Key   -> Extension
     * Value -> File signature
     */
    private static final Map<String, byte[]> MAGIC_NUMBERS = Map.of(
            "pdf", new byte[] {
                    0x25, 0x50, 0x44, 0x46
            },

            "png", new byte[] {
                    (byte) 0x89, 0x50, 0x4E, 0x47,
                    0x0D, 0x0A, 0x1A, 0x0A
            },

            "jpg", new byte[] {
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
            },

            "jpeg", new byte[] {
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
            },

            "xls", new byte[] {
                    (byte) 0xD0, (byte) 0xCF,
                    0x11, (byte) 0xE0,
                    (byte) 0xA1, (byte) 0xB1,
                    0x1A, (byte) 0xE1
            },

            "doc", new byte[] {
                    (byte) 0xD0, (byte) 0xCF,
                    0x11, (byte) 0xE0,
                    (byte) 0xA1, (byte) 0xB1,
                    0x1A, (byte) 0xE1
            },

            "xlsx", new byte[] {
                    0x50, 0x4B, 0x03, 0x04
            },

            "docx", new byte[] {
                    0x50, 0x4B, 0x03, 0x04
            }
    );


    /**
     * Validates file size, extension and magic number.
     *
     * @param fileBytes          decoded file bytes
     * @param fileName           original file name
     * @param maxSizeBytes       maximum allowed size in bytes
     * @param allowedExtensions extensions allowed for this API
     *
     * @return true if file is valid
     *
     * @throws FileValidationException if validation fails
     */
    public static boolean validateFile(
            byte[] fileBytes,
            String fileName,
            long maxSizeBytes,
            Set<String> allowedExtensions) {

        // 1. Null / empty check
        if (fileBytes == null || fileBytes.length == 0) {
            throw new FileValidationException(
                    "File is empty or not provided");
        }

        // 2. File name validation
        if (fileName == null || fileName.isBlank()) {
            throw new FileValidationException(
                    "File name is required");
        }

        // 3. Maximum size validation
        if (fileBytes.length > maxSizeBytes) {
            throw new FileValidationException(
                    "File size exceeds the maximum allowed size");
        }

        // 4. Extension extraction
        String extension = getExtension(fileName);

        if (extension == null) {
            throw new FileValidationException(
                    "File extension is missing");
        }

        extension = extension.toLowerCase();

        // 5. Allowed extension validation
        if (allowedExtensions == null
                || allowedExtensions.isEmpty()
                || !allowedExtensions.stream()
                        .map(String::toLowerCase)
                        .anyMatch(extension::equals)) {

            throw new FileValidationException(
                    "File extension is not allowed: "
                            + extension);
        }

        // 6. Magic number validation
        byte[] expectedMagic =
                MAGIC_NUMBERS.get(extension);

        if (expectedMagic == null) {
            throw new FileValidationException(
                    "File type is not supported for "
                            + "magic number validation: "
                            + extension);
        }

        if (!matchesMagicNumber(
                fileBytes,
                expectedMagic)) {

            throw new FileValidationException(
                    "File content does not match "
                            + "the file extension: "
                            + extension);
        }

        return true;
    }


    /**
     * Extracts file extension.
     */
    private static String getExtension(String fileName) {

        int lastDot = fileName.lastIndexOf('.');

        if (lastDot <= 0
                || lastDot == fileName.length() - 1) {
            return null;
        }

        return fileName
                .substring(lastDot + 1)
                .toLowerCase();
    }


    /**
     * Checks whether file starts with expected magic number.
     */
    private static boolean matchesMagicNumber(
            byte[] fileBytes,
            byte[] expectedMagic) {

        if (fileBytes.length < expectedMagic.length) {
            return false;
        }

        for (int i = 0; i < expectedMagic.length; i++) {

            if (fileBytes[i] != expectedMagic[i]) {
                return false;
            }
        }

        return true;
    }
}