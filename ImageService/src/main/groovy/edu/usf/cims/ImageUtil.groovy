package edu.usf.cims

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class ImageUtil
{
    public static getPercentDifference(File file1, File file2)
    {
        def img1 = ImageIO.read(file1)
        def img2 = ImageIO.read(file2)

        def width1 = img1.getWidth()
        def width2 = img2.getWidth()
        def height1 = img1.getHeight()
        def height2 = img2.getHeight()
        if ((width1 != width2) || (height1 != height2)) {
            System.err.println("Error: Images dimensions mismatch");
            return null
        }
        long diff = 0
        for (int y = 0; y < height1; y++) {
            for (int x = 0; x < width1; x++) {
                int rgb1 = img1.getRGB(x, y)
                int rgb2 = img2.getRGB(x, y)
                int r1 = (rgb1 >> 16) & 0xff
                int g1 = (rgb1 >>  8) & 0xff
                int b1 = (rgb1      ) & 0xff
                int r2 = (rgb2 >> 16) & 0xff
                int g2 = (rgb2 >>  8) & 0xff
                int b2 = (rgb2      ) & 0xff
                diff += Math.abs(r1 - r2)
                diff += Math.abs(g1 - g2)
                diff += Math.abs(b1 - b2)
            }
        }
        def n = width1 * height1 * 3
        return (diff / n / 255.0) * 100.0
    }
}
