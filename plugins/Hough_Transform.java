import ij.IJ; 
import ij.ImagePlus; 
 import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.plugin.filter.PlugInFilter;

public class Hough_Transform implements PlugInFilter {

	public int setup(String args, ImagePlus im) {
		return DOES_8G;
	}
	
	public void run(ImageProcessor imageSpace) {
		int height = imageSpace.getHeight(), h2 = height / 2;
		int width = imageSpace.getWidth(), w2 = width / 2;
		ImageProcessor paramSpace = new ColorProcessor(width, (int) Math.hypot(height, width));

		double tMax = Math.PI, dt = tMax / width;
		double rMax = Math.hypot(width, height), dr = rMax / paramSpace.getHeight(), rMax2 = rMax / 2;
		double r, maxGrey = 0;
		int i, j, pixel;
		
		for (int col = -w2; col < w2; col++)
			for (int row = -h2; row < h2; row++) 
				if (imageSpace.getPixel(col + w2, row + h2) < 150) 
					for (double t = 0; t < tMax; t += dt) {
						r = col * Math.cos(t) + row * Math.sin(t);
						i = (int) (t / dt + 0.5);
						j = (int) ((r + rMax2) / dr + 0.5);
						paramSpace.putPixel(i, j, paramSpace.getPixel(i, j) + 1);
					}
					
		for (int col = 0; col < width; col++)
			for (int row = 0; row < paramSpace.getHeight(); row++) {
				pixel = paramSpace.getPixel(col, row);
				if (pixel > maxGrey)
					maxGrey = pixel;
			}
		maxGrey = 255 / maxGrey;
		
		for (int col = 0; col < width; col++)
			for (int row = 0; row < paramSpace.getHeight(); row++)
			{
				pixel = 255 - (int) (maxGrey * paramSpace.getPixel(col, row));
				pixel *= 65793; 
				paramSpace.putPixel(col, row, pixel);
			}
			
		(new ImagePlus("Hough", paramSpace)).show();
	}
} 