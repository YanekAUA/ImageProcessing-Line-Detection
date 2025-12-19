import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.plugin.filter.PlugInFilter;

public class Hough_Transform implements PlugInFilter {

    public int setup(String args, ImagePlus im) {
        return DOES_8G;
    }

    public void run(ImageProcessor imageSpace) {

        int height = imageSpace.getHeight();
        int width = imageSpace.getWidth();
        int h2 = height / 2;
        int w2 = width / 2;

        int rHeight = (int) Math.hypot(height, width);

        ImageProcessor paramSpace = new ByteProcessor(width, rHeight);

        double tMax = Math.PI;
        double dt = tMax / width;

        double rMax = Math.hypot(width, height);
        double dr = rMax / rHeight;
        double rMax2 = rMax / 2.0;

        int i, j;
        double r;
        int maxVote = 0;

        /* -------- Hough voting -------- */
        for (int col = -w2; col < w2; col++) {
            for (int row = -h2; row < h2; row++) {

                if (imageSpace.getPixel(col + w2, row + h2) > 120) {

                    for (double t = 0; t < tMax; t += dt) {

                        r = col * Math.cos(t) + row * Math.sin(t);

                        i = (int) (t / dt + 0.5);
                        j = (int) ((r + rMax2) / dr + 0.5);

                        int v = paramSpace.getPixel(i, j) + 1;
                        paramSpace.putPixel(i, j, v);

                        if (v > maxVote)
                            maxVote = v;
                    }
                }
            }
        }

        /* -------- Normalize to 0–255 -------- */
        double scale = 255.0 / maxVote;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < rHeight; y++) {
                int pixel = (int) (paramSpace.getPixel(x, y) * scale);
                paramSpace.putPixel(x, y, pixel);
            }
        }

        new ImagePlus("Hough Transform", paramSpace).show();
    }
}
