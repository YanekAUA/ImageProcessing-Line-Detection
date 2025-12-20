# Text Line Recognition Project
<!-- 
The Project aims at detection / recognition of stone inscriptions. Due to weathering, vandalism,
erosion, and the complexity of ancient scripts, many of these texts are hard to read. Study, apply
and test Image Processing methods and algorithms to detect Armenian inscriptions.
The shapes of Armenian letters are based on vertical strokes. Based on this observation, implement
and test a pipeline outlined below. Implement the steps as ImageJ plug-ins or menu commands,
and save them using the ImageJ macro recorder. In addition to the recorded macro(s), submit the
code of all implemented plug-ins. Other image processing and programming environments may
be used only for testing purposes. -->

The Project aims at detection / recognition of stone inscriptions. Due to weathering, vandalism, erosion, and the complexity of ancient scripts, many of these texts are hard to read. Study, apply and test Image Processing methods and algorithms to detect Armenian inscriptions. The shapes of Armenian letters are based on vertical strokes. Based on this observation, implement and test a pipeline outlined below. Implement the steps as ImageJ plug-ins or menu commands, and save them using the ImageJ macro recorder. In addition to the recorded macro(s), submit the code of all implemented plug-ins. Other image processing and programming environments may be used only for testing purposes.

# Part 1: Text Line Detection
## Macro to run:

- [`macros/LineDetection.ijm`](macros/LineDetection.ijm) — Macro implementing Part 1 (Text Line Detection). Runs the full pipeline: prepare workspace, vertical edge detection (east/west), edge strengthening and denoising, bandpass filtering for horizontally aligned text regions, particle/ellipse summarization, skeletonization, Hough transform and peak selection, mapping peaks back to the original image and drawing bounding boxes. Saves intermediate TIFFs under `.tmp/<image>/` as described in the STEPS table.
- [`macros/SetupPlugins.ijm`](macros/SetupPlugins.ijm) — Helper macro that installs the Java plugins required for the Part 1 pipeline into a configured ImageJ plugins folder (see the "About [`macros/SetupPlugins.ijm`](macros/SetupPlugins.ijm)" note later in this README).


## STEPS:
### Combined description + implementation (concise)

| Step  | Description (what the step should do)                                           | What the macro does (implementation detail)                                                                                                                                         | Output / Submission                                                     | Status |
| :---: | :------------------------------------------------------------------------------ | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :---------------------------------------------------------------------- | :----: |
|   0   | Prepare workspace: collect inputs, save initial gray image, create temp folder. | `Close All`; `File.openDialog()`; `run("8-bit")`; create `.tmp/<name>/`; save `00-gray-source.tif`.                                                                                 | `.tmp/<image>/00-gray-source.tif`                                       |   ✅    |
|   1   | Detect vertical edges (east/west) and produce binary edge images.               | Duplicate gray → `Convolve` with Sobel (east) and inverted Sobel (west); `Convert to Mask`; `imageCalculator("OR")` → `01/02/03` files.                                             | `01-east-edges.tif`, `02-west-edges.tif`, `03-vertical-edges.tif`       |   ✅    |
|   2   | Strengthen edges: connect stroke fragments horizontally and thin vertically.    | Duplicate `vertical-edges`; horizontal dilation via `Convolve [0 0 0;1 1 1;0 0 0]`; save; invert → vertical erosion via `Convolve [0 1 0;0 1 0;0 1 0]` + threshold → mask → invert. | `04-vertical-edges-dilated.tif`, `05-vertical-edges-dilated-eroded.tif` |   ✅    |
|   3   | Denoise while keeping binary structure.                                         | `Median... radius=2` on strengthened edges, convert/maintain binary, save.                                                                                                          | `06-vertical-edges-denoised.tif`                                        |   ✅    |
|   4   | Highlight horizontally-aligned text regions using frequency filtering.          | Compute bandpass sizes from image height (`BP5_large/small`); `Bandpass Filter... suppress=Vertical` → mask-like regions corresponding to text lines.                               | `07-vertical-edges-bandpassed.tif`                                      |   ✅    |
|   5   | Use bandpass result as mask to restrict true edges to text regions.             | Convert bandpass to mask; `imageCalculator("AND")` with denoised edges → masked edges.                                                                                              | `08-vertical-edges-bandpass-mask.tif`, `09-vertical-edges-masked.tif`   |   ✅    |
|   6   | Refine masked image with bandpass again (optional but applied).                 | Duplicate masked image; apply `Bandpass Filter...` with BP7 params; threshold/convert to mask.                                                                                      | `10-vertical-edges-masked-bandpassed.tif`                               |   ✅    |
|   7   | Summarize connected components as fitted ellipses to localize text blobs.       | `Set Measurements...` then `Analyze Particles... show=Ellipses size=20-Infinity`; `Convert to Mask`; `Fill Holes`; save ellipse overlay.                                            | `11-vertical-edges-ellipses.tif`                                        |   ✅    |
|   8   | Skeletonize strokes to prepare for Hough line detection.                        | Duplicate filtered/masked image and `Skeletonize` → produce thin skeletons.                                                                                                         | `12-vertical-edges-skeleton.tif`                                        |   ✅    |
|   9   | Compute Hough space to detect line peaks (horizontal lines at angle π/2).       | `run("Hough Transform")` (plugin); save Hough image; `Enhance Contrast` and `Apply LUT`.                                                                                            | `13-Hough.tif`, `14-hough-enhanced.tif`                                 |   ✅    |
|  10   | Threshold Hough near π/2 and select prominent peaks representing text lines.    | Auto-threshold/convert Hough to mask; `Horizontal Elbow Filter` plugin selects max-area peaks around angle π/2.                                                                     | `15-hough-thresholded.tif`                                              |   ✅    |
|  11   | Map selected Hough peaks back to original image and draw bounding boxes.        | `Hough To BoundingBox` plugin: inputs `hough_thresholded` and preprocessed image → draws boxes on original; save annotated original.                                                | `17-Original-with-Bounding-Boxes.tif` (and `Burned_...`)                |   ✅    |
|  12   | Produce a right-side projection visualization to verify detected lines.         | `Right Projection` plugin on final filtered image to compute horizontal projection highlighting line alignments.                                                                    | `16-Right-Projection.tif`                                               |   ✅    |

Notes:
- Intermediate TIFFs are saved in `.tmp/<image>/` with descriptive names for traceability.
- Bandpass filter sizes are computed from image height (`BP5_large`, `BP5_small`, `BP7_large`, `BP7_small`) so the pipeline adapts to scale.
- Convolution kernels and morphological steps are implemented inline in the macro; Hough and mapping require the `plugins/` Java filters (e.g., `Hough_Transform.java`, `Hough_To_BoundingBox.java`, `Horizontal_Elbow_Filter.java`, `Right_Projection.java`).


# Part 2: Character Detection
## Macro to run:

- [`macros/CharacterDetection.ijm`](macros/CharacterDetection.ijm) — Macro implementing Part 2 (Character Detection). Runs the steps for cropping text lines (manual or using Part 1 results), contrast enhancement, FFT-based analysis, detection of stroke periods, vertical projection based stroke localization, and drawing the equal-width cell boxes used for character segmentation (steps 1–8).
- [`macros/GetArmenianLetterHu.ijm`](macros/GetArmenianLetterHu.ijm) — Utility macro/script that computes Hu's invariant moments for skeletonized letter crops (used in steps 9–11). Use this to generate the 7-component Hu vectors for the alphabet samples and for cells extracted from text line images.


## STEPS:
### Project 2 Implementation Steps

| #      | Description                                                                                                                                                                                                                                                                                                    | Submission                                                                                                          | Status                                                                 |
| ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| **0**  | Create in the shared Google drive a subfolder `\Lines2`. If needed, revisit Project 1 collect the revisited deliverables in this subfolder. The original subfolder `\Lines2` must not be changed.                                                                                                              |                                                                                                                     | ✅                                                                      |
| **1**  | Use the same images as in Project 1. Crop the text lines manually or, ideally, based on the results of Project 1. Apply the subsequent steps to different cropped text line images.                                                                                                                            | The cropped image files of individual text lines together with the coordinates and dimensions of the cropping boxes | ✅                                                                      |
| **2**  | Enhance the contrast of the carved symbols in the text line by trying different combinations of Image → Adjust → Brightness/Contrast... → Auto, Process → Sharpen, Process Filters → Gaussian Blur... (radius 1.00), Cut_After_Max command (the last Quiz). Find Cut_After_Max.java plugin uploaded in Moodle. | Histogram of the original cropped images and histograms after each applied command                                  | ✅                                                                      |
| **3**  | Apply Fourier Transform to the cropped image of enhanced contrast from step 2.                                                                                                                                                                                                                                 | The image of the FFT result                                                                                         | ✅                                                                      |
| **4**  | Enhance the contrast of the FFT result from step 3 using the same Image → Adjust → Brightness/Contrast... → Auto command.                                                                                                                                                                                      | The image of the enhanced FFT result                                                                                | ✅                                                                      |
| **5**  | Extract the brightest pixels from the enhanced FFT image from step 4 by running Image → Adjust → Threshold... command.                                                                                                                                                                                         | The filtered binarized FFT image                                                                                    | ✅                                                                      |
| **6**  | In the binarized FFT image from step 5 detect average periods between the strokes and / or symbols manually or, ideally, using Analyze Particles... command. Preprocess the FFT image as needed.                                                                                                               | The detected period(s)                                                                                              | ✅                                                                      |
| **7**  | Identify the most prominent stroke in the enhanced cropped image from step 2. For example, compute the vertical projection and locate the maximum point.                                                                                                                                                       | The x coordinate of the most prominent stroke                                                                       | ✅                                                                      |
| **8**  | Binarize the image of the text line and draw boxes of equal width detected in step 6 both sides from the stroke detected in step 7.                                                                                                                                                                            | The image of the binarized text line with the drawn boxes                                                           | ✅The boxes are not that good (maybe only the prominent is not enough)  |
| **9**  | Select a binary image of the Armenian alphabet, skeletonize it and compute Hu's invariant moments for each letter (chapter 10, section 10.6.4).                                                                                                                                                                | The skeletonized image and the 7-component vector of Hu's invariant moments for each letter                         | ✅Have the letter crops, skeletonization and feature calculation script |
| **10** | Compute Hu's invariant moments of binary regions in individual and / or two or more adjacent cells from step 8.                                                                                                                                                                                                | 7-component vector of Hu's invariant moments for each computed cell                                                 | ⌛                                                                      |
| **11** | Use the results of step 9 and step 10 to estimate / classify a symbol in each cell.                                                                                                                                                                                                                            | Classification of the symbols                                                                                       | ⌛                                                                      |
| **12** | Implement (fully or partially) the steps 1-8 as a macro and construct a table to indicate which step succeeded and which one failed for each processed text line.                                                                                                                                              | The constructed table electronically and in hard copy                                                               | ✅                                                                      |


# Required Plugins

- The following Java plugins (located in the `plugins/` folder of this repository) are required by the macros and should be installed into your ImageJ plugins directory before running the pipelines:
	- [`Cut_After_Max.java`](plugins/Cut_After_Max.java)
	- [`Elbow_Filter_Plugin.java`](plugins/Elbow_Filter_Plugin.java)
	- [`FFT_Character_Segmenter.java`](plugins/FFT_Character_Segmenter.java)
	- [`FFT_R_to_Vertical_Lines.java`](plugins/FFT_R_to_Vertical_Lines.java)
	- [`Get_Horizontal_Lines_From_Hough.java`](plugins/Get_Horizontal_Lines_From_Hough.java)
	- [`Horizontal_Elbow_Filter.java`](plugins/Horizontal_Elbow_Filter.java)
	- [`Hough_Horizontal_Lines.java`](plugins/Hough_Horizontal_Lines.java)
	- [`Hough_To_BoundingBox.java`](plugins/Hough_To_BoundingBox.java)
	- [`Hough_Transform.java`](plugins/Hough_Transform.java)
	- [`Particle_Analyzer_TopK.java`](plugins/Particle_Analyzer_TopK.java)
	- [`Right_Projection.java`](plugins/Right_Projection.java)
	- [`Vertical_Projection.java`](plugins/Vertical_Projection.java)

- Installation: copy the `.java` files from `plugins/` into your ImageJ plugins folder and restart ImageJ so they can be compiled/loaded. Alternatively, run the provided [`macros/SetupPlugins.ijm`](macros/SetupPlugins.ijm) macro after adjusting its target paths (see below) to automate the installation.

**About [`macros/SetupPlugins.ijm`](macros/SetupPlugins.ijm) **

The [`macros/SetupPlugins.ijm`](macros/SetupPlugins.ijm) macro included in the [`macros/`](macros/) folder automates copying/installing the plugin source files into a configured ImageJ plugins folder. It uses ImageJ's `Install...` command to copy each `.java` file.
Before running [`macros/SetupPlugins.ijm`](macros/SetupPlugins.ijm), open the macro and update the `save=[...]` paths to point to your local ImageJ plugins folder (for example `C:/Users/<you>/ImageJ/plugins/` or similar). After installation, restart ImageJ so the newly installed plugins are compiled and available.
