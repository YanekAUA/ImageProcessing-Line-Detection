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

