package se233.project.vector;

import java.util.*;

final class MedianCutQuantizer {
    private MedianCutQuantizer() {}

    static void reduce(PixelGrid grid, int maxColors) {
        int limit = Math.max(1, Math.min(5, maxColors));
        List<ColorBucket> buckets = buildBuckets(grid);
        Set<Integer> palette = medianCutPalette(buckets, limit);
        int[][] pixels = grid.pixels();
        for (int y = 0; y < grid.height(); y++)
            for (int x = 0; x < grid.width(); x++)
                pixels[y][x] = nearest(pixels[y][x], palette);
    }

    private static List<ColorBucket> buildBuckets(PixelGrid grid) {
        int[] counts = new int[4096];
        long[] red = new long[4096];
        long[] green = new long[4096];
        long[] blue = new long[4096];
        for (int[] row : grid.pixels()) {
            for (int rgb : row) {
                int bucket = (((rgb >> 20) & 15) << 8) | (((rgb >> 12) & 15) << 4) | ((rgb >> 4) & 15);
                counts[bucket]++;
                red[bucket] += (rgb >> 16) & 255;
                green[bucket] += (rgb >> 8) & 255;
                blue[bucket] += rgb & 255;
            }
        }
        List<ColorBucket> buckets = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] == 0) continue;
            int count = counts[i];
            buckets.add(new ColorBucket(
                    (int) (red[i] / count),
                    (int) (green[i] / count),
                    (int) (blue[i] / count),
                    count));
        }
        return buckets;
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

    private record ColorBucket(int red, int green, int blue, int count) {
        private int channel(int channel) {
            return switch (channel) {
                case 0 -> red;
                case 1 -> green;
                default -> blue;
            };
        }
    }
}
