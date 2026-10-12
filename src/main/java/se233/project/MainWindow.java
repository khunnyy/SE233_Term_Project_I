package se233.project;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainWindow {
    private final Stage stage;
    private final List<ImageItem> items = new ArrayList<>();
    private int currentIndex = 0;
    private double zoom = 1.0;

    private final ImageView originalView = new ImageView();
    private final WebView resultView = new WebView();
    private final Label fileLabel = new Label("No image loaded");
    private final Label statusLabel = new Label("Drop JPG, PNG, or ZIP files here");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final ComboBox<String> detailBox = new ComboBox<>();
    private final ComboBox<String> colorModeBox = new ComboBox<>();
    private final ComboBox<Integer> colorCountBox = new ComboBox<>();
    private final CheckBox removeBackground = new CheckBox("Remove background");
    private final Button backButton = new Button("Back");
    private final Button nextButton = new Button("Next");
    private final Button exportButton = new Button("Export All");

    private BorderPane root;
    private VBox dropPane;
    private HBox editorPane;

    public MainWindow(Stage stage) {
        this.stage = stage;
        buildUI();
        setDropHandlers(root);
        showDropScreen();
    }

    public void show() {
        stage.setTitle("Vector Studio — Raster to SVG");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(new Scene(root, 1240, 800));
        stage.show();
    }

    private void buildUI() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f6fb; -fx-font-family: 'Segoe UI', 'Arial';");

        Label brandMark = new Label("V");
        brandMark.setAlignment(Pos.CENTER);
        brandMark.setMinSize(42, 42);
        brandMark.setStyle("-fx-background-color: linear-gradient(to bottom right, #6677f5, #7657dc);"
                + "-fx-background-radius: 13; -fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label title = new Label("Vector Studio");
        title.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #172033;");
        Label subtitle = new Label("RASTER TO VECTOR");
        subtitle.setStyle("-fx-text-fill: #8992a5; -fx-font-size: 10px; -fx-font-weight: bold;"
                + "-fx-letter-spacing: 1.2px;");
        VBox heading = new VBox(3, title, subtitle);
        HBox brand = new HBox(12, brandMark, heading);
        brand.setAlignment(Pos.CENTER_LEFT);
        Label formatBadge = new Label("SVG  ·  BATCH WORKSPACE");
        formatBadge.setStyle("-fx-background-color: #f0efff; -fx-text-fill: #635bcb;"
                + "-fx-background-radius: 20; -fx-padding: 9 14; -fx-font-size: 10px; -fx-font-weight: bold;");
        HBox header = new HBox(brand, formatBadge);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 28, 15, 28));
        header.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #e9ecf3 transparent;"
                + "-fx-border-width: 0 0 1 0;");
        HBox.setHgrow(brand, Priority.ALWAYS);
        root.setTop(header);

        dropPane = new VBox(16);
        dropPane.setAlignment(Pos.CENTER);
        dropPane.setPadding(new Insets(36));
        dropPane.setMaxWidth(820);
        dropPane.setMaxHeight(500);
        dropPane.setStyle("-fx-background-color: white; -fx-border-color: #d7d8fb; -fx-border-width: 1.5;"
                + "-fx-border-style: dashed; -fx-background-radius: 24; -fx-border-radius: 24;");
        StackPane uploadIcon = new StackPane();
        uploadIcon.setMinSize(76, 76);
        uploadIcon.setMaxSize(76, 76);
        uploadIcon.setStyle("-fx-background-color: #f0efff; -fx-background-radius: 24;");
        Label dropIcon = new Label("↑");
        dropIcon.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: #695de0;");
        uploadIcon.getChildren().add(dropIcon);
        Label dropEyebrow = new Label("YOUR NEXT CREATION STARTS HERE");
        dropEyebrow.setStyle("-fx-text-fill: #7066d7; -fx-font-size: 10px; -fx-font-weight: bold;"
                + "-fx-letter-spacing: 1.5px;");
        Label dropTitle = new Label("Bring your images to life");
        dropTitle.setStyle("-fx-font-size: 27px; -fx-font-weight: bold; -fx-text-fill: #1c2540;");
        Label dropText = new Label("Drop files anywhere in this window, or browse your computer.");
        dropText.setStyle("-fx-font-size: 13px; -fx-text-fill: #788196;");
        Button browse = new Button("Choose images");
        browse.setCursor(javafx.scene.Cursor.HAND);
        browse.setStyle("-fx-background-color: linear-gradient(to right, #626fe8, #795bdc);"
                + "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"
                + "-fx-background-radius: 11; -fx-padding: 12 24;");
        browse.setOnAction(e -> chooseFiles());
        HBox formatChips = new HBox(8, formatChip("JPG"), formatChip("PNG"), formatChip("ZIP ARCHIVE"));
        formatChips.setAlignment(Pos.CENTER);
        Label dropHint = new Label("Up to multiple images at once");
        dropHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #9aa2b2;");
        dropPane.getChildren().addAll(uploadIcon, dropEyebrow, dropTitle, dropText, browse, formatChips, dropHint);
        BorderPane.setMargin(dropPane, new Insets(36, 40, 40, 40));
        root.setCenter(dropPane);

        originalView.setPreserveRatio(true);
        originalView.setSmooth(false);
        resultView.setMinSize(0, 0);
        resultView.setPrefSize(390, 390);

        VBox originalBox = previewBox("ORIGINAL IMAGE", originalView);
        VBox resultBox = previewBox("VECTORIZED RESULT", resultView);
        HBox previews = new HBox(14, originalBox, resultBox);
        previews.setAlignment(Pos.CENTER);
        HBox.setHgrow(originalBox, Priority.ALWAYS);
        HBox.setHgrow(resultBox, Priority.ALWAYS);
        originalBox.setMaxWidth(Double.MAX_VALUE);
        resultBox.setMaxWidth(Double.MAX_VALUE);

        Button zoomIn = new Button("Zoom In");
        Button zoomOut = new Button("Zoom Out");
        Button zoomFit = new Button("Zoom to Fit");
        for (Button button : List.of(zoomIn, zoomOut, zoomFit)) {
            button.setCursor(javafx.scene.Cursor.HAND);
            button.setStyle("-fx-background-color: white; -fx-text-fill: #525d75; -fx-font-size: 11px;"
                    + "-fx-background-radius: 8; -fx-border-color: #e7eaf1; -fx-border-radius: 8;"
                    + "-fx-padding: 8 12;");
        }
        zoomIn.setOnAction(e -> { zoom = Math.min(zoom * 1.2, 4); updatePreviewSizes(); });
        zoomOut.setOnAction(e -> { zoom = Math.max(zoom / 1.2, 0.25); updatePreviewSizes(); });
        zoomFit.setOnAction(e -> { zoom = 1; updatePreviewSizes(); });
        HBox topControls = new HBox(8, zoomIn, zoomOut, zoomFit);
        Label workspaceTitle = new Label("Live preview");
        workspaceTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1c2540;");
        HBox previewHeading = new HBox(workspaceTitle, topControls);
        previewHeading.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(workspaceTitle, Priority.ALWAYS);

        detailBox.getItems().addAll("Low", "Medium", "High");
        detailBox.setValue("Medium");
        colorModeBox.getItems().addAll("Unlimited", "Custom");
        colorModeBox.setValue("Custom");
        colorCountBox.getItems().addAll(2, 3, 4, 5);
        colorCountBox.setValue(2);
        colorCountBox.setDisable(false);
        colorModeBox.valueProperty().addListener((obs, old, val) -> {
            boolean custom = "Custom".equals(val);
            colorCountBox.setDisable(!custom);
            updateResultPreview();
        });
        detailBox.valueProperty().addListener((obs, old, val) -> updateResultPreview());
        colorCountBox.valueProperty().addListener((obs, old, val) -> updateResultPreview());
        removeBackground.selectedProperty().addListener((obs, old, val) -> updateResultPreview());

        VBox settings = new VBox(12);
        settings.setPadding(new Insets(18));
        settings.setPrefWidth(258);
        settings.setMinWidth(230);
        settings.setStyle("-fx-background-color: white; -fx-background-radius: 18;"
                + "-fx-border-color: #eaedf4; -fx-border-radius: 18;");
        Label settingsTitle = new Label("Fine-tune your SVG");
        settingsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1c2540;");
        Label settingsSubtitle = new Label("Make it yours with a few adjustments.");
        settingsSubtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8992a5;");
        Label detailLabel = new Label("Detail level");
        Label colorLabel = new Label("Colors");
        Label countLabel = new Label("Custom color limit");
        for (Label label : List.of(detailLabel, colorLabel, countLabel)) {
            label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #525d75;");
        }
        for (ComboBox<?> comboBox : List.of(detailBox, colorModeBox, colorCountBox)) {
            comboBox.setMaxWidth(Double.MAX_VALUE);
            comboBox.setStyle("-fx-background-color: #f8f9fc; -fx-background-radius: 8;"
                    + "-fx-border-color: #e8ebf2; -fx-border-radius: 8; -fx-font-size: 12px;");
        }
        backButton.setMaxWidth(Double.MAX_VALUE);
        nextButton.setMaxWidth(Double.MAX_VALUE);
        exportButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setCursor(javafx.scene.Cursor.HAND);
        nextButton.setCursor(javafx.scene.Cursor.HAND);
        exportButton.setCursor(javafx.scene.Cursor.HAND);
        backButton.setStyle("-fx-background-color: #f5f6fa; -fx-text-fill: #525d75;"
                + "-fx-background-radius: 8; -fx-padding: 9 8; -fx-font-size: 11px;");
        nextButton.setStyle("-fx-background-color: #f5f6fa; -fx-text-fill: #525d75;"
                + "-fx-background-radius: 8; -fx-padding: 9 8; -fx-font-size: 11px;");
        exportButton.setStyle("-fx-background-color: linear-gradient(to right, #626fe8, #795bdc);"
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;"
                + "-fx-background-radius: 9; -fx-padding: 11 12;");
        backButton.setOnAction(e -> navigate(-1));
        nextButton.setOnAction(e -> navigate(1));
        exportButton.setOnAction(e -> exportAll());
        HBox nav = new HBox(8, backButton, nextButton);
        HBox.setHgrow(backButton, Priority.ALWAYS);
        HBox.setHgrow(nextButton, Priority.ALWAYS);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setStyle("-fx-accent: #6b65e8;");
        removeBackground.setStyle("-fx-text-fill: #525d75; -fx-font-size: 11px;");
        statusLabel.setStyle("-fx-background-color: #f5f6fa; -fx-background-radius: 8;"
                + "-fx-padding: 9; -fx-text-fill: #68738a; -fx-font-size: 10px;");
        settings.getChildren().addAll(settingsTitle, settingsSubtitle, new Separator(),
                detailLabel, detailBox, colorLabel, colorModeBox, countLabel, colorCountBox,
                removeBackground, new Separator(), nav, exportButton, progressBar, statusLabel);
        statusLabel.setWrapText(true);

        fileLabel.setStyle("-fx-background-color: #eeefff; -fx-background-radius: 20;"
                + "-fx-padding: 7 12; -fx-text-fill: #5e59c6; -fx-font-size: 11px;");
        VBox center = new VBox(14, previewHeading, previews, fileLabel);
        center.setPadding(new Insets(4, 0, 0, 0));
        VBox.setVgrow(previews, Priority.ALWAYS);
        editorPane = new HBox(16, center, settings);
        editorPane.setPadding(new Insets(24, 28, 28, 28));
        HBox.setHgrow(center, Priority.ALWAYS);
        root.setCenter(dropPane);
    }

    private Label formatChip(String text) {
        Label chip = new Label(text);
        chip.setStyle("-fx-background-color: #f5f6fa; -fx-background-radius: 20;"
                + "-fx-padding: 7 12; -fx-text-fill: #667087; -fx-font-size: 10px; -fx-font-weight: bold;");
        return chip;
    }

    private VBox previewBox(String title, Node view) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #6e7890;"
                + "-fx-letter-spacing: 1px;");
        ScrollPane preview = new ScrollPane(view);
        preview.setFitToWidth(false);
        preview.setFitToHeight(false);
        preview.setPannable(true);
        preview.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        StackPane frame = new StackPane(preview);
        frame.setMinHeight(300);
        frame.setStyle("-fx-background-color: white; -fx-border-color: #e6e9f1;"
                + "-fx-border-radius: 14; -fx-background-radius: 14;");
        VBox box = new VBox(10, label, frame);
        VBox.setVgrow(frame, Priority.ALWAYS);
        return box;
    }

    private void showDropScreen() {
        root.setCenter(dropPane);
    }

    private void showEditorScreen() {
        root.setCenter(editorPane);
        editorPane.setVisible(true);
        updateNavigation();
        updatePreviewSizes();
    }

    private void setDropHandlers(Region target) {
        target.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });
        target.setOnDragDropped(event -> {
            boolean success = false;
            try {
                if (event.getDragboard().hasFiles()) {
                    loadFiles(event.getDragboard().getFiles());
                    success = true;
                }
            } catch (Exception ex) {
                showError("Could not load dropped files", ex);
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void chooseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select images or ZIP archives");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images and ZIP", "*.jpg", "*.jpeg", "*.png", "*.zip"));
        List<File> selected = chooser.showOpenMultipleDialog(stage);
        if (selected != null) loadFiles(selected);
    }

    private void loadFiles(List<File> files) {
        List<ImageItem> loaded = new ArrayList<>();
        try {
            for (File file : files) {
                String name = file.getName().toLowerCase(Locale.ROOT);
                if (name.endsWith(".zip")) {
                    loaded.addAll(readZip(file));
                } else if (isImage(file.getName())) {
                    Image image = new Image(file.toURI().toString(), false);
                    if (image.isError() || image.getWidth() < 1) throw new IOException("Unsupported or corrupted image: " + file.getName());
                    loaded.add(new ImageItem(file.getName(), file, image));
                }
            }
            if (loaded.isEmpty()) {
                showAlert("No supported JPG/PNG images found.");
                return;
            }
            items.clear();
            items.addAll(loaded);
            currentIndex = 0;
            zoom = 1;
            showEditorScreen();
            showCurrentImage();
            statusLabel.setText("Loaded " + items.size() + " image(s).");
        } catch (Exception ex) {
            showError("Unable to import files", ex);
        }
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
                    if (image.isError() || image.getWidth() < 1) throw new IOException("Corrupted image inside ZIP: " + entry.getName());
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

    private void showCurrentImage() {
        if (items.isEmpty()) return;
        ImageItem item = items.get(currentIndex);
        originalView.setImage(item.getImage());
        fileLabel.setText((currentIndex + 1) + " / " + items.size() + " — " + item.getName());
        updateNavigation();
        updatePreviewSizes();
        updateResultPreview();
    }

    private void updatePreviewSizes() {
        double width = 390 * zoom;
        double height = 390 * zoom;
        originalView.setFitWidth(width);
        originalView.setFitHeight(height);
        resultView.setMinSize(width, height);
        resultView.setPrefSize(width, height);
    }

    private void updateResultPreview() {
        if (items.isEmpty()) { resultView.getEngine().loadContent(""); return; }
        try {
            String svg = makeSvg();
            String previewSvg = svg.replaceFirst("width=\"\\d+\" height=\"\\d+\"",
                    "width=\"100%\" height=\"100%\"");
            resultView.getEngine().loadContent(previewSvg, "image/svg+xml");
            statusLabel.setText("Preview updated • " + detailBox.getValue() + " detail • " +
                    ("Unlimited".equals(colorModeBox.getValue()) ? "Unlimited colors" : colorCountBox.getValue() + " colors") +
                    (removeBackground.isSelected() ? " • background removal on" : ""));
        } catch (Exception ex) {
            statusLabel.setText("Preview unavailable: " + ex.getMessage());
        }
    }

    private String makeSvg() {
        ImageItem item = items.get(currentIndex);
        int detail = switch (detailBox.getValue()) { case "Low" -> 0; case "High" -> 2; default -> 1; };
        boolean unlimited = "Unlimited".equals(colorModeBox.getValue());
        int colors = colorCountBox.getValue() == null ? 2 : colorCountBox.getValue();
        return RasterToSvg.convert(item.getImage(), detail, colors, unlimited, removeBackground.isSelected());
    }

    private void navigate(int direction) {
        if (items.isEmpty()) return;
        currentIndex = Math.floorMod(currentIndex + direction, items.size());
        showCurrentImage();
    }

    private void updateNavigation() {
        backButton.setDisable(items.size() < 2);
        nextButton.setDisable(items.size() < 2);
        exportButton.setDisable(items.isEmpty());
    }

    private void exportAll() {
        if (items.isEmpty()) return;
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose output folder for SVG files");
        File folder = chooser.showDialog(stage);
        if (folder == null) return;

        boolean parallel = items.size() > 1 && "Medium".equals(detailBox.getValue())
                && "Custom".equals(colorModeBox.getValue());
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        exportButton.setDisable(true);
        statusLabel.setText(parallel ? "Exporting files in parallel..." : "Exporting files...");
        List<ImageItem> work = new ArrayList<>(items);
        int detail = switch (detailBox.getValue()) { case "Low" -> 0; case "High" -> 2; default -> 1; };
        int colors = colorCountBox.getValue() == null ? 2 : colorCountBox.getValue();
        boolean unlimited = "Unlimited".equals(colorModeBox.getValue());
        boolean transparent = removeBackground.isSelected();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                ExecutorService pool = parallel ? Executors.newFixedThreadPool(Math.min(work.size(), Runtime.getRuntime().availableProcessors()))
                                                : Executors.newSingleThreadExecutor();
                try {
                    List<Future<?>> futures = new ArrayList<>();
                    for (int i = 0; i < work.size(); i++) {
                        final int index = i;
                        futures.add(pool.submit(() -> {
                            ImageItem item = work.get(index);
                            String svg = RasterToSvg.convert(item.getImage(), detail, colors, unlimited, transparent);
                            String base = item.getName().replaceFirst("(?i)\\.(jpg|jpeg|png)$", "");
                            File out = new File(folder, base + ".svg");
                            Files.writeString(out.toPath(), svg, StandardCharsets.UTF_8);
                            return null;
                        }));
                    }
                    for (int i = 0; i < futures.size(); i++) {
                        futures.get(i).get();
                        updateProgress(i + 1, futures.size());
                        updateMessage("Exported " + (i + 1) + " / " + futures.size());
                    }
                } finally {
                    pool.shutdown();
                }
                return null;
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            progressBar.setVisible(false);
            exportButton.setDisable(false);
            statusLabel.setText("Export complete. Saved " + work.size() + " SVG file(s) to " + folder.getAbsolutePath());
            showAlert("Export complete! Saved " + work.size() + " SVG file(s).");
        });
        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            progressBar.setVisible(false);
            exportButton.setDisable(false);
            showError("Export failed", task.getException());
        });
        Thread thread = new Thread(task, "svg-export-worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.initOwner(stage);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String title, Throwable ex) {
        String message = ex == null ? "Unknown error" : ex.getMessage();
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Unknown error" : message, ButtonType.OK);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
