package se233.project;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.util.*;

public final class RasterToSvg {
    private RasterToSvg() {}

    public static String convert(Image image, int detail, int maxColors, boolean unlimited,
                                 boolean removeBackground) {
        int originalW = (int) image.getWidth();
        int originalH = (int) image.getHeight();
        if (originalW < 1 || originalH < 1) throw new IllegalArgumentException("Image has no pixels.");

        // Downsample according to detail level to keep the educational tracer responsive.
        int maxSide = switch (detail) { case 0 -> 90; case 2 -> 260; default -> 160; };
        double scale = Math.min(1.0, maxSide / (double) Math.max(originalW, originalH));
        int w = Math.max(1, (int) Math.round(originalW * scale));
        int h = Math.max(1, (int) Math.round(originalH * scale));
        PixelReader reader = image.getPixelReader();
        int[][] pixels = new int[h][w];
        Map<Integer, Integer> counts = new HashMap<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = reader.getColor(Math.min(originalW - 1, (int) (x / scale)),
                                          Math.min(originalH - 1, (int) (y / scale)));
                int rgb = toRgb(c);
                pixels[y][x] = rgb;
                counts.merge(rgb, 1, Integer::sum);
            }
        }

        Set<Integer> palette;
        if (unlimited) {
            palette = new LinkedHashSet<>(counts.keySet());
        } else {
            int limit = Math.max(1, Math.min(5, maxColors));
            List<Integer> ranked = new ArrayList<>(counts.keySet());
            ranked.sort((a, b) -> Integer.compare(counts.get(b), counts.get(a)));
            palette = new LinkedHashSet<>(ranked.subList(0, Math.min(limit, ranked.size())));
            // Map every pixel to the closest selected color.
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    pixels[y][x] = nearest(pixels[y][x], palette);
        }

        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
           .append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
           .append(originalW).append("\" height=\"").append(originalH)
           .append("\" viewBox=\"0 0 ").append(w).append(' ').append(h).append("\">\n");

        // Optional background removal: treat the top-left pixel's color as background.
        int background = pixels[0][0];
        int[][] labels = new int[h][w];
        for (int[] row : labels) Arrays.fill(row, -1);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = pixels[y][x];
                if (removeBackground && rgb == background) continue;
                if (labels[y][x] != -1) continue;
                int id = rgb;
                // A compact educational vectorization: contiguous same-color pixels become rectangles.
                int run = x;
                while (run + 1 < w && pixels[y][run + 1] == rgb &&
                       !(removeBackground && pixels[y][run + 1] == background)) run++;
                int height = 1;
                outer: while (y + height < h) {
                    for (int xx = x; xx <= run; xx++) {
                        if (pixels[y + height][xx] != rgb || labels[y + height][xx] != -1) break outer;
                    }
                    height++;
                }
                for (int yy = y; yy < y + height; yy++)
                    for (int xx = x; xx <= run; xx++) labels[yy][xx] = id;
                svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                   .append("\" width=\"").append(run - x + 1).append("\" height=\"")
                   .append(height).append("\" fill=\"").append(hex(rgb)).append("\"/>\n");
            }
        }
        svg.append("</svg>\n");
        return svg.toString();
    }

    private static int toRgb(Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return (r << 16) | (g << 8) | b;
    }

    private static int nearest(int rgb, Set<Integer> palette) {
        int best = 0, bestDistance = Integer.MAX_VALUE;
        for (int p : palette) {
            int dr = ((rgb >> 16) & 255) - ((p >> 16) & 255);
            int dg = ((rgb >> 8) & 255) - ((p >> 8) & 255);
            int db = (rgb & 255) - (p & 255);
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) { bestDistance = distance; best = p; }
        }
        return best;
    }

    private static String hex(int rgb) { return String.format("#%06X", rgb & 0xFFFFFF); }
}
