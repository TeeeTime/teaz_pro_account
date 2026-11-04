package pro.teaz.account.avatar;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Turns an upload into the JPEGs we serve: centre-cropped to a square, on
 * white where it was transparent. Nothing of the upload is kept but its
 * pixels, so metadata (EXIF, GPS) and anything hidden in the file are gone.
 */
final class AvatarImage {

	static final List<Integer> SIZES = List.of(64, 128, 256, 512);

	/** Checked before decoding: a small file can unpack to a huge bitmap, and the heap is small. */
	static final long MAX_PIXELS = 16_000_000;

	private static final float JPEG_QUALITY = 0.88f;

	private AvatarImage() {
	}

	/** One JPEG per entry of {@link #SIZES}. */
	static Map<Integer, byte[]> render(byte[] upload) {
		BufferedImage square = cropToSquare(decode(upload));
		Map<Integer, byte[]> rendered = new LinkedHashMap<>();
		for (int size : SIZES) rendered.put(size, encode(scale(square, size)));
		return rendered;
	}

	private static BufferedImage decode(byte[] upload) {
		if (!isAcceptedType(upload)) throw unsupported();
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(upload))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) throw unsupported();
			ImageReader reader = readers.next();
			try {
				reader.setInput(input, true, true);
				if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
					throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "This image has too many pixels.");
				}
				return reader.read(0);
			} finally {
				reader.dispose();
			}
		} catch (IOException | IllegalArgumentException | IndexOutOfBoundsException e) {
			throw unsupported();
		}
	}

	/** The real type from the first bytes, whatever the upload claims. */
	private static boolean isAcceptedType(byte[] bytes) {
		return startsWith(bytes, 0x89, 'P', 'N', 'G') || startsWith(bytes, 0xFF, 0xD8, 0xFF)
				|| startsWith(bytes, 'G', 'I', 'F', '8');
	}

	private static boolean startsWith(byte[] bytes, int... expected) {
		if (bytes.length < expected.length) return false;
		byte[] prefix = new byte[expected.length];
		for (int i = 0; i < expected.length; i++) prefix[i] = (byte) expected[i];
		return Arrays.equals(Arrays.copyOf(bytes, expected.length), prefix);
	}

	private static BufferedImage cropToSquare(BufferedImage image) {
		int side = Math.min(image.getWidth(), image.getHeight());
		int x = (image.getWidth() - side) / 2;
		int y = (image.getHeight() - side) / 2;
		BufferedImage square = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = square.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, side, side);
		graphics.drawImage(image, -x, -y, null);
		graphics.dispose();
		return square;
	}

	/** Halves step by step: one big bilinear step would alias badly. */
	private static BufferedImage scale(BufferedImage image, int size) {
		BufferedImage current = image;
		while (current.getWidth() != size) {
			int next = current.getWidth() > size ? Math.max(size, current.getWidth() / 2) : size;
			BufferedImage scaled = new BufferedImage(next, next, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = scaled.createGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.drawImage(current, 0, 0, next, next, null);
			graphics.dispose();
			current = scaled;
		}
		return current;
	}

	private static byte[] encode(BufferedImage image) {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(JPEG_QUALITY);
			writer.setOutput(output);
			writer.write(null, new IIOImage(image, null, null), param);
		} catch (IOException e) {
			throw new IllegalStateException("Could not encode an avatar", e);
		} finally {
			writer.dispose();
		}
		return bytes.toByteArray();
	}

	private static ResponseStatusException unsupported() {
		return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PNG, JPEG and GIF images.");
	}
}
