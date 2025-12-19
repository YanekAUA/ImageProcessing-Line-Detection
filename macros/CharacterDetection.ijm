run("Close All")
open("D:/Yan/AUA/Image Processing/Text Recognition Project/images/7.jpg");
makeRectangle(523, 1759, 593, 87);
// makeRectangle(522, 1834, 593, 87); // Line 2
// makeRectangle(522, 1900, 593, 87); // Line 3
// makeRectangle(356, 2091, 912, 82); // Last Line
run("Duplicate...", "title=text_crop.jpg");
run("8-bit");


run("Cut After Max") // We zero down the pixels after the maximum frequency of histogtam  and then we stretch the histogram to the right

selectImage("text_crop.jpg");

run("Enhance Contrast", "saturated=0.35");
run("Apply LUT");
run("Sharpen");
run("Gaussian Blur...", "sigma=1");
run("Cut After Max");
run("Enhance Contrast", "saturated=0.35");
run("Apply LUT");
run("Sharpen");

run("FFT");

run("Enhance Contrast", "saturated=0.35");
run("Apply LUT");

run("Threshold...");
setThreshold(255, 255);
run("Convert to Mask");
run("Close-");
run("Close-");
