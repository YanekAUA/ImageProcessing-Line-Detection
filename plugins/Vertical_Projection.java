import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Vertical_Projection implements PlugInFilter {
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL;
    }

    public void run(ImageProcessor ip) {
        // 1) Make sure the image is binary
        // 2) For each row count the number of 1's
        // 3) Make the pixels from `width - count` to `width` 1, the rest 0
        // 4) Display the resulting image

        int width = ip.getWidth();
        int height = ip.getHeight();
        ImageProcessor resultIp = ip.duplicate();
        resultIp.setValue(0);
        resultIp.fill();
        resultIp.setValue(255);
        for (int x = 0; x < width; x++) {
            int count = 0;
            for (int y = 0; y < height; y++) {
                if (ip.getPixel(x, y) == 255) {
                    count++;
                }
            }
            for (int y = height - count; y < height; y++) {
                resultIp.putPixel(x, y, 255);
            }
        }
        new ImagePlus("Vertical Projection", resultIp).show();
    }

}
