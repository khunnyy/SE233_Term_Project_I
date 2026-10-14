package se233.project.service;

import javafx.scene.image.Image;
import se233.project.model.ImageItem;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ImageLoader {

    public List<ImageItem> load(List<File> files) throws IOException {
        List<ImageItem> loaded = new ArrayList<>();
        for (File file : files) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".zip")) {
                loaded.addAll(readZip(file));
            } else if (isImage(file.getName())) {
                Image image = new Image(file.toURI().toString(), false);
                if (image.isError() || image.getWidth() < 1)
                    throw new IOException("Unsupported or corrupted image: " + file.getName());
                loaded.add(new ImageItem(file.getName(), file, image));
            }
        }
        return loaded;
    }

    private List<ImageItem> readZip(File zipFile) throws IOException {
        List<ImageItem> result = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && isImage(entry.getName())) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    zis.transferTo(out);
                    byte[] bytes = out.toByteArray();
                    Image image = new Image(new ByteArrayInputStream(bytes));
                    if (image.isError() || image.getWidth() < 1)
                        throw new IOException("Corrupted image inside ZIP: " + entry.getName());
                    result.add(new ImageItem(new File(entry.getName()).getName(), null, image));
                }
                zis.closeEntry();
            }
        }
        return result;
    }

    private boolean isImage(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
    }
}
