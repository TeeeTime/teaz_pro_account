package pro.teaz.account.avatar;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Pictures on disk: {@code <dir>/<user id>/<size>.jpg}. The file is the whole
 * record, so this app needs no database. Keyed by UUID only, which can't
 * contain path separators.
 */
@Component
public class AvatarStorage {

	private final Path dir;

	public AvatarStorage(@Value("${account.storage-dir}") Path dir) {
		this.dir = dir;
	}

	/** Each file is replaced atomically, so a reader always gets a complete image. */
	public void save(UUID user, Map<Integer, byte[]> sizes) {
		try {
			Path folder = Files.createDirectories(dir.resolve(user.toString()));
			for (Map.Entry<Integer, byte[]> size : sizes.entrySet()) {
				Path temp = Files.createTempFile(folder, "upload", ".tmp");
				Files.write(temp, size.getValue());
				Files.move(temp, file(user, size.getKey()), StandardCopyOption.ATOMIC_MOVE,
						StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Optional<Path> find(UUID user, int size) {
		return Optional.of(file(user, size)).filter(Files::isRegularFile);
	}

	public void delete(UUID user) {
		Path folder = dir.resolve(user.toString());
		if (!Files.isDirectory(folder)) return;
		try (Stream<Path> files = Files.list(folder)) {
			for (Path file : files.toList()) Files.deleteIfExists(file);
			Files.deleteIfExists(folder);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Path file(UUID user, int size) {
		return dir.resolve(user.toString()).resolve(size + ".jpg");
	}
}
