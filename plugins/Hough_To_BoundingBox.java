import ij.*;
import ij.process.*;
import ij.plugin.filter.PlugInFilter;
import ij.measure.ResultsTable;
import ij.gui.Roi;
import ij.gui.Overlay;
import ij.gui.GenericDialog;
import ij.plugin.frame.RoiManager;
import java.awt.*;
import java.util.*;

public class Hough_To_BoundingBox implements PlugInFilter {
    ImagePlus houghImp;
    ImagePlus origImp;
    ImagePlus drawImp;

    public int setup(String arg, ImagePlus imp) {
        return DOES_ALL + NO_CHANGES; 
    }

    public void run(ImageProcessor ip) {
        String[] imageList = WindowManager.getImageTitles();
        if (imageList.length < 2) {
            IJ.error("Need at least two images open.");
            return;
        }
        
        GenericDialog gd = new GenericDialog("Hough Mapping Parameters");
        gd.addChoice("Hough Image (Reference):", imageList, imageList[0]);
        gd.addChoice("Preprocessed Image (For Analysis):", imageList, imageList[imageList.length - 1]);
        gd.addChoice("Target Image (To Burn Boxes On):", imageList, imageList[0]);
        gd.showDialog();
        
        if (gd.wasCanceled()) return;

        houghImp = WindowManager.getImage(gd.getNextChoice());
        origImp = WindowManager.getImage(gd.getNextChoice());
        drawImp = WindowManager.getImage(gd.getNextChoice());

        ResultsTable rt = ResultsTable.getResultsTable();
        int n = rt.getCounter();
        if (n == 0) return;

        int houghH = houghImp.getHeight();
        int origH = origImp.getHeight();
        int origW = origImp.getWidth();
        double houghCenterY = houghH / 2.0;
        double origCenterY = origH / 2.0;

        ArrayList<Double> mappedYs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double yHough = rt.getValue("Y", i);
            double diff = yHough - houghCenterY;
            mappedYs.add(origCenterY + diff);
        }
        Collections.sort(mappedYs);

        float[] projection = new float[origH];
        ImageProcessor origIp = origImp.getProcessor();
        for (int y = 0; y < origH; y++) {
            float rowSum = 0;
            for (int x = 0; x < origW; x++) {
                if (origIp.getPixel(x, y) > 0) rowSum++;
            }
            projection[y] = rowSum;
        }

        // Initialize ROI Manager and Overlay
        RoiManager rm = RoiManager.getInstance();
        if (rm == null) rm = new RoiManager();
        rm.reset();

        Overlay overlay = drawImp.getOverlay();
        if (overlay == null) {
            overlay = new Overlay();
        } else {
            overlay.clear();
        }

        if (drawImp.getType() != ImagePlus.COLOR_RGB) {
            IJ.run(drawImp, "RGB Color", "");
        }

        for (int i = 0; i < mappedYs.size(); i++) {
            int targetY = mappedYs.get(i).intValue();
            int topSearchLimit = (i == 0) ? 0 : mappedYs.get(i-1).intValue();
            int bottomSearchLimit = (i == mappedYs.size() - 1) ? origH - 1 : mappedYs.get(i+1).intValue();

            int top = findValley(projection, targetY, topSearchLimit, -1);
            int bottom = findValley(projection, targetY, bottomSearchLimit, 1);

            if (bottom > top) {
                Roi r = new Roi(0, top, drawImp.getWidth(), bottom - top);
                r.setStrokeColor(Color.YELLOW);
                r.setStrokeWidth(2);
                overlay.add(r);
                rm.addRoi(r);
            }
        }

        drawImp.setOverlay(overlay);
        ImagePlus flattened = drawImp.flatten(); 
        flattened.setTitle("Burned_" + drawImp.getTitle());
        flattened.show();
    }

    private int findValley(float[] projection, int startY, int limitY, int dir) {
        int bestY = startY;
        float minVal = projection[startY];
        int current = startY;
        while (current != limitY) {
            if (projection[current] <= minVal) {
                minVal = projection[current];
                bestY = current;
            }
            current += dir;
            if (current < 0 || current >= projection.length) break;
        }
        return bestY;
    }
}