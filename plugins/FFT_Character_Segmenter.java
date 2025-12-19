import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.Blitter;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FFTProminentFreqToBoxes
 *
 * ImageJ plugin that takes a binarized FFT magnitude image and a target (grayscale) image
 * and tries to detect prominent frequencies (on the right half of the FFT, excluding the center),
 * use them to estimate expected letter-separation periods, then find column minima in the target
 * projection that match those periods and produce vertical separators. From separators it builds
 * bounding boxes across the line of text and displays/burns them on the target image.
 *
 * Parameters shown in a dialog:
 *  - FFT binary image (open images presented)
 *  - Target image (the original cropped line)
 *  - Center exclusion radius (px) around the FFT center to ignore
 *  - Max candidates (approx number of separators to keep)
 *  - Min distance between separators (px)
 *  - Min box width (px)
 *  - Use burn? (burn rectangles into image) otherwise overlay is used
 *
 * Notes / limitations:
 *  - This is a heuristic approach that uses the binary FFT magnitude to find strong frequency bins,
 *    then translates them to an estimated period = targetWidth/(k - center). The plugin then finds
 *    local minima in the column-sum projection of the target image and keeps minima that agree
 *    with the estimated periods.  Finally separators are merged and used to create bounding boxes.
 *  - Because many choices are heuristic, you may need to tune dialog parameters.
 */
public class FFT_Character_Segmenter implements PlugIn {

    @Override
    public void run(String arg) {
        String[] titles = WindowManager.getImageTitles();
        if (titles == null || titles.length < 2) {
            IJ.error("Please open at least two images: the FFT binary magnitude image and the target line image.");
            return;
        }

        GenericDialog gd = new GenericDialog("FFT -> Prominent Frequencies -> Boxes");
        gd.addChoice("FFT binary image:", titles, titles[titles.length - 1]);
        gd.addChoice("Target image:", titles, titles[0]);
        gd.addNumericField("Center exclusion radius (px):", 8, 0);
        gd.addNumericField("Max candidates (boxes):", 60, 0);
        gd.addNumericField("Min distance between separators (px):", 6, 0);
        gd.addNumericField("Min box width (px):", 4, 0);
        gd.addCheckbox("Burn rectangles into image (unchecked = overlay):", false);
        gd.showDialog();
        if (gd.wasCanceled()) return;

        String fftTitle = gd.getNextChoice();
        String targetTitle = gd.getNextChoice();
        int excl = (int) gd.getNextNumber();
        int maxCandidates = (int) gd.getNextNumber();
        int minDist = (int) gd.getNextNumber();
        int minBoxW = (int) gd.getNextNumber();
        boolean burn = gd.getNextBoolean();

        ImagePlus fftImp = WindowManager.getImage(fftTitle);
        ImagePlus targetImp = WindowManager.getImage(targetTitle);
        if (fftImp == null || targetImp == null) {
            IJ.error("Selected images not available.");
            return;
        }

        ImageProcessor fftIp = fftImp.getProcessor().convertToByte(true);
        ImageProcessor targetIp = targetImp.getProcessor().convertToByte(true);

        int fw = fftIp.getWidth();
        int fh = fftIp.getHeight();
        int tw = targetIp.getWidth();

        int center = fw / 2;
        int startX = Math.min(fw - 1, center + Math.max(1, excl));

        // Build column sums on FFT (right half) -- white pixels assumed 255
        double[] colSums = new double[fw - startX];
        for (int x = startX; x < fw; x++) {
            double s = 0;
            for (int y = 0; y < fh; y++) {
                s += (fftIp.getPixel(x, y) & 0xff) / 255.0; // normalized
            }
            colSums[x - startX] = s;
        }

        // Smooth the colSums mildly with a 3-wide moving average to reduce noise
        double[] smooth = smooth1D(colSums, 3);

        // Detect peaks (local maxima) that are above mean + std
        double mean = mean(smooth);
        double sd = std(smooth, mean);
        double threshold = mean + sd * 0.5; // gentle threshold
        List<Integer> peakIndices = findPeaks(smooth, threshold);

        if (peakIndices.isEmpty()) {
            IJ.log("No peaks detected in FFT right half using threshold; trying lower threshold.");
            threshold = mean + sd * 0.1;
            peakIndices = findPeaks(smooth, threshold);
        }

        // Convert peak indices to absolute FFT x indices
        List<Integer> peakXs = new ArrayList<>();
        for (int pi : peakIndices) peakXs.add(startX + pi);

        // Sort peaks by descending strength and keep the most important using an elbow-like method
        Collections.sort(peakXs, (a, b) -> Double.compare(smooth[b - startX], smooth[a - startX]));

        // Apply a simple elbow: choose top N where N is determined by kneedle on strengths
        int keepN = determineElbowCount(smooth, peakXs, startX, maxCandidates);
        if (keepN <= 0) keepN = Math.min(maxCandidates, peakXs.size());
        List<Integer> kept = peakXs.subList(0, Math.min(keepN, peakXs.size()));

        IJ.log("Detected " + kept.size() + " prominent frequency bins.");

        // Prepare column projection of target image (sums of darkness). Letters are darker on carved rock.
        double[] colProj = new double[tw];
        for (int x = 0; x < tw; x++) {
            double s = 0;
            for (int y = 0; y < targetIp.getHeight(); y++) {
                s += 255 - (targetIp.getPixel(x, y) & 0xff); // darker pixels -> larger values
            }
            colProj[x] = s;
        }
        double projMean = mean(colProj);
        double projSd = std(colProj, projMean);

        // Find local minima in projection (gaps between letters) -- minima lower than mean - 0.25*sd
        List<Integer> minima = findLocalMinima(colProj, projMean - projSd * 0.25);

        // For each kept frequency bin compute estimated period and select minima that match period multiples
        Set<Integer> separators = new HashSet<>();
        for (int fx : kept) {
            int delta = fx - center;
            if (delta == 0) continue;
            // estimated period in pixels
            double period = (double) tw / Math.abs(delta);
            if (period < 6 || period > tw * 2) continue; // ignore nonsense periods

            // collect minima that fit into periodic grid
            List<Integer> candidatesForThis = pickMinimaByPeriod(minima, period, tw, (int) Math.max(1, Math.round(period * 0.4)));
            separators.addAll(candidatesForThis);
        }

        // Fallback: if no separators found, use minima directly but prune
        if (separators.isEmpty()) {
            IJ.log("No separators matched the estimated periods; falling back to minima list.");
            separators.addAll(minima);
        }

        // Convert to list and sort
        List<Integer> sepList = new ArrayList<>(separators);
        Collections.sort(sepList);

        // Merge separators that are too close
        sepList = mergeNearby(sepList, minDist);

        // Add bounds at ends
        if (sepList.isEmpty()) {
            IJ.log("No separators after merging; aborting.");
            return;
        }

        List<int[]> boxes = new ArrayList<>(); // x0,x1
        int prev = 0;
        for (int s : sepList) {
            int x0 = prev;
            int x1 = Math.max(s, x0 + minBoxW);
            boxes.add(new int[]{x0, x1});
            prev = x1;
        }
        // last box to image end
        if (prev < tw - 1) boxes.add(new int[]{prev, tw - 1});

        // Convert boxes into rectangular ROIs using vertical extents from the image
        Overlay overlay = new Overlay();
        overlay.setStrokeColor(Color.red);
        overlay.setStrokeWidth(1.5);

        ImageProcessor drawIp = targetIp.duplicate();
        for (int[] b : boxes) {
            int x0 = Math.max(0, b[0]);
            int x1 = Math.min(tw - 1, b[1]);
            if (x1 - x0 < minBoxW) continue;
            // compute vertical extent where dark pixels exist inside [x0,x1]
            int yMin = targetIp.getHeight();
            int yMax = 0;
            for (int x = x0; x <= x1; x++) {
                for (int y = 0; y < targetIp.getHeight(); y++) {
                    int v = targetIp.getPixel(x, y) & 0xff;
                    if (v < 250) { // not white
                        if (y < yMin) yMin = y;
                        if (y > yMax) yMax = y;
                    }
                }
            }
            if (yMax < yMin) {
                // no dark pixels found -- create a small box around midline
                yMin = targetIp.getHeight() / 3;
                yMax = 2 * targetIp.getHeight() / 3;
            }
            int w = x1 - x0 + 1;
            int h = yMax - yMin + 1;
            Roi r = new Roi(x0, yMin, w, h);
            overlay.add(r);

            if (burn) {
                drawIp.setColor(Color.red);
                drawIp.drawRect(x0, yMin, w - 1, h - 1);
            }
        }

        if (burn) {
            // burn into the image by drawing the duplicated processor onto the original
            targetImp.getProcessor().copyBits(drawIp, 0, 0, Blitter.COPY);
            targetImp.updateAndDraw();
        } else {
            targetImp.setOverlay(overlay);
            targetImp.updateAndDraw();
        }

        IJ.log("Done. Boxes created: " + overlay.size());
    }

    // ---------- utility methods ----------

    private static double[] smooth1D(double[] a, int window) {
        int n = a.length;
        double[] res = new double[n];
        int r = window / 2;
        for (int i = 0; i < n; i++) {
            double s = 0;
            int c = 0;
            for (int j = i - r; j <= i + r; j++) {
                if (j >= 0 && j < n) {
                    s += a[j];
                    c++;
                }
            }
            res[i] = s / Math.max(1, c);
        }
        return res;
    }

    private static double mean(double[] a) {
        double s = 0;
        for (double v : a) s += v;
        return s / a.length;
    }

    private static double std(double[] a, double mean) {
        double s = 0;
        for (double v : a) {
            double d = v - mean;
            s += d * d;
        }
        return Math.sqrt(s / a.length);
    }

    private static List<Integer> findPeaks(double[] arr, double threshold) {
        List<Integer> peaks = new ArrayList<>();
        for (int i = 1; i < arr.length - 1; i++) {
            if (arr[i] > arr[i - 1] && arr[i] > arr[i + 1] && arr[i] >= threshold) peaks.add(i);
        }
        return peaks;
    }

    private static int determineElbowCount(double[] smooth, List<Integer> peakXs, int startX, int maxCandidates) {
        // strengths sorted descending in parallel to peakXs
        List<Double> strengths = new ArrayList<>();
        for (int p : peakXs) strengths.add(smooth[p - startX]);
        if (strengths.size() <= 1) return strengths.size();

        // compute kneedle: find point with maximal distance from line connecting first and last
        int n = strengths.size();
        double x1 = 0, y1 = strengths.get(0);
        double x2 = n - 1, y2 = strengths.get(n - 1);
        double maxDist = -1;
        int bestIdx = Math.min(n, maxCandidates) - 1;
        for (int i = 1; i < Math.min(n, maxCandidates); i++) {
            double xi = i;
            double yi = strengths.get(i);
            // distance from point to line
            double num = Math.abs((y2 - y1) * xi - (x2 - x1) * yi + x2 * y1 - y2 * x1);
            double den = Math.hypot(y2 - y1, x2 - x1);
            double d = den == 0 ? 0 : num / den;
            if (d > maxDist) {
                maxDist = d;
                bestIdx = i;
            }
        }
        // return bestIdx+1 (count)
        return Math.max(1, Math.min(maxCandidates, bestIdx + 1));
    }

    private static List<Integer> findLocalMinima(double[] arr, double belowThreshold) {
        List<Integer> mins = new ArrayList<>();
        for (int i = 1; i < arr.length - 1; i++) {
            if (arr[i] < arr[i - 1] && arr[i] < arr[i + 1] && arr[i] <= belowThreshold) mins.add(i);
        }
        return mins;
    }

    private static List<Integer> pickMinimaByPeriod(List<Integer> minima, double period, int width, int tol) {
        List<Integer> out = new ArrayList<>();
        if (minima.isEmpty()) return out;
        Collections.sort(minima);
        // attempt to find chains of minima spaced roughly by period
        for (int startIdx = 0; startIdx < minima.size(); startIdx++) {
            int seed = minima.get(startIdx);
            List<Integer> chain = new ArrayList<>();
            chain.add(seed);
            int last = seed;
            for (int j = startIdx + 1; j < minima.size(); j++) {
                int cand = minima.get(j);
                int diff = cand - last;
                if (Math.abs(diff - period) <= Math.max(tol, period * 0.35)) {
                    chain.add(cand);
                    last = cand;
                } else if (diff > period * 1.5) {
                    // gap too large - break chain
                    break;
                }
            }
            if (chain.size() >= 2) {
                out.addAll(chain);
            }
        }
        // As a fallback, if out is empty but minima exist, pick minima roughly at intervals of period
        if (out.isEmpty()) {
            double p = period;
            double pos = p / 2.0;
            while (pos < width) {
                // pick nearest minima to pos
                int best = -1;
                double bestd = Double.MAX_VALUE;
                for (int m : minima) {
                    double d = Math.abs(m - pos);
                    if (d < bestd) {
                        bestd = d;
                        best = m;
                    }
                }
                if (best != -1 && bestd <= Math.max(3, p * 0.35)) out.add(best);
                pos += p;
            }
        }

        // clamp to image bounds and deduplicate
        List<Integer> uniq = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (int v : out) {
            int vv = Math.max(0, Math.min(width - 1, v));
            if (!seen.contains(vv)) {
                uniq.add(vv);
                seen.add(vv);
            }
        }
        return uniq;
    }

    private static List<Integer> mergeNearby(List<Integer> vals, int minDist) {
        if (vals.isEmpty()) return vals;
        Collections.sort(vals);
        List<Integer> out = new ArrayList<>();
        int cur = vals.get(0);
        for (int i = 1; i < vals.size(); i++) {
            int v = vals.get(i);
            if (v - cur <= minDist) {
                // merge by averaging
                cur = (cur + v) / 2;
            } else {
                out.add(cur);
                cur = v;
            }
        }
        out.add(cur);
        return out;
    }
}
