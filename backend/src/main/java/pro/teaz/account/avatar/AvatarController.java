package pro.teaz.account.avatar;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pro.teaz.account.security.CurrentUser;

/**
 * Profile pictures, linked to Keycloak accounts by user id (sub). Anyone can
 * read one; a user can only change their own, as the id comes from the token.
 */
@RestController
@RequestMapping("/api")
public class AvatarController {

	/** The URL stays the same when a picture changes, so caches only hold it briefly. */
	private static final CacheControl CACHE_PICTURE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();
	private static final CacheControl CACHE_MISSING = CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic();

	private final CurrentUser user;
	private final AvatarStorage storage;

	public AvatarController(CurrentUser user, AvatarStorage storage) {
		this.user = user;
		this.storage = storage;
	}

	/** JPEG at the smallest available size of at least {@code size} px. 404 without a picture. */
	@GetMapping("/avatars/{userId}")
	public ResponseEntity<Resource> picture(@PathVariable UUID userId, @RequestParam(defaultValue = "128") int size) {
		return storage.find(userId, closestSize(size))
				.map(AvatarController::serve)
				.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(CACHE_MISSING).build());
	}

	/** Multipart field "file": PNG, JPEG or GIF, up to 5 MB. Replaces the caller's picture. */
	@PutMapping("/me/avatar")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void upload(@RequestParam("file") MultipartFile file) throws IOException {
		storage.save(user.id(), AvatarImage.render(file.getBytes()));
	}

	@DeleteMapping("/me/avatar")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete() {
		storage.delete(user.id());
	}

	private static int closestSize(int requested) {
		return AvatarImage.SIZES.stream().filter(size -> size >= requested).findFirst().orElse(AvatarImage.SIZES.getLast());
	}

	private static ResponseEntity<Resource> serve(Path file) {
		try {
			return ResponseEntity.ok()
					.contentType(MediaType.IMAGE_JPEG)
					.cacheControl(CACHE_PICTURE)
					.eTag(Files.getLastModifiedTime(file).toMillis() + "-" + Files.size(file))
					.header("X-Content-Type-Options", "nosniff")
					// Pictures are embedded by every other Teaz app.
					.header("Cross-Origin-Resource-Policy", "cross-origin")
					.body(new FileSystemResource(file));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
