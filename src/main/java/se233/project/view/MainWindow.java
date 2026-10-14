package se233.project.view;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import se233.project.model.SvgSettings;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class MainWindow {
    private final Stage stage;
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

    private Runnable onChooseFiles;
    private Consumer<List<File>> onFilesDropped;
    private IntConsumer onNavigate;
    private Runnable onExport;
    private Runnable onSettingsChanged;

    public MainWindow(Stage stage) {
        this.stage = stage;
        buildUI();
        setDropHandlers(root);
        showDropScreen();
    }

    public void setOnChooseFiles(Runnable handler) { this.onChooseFiles = handler; }
    public void setOnFilesDropped(Consumer<List<File>> handler) { this.onFilesDropped = handler; }
    public void setOnNavigate(IntConsumer handler) { this.onNavigate = handler; }
    public void setOnExport(Runnable handler) { this.onExport = handler; }
    public void setOnSettingsChanged(Runnable handler) { this.onSettingsChanged = handler; }

    private void fireSettingsChanged() {
        if (onSettingsChanged != null) onSettingsChanged.run();
    }

    public void show() {
        stage.setTitle("Vector Studio — Raster to SVG");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(new Scene(root, 1240, 800));
        stage.show();
    }

    public void showDropScreen() {
        root.setCenter(dropPane);
    }

    public void showEditorScreen() {
        root.setCenter(editorPane);
        editorPane.setVisible(true);
        updatePreviewSizes();
    }

    public SvgSettings getSettings() {
        int detail = switch (detailBox.getValue()) { case "Low" -> 0; case "High" -> 2; default -> 1; };
        boolean unlimited = "Unlimited".equals(colorModeBox.getValue());
        int colors = colorCountBox.getValue() == null ? 2 : colorCountBox.getValue();
        return new SvgSettings(detail, colors, unlimited, removeBackground.isSelected());
    }

    public String getSettingsSummary() {
        String colors = "Unlimited".equals(colorModeBox.getValue())
                ? "Unlimited colors" : colorCountBox.getValue() + " colors";
        return detailBox.getValue() + " detail • " + colors
                + (removeBackground.isSelected() ? " • background removal on" : "");
    }

    public void setOriginalImage(Image image) { originalView.setImage(image); }

    public void setFileLabel(String text) { fileLabel.setText(text); }

    public void setResultSvg(String svg) { resultView.getEngine().loadContent(svg, "image/svg+xml"); }

    public void clearResult() { resultView.getEngine().loadContent(""); }

    public void setStatus(String text) {
        if (statusLabel.textProperty().isBound()) return;
        statusLabel.setText(text);
    }

    public void resetZoom() {
        zoom = 1;
        updatePreviewSizes();
    }

    public void setNavigationState(int itemCount) {
        boolean multiple = itemCount >= 2;
        backButton.setDisable(!multiple);
        nextButton.setDisable(!multiple);
        exportButton.setDisable(itemCount == 0);
    }

    public void setExporting(boolean exporting) {
        progressBar.setVisible(exporting);
        exportButton.setDisable(exporting);
        if (exporting) progressBar.setProgress(0);
    }

    public void bindProgress(Task<?> task) {
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
    }

    public void unbindProgress() {
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
    }

    public List<File> askForFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select images or ZIP archives");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images and ZIP", "*.jpg", "*.jpeg", "*.png", "*.zip"));
        return chooser.showOpenMultipleDialog(stage);
    }

    public File askForOutputFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose output folder for SVG files");
        return chooser.showDialog(stage);
    }

    public void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.initOwner(stage);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public void showError(String title, Throwable ex) {
        String message = ex == null ? "Unknown error" : ex.getMessage();
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Unknown error" : message, ButtonType.OK);
        alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
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
        browse.setOnAction(e -> { if (onChooseFiles != null) onChooseFiles.run(); });
        HBox formatChips = new HBox(8, formatChip("JPG"), formatChip("PNG"), formatChip("ZIP ARCHIVE"));
        formatChips.setAlignment(Pos.CENTER);
        Label dropHint = new Label("Up to multiple images at once");
        dropHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #9aa2b2;");
        dropPane.getChildren().addAll(uploadIcon, dropEyebrow, dropTitle, dropText, browse, formatChips, dropHint);
        BorderPane.setMargin(dropPane, new Insets(36, 40, 40, 40));

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
            fireSettingsChanged();
        });
        detailBox.valueProperty().addListener((obs, old, val) -> fireSettingsChanged());
        colorCountBox.valueProperty().addListener((obs, old, val) -> fireSettingsChanged());
        removeBackground.selectedProperty().addListener((obs, old, val) -> fireSettingsChanged());

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
        backButton.setOnAction(e -> { if (onNavigate != null) onNavigate.accept(-1); });
        nextButton.setOnAction(e -> { if (onNavigate != null) onNavigate.accept(1); });
        exportButton.setOnAction(e -> { if (onExport != null) onExport.run(); });
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

    private void updatePreviewSizes() {
        double width = 390 * zoom;
        double height = 390 * zoom;
        originalView.setFitWidth(width);
        originalView.setFitHeight(height);
        resultView.setMinSize(width, height);
        resultView.setPrefSize(width, height);
    }

    private void setDropHandlers(Region target) {
        target.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });
        target.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasFiles() && onFilesDropped != null) {
                onFilesDropped.accept(event.getDragboard().getFiles());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }
}
