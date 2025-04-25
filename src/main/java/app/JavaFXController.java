package app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class JavaFXController {

    @FXML
    private Label input;

    @FXML
    private Button percentButton;

    @FXML
    private void oneButtonClick() {
        input.setText(input.getText() + "1");
    }

    @FXML
    public void initialize() {
        percentButton.setText("%");  // ✅ Set % text safely here
    }
}
