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
        int[] bucketCounts = new int[4096];
        long[] bucketRed = new long[4096];
        long[] bucketGreen = new long[4096];
        long[] bucketBlue = new long[4096];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = reader.getColor(Math.min(originalW - 1, (int) (x / scale)),
                                          Math.min(originalH - 1, (int) (y / scale)));
                int rgb = toRgb(c);
                pixels[y][x] = rgb;
                counts.merge(rgb, 1, Integer::sum);
                int bucket = (((rgb >> 20) & 15) << 8) | (((rgb >> 12) & 15) << 4) | ((rgb >> 4) & 15);
                bucketCounts[bucket]++;
                bucketRed[bucket] += (rgb >> 16) & 255;
                bucketGreen[bucket] += (rgb >> 8) & 255;
                bucketBlue[bucket] += rgb & 255;
            }
        }

        Set<Integer> palette;
        if (unlimited) {
            palette = new LinkedHashSet<>(counts.keySet());
        } else {
            int limit = Math.max(1, Math.min(5, maxColors));
            List<ColorBucket> buckets = new ArrayList<>();
            for (int i = 0; i < bucketCounts.length; i++) {
                if (bucketCounts[i] == 0) continue;
                int count = bucketCounts[i];
                buckets.add(new ColorBucket(
                        (int) (bucketRed[i] / count),
                        (int) (bucketGreen[i] / count),
                        (int) (bucketBlue[i] / count),
                        count));
            }
            palette = medianCutPalette(buckets, limit);
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

    private static Set<Integer> medianCutPalette(List<ColorBucket> buckets, int limit) {
        List<List<ColorBucket>> boxes = new ArrayList<>();
        boxes.add(buckets);
        while (boxes.size() < limit) {
            int splitIndex = -1;
            int splitRange = -1;
            int splitChannel = 0;
            for (int i = 0; i < boxes.size(); i++) {
                List<ColorBucket> box = boxes.get(i);
                if (box.size() < 2) continue;
                int[] ranges = colorRanges(box);
                int channel = ranges[1] > ranges[0] ? 1 : 0;
                if (ranges[2] > ranges[channel]) channel = 2;
                if (ranges[channel] > splitRange) {
                    splitIndex = i;
                    splitRange = ranges[channel];
                    splitChannel = channel;
                }
            }
            if (splitIndex < 0) break;

            List<ColorBucket> box = boxes.remove(splitIndex);
            int channelToSplit = splitChannel;
            box.sort(Comparator.comparingInt(bucket -> bucket.channel(channelToSplit)));
            int total = box.stream().mapToInt(ColorBucket::count).sum();
            int half = (total + 1) / 2;
            int accumulated = 0;
            int middle = 0;
            while (middle < box.size() - 1 && accumulated < half) {
                accumulated += box.get(middle).count();
                middle++;
            }
            boxes.add(box.subList(0, middle));
            boxes.add(box.subList(middle, box.size()));
        }

        Set<Integer> palette = new LinkedHashSet<>();
        for (List<ColorBucket> box : boxes) {
            long red = 0, green = 0, blue = 0;
            int count = 0;
            for (ColorBucket bucket : box) {
                red += (long) bucket.red() * bucket.count();
                green += (long) bucket.green() * bucket.count();
                blue += (long) bucket.blue() * bucket.count();
                count += bucket.count();
            }
            palette.add(((int) (red / count) << 16) | ((int) (green / count) << 8) | (int) (blue / count));
        }
        return palette;
    }

    private static int[] colorRanges(List<ColorBucket> box) {
        int[] minimum = {255, 255, 255};
        int[] maximum = {0, 0, 0};
        for (ColorBucket bucket : box) {
            for (int channel = 0; channel < 3; channel++) {
                int value = bucket.channel(channel);
                minimum[channel] = Math.min(minimum[channel], value);
                maximum[channel] = Math.max(maximum[channel], value);
            }
        }
        return new int[] {
                maximum[0] - minimum[0],
                maximum[1] - minimum[1],
                maximum[2] - minimum[2]
        };
    }

    private record ColorBucket(int red, int green, int blue, int count) {
        private int channel(int channel) {
            return switch (channel) {
                case 0 -> red;
                case 1 -> green;
                default -> blue;
            };
        }
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
