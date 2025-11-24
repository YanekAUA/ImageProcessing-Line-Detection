// % IMAGE J MACRO %
// VERTICAL EDGES DETECTION
// DESCRIPTION:
// This macro detects vertical edges in an image using convolution with Sobel operators.
// It creates two binary images for east and west edges, then combines them.
// All intermediate images are saved in a temporary directory with the source image name.

run("Close All");

// ----------------- SELECT AN IMAGE ---------------------//
path = File.openDialog("Select a File");
dir = File.getParent(path);
name = File.getName(path);

// --------------- CREATE TEMP DIR -------------------//
tmp_dir_name = ".tmp";
tmp_dir = dir + "\\" + tmp_dir_name;
File.makeDirectory(tmp_dir);
tmp_dir = tmp_dir + "\\" + name + "\\"
File.makeDirectory(tmp_dir);
print(tmp_dir);
//-----------------------------------------------------------------//

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

// **Strengthen the detected vertical edges** by applying **1 pixel-wide dilation in horizontal direction** and **1 pixel-wide erosion in vertical direction** (Chapter 9).
// DILATE HORIZONTALLY
selectImage("vertical-edges.tif");
run("Duplicate...", "title=vertical-edges-right-shift.tif");
run("Duplicate...", "title=vertical-edges-left-shift.tif");

selectImage("vertical-edges-left-shift.tif");
run("Translate...", "x=-1 y=0 interpolation=None");

selectImage("vertical-edges-right-shift.tif");
run("Translate...", "x=1 y=0 interpolation=None");

selectImage("vertical-edges.tif");
imageCalculator("OR create", "vertical-edges.tif","vertical-edges-left-shift.tif");
saveAs("Tiff", tmp_dir + "vertical-edges-dilated.tif");
selectImage("vertical-edges-dilated.tif");
imageCalculator("OR create", "vertical-edges-dilated.tif","vertical-edges-right-shift.tif");
saveAs("Tiff", tmp_dir + "vertical-edges-dilated.tif");

// EROODE VERTICALLY
selectImage("vertical-edges-dilated.tif");
run("Duplicate...", "title=vertical-edges-up-shift.tif");
run("Duplicate...", "title=vertical-edges-down-shift.tif");
selectImage("vertical-edges-up-shift.tif");
run("Translate...", "x=0 y=-1 interpolation=None");
selectImage("vertical-edges-down-shift.tif");
run("Translate...", "x=0 y=1 interpolation=None");
selectImage("vertical-edges-dilated.tif");
imageCalculator("AND create", "vertical-edges-dilated.tif","vertical-edges-up-shift.tif");
saveAs("Tiff", tmp_dir + "vertical-edges-eroded.tif");
selectImage("vertical-edges-eroded.tif");
imageCalculator("AND create", "vertical-edges-eroded.tif","vertical-edges-down-shift.tif");
saveAs("Tiff", tmp_dir + "vertical-edges-eroded.tif");
selectImage("vertical-edges-eroded.tif");
rename("vertical-edges-final.tif");


// CLOSE UNNECESSARY IMAGES
selectImage("vertical-edges-right-shift.tif");
close();
selectImage("vertical-edges-left-shift.tif");
close();
selectImage("vertical-edges-up-shift.tif");
close();
selectImage("vertical-edges-down-shift.tif");
close();
selectImage("vertical-edges-dilated.tif");
close();
selectImage("vertical-edges-eroded.tif");
close();
selectImage("vertical-edges-dilated.tif");
close();