package se233.project.vector;

final class BackgroundDetector {
    private BackgroundDetector() {}

    static int detect(PixelGrid grid) {
        return grid.pixels()[0][0];
    }
}
