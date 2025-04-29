package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CalculatorApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/CalculatorLayout.fxml"));
            Parent root = loader.load();
            primaryStage.setTitle("Calculator");

            Scene scene = new Scene(root, 400, 600);

            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());

            primaryStage.setScene(scene);

            primaryStage.setMinHeight(480);
            primaryStage.setMinWidth(320);

            primaryStage.show();
        } catch (java.io.IOException | java.lang.NullPointerException e) {
            System.err.println("Error loading files: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
