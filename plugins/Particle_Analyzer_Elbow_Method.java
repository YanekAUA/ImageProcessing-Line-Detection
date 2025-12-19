import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.PlugInFilter;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.measure.Measurements;
import java.util.*;

public class Particle_Elbow_Method implements PlugInFilter {
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_8G;
    }

    public void run(ImageProcessor ip) {
        // 1. Get all particle data
        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.SHOW_NONE, Measurements.AREA | Measurements.CENTROID, rt, 0, Double.MAX_VALUE);
        pa.analyze(imp);

        int count = rt.getCounter();
        if (count < 3) {
            IJ.log("Not enough particles to determine an elbow.");
            return;
        }

        ArrayList<ParticleData> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new ParticleData(rt.getValue("Area", i), rt.getValue("X", i), rt.getValue("Y", i)));
        }

        // 2. Sort areas descending
        Collections.sort(list, (p1, p2) -> Double.compare(p2.area, p1.area));

        // 3. Find the "Elbow" index
        int elbowIndex = findElbowPoint(list);
        
        // 4. Print results
        IJ.log("Detected " + (elbowIndex + 1) + " significant particles via Elbow Method.");
        for (int i = 0; i <= elbowIndex; i++) {
            ParticleData p = list.get(i);
            IJ.log(String.format("Rank %d: Area=%.2f, Center=(%.2f, %.2f)", (i + 1), p.area, p.x, p.y));
        }
    }

    /**
     * Calculates the point of maximum distance from the line joining start and end points.
     */
    private int findElbowPoint(ArrayList<ParticleData> list) {
        int n = list.size();
        double maxDistance = -1;
        int elbowIndex = 0;

        // Line endpoints (x1, y1) and (x2, y2)
        // x is the index (0 to n-1), y is the area
        double x1 = 0;
        double y1 = list.get(0).area;
        double x2 = n - 1;
        double y2 = list.get(n - 1).area;

        for (int i = 0; i < n; i++) {
            double x0 = i;
            double y0 = list.get(i).area;

            // Distance from point to line formula: 
            // d = |(y2-y1)x0 - (x2-x1)y0 + x2y1 - y2x1| / sqrt((y2-y1)^2 + (x2-x1)^2)
            double num = Math.abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1);
            double den = Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
            double distance = num / den;

            if (distance > maxDistance) {
                maxDistance = distance;
                elbowIndex = i;
            }
        }
        return elbowIndex;
    }

    class ParticleData {
        double area, x, y;
        ParticleData(double area, double x, double y) {
            this.area = area; this.x = x; this.y = y;
        }
    }
}