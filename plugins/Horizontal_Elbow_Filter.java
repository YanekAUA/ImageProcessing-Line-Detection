import ij.*;
import ij.process.*;
import ij.plugin.filter.PlugInFilter;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.measure.Measurements;
import java.util.*;

public class Horizontal_Elbow_Filter implements PlugInFilter {
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_8G;
    }

    public void run(ImageProcessor ip) {
        ResultsTable rt = new ResultsTable(); 
        rt.reset();

        // Standard Particle Analysis
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE, 
                                 Measurements.AREA | Measurements.CENTROID, rt, 0, Double.MAX_VALUE);
        pa.analyze(imp);

        int count = rt.getCounter();
        if (count == 0) return;

        // 1. Define the "Horizontal" Filter Range (Center X +/- 5%)
        double imgWidth = ip.getWidth();
        double centerX = imgWidth / 2.0;
        double tolerance = imgWidth * 0.05;
        double lowerBound = centerX - tolerance;
        double upperBound = centerX + tolerance;

        IJ.log(String.format("Filtering for Horizontal Lines: X must be between %.2f and %.2f", lowerBound, upperBound));

        // 2. Filter Particles by X-Coordinate
        ArrayList<double[]> filteredData = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double x = rt.getValue("X", i);
            if (x >= lowerBound && x <= upperBound) {
                filteredData.add(new double[]{
                    rt.getValue("Area", i), 
                    x, 
                    rt.getValue("Y", i)
                });
            }
        }

        if (filteredData.size() < 2) {
            IJ.log("No horizontal particles found within the 5% center range.");
            return;
        }

        // 3. Sort by Area (Descending)
        filteredData.sort((a, b) -> Double.compare(b[0], a[0]));

        // 4. Find Elbow within the filtered list
        int elbowIndex = findElbow(filteredData);
        
        // Apply your MAX_COUNT limit (e.g., max 10 lines)
        int MAX_COUNT = 10;
        int finalCount = Math.min(elbowIndex + 1, Math.min(MAX_COUNT, filteredData.size()));

        // 5. Update Global Results Table for the next plugin
        ResultsTable globalRt = ResultsTable.getResultsTable();
        globalRt.reset();
        for (int i = 0; i < finalCount; i++) {
            globalRt.incrementCounter();
            globalRt.addValue("Area", filteredData.get(i)[0]);
            globalRt.addValue("X", filteredData.get(i)[1]);
            globalRt.addValue("Y", filteredData.get(i)[2]);
        }
        
        globalRt.show("Results");
        IJ.log("Kept " + finalCount + " particles representing horizontal lines.");
    }

    private int findElbow(ArrayList<double[]> list) {
        int n = list.size();
        if (n < 3) return n - 1;
        
        double maxDist = -1;
        int index = 0;
        double x1 = 0, y1 = list.get(0)[0];
        double x2 = n - 1, y2 = list.get(n - 1)[0];

        for (int i = 0; i < n; i++) {
            double dist = Math.abs((y2 - y1) * i - (x2 - x1) * list.get(i)[0] + x2 * y1 - y2 * x1) 
                          / Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
            if (dist > maxDist) {
                maxDist = dist;
                index = i;
            }
        }
        return index;
    }
}