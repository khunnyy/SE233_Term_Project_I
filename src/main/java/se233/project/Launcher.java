package se233.project;

import javafx.application.Application;
import javafx.stage.Stage;

public class Launcher extends Application {
    @Override
    public void start(Stage stage) {
        new MainWindow(stage).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
