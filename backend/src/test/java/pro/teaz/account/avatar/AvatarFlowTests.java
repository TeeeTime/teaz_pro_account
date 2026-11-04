package pro.teaz.account.avatar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class AvatarFlowTests {

	@TempDir
	static Path storage;

	@DynamicPropertySource
	static void storage(DynamicPropertyRegistry registry) {
		registry.add("account.storage-dir", storage::toString);
	}

	@Autowired
	MockMvc mvc;

	private final UUID user = UUID.randomUUID();

	private RequestPostProcessor as(UUID id) {
		return jwt().jwt(token -> token.subject(id.toString()));
	}

	private ResultActions upload(byte[] bytes, RequestPostProcessor auth) throws Exception {
		return mvc.perform(multipart(HttpMethod.PUT, "/api/me/avatar")
				.file(new MockMultipartFile("file", "picture.png", "image/png", bytes)).with(auth));
	}

	private static BufferedImage image(int width, int height, Color color) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setBackground(color);
		graphics.clearRect(0, 0, width, height);
		graphics.dispose();
		return image;
	}

	private static byte[] png(BufferedImage image) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(image, "png", bytes);
		return bytes.toByteArray();
	}

	private BufferedImage served(UUID id, int size) throws Exception {
		byte[] body = mvc.perform(get("/api/avatars/{id}?size={size}", id, size))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "image/jpeg"))
				.andReturn().getResponse().getContentAsByteArray();
		return ImageIO.read(new ByteArrayInputStream(body));
	}

	@Test
	void servesEverySizeAsPublicJpeg() throws Exception {
		upload(png(image(900, 600, Color.RED)), as(user)).andExpect(status().isNoContent());

		for (int size : AvatarImage.SIZES) {
			BufferedImage image = served(user, size);
			assertThat(image.getWidth()).isEqualTo(size);
			assertThat(image.getHeight()).isEqualTo(size);
		}
		assertThat(served(user, 100).getWidth()).isEqualTo(128);
		assertThat(served(user, 9000).getWidth()).isEqualTo(512);
	}

	@Test
	void cachesBrieflyAndAnswersConditionalRequests() throws Exception {
		upload(png(image(64, 64, Color.BLUE)), as(user)).andExpect(status().isNoContent());

		String etag = mvc.perform(get("/api/avatars/{id}", user))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "max-age=300, public"))
				.andExpect(header().string("Cross-Origin-Resource-Policy", "cross-origin"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andReturn().getResponse().getHeader("ETag");

		mvc.perform(get("/api/avatars/{id}", user).header("If-None-Match", etag))
				.andExpect(status().isNotModified());
	}

	@Test
	void missingPictureIs404ForAMinute() throws Exception {
		mvc.perform(get("/api/avatars/{id}", user))
				.andExpect(status().isNotFound())
				.andExpect(header().string("Cache-Control", "max-age=60, public"));
	}

	@Test
	void transparencyBecomesWhite() throws Exception {
		upload(png(image(64, 64, new Color(0, 0, 0, 0))), as(user)).andExpect(status().isNoContent());

		Color pixel = new Color(served(user, 64).getRGB(30, 30));
		assertThat(pixel.getRed()).isGreaterThan(250);
	}

	@Test
	void metadataIsNotKept() throws Exception {
		byte[] picture = jpegWithComment("GPS-SECRET");
		assertThat(new String(picture, StandardCharsets.ISO_8859_1)).contains("GPS-SECRET");

		upload(picture, as(user)).andExpect(status().isNoContent());

		byte[] body = mvc.perform(get("/api/avatars/{id}", user)).andReturn().getResponse().getContentAsByteArray();
		assertThat(body).isNotEmpty();
		assertThat(new String(body, StandardCharsets.ISO_8859_1)).doesNotContain("GPS-SECRET");
	}

	private static byte[] jpegWithComment(String comment) throws IOException {
		BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), null);
		IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
		IIOMetadataNode node = new IIOMetadataNode("com");
		node.setAttribute("comment", comment);
		root.getElementsByTagName("markerSequence").item(0).appendChild(node);
		metadata.setFromTree("javax_imageio_jpeg_image_1.0", root);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
			writer.setOutput(output);
			writer.write(new IIOImage(image, null, metadata));
		}
		writer.dispose();
		return bytes.toByteArray();
	}

	@Test
	void rejectsWhatIsNoImage() throws Exception {
		upload("<svg onload=alert(1)/>".getBytes(), as(user)).andExpect(status().isUnsupportedMediaType());
		// The right magic bytes on something that isn't an image.
		upload(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0}, as(user)).andExpect(status().isUnsupportedMediaType());
		mvc.perform(get("/api/avatars/{id}", user)).andExpect(status().isNotFound());
	}

	@Test
	void rejectsTooManyPixels() throws Exception {
		upload(png(image(5000, 4000, Color.WHITE)), as(user)).andExpect(status().isUnprocessableContent());
	}

	@Test
	void rejectsInvalidUserId() throws Exception {
		mvc.perform(get("/api/avatars/mock-jordan")).andExpect(status().isBadRequest());
	}

	@Test
	void changingAPictureNeedsAToken() throws Exception {
		mvc.perform(multipart(HttpMethod.PUT, "/api/me/avatar")
				.file(new MockMultipartFile("file", "a.png", "image/png", png(image(64, 64, Color.RED)))))
				.andExpect(status().isUnauthorized());
		mvc.perform(delete("/api/me/avatar")).andExpect(status().isUnauthorized());
	}

	@Test
	void usersOnlyChangeTheirOwnPicture() throws Exception {
		UUID other = UUID.randomUUID();
		upload(png(image(64, 64, Color.RED)), as(user)).andExpect(status().isNoContent());
		upload(png(image(64, 64, Color.BLUE)), as(other)).andExpect(status().isNoContent());

		mvc.perform(delete("/api/me/avatar").with(as(other))).andExpect(status().isNoContent());

		mvc.perform(get("/api/avatars/{id}", other)).andExpect(status().isNotFound());
		mvc.perform(get("/api/avatars/{id}", user)).andExpect(status().isOk());
	}

	@Test
	void replacingAndDeletingWork() throws Exception {
		upload(png(image(64, 64, Color.RED)), as(user)).andExpect(status().isNoContent());
		upload(png(image(64, 64, Color.BLUE)), as(user)).andExpect(status().isNoContent());

		Color pixel = new Color(served(user, 64).getRGB(30, 30));
		assertThat(pixel.getBlue()).isGreaterThan(pixel.getRed());

		mvc.perform(delete("/api/me/avatar").with(as(user))).andExpect(status().isNoContent());
		mvc.perform(get("/api/avatars/{id}", user)).andExpect(status().isNotFound());
	}
}
