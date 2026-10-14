package se233.project.vector;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

record PixelGrid(int[][] pixels, int width, int height, int originalWidth, int originalHeight) {

    static PixelGrid from(Image image, int detail) {
        int originalW = (int) image.getWidth();
        int originalH = (int) image.getHeight();
        if (originalW < 1 || originalH < 1) throw new IllegalArgumentException("Image has no pixels.");

        int maxSide = switch (detail) { case 0 -> 90; case 2 -> 260; default -> 160; };
        double scale = Math.min(1.0, maxSide / (double) Math.max(originalW, originalH));
        int w = Math.max(1, (int) Math.round(originalW * scale));
        int h = Math.max(1, (int) Math.round(originalH * scale));

        PixelReader reader = image.getPixelReader();
        int[][] pixels = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = reader.getColor(Math.min(originalW - 1, (int) (x / scale)),
                        Math.min(originalH - 1, (int) (y / scale)));
                pixels[y][x] = toRgb(c);
            }
        }
        return new PixelGrid(pixels, w, h, originalW, originalH);
    }

    private static int toRgb(Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return (r << 16) | (g << 8) | b;
    }
}
