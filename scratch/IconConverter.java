package scratch;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IconConverter {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java IconConverter.java <input-png-path> <output-ico-path>");
            System.exit(1);
        }

        String inputPng = args[0];
        String outputIco = args[1];

        try {
            // Load source image
            BufferedImage source = ImageIO.read(new File(inputPng));
            if (source == null) {
                System.err.println("Error: Could not read source image " + inputPng);
                System.exit(1);
            }

            // Resize to 256x256 (standard high-res ICO size)
            BufferedImage resized = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(source, 0, 0, 256, 256, null);
            g2d.dispose();

            // Write resized image to PNG bytes
            ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
            ImageIO.write(resized, "PNG", pngOut);
            byte[] pngBytes = pngOut.toByteArray();

            // Prepare ICO file
            try (FileOutputStream fos = new FileOutputStream(outputIco)) {
                // Header (6 bytes)
                ByteBuffer header = ByteBuffer.allocate(6);
                header.order(ByteOrder.LITTLE_ENDIAN);
                header.putShort((short) 0); // Reserved
                header.putShort((short) 1); // Type (1 = ICO)
                header.putShort((short) 1); // Count (1 image)
                fos.write(header.array());

                // Directory Entry (16 bytes)
                ByteBuffer entry = ByteBuffer.allocate(16);
                entry.order(ByteOrder.LITTLE_ENDIAN);
                entry.put((byte) 0); // Width (0 means 256)
                entry.put((byte) 0); // Height (0 means 256)
                entry.put((byte) 0); // Color count (0 = no palette)
                entry.put((byte) 0); // Reserved
                entry.putShort((short) 1); // Color planes (1)
                entry.putShort((short) 32); // Bits per pixel (32)
                entry.putInt(pngBytes.length); // Size of PNG data in bytes
                entry.putInt(22); // Offset to PNG data (6 bytes header + 16 bytes entry = 22)
                fos.write(entry.array());

                // Write PNG data
                fos.write(pngBytes);
            }

            System.out.println("Successfully converted " + inputPng + " to " + outputIco);
        } catch (IOException e) {
            System.err.println("Error during icon conversion: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
