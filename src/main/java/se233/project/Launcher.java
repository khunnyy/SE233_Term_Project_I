package se233.project;

import javafx.application.Application;
import javafx.stage.Stage;
import se233.project.controller.MainController;
import se233.project.view.MainWindow;

public class Launcher extends Application {
    @Override
    public void start(Stage stage) {
        MainWindow view = new MainWindow(stage);
        new MainController(view);   // ผูก event ของ View เข้ากับ Controller
        view.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
