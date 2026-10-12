package se233.project;

import javafx.scene.image.Image;

import java.io.File;
import java.util.Objects;

public class ImageItem {
    private final String name;
    private final File sourceFile;
    private final Image image;

    public ImageItem(String name, File sourceFile, Image image) {
        this.name = Objects.requireNonNull(name);
        this.sourceFile = sourceFile;
        this.image = Objects.requireNonNull(image);
    }

    public String getName() { return name; }
    public File getSourceFile() { return sourceFile; }
    public Image getImage() { return image; }
}
