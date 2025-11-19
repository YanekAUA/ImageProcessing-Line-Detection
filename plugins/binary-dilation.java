import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import java.awt.*;

class BinaryDilation implements PlugIn {

    public void run(String arg) {
        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            IJ.noImage();
            return;
        }

        IJ.log("BinaryDilation: running on image '" + imp.getTitle() + "' (type=" + imp.getType() + ")");

        // If image is not 8-bit, make a copy and convert to binary for processing.
        ImagePlus procImp = imp.duplicate();
        ImageProcessor ip = procImp.getProcessor();
        if (!(ip instanceof ByteProcessor)) {
            // Use ImageJ commands to convert to 8-bit and then to binary.
            IJ.run(procImp, "8-bit", "");
            IJ.run(procImp, "Make Binary", "");
            ip = procImp.getProcessor();
            IJ.log("BinaryDilation: converted duplicate to 8-bit and binary (w=" + procImp.getWidth() + ", h=" + procImp.getHeight() + ")");
        } else {
            IJ.log("BinaryDilation: image is already 8-bit binary (w=" + procImp.getWidth() + ", h=" + procImp.getHeight() + ")");
        }

        // Dialog: user provides structuring element as rows of 0/1
        GenericDialog gd = new GenericDialog("Binary Dilation");
        String defaultKernel = "1 1 1\n1 1 1\n1 1 1";
        gd.addMessage("Enter structuring element rows using 1 (foreground) and 0 (background):");
        // GenericDialog in some ImageJ builds lacks addTextArea, so add an AWT TextArea manually
        TextArea ta = new TextArea(defaultKernel, 5, 24);
        Panel p = new Panel(new BorderLayout());
        p.add(ta, BorderLayout.CENTER);
        gd.addPanel(p);
        gd.showDialog();
        if (gd.wasCanceled()) return;

        String kernelText = ta.getText();
        boolean[][] kernel = parseKernel(kernelText);
        if (kernel == null) {
            IJ.error("Invalid structuring element. Use lines of 0/1 separated by spaces or commas.");
            return;
        }

        IJ.log("BinaryDilation: kernel text:\n" + kernelText);

        int kh = kernel.length;
        int kw = kernel[0].length;
        int anchorX = kw / 2;
        int anchorY = kh / 2;

        IJ.log("BinaryDilation: kernel size=" + kw + "x" + kh + " anchor=" + anchorX + "," + anchorY);

        ImageProcessor src = procImp.getProcessor().convertToByte(true);
        int w = src.getWidth();
        int h = src.getHeight();
        ByteProcessor out = new ByteProcessor(w, h);
        out.setValue(0);
        out.fill();

        // Dilation: for each foreground pixel in src, set pixels in out where kernel == 1
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = src.getPixel(x, y) & 0xff;
                if (v != 0) {
                    // foreground pixel found at (x,y)
                    for (int ky = 0; ky < kh; ky++) {
                        for (int kx = 0; kx < kw; kx++) {
                            if (!kernel[ky][kx]) continue;
                            int nx = x + (kx - anchorX);
                            int ny = y + (ky - anchorY);
                            if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                                out.set(nx, ny, 255);
                            }
                        }
                    }
                }
            }
        }

        // Count written (foreground) pixels in output for logging
        int written = 0;
        for (int y2 = 0; y2 < h; y2++) {
            for (int x2 = 0; x2 < w; x2++) {
                if ((out.getPixel(x2, y2) & 0xff) != 0) written++;
            }
        }

        ImagePlus result = new ImagePlus(imp.getTitle() + " - Dilation", out);
        result.show();
        IJ.log("BinaryDilation: result shown ('" + result.getTitle() + "'), foreground pixels=" + written);
    }

    // Parse a structuring element from text. Lines -> rows; tokens separated by spaces or commas.
    private static boolean[][] parseKernel(String text) {
        if (text == null) return null;
        String[] lines = text.split("\\r?\\n");
        java.util.List<boolean[]> rows = new java.util.ArrayList<boolean[]>();
        int width = -1;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.length() == 0) continue;
            String[] tokens = line.split("[ ,\\t]+");
            if (width == -1) width = tokens.length;
            if (tokens.length != width) return null; // inconsistent row lengths
            boolean[] row = new boolean[width];
            for (int i = 0; i < tokens.length; i++) {
                String t = tokens[i].trim();
                if (!(t.equals("0") || t.equals("1"))) return null;
                row[i] = t.equals("1");
            }
            rows.add(row);
        }
        if (rows.size() == 0) return null;
        boolean[][] kernel = new boolean[rows.size()][];
        for (int i = 0; i < rows.size(); i++) kernel[i] = rows.get(i);
        return kernel;
    }

}