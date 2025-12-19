import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.PlugInFilter;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.measure.Measurements;
import java.util.*;

public class Particle_Analyzer_TopK implements PlugInFilter {
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        // Requires an 8-bit image; should be binary (0 and 255)
        return DOES_8G; 
    }

    public void run(ImageProcessor ip) {
        // Example: Get the top 5 particles
        topKWithAreas(5);
    }

    /**
     * Finds the top K particles by area and prints their details.
     * @param k The number of largest particles to retrieve.
     */
    public void topKWithAreas(int k) {
        // 1. Setup Results Table and Analyzer
        ResultsTable rt = new ResultsTable();
        int options = ParticleAnalyzer.SHOW_NONE;
        int measurements = Measurements.AREA | Measurements.CENTROID;
        
        // Analyze particles (0 to Double.MAX_VALUE filters nothing by size)
        ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt, 0, Double.MAX_VALUE);
        pa.analyze(imp);

        int count = rt.getCounter();
        if (count == 0) {
            IJ.log("No particles found. Ensure the image is binary (Process > Binary > Make Binary).");
            return;
        }

        // 2. Store results in a list
        ArrayList<ParticleData> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new ParticleData(
                rt.getValue("Area", i),
                rt.getValue("X", i),
                rt.getValue("Y", i)
            ));
        }

        // 3. Sort by Area (Descending)
        Collections.sort(list, (p1, p2) -> Double.compare(p2.area, p1.area));

        // 4. Print results for Top K
        IJ.log("--- Top " + k + " Particles by Area ---");
        int limit = Math.min(k, list.size());
        for (int i = 0; i < limit; i++) {
            ParticleData p = list.get(i);
            IJ.log(String.format("Rank %d: Area=%.2f, Center=(%.2f, %.2f)", 
                   (i + 1), p.area, p.x, p.y));
        }
    }

    // Simple data container
    class ParticleData {
        double area, x, y;
        ParticleData(double area, double x, double y) {
            this.area = area;
            this.x = x;
            this.y = y;
        }
    }
}