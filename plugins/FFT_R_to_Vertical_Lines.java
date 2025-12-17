import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;

import java.util.ArrayList;
import java.util.Collections;

public class FFT_R_to_Vertical_Lines implements PlugIn {

    public void run(String arg) {

        int[] ids = WindowManager.getIDList();
        if (ids == null || ids.length < 2) {
            IJ.showMessage("Error", "Open at least two images:\n1) original cropped image\n2) FFT binary mask");
            return;
        }

        String[] titles = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            titles[i] = WindowManager.getImage(ids[i]).getTitle();
        }

        GenericDialog gd = new GenericDialog("FFT → Vertical Lines");
        gd.addChoice("Original (spatial) image:", titles, titles[titles.length - 2]);
        gd.addChoice("FFT binary mask image:", titles, titles[titles.length - 1]);
        gd.addCheckbox("Keep only horizontal features:", true);
        gd.addNumericField("Angle tolerance (degrees):", 15, 0);
        gd.addNumericField("Deduplicate r tolerance (pixels):", 8, 1);
        gd.addNumericField("Minimum period (pixels):", 5, 0);
        gd.showDialog();
        if (gd.wasCanceled()) return;

        ImagePlus orig = WindowManager.getImage(gd.getNextChoice());
        ImagePlus mask = WindowManager.getImage(gd.getNextChoice());
        boolean filterByAngle = gd.getNextBoolean();
        double angleTol = gd.getNextNumber();
        double rTol = gd.getNextNumber();
        int minPeriod = (int) gd.getNextNumber();

        int W = orig.getWidth();
        int H = orig.getHeight();

        // --- Analyze FFT peaks ---
        ResultsTable rt = new ResultsTable();
        int measurements = Measurements.CENTROID;
        ParticleAnalyzer pa = new ParticleAnalyzer(
                ParticleAnalyzer.SHOW_NONE,
                measurements,
                rt,
                0,
                Double.POSITIVE_INFINITY
        );

        pa.analyze(mask);

        int n = rt.getCounter();
        if (n == 0) {
            IJ.showMessage("No FFT peaks found.");
            return;
        }

        double cx = mask.getWidth() / 2.0;
        double cy = mask.getHeight() / 2.0;

        ArrayList<Double> rVals = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            double x = rt.getValue("X", i);
            double y = rt.getValue("Y", i);
            double dx = x - cx;
            double dy = y - cy;

            double r = Math.sqrt(dx * dx + dy * dy);
            double theta = Math.toDegrees(Math.atan2(dy, dx));

            if (filterByAngle) {
                double d = Math.abs(Math.abs(theta) - 90.0);
                if (d > angleTol) continue;
            }

            rVals.add(r);
        }

        if (rVals.isEmpty()) {
            IJ.showMessage("No peaks survived angle filtering.");
            return;
        }

        // --- Deduplicate r values ---
        Collections.sort(rVals);
        ArrayList<Double> uniqueR = new ArrayList<>();

        double acc = rVals.get(0);
        int cnt = 1;

        for (int i = 1; i < rVals.size(); i++) {
            if (Math.abs(rVals.get(i) - acc / cnt) <= rTol) {
                acc += rVals.get(i);
                cnt++;
            } else {
                uniqueR.add(acc / cnt);
                acc = rVals.get(i);
                cnt = 1;
            }
        }
        uniqueR.add(acc / cnt);

        // --- Convert r → spatial period ---
        ArrayList<Integer> periods = new ArrayList<>();
        for (double r : uniqueR) {
            if (r <= 0) continue;
            int p = (int) Math.round((double) W / r);
            if (p >= minPeriod && p < W / 2) {
                periods.add(p);
            }
        }

        if (periods.isEmpty()) {
            IJ.showMessage("No valid spatial periods found.");
            return;
        }

        // --- Draw vertical lines ---
        ByteProcessor bp = new ByteProcessor(W, H);
        bp.setValue(0);
        bp.fill();

        for (int p : periods) {
            for (int x = 0; x < W; x += p) {
                for (int y = 0; y < H; y++) {
                    bp.set(x, y, 255);
                }
            }
        }

        new ImagePlus("FFT_vertical_lines", bp).show();

        // --- Overlay on clone ---
        ColorProcessor cp = orig.getProcessor().duplicate().convertToColorProcessor();
        int cyan = 0xff00ffff;

        for (int p : periods) {
            for (int x = 0; x < W; x += p) {
                for (int y = 0; y < H; y++) {
                    cp.set(x, y, cyan);
                }
            }
        }

        new ImagePlus("Original_with_lines", cp).show();

        IJ.showStatus("Detected " + periods.size() + " dominant spacings.");
    }
}
