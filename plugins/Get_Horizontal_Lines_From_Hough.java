import ij.*;
import ij.process.*;
import ij.plugin.filter.*;

public class Get_Horizontal_Lines_From_Hough implements PlugInFilter {

    ImagePlus imp;

    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        IJ.log("Plugin setup");
        return DOES_8G;
    }

    @Override
    public void run(ImageProcessor ip) {

        int width  = ip.getWidth();
        int height = ip.getHeight();

        int angleCenter = width / 2;
        int angleRange  = width / 20;

        IJ.log("Hough image size: " + width + " x " + height);
        IJ.log("pi/2 at x=" + angleCenter);

        // --- Compute response per rho ---
        int[] response = new int[height];
        int maxVal = 0;

        for (int rho = 0; rho < height; rho++) {
            int sum = 0;
            for (int x = angleCenter - angleRange; x <= angleCenter + angleRange; x++) {
                sum += ip.getPixel(x, rho);
            }
            response[rho] = sum;
            if (sum > maxVal) maxVal = sum;
        }

        IJ.log("Max rho response = " + maxVal);

        int threshold = (int)(0.98 * maxVal); // stricter
        int minRhoDistance = 15;              // suppress nearby peaks

        IJ.log("Threshold = " + threshold);
        IJ.log("Min rho distance = " + minRhoDistance);

        ByteProcessor out = new ByteProcessor(width, height);
        out.setValue(0);
        out.fill();
        out.setValue(255);

        int detected = 0;
        int lastAcceptedRho = -minRhoDistance;

        // --- Peak detection with suppression ---
        for (int rho = 1; rho < height - 1; rho++) {

            boolean isLocalMax =
                response[rho] > response[rho - 1] &&
                response[rho] > response[rho + 1];

            boolean strongEnough = response[rho] >= threshold;
            boolean farEnough = (rho - lastAcceptedRho) >= minRhoDistance;

            if (isLocalMax && strongEnough && farEnough) {

                lastAcceptedRho = rho;
                detected++;

                IJ.log(">> Accepted line at rho=" + rho +
                       " value=" + response[rho]);

                // Draw horizontal line
                for (int x = 0; x < width; x++) {
                    out.putPixel(x, rho, 255);
                }
            }
        }

        IJ.log("Total detected horizontal lines: " + detected);

        new ImagePlus("Detected Horizontal Lines (Hough Space)", out).show();
    }
}
