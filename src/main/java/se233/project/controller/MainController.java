package se233.project.controller;

import javafx.concurrent.Task;
import se233.project.model.ImageItem;
import se233.project.model.SvgSettings;
import se233.project.service.ExportService;
import se233.project.service.ImageLoader;
import se233.project.vector.RasterToSvg;
import se233.project.view.MainWindow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainController {
    private final MainWindow view;
    private final ImageLoader loader = new ImageLoader();
    private final ExportService exporter = new ExportService();
    private final List<ImageItem> items = new ArrayList<>();
    private int currentIndex = 0;

    public MainController(MainWindow view) {
        this.view = view;
        view.setOnChooseFiles(this::chooseFiles);
        view.setOnFilesDropped(this::loadFiles);
        view.setOnNavigate(this::navigate);
        view.setOnExport(this::exportAll);
        view.setOnSettingsChanged(this::updateResultPreview);
    }

    private void chooseFiles() {
        List<File> selected = view.askForFiles();
        if (selected != null) loadFiles(selected);
    }

    private void loadFiles(List<File> files) {
        try {
            List<ImageItem> loaded = loader.load(files);
            if (loaded.isEmpty()) {
                view.showAlert("No supported JPG/PNG images found.");
                return;
            }
            items.clear();
            items.addAll(loaded);
            currentIndex = 0;
            view.resetZoom();
            view.showEditorScreen();
            showCurrentImage();
            view.setStatus("Loaded " + items.size() + " image(s).");
        } catch (Exception ex) {
            view.showError("Unable to import files", ex);
        }
    }

    private void showCurrentImage() {
        if (items.isEmpty()) return;
        ImageItem item = items.get(currentIndex);
        view.setOriginalImage(item.getImage());
        view.setFileLabel((currentIndex + 1) + " / " + items.size() + " — " + item.getName());
        view.setNavigationState(items.size());
        updateResultPreview();
    }

    private void navigate(int direction) {
        if (items.isEmpty()) return;
        currentIndex = Math.floorMod(currentIndex + direction, items.size());
        showCurrentImage();
    }

    private void updateResultPreview() {
        if (items.isEmpty()) {
            view.clearResult();
            return;
        }
        try {
            SvgSettings settings = view.getSettings();
            String svg = RasterToSvg.convert(items.get(currentIndex).getImage(), settings);
            // ให้ SVG ขยายเต็มกรอบพรีวิว
            String previewSvg = svg.replaceFirst("width=\"\\d+\" height=\"\\d+\"",
                    "width=\"100%\" height=\"100%\"");
            view.setResultSvg(previewSvg);
            view.setStatus("Preview updated • " + view.getSettingsSummary());
        } catch (Exception ex) {
            view.setStatus("Preview unavailable: " + ex.getMessage());
        }
    }

    private void exportAll() {
        if (items.isEmpty()) return;
        File folder = view.askForOutputFolder();
        if (folder == null) return;

        List<ImageItem> work = new ArrayList<>(items);
        SvgSettings settings = view.getSettings();
        boolean parallel = ExportService.isParallel(work.size(), settings);

        view.setExporting(true);
        view.setStatus(parallel ? "Exporting files in parallel..." : "Exporting files...");

        Task<Void> task = exporter.createTask(work, settings, folder, parallel);
        view.bindProgress(task);
        task.setOnSucceeded(e -> {
            view.unbindProgress();
            view.setExporting(false);
            view.setStatus("Export complete. Saved " + work.size() + " SVG file(s) to " + folder.getAbsolutePath());
            view.showAlert("Export complete! Saved " + work.size() + " SVG file(s).");
        });
        task.setOnFailed(e -> {
            view.unbindProgress();
            view.setExporting(false);
            view.showError("Export failed", task.getException());
        });

        Thread thread = new Thread(task, "svg-export-worker");
        thread.setDaemon(true);
        thread.start();
    }
}
