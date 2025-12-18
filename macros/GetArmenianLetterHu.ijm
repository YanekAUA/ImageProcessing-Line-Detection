run("Close All")
// Ask for a folder containing images
dir = getDirectory("Choose a Directory Containing binary images of Armenian letters");

// --------------- CREATE TEMP DIR -------------------//
tmp_dir_name = ".tmp";
tmp_dir = dir + "\\" + tmp_dir_name;
File.makeDirectory(tmp_dir);
print(tmp_dir);

//-----------------------------------------------------------------//

// 1) Go through all .png/.jpg/.tif files
// 2) Open each file
// 3) Binarize via Convert to Mask
// 4) Skeletonize
// 5) Measure and save results to tmp_dir under the file name folder
// 6) Save also the skeletonized image for visual verification

list = getFileList(dir);
for (i = 0; i < list.length; i++) {
    filename = list[i];
    if (endsWith(filename, ".png") || endsWith(filename, ".jpg") || endsWith(filename, ".tif")) {
        print("Processing " + filename);
        file_folder = tmp_dir + "\\" + filename;
        File.makeDirectory(file_folder);

        open(dir + filename);
        run("8-bit");
        run("Convert to Mask");
        run("Skeletonize");
        
        // Save skeletonized image
        saveAs("Tiff", file_folder + "\\" + filename);
        
        // Measure
        run("Set Measurements...", "area perimeter shape feret's redirect=None decimal=3");
        
        run("Measure");
        saveAs("Results", file_folder + "\\" + filename + "_results.csv");
        run("Clear Results");
        close();
    }
}