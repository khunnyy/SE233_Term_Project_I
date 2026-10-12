# SE233 Term Project I — Raster to SVG

JavaFX desktop application for importing JPG/PNG images (including ZIP archives), previewing a simple raster-to-vector conversion, adjusting settings, and exporting SVG files.

## Requirements
- JDK 21
- Maven 3.9+ (or IntelliJ IDEA's bundled Maven)
- Internet access on the first Maven run to download dependencies

## Run
1. Open this folder as a Maven project in IntelliJ IDEA.
2. Set Project SDK to JDK 21.
3. Wait for Maven to load `pom.xml`.
4. Run `se233.project.Launcher`, or run `mvn javafx:run` in the terminal.

## Important implementation note
This project contains a built-in Java raster-to-SVG tracer so it does not depend on a separate command-line program. It creates SVG paths by grouping pixels into color regions. This is a basic educational tracer, not a pixel-perfect replacement for Vector Magic. Compare the result with the rubric and test it with your own images.

## Features
- Drag-and-drop JPG/PNG and ZIP files
- Original and vector-result previews
- Zoom In, Zoom Out, Zoom to Fit
- Low/Medium/High detail settings
- Unlimited colors or Custom colors (default 2, maximum 5)
- Remove background (transparent output)
- Back/Next navigation
- Export All with folder selection
- Background worker thread and progress indicator for batch export
