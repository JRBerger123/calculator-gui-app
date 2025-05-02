package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * CalculatorApp is the main class for the calculator application.
 * It initializes the JavaFX application and loads the FXML layout.
 * It also sets the minimum and default size for the application window.
 * The application uses a CSS stylesheet for styling.
 * 
 * @author Brandon Berger,
 * @version 1.0
 * @since 2025.05.02
 * @see <a href="https://github.com/JRBerger123/calculator-gui-app">GitHub Repository</a>
 * @see <a href="https://github.com/JRBerger123">Brandon Berger's GitHub</a>
 * 
 */
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
