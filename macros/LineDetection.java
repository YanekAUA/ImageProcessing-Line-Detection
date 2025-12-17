// % IMAGE J MACRO %
// VERTICAL EDGES DETECTION
// DESCRIPTION:
// This macro detects vertical edges in an image using convolution with Sobel operators.
// It creates two binary images for east and west edges, then combines them.
// All intermediate images are saved in a temporary directory with the source image name.
// MARK: Step 1: **Setup** the environment by clearing any existing images and selecting the input image file. Create a temporary directory to store intermediate results.
run("Close All");

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
saveAs("Tiff", tmp_dir + "gray-source.tif");

run("Duplicate...", "title=east-edges.tif");
selectImage("east-edges.tif");

run("Duplicate...", "title=west-edges.tif");
selectImage("west-edges.tif");

selectImage("east-edges.tif");
run("Convolve...", "text1=[-1 0 1\n-2 0 2\n-1 0 1\n] normalize");
setOption("BlackBackground", true);
run("Convert to Mask");
saveAs("Tiff", tmp_dir + "east-edges.tif");

selectImage("west-edges.tif");
run("Convolve...", "text1=[1 0 -1\n2 0 -2\n1 0 -1\n] normalize");
setOption("BlackBackground", true);
run("Convert to Mask");
saveAs("Tiff", tmp_dir + "west-edges.tif");

imageCalculator("OR create", "east-edges.tif","west-edges.tif");
saveAs("Tiff", tmp_dir + "vertical-edges.tif");

// CLOSE UNNECESSARY IMAGES
selectImage("east-edges.tif");
close();
selectImage("west-edges.tif");
close();
selectImage("gray-source.tif");
close();


// MARK: Step 3: **Strengthen the detected vertical edges** by applying **1 pixel-wide dilation in horizontal direction** and **1 pixel-wide erosion in vertical direction** (Chapter 9).
// DILATE HORIZONTALLY
selectImage("vertical-edges.tif");
run("Duplicate...", "title=vertical-edges-dilated.tif");
selectImage("vertical-edges-dilated.tif");
run("Convolve...", "text1=[0 0 0\n1 1 1\n0 0 0\n] normalize");
setOption("BlackBackground", true);
run("Convert to Mask");
saveAs("Tiff", tmp_dir + "vertical-edges-dilated.tif");

// EROODE VERTICALLY
selectImage("vertical-edges-dilated.tif");
rename("vertical-edges-dilated-eroded.tif");
run("Invert")
run("Convolve...", "text1=[0 1 0\n0 1 0\n0 1 0\n] normalize");
setAutoThreshold("Default dark no-reset");
run("Threshold...");
setThreshold(128, 255, "raw");
run("Convert to Mask");
run("Close");
run("Invert")


saveAs("Tiff", tmp_dir + "vertical-edges-dilated-eroded.tif");

if (isOpen("vertical-edges.tif")) {
    selectImage("vertical-edges.tif");
    close();
}


// MARK: Step 4: **Denoise** the image of the strengthened vertical edges by applying **linear and/or nonlinear filters of unit radius** (Chapter 5). Make sure the image stays **binary** after denoising.
selectImage("vertical-edges-dilated-eroded.tif");
run("Median...", "radius=2");
saveAs("Tiff", tmp_dir + "vertical-edges-denoised.tif");

// MARK: Step 5: To detect text regions (which have a high-frequency structure), apply the**Bandpass Filter**. Try different values for **large structures** and **small structures limits** (e.g., 40 and 30 pixels) to produce horizontally aligned regions that resemble words or entire text lines (Chapter 19).
selectImage("vertical-edges-denoised.tif");
run("Duplicate...", "title=vertical-edges-bandpassed.tif");
selectImage("vertical-edges-bandpassed.tif");
// run("Bandpass Filter...", "filter_large=40 filter_small=30 suppress=None tolerance=5 autoscale saturate");
run("Bandpass Filter...", "filter_large=" + BP5_large + " filter_small=" + BP5_small + " suppress=Vertical tolerance=5 autoscale saturate");
saveAs("Tiff", tmp_dir + "vertical-edges-bandpassed.tif");

// MARK: Step 6: If necessary, use the filtered image from step 5 as a **binary mask** for the denoised image of the vertical edges from step 4 by applying the **AND operation**.                                                                                                                                                                     
selectImage("vertical-edges-bandpassed.tif");
setAutoThreshold("Default dark no-reset");
run("Convert to Mask");
saveAs("Tiff", tmp_dir + "vertical-edges-bandpass-mask.tif");
imageCalculator("AND create", "vertical-edges-denoised.tif","vertical-edges-bandpass-mask.tif");
selectImage("Result of vertical-edges-denoised.tif");
saveAs("Tiff", tmp_dir + "vertical-edges-masked.tif");

// MARK: Step 7: If step 6 was implemented, apply the same **Bandpass Filter** from step 5 to the masked image.                                                                                                                                                                                                                                        
selectImage("vertical-edges-masked.tif");
rename("vertical-edges-masked-bandpassed.tif");
run("Bandpass Filter...", "filter_large=" + BP7_large + " filter_small=" + BP7_small + " suppress=Vertical tolerance=5 autoscale saturate");
saveAs("Tiff", tmp_dir + "vertical-edges-masked-bandpassed.tif");
setAutoThreshold("Default dark no-reset");
run("Convert to Mask");


// MARK: Step 8: **Analyze the particles** in the filtered image from step 5 or step 7 and show the **fitting ellipses** (Chapter 10).
selectImage("vertical-edges-masked-bandpassed.tif");

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
saveAs("Tiff", tmp_dir + "vertical-edges-ellipses.tif");


// MARK: Step 9: **Skeletonize** the filtered image from step 5 or step 7 or the fitting ellipses from step 8 (Chapter 9).                                                                                                                                                                                                                             
selectImage("vertical-edges-ellipses.tif");
run("Skeletonize");
// run("Invert");
saveAs("Tiff", tmp_dir + "vertical-edges-ellipses-skeleton.tif");

// MARK: Step 10: **Detect the horizontal lines** by applying **Hough Transform** (Chapter 7). Use **Hough_Transform.java PlugInFilter**. The horizontal lines are identified by the angle $\pi/2$. Convert the image of the Hough Transform to grayscale and apply a **threshold** to its region around angle $\pi/2$ to locate the horizontal lines. 
selectImage("vertical-edges-ellipses-skeleton.tif");
run("Hough Transform");
saveAs("Tiff", tmp_dir + "Hough.tif");

// MARK: Step 11: Find the maximum areas in the Hough Transform image around angle $\pi/2$ and use these to determine the positions of the horizontal lines in the original image. Draw these lines on the original image to visualize the detected text lines.
selectImage("Hough");
run("8-bit");
run("Get Horizontal Lines From Hough");
saveAs("Tiff", tmp_dir + "Hough-horizontal-lines.tif");

// MARK: Step 12: **Right Projection**: Apply the **Right_Projection.java PlugInFilter** to the final filtered image from step 5 or step 7 to visualize the text lines more clearly.
selectImage("vertical-edges-masked-bandpassed.tif");
run("Right Projection");
saveAs("Tiff", tmp_dir + "Right-Projection.tif");