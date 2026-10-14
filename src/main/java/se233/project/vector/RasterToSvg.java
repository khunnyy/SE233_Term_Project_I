package se233.project.vector;

import javafx.scene.image.Image;
import se233.project.model.SvgSettings;

public final class RasterToSvg {
    private RasterToSvg() {}

    public static String convert(Image image, SvgSettings settings) {
        PixelGrid grid = PixelGrid.from(image, settings.detail());              // 1. ย่อภาพ อ่านพิกเซล
        if (!settings.unlimited()) {
            MedianCutQuantizer.reduce(grid, settings.maxColors());              // 2. ลดสี
        }
        Integer background = settings.removeBackground()
                ? BackgroundDetector.detect(grid) : null;                       // 3. หาสีพื้นหลัง (ถ้าเปิด)
        return SvgRectWriter.write(grid, background);                           // 4. เขียน SVG
    }
}
