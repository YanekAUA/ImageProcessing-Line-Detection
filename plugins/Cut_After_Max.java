import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Cut_After_Max implements PlugInFilter {
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_8G;
    }

    public void run(ImageProcessor ip) {
        // Find the max occuring pixel for 8 bit image: max(histogram)
        // Set the I(x,y) to max(histogram) 
        int[] histogram = ip.getHistogram();
        int maxCount = 0;
        int maxPixelValue = 0;
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > maxCount) {
                maxCount = histogram[i];
                maxPixelValue = i;
            }
        }
        
        for (int i = 0; i < ip.getHeight(); i++) {
            for (int j = 0; j < ip.getWidth(); j++) {
                if (ip.getPixel(j, i) > maxPixelValue) {
                    ip.putPixel(j, i, maxPixelValue);
                }
            }
        }
    }

}
