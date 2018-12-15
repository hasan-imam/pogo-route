package pogo.assistance.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class FileIOUtils {

    /*
     * Initialized once. Used to resolve paths to resource files in side jar. Never closed.
     */
    private static FileSystem FILE_SYSTEM_FOR_JAR = null;

    public static synchronized Path resolvePackageLocalFilePath(final String fileName, final Class<?> aClass) {
        try {
            final URI uri = aClass.getResource(fileName).toURI();
            if ("jar".equals(uri.getScheme())) {
                final String[] array = uri.toString().split("!");
                if (FILE_SYSTEM_FOR_JAR == null) {
                    final URI partialUri = URI.create(array[0]);
                    try {
                        FILE_SYSTEM_FOR_JAR = Optional.ofNullable(FileSystems.getFileSystem(partialUri))
                                .filter(FileSystem::isOpen)
                                .orElseThrow(FileSystemNotFoundException::new);
                    } catch (final FileSystemNotFoundException e) {
                        FILE_SYSTEM_FOR_JAR = FileSystems.newFileSystem(partialUri, Collections.emptyMap());
                    }
                }
                return FILE_SYSTEM_FOR_JAR.getPath(array[1]);
            }
            return Paths.get(uri);
        } catch (final URISyntaxException | IOException e) {
            throw new RuntimeException("Failed to resolve file: " + fileName, e);
        }
    }

}
