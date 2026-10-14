package se233.project.service;

import javafx.concurrent.Task;
import se233.project.model.ImageItem;
import se233.project.model.SvgSettings;
import se233.project.vector.RasterToSvg;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExportService {

    public static boolean isParallel(int imageCount, SvgSettings settings) {
        return imageCount > 1 && settings.detail() == 1 && !settings.unlimited();
    }

    public Task<Void> createTask(List<ImageItem> work, SvgSettings settings, File folder, boolean parallel) {

        List<File> outputs = buildOutputFiles(work, folder);

        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                int threads = parallel
                        ? Math.max(1, Math.min(work.size(), Runtime.getRuntime().availableProcessors()))
                        : 1;
                ExecutorService pool = Executors.newFixedThreadPool(threads);
                try {
                    List<Future<?>> futures = new ArrayList<>();
                    for (int i = 0; i < work.size(); i++) {
                        final int index = i;
                        futures.add(pool.submit(() -> {
                            String svg = RasterToSvg.convert(work.get(index).getImage(), settings);
                            Files.writeString(outputs.get(index).toPath(), svg, StandardCharsets.UTF_8);
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
    }

    private static List<File> buildOutputFiles(List<ImageItem> work, File folder) {
        Set<String> used = new HashSet<>();
        List<File> result = new ArrayList<>();
        for (ImageItem item : work) {
            String base = item.getName().replaceFirst("(?i)\\.(jpg|jpeg|png)$", "");
            String name = base;
            int n = 2;
            while (!used.add(name.toLowerCase(Locale.ROOT))) {
                name = base + "_" + n++;
            }
            result.add(new File(folder, name + ".svg"));
        }
        return result;
    }
}
