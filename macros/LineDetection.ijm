// % IMAGE J MACRO %
// VERTICAL EDGES DETECTION
// DESCRIPTION:
// This macro detects vertical edges in an image using convolution with Sobel operators.
// It creates two binary images for east and west edges, then combines them.
// All intermediate images are saved in a temporary directory with the source image name.
// MARK: Step 1: **Setup** the environment by clearing any existing images and selecting the input image file. Create a temporary directory to store intermediate results.
run("Close All");
// --- Filename variables (use these instead of repeating strings) ---
eastEdges = "01-east-edges.tif";
westEdges = "02-west-edges.tif";
verticalEdges = "03-vertical-edges.tif";
verticalEdgesDilated = "04-vertical-edges-dilated.tif";
verticalEdgesDilatedEroded = "05-vertical-edges-dilated-eroded.tif";
verticalEdgesDenoised = "06-vertical-edges-denoised.tif";
verticalEdgesBandpassed = "07-vertical-edges-bandpassed.tif";
verticalEdgesBandpassMask = "08-vertical-edges-bandpass-mask.tif";
verticalEdgesMasked = "09-vertical-edges-masked.tif";
verticalEdgesMaskedBandpassed = "10-vertical-edges-masked-bandpassed.tif";
verticalEdgesEllipses = "11-vertical-edges-ellipses.tif";
verticalEdgesSkeleton = "12-vertical-edges-skeleton.tif";
hough = "13-Hough.tif";
houghHorizontalLines = "14-Hough-horizontal-lines.tif";
rightProjection = "15-Right-Projection.tif";
graySource = "16-gray-source.tif";

// ----------------- SELECT AN IMAGE ---------------------//
path = File.openDialog("Select a File");
dir = File.getParent(path);
name = File.getName(path);
BP5_large = 40;
BP5_small = 30;

BP7_large = 40;
BP7_small = 30;

// --------------- CREATE TEMP DIR -------------------//
tmp_dir_name = ".tmp";
tmp_dir = dir + "\\" + tmp_dir_name;
File.makeDirectory(tmp_dir);
tmp_dir = tmp_dir + "\\" + name + "\\"
File.makeDirectory(tmp_dir);
print(tmp_dir);
//-----------------------------------------------------------------//


// MARK: Step 2: **Detect vertical edges** by applying **Sobel operators** in the horizontal direction (Chapter 4). Create two binary images: one for the east edges and one for the west edges. Combine these two images using the **OR operation** to produce a single binary image of vertical edges.
open(path); // LOAD
run("8-bit"); // GRAY SCALE
saveAs("Tiff", tmp_dir + graySource);

run("Duplicate...", "title=" + eastEdges);
selectImage(eastEdges);

run("Duplicate...", "title=" + westEdges);
selectImage(westEdges);

selectImage(eastEdges);
run("Convolve...", "text1=[-1 0 1\n-2 0 2\n-1 0 1\n] normalize");
setOption("BlackBackground", true);
run("Convert to Mask");
saveAs("Tiff", tmp_dir + eastEdges);

selectImage(westEdges);
run("Convolve...", "text1=[1 0 -1\n2 0 -2\n1 0 -1\n] normalize");
setOption("BlackBackground", true);
run("Convert to Mask");
saveAs("Tiff", tmp_dir + westEdges);

imageCalculator("OR create", eastEdges, westEdges);
saveAs("Tiff", tmp_dir + verticalEdges);

// CLOSE UNNECESSARY IMAGES
selectImage(eastEdges);
close();
selectImage(westEdges);
close();
selectImage(graySource);
close();


// MARK: Step 3: **Strengthen the detected vertical edges** by applying **1 pixel-wide dilation in horizontal direction** and **1 pixel-wide erosion in vertical direction** (Chapter 9).
// DILATE HORIZONTALLY
selectImage(verticalEdges);
run("Duplicate...", "title=" + verticalEdgesDilated);
selectImage(verticalEdgesDilated);
run("Convolve...", "text1=[0 0 0\n1 1 1\n0 0 0\n] normalize");
setOption("BlackBackground", true);
run("Convert to Mask");
saveAs("Tiff", tmp_dir + verticalEdgesDilated);

// EROODE VERTICALLY
selectImage(verticalEdgesDilated);
rename(verticalEdgesDilatedEroded);
run("Invert")
run("Convolve...", "text1=[0 1 0\n0 1 0\n0 1 0\n] normalize");
setAutoThreshold("Default dark no-reset");
run("Threshold...");
setThreshold(128, 255, "raw");
run("Convert to Mask");
run("Close");
run("Invert")


saveAs("Tiff", tmp_dir + verticalEdgesDilatedEroded);

if (isOpen(verticalEdges)) {
    selectImage(verticalEdges);
    close();
}


