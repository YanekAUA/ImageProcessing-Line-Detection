import ij.*;
import ij.process.*;
import ij.plugin.filter.PlugInFilter;

public class Hough_Horizontal_Lines implements PlugInFilter {

    @Override
    public int setup(String arg, ImagePlus imp) {
        return DOES_8G;
    }

    @Override
    public void run(ImageProcessor ip) {

        int width  = ip.getWidth();
        int height = ip.getHeight();

        IJ.log("Input image size: " + width + " x " + height);

        // Accumulator: one value per image row
        FloatProcessor hough = new FloatProcessor(width, height);
        hough.setValue(0);
        hough.fill();

        // --- Voting ---
        // Horizontal line: y = constant → r = y
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (ip.getPixel(x, y) > 150) {
                    hough.putPixelValue(x, y,
                        hough.getPixelValue(x, y) + 1.0f);
                }
            }
        }

        // --- Collapse x dimension (sum columns) ---
        float[] response = new float[height];

        for (int y = 0; y < height; y++) {
            float sum = 0;
            for (int x = 0; x < width; x++) {
                sum += hough.getPixelValue(x, y);
            }
            response[y] = sum;
        }

        // --- Peak detection ---
        float maxVal = 0;
        for (float v : response)
            if (v > maxVal) maxVal = v;

        float threshold = 0.5f * maxVal;
        int minDist = 10;

        IJ.log("Max response = " + maxVal);
        IJ.log("Threshold = " + threshold);

        ByteProcessor out = new ByteProcessor(width, height);
        out.setValue(0);
        out.fill();
        out.setValue(255);

        int lastY = -minDist;
        int detected = 0;

        for (int y = 1; y < height - 1; y++) {
            if (response[y] > threshold &&
                response[y] > response[y - 1] &&
                response[y] > response[y + 1] &&
                (y - lastY) >= minDist) {

                lastY = y;
                detected++;

                IJ.log(">> Horizontal line at y=" + y +
                       " value=" + response[y]);

                for (int x = 0; x < width; x++) {
                    out.putPixel(x, y, 255);
                }
            }
        }

        IJ.log("Total detected horizontal lines: " + detected);

        new ImagePlus("Hough (Horizontal Lines)", hough).show();
        new ImagePlus("Detected Horizontal Lines", out).show();
    }
}
