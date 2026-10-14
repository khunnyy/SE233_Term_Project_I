package se233.project.vector;

final class SvgRectWriter {
    private SvgRectWriter() {}

    static String write(PixelGrid grid, Integer background) {
        int w = grid.width();
        int h = grid.height();
        int[][] pixels = grid.pixels();

        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(grid.originalWidth()).append("\" height=\"").append(grid.originalHeight())
                .append("\" viewBox=\"0 0 ").append(w).append(' ').append(h).append("\">\n");

        boolean[][] used = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = pixels[y][x];
                if (background != null && rgb == background) continue;
                if (used[y][x]) continue;

                // ขยายไปทางขวาตราบที่สีเดียวกัน
                int run = x;
                while (run + 1 < w && pixels[y][run + 1] == rgb) run++;

                // ขยายลงล่างตราบที่ทั้งแถวสีเดียวกันและยังไม่ถูกใช้
                int height = 1;
                outer:
                while (y + height < h) {
                    for (int xx = x; xx <= run; xx++) {
                        if (pixels[y + height][xx] != rgb || used[y + height][xx]) break outer;
                    }
                    height++;
                }

                for (int yy = y; yy < y + height; yy++)
                    for (int xx = x; xx <= run; xx++) used[yy][xx] = true;

                svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                        .append("\" width=\"").append(run - x + 1).append("\" height=\"")
                        .append(height).append("\" fill=\"").append(hex(rgb)).append("\"/>\n");
            }
        }
        svg.append("</svg>\n");
        return svg.toString();
    }

    private static String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }
}