// MARK: Step 4: **Denoise** the image of the strengthened vertical edges by applying **linear and/or nonlinear filters of unit radius** (Chapter 5). Make sure the image stays **binary** after denoising.
selectImage(verticalEdgesDilatedEroded);
run("Median...", "radius=2");
saveAs("Tiff", tmp_dir + verticalEdgesDenoised);

// MARK: Step 5: To detect text regions (which have a high-frequency structure), apply the**Bandpass Filter**. Try different values for **large structures** and **small structures limits** (e.g., 40 and 30 pixels) to produce horizontally aligned regions that resemble words or entire text lines (Chapter 19).
selectImage(verticalEdgesDenoised);
run("Duplicate...", "title=" + verticalEdgesBandpassed);
selectImage(verticalEdgesBandpassed);
// run("Bandpass Filter...", "filter_large=40 filter_small=30 suppress=None tolerance=5 autoscale saturate");
run("Bandpass Filter...", "filter_large=" + BP5_large + " filter_small=" + BP5_small + " suppress=Vertical tolerance=5 autoscale saturate");
saveAs("Tiff", tmp_dir + verticalEdgesBandpassed);

// MARK: Step 6: If necessary, use the filtered image from step 5 as a **binary mask** for the denoised image of the vertical edges from step 4 by applying the **AND operation**.                                                                                                                                                                     
selectImage(verticalEdgesBandpassed);
setAutoThreshold("Default dark no-reset");
run("Convert to Mask");
saveAs("Tiff", tmp_dir + verticalEdgesBandpassMask);
imageCalculator("AND create", verticalEdgesDenoised, verticalEdgesBandpassMask);
selectImage("Result of " + verticalEdgesDenoised);
saveAs("Tiff", tmp_dir + verticalEdgesMasked);

// // MARK: Step 7: If step 6 was implemented, apply the same **Bandpass Filter** from step 5 to the masked image.                                                                                                                                                                                                                                        
selectImage(verticalEdgesMasked);
rename(verticalEdgesMaskedBandpassed);
run("Bandpass Filter...", "filter_large=" + BP7_large + " filter_small=" + BP7_small + " suppress=Vertical tolerance=5 autoscale saturate");
saveAs("Tiff", tmp_dir + verticalEdgesMaskedBandpassed);
setAutoThreshold("Default dark no-reset");
run("Convert to Mask");


// MARK: Step 8: **Analyze the particles** in the filtered image from step 5 or step 7 and show the **fitting ellipses** (Chapter 10).
selectImage(verticalEdgesMaskedBandpassed);

// Analyze Particles settings:
// - show=Ellipses → draws ellipses on a new image
// - display → shows measurement table
// - exclude → ignore objects touching border
// - clear → clear previous results
// - add → add to ROI Manager
// - in_situ → draw ellipses directly on the image
run("Set Measurements...", "area centroid fit shape redirect=None decimal=3");
run("Analyze Particles...", "size=20-Infinity show=Ellipses");
run("Convert to Mask");
run("Fill Holes");
// Save ellipse overlay
saveAs("Tiff", tmp_dir + verticalEdgesEllipses);


// MARK: Step 9: **Skeletonize** the filtered image from step 5 or step 7 or the fitting ellipses from step 8 (Chapter 9).                                                                                                                                                                                                                             
selectImage(verticalEdgesMaskedBandpassed);
// selectImage(verticalEdgesEllipses);
run("Duplicate...", "title=" + verticalEdgesSkeleton);
run("Skeletonize");
// run("Invert");
saveAs("Tiff", tmp_dir + verticalEdgesSkeleton);

// MARK: Step 10: **Detect the horizontal lines** by applying **Hough Transform** (Chapter 7). Use **Hough_Transform.java PlugInFilter**. The horizontal lines are identified by the angle $\pi/2$. Convert the image of the Hough Transform to grayscale and apply a **threshold** to its region around angle $\pi/2$ to locate the horizontal lines. 
selectImage(verticalEdgesSkeleton);
run("Hough Transform");
saveAs("Tiff", tmp_dir + hough);

// MARK: Step 11: Find the maximum areas in the Hough Transform image around angle $\pi/2$ and use these to determine the positions of the horizontal lines in the original image. Draw these lines on the original image to visualize the detected text lines.
selectImage(hough);
run("8-bit");
run("Get Horizontal Lines From Hough");
saveAs("Tiff", tmp_dir + houghHorizontalLines);

// MARK: Step 12: **Right Projection**: Apply the **Right_Projection.java PlugInFilter** to the final filtered image from step 5 or step 7 to visualize the text lines more clearly.
selectImage(verticalEdgesMaskedBandpassed);
run("Right Projection");
saveAs("Tiff", tmp_dir + rightProjection);