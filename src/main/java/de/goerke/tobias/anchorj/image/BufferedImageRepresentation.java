package de.goerke.tobias.anchorj.image;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;

/**
 * Implements the {@link ImageRepresentation} for using {@link BufferedImage}s.
 */
public class BufferedImageRepresentation extends BufferedImage implements ImageRepresentation {

    /**
     * Creates the instance
     *
     * @param source the image to copy from
     */
    public BufferedImageRepresentation(BufferedImage source) {
        super(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = this.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
    }

    /**
     * Creates an empty image.
     *
     * @param width     the image's width
     * @param height    the image's height
     * @param imageType bufferedimage color type
     */
    private BufferedImageRepresentation(int width, int height, int imageType) {
        super(width, height, imageType);
    }

    private static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }


    @Override
    public ImageRepresentation createBlankCanvas() {
        return new BufferedImageRepresentation(getWidth(), getHeight(), getType());
    }

    @Override
    public BufferedImage asBufferedImage() {
        return this;
    }

    @Override
    public byte[] asByteArray() {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        ImageIO.write(image, "jpg", baos);
//        return baos.toByteArray();
        // Following method does not use I/O
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageOutputStream stream = new MemoryCacheImageOutputStream(baos);
            ImageIO.write(this, "jpg", stream);
            stream.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    @Override
//    public int[] getPixels() {
//        return this.getRGB(0, 0, getHeight(), getHeight(), null, 0, getWidth());
//    }

    @Override
    public int getPixel(int x, int y) {
        return getRGB(x, y);
    }

    @Override
    public void setPixel(int x, int y, int rgb) {
        setRGB(x, y, rgb);
    }
}
