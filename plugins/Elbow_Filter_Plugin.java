import ij.*;
import ij.process.*;
import ij.plugin.filter.PlugInFilter;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.measure.Measurements;
import java.util.*;

public class Elbow_Filter_Plugin implements PlugInFilter {
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_8G;
    }

    public void run(ImageProcessor ip) {
        ResultsTable rt = ResultsTable.getResultsTable(); // Use the global table
        rt.reset(); // Clear previous results

        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE, 
                                 Measurements.AREA | Measurements.CENTROID, rt, 0, Double.MAX_VALUE);
        pa.analyze(imp);

        int count = rt.getCounter();
        if (count < 3) return;

        // 1. Collect and Sort
        ArrayList<double[]> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            data.add(new double[]{rt.getValue("Area", i), rt.getValue("X", i), rt.getValue("Y", i)});
        }
        data.sort((a, b) -> Double.compare(b[0], a[0]));

        // 2. Find Elbow
        int elbowIndex = findElbow(data);
        int MAX_COUNT = 10;
        if (elbowIndex > MAX_COUNT) elbowIndex = MAX_COUNT;
        
        // 3. REWRITE the Results Table with only the filtered particles
        rt.reset();
        for (int i = 0; i < elbowIndex; i++) {
            rt.incrementCounter();
            rt.addValue("Area", data.get(i)[0]);
            rt.addValue("X", data.get(i)[1]);
            rt.addValue("Y", data.get(i)[2]);
        }
        rt.show("Results");
    }

    private int findElbow(ArrayList<double[]> list) {
        int n = list.size();
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