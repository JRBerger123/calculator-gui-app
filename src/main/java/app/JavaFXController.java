package app;

import java.text.DecimalFormat;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class JavaFXController {

    @FXML
    private Label input;

    @FXML
    private Label expressionDisplay;

    @FXML
    private Button percentButton;

    @FXML
    private AnchorPane root;

    @FXML
    private VBox sidePanel;

    @FXML
    private VBox calculatorRoot;

    @FXML
    private Button historyButton;

    @FXML
    private Button memoryButton;

    @FXML
    private VBox historyMemoryPanel;

    @FXML
    private ListView<String> historyMemoryListView;

    private String currentInput = "";
    private String operator = "";
    private double firstOperand = 0;
    private boolean startNewInput = true;
    private boolean hasCalculated = false;

    private final ObservableList<String> historyList = FXCollections.observableArrayList();
    private final ObservableList<String> memoryList = FXCollections.observableArrayList();

    private final DecimalFormat df = new DecimalFormat("#.##########");
    private final DecimalFormat scientificDF = new DecimalFormat("0.######E0");

    private static final double RESPONSIVE_THRESHOLD = 555.0;

    @FXML
    public void initialize() {
        percentButton.setText("%");

        expressionDisplay.setText("");

        // Initialize history and memory components
        historyMemoryListView.setItems(historyList);

        // Default to history panel
        showHistoryPanel();

        // Configure responsive layout
        setupResponsiveLayout();
    }

    private void setupResponsiveLayout() {
        // This ensures the sidePanel takes up no space when hidden
        sidePanel.managedProperty().bind(sidePanel.visibleProperty());

        // Initial visibility check based on scene width
        sidePanel.setVisible(false); // Default to hidden until we know the width

        // Listen for scene creation and changes
        root.sceneProperty().addListener((obsS, oldScene, newScene) -> {
            if (newScene != null) {
                // Initial check when scene is created
                boolean shouldShow = newScene.getWidth() > RESPONSIVE_THRESHOLD;
                sidePanel.setVisible(shouldShow);
                adjustCalculatorLayout(shouldShow);

                // Add listener for width changes
                newScene.widthProperty().addListener((obsW, oldW, newW) -> {
                    boolean showPanel = newW.doubleValue() > RESPONSIVE_THRESHOLD;
                    sidePanel.setVisible(showPanel);
                    adjustCalculatorLayout(showPanel);
                });
            }
        });
    }

    private void adjustCalculatorLayout(boolean sidePanelVisible) {
        // When side panel is hidden, calculator takes full width
        if (sidePanelVisible) {
            AnchorPane.setRightAnchor(calculatorRoot, 245.0);
        } else {
            AnchorPane.setRightAnchor(calculatorRoot, 0.0);
        }
    }

    @FXML
    private void showHistoryPanel() {
        //historyPanel.setVisible(true);
        //memoryPanel.setVisible(false);
        historyMemoryListView.setItems(historyList);
        historyButton.setStyle("-fx-background-color: #27c0c5");
        memoryButton.setStyle("-fx-background-color: #bdbdbd");
    }

    @FXML
    private void showMemoryPanel() {
        //historyPanel.setVisible(false);
        //memoryPanel.setVisible(true);
        historyMemoryListView.setItems(memoryList);
        historyButton.setStyle("-fx-background-color: #bdbdbd");
        memoryButton.setStyle("-fx-background-color: #27c0c5");
    }

    // All other methods remain the same

    @FXML
    @SuppressWarnings("unused")
    private void handleLeftClick(javafx.event.ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String buttonText = clickedButton.getText();

        switch (buttonText) {
            case "C" -> clear();
            case "CE" -> clearEntry();
            case "⌫" -> backspace();
            case "+", "-", "x", "÷" -> setOperator(buttonText);
            case "=" -> calculateResult();
            case "%" -> percent();
            case "√" -> sqrt();
            case "x²" -> square();
            case "1/x" -> reciprocal();
            case "(-)" -> toggleSign();
            case "History" -> showHistoryPanel();
            case "Memory" -> showMemoryPanel();
            case "MS" -> memorySave();
            case "M+" -> memoryAdd();
            case "M-" -> memorySubtract();
            case "MR" -> memoryRecall();
            case "MC" -> memoryClear();
            case "π" -> appendPi();
            case "e" -> appendE();
            default -> appendText(buttonText); // Digits and decimal point
        }
    }

    private void appendText(String text) {
        if (startNewInput || hasCalculated) {
            currentInput = "";
            startNewInput = false;
            hasCalculated = false;
        }
        if (text.equals(".") && currentInput.contains(".")) {
            return; // Prevent multiple dots
        }
        currentInput += text;
        input.setText(formatNumber(currentInput));
    }

    private void setOperator(String op) {
        if (!currentInput.isEmpty()) {
            if (!operator.isEmpty()) {
                calculateResult();
            }

            firstOperand = Double.parseDouble(currentInput);
            operator = op;
            expressionDisplay.setText(formatNumber(firstOperand) + " " + operator);
            startNewInput = true;
        } else if (!operator.isEmpty()) {
            // Allow changing the operator
            operator = op;
            expressionDisplay.setText(formatNumber(firstOperand) + " " + operator);
        }
    }

    private void calculateResult() {
        if (operator.isEmpty() || (currentInput.isEmpty() && !hasCalculated)) {
            return;
        }

        double secondOperand;
        if (hasCalculated) {
            // If we've already calculated, use the current result as first operand
            secondOperand = firstOperand;
        } else {
            secondOperand = Double.parseDouble(currentInput);
        }

        // Add to history before calculation
        String expression = formatNumber(firstOperand) + " " + operator + " " + formatNumber(secondOperand);

        double result;
        boolean error = false;

        result = switch (operator) {
            case "+" -> firstOperand + secondOperand;
            case "-" -> firstOperand - secondOperand;
            case "x" -> firstOperand * secondOperand;
            case "÷" -> {
                if (secondOperand == 0) {
                    input.setText("Error: Division by zero");
                    error = true;
                    // In switch statements, yield is used to return a value from the case block
                    yield 0; // Return 0 in case of error
                }
                yield firstOperand / secondOperand;
            }
            default -> 0; // Default case to handle unexpected operators
        };

        if (!error) {
            String formattedResult = formatNumber(result);
            historyList.add(0, expression + " = " + formattedResult);
            currentInput = String.valueOf(result);
            input.setText(formattedResult);
            expressionDisplay.setText(expression + " =");
            firstOperand = result;
        }

        operator = "";
        hasCalculated = true;
    }

    private String formatNumber(String numStr) {
        try {
            return formatNumber(Double.parseDouble(numStr));
        } catch (NumberFormatException e) {
            return numStr;
        }
    }

    private String formatNumber(double num) {
        // Check if number is integer
        if (num == (long) num) {
            return String.format("%d", (long) num);
        }

        // Check if number is very large or very small
        if (Math.abs(num) < 0.0000001 || Math.abs(num) > 10000000) {
            return scientificDF.format(num);
        }

        return df.format(num);
    }

    private void clear() {
        currentInput = "";
        firstOperand = 0;
        operator = "";
        hasCalculated = false;
        input.setText("0");
        expressionDisplay.setText("");
    }

    private void clearEntry() {
        currentInput = "";
        input.setText("0");
    }

    private void backspace() {
        if (!currentInput.isEmpty() && !hasCalculated) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
            input.setText(currentInput.isEmpty() ? "0" : formatNumber(currentInput));
        }
    }

    private void percent() {
        if (!currentInput.isEmpty()) {
            double value = Double.parseDouble(currentInput);

            if (!operator.isEmpty()) {
                // In the context of an operation, percent works relative to the first operand
                value = firstOperand * (value / 100.0);
            } else {
                value = value / 100.0;
            }

            currentInput = String.valueOf(value);
            input.setText(formatNumber(value));

            historyList.add(0, "percent(" + formatNumber(Double.parseDouble(currentInput)) + ") = " + formatNumber(value));
        }
    }

    private void sqrt() {
        if (!currentInput.isEmpty()) {
            double value = Double.parseDouble(currentInput);
            if (value < 0) {
                input.setText("Error: Invalid input");
                return;
            }
            double result = Math.sqrt(value);
            currentInput = String.valueOf(result);
            input.setText(formatNumber(result));

            historyList.add(0, "√(" + formatNumber(value) + ") = " + formatNumber(result));
            hasCalculated = true;
        }
    }

    private void square() {
        if (!currentInput.isEmpty()) {
            double value = Double.parseDouble(currentInput);
            double result = Math.pow(value, 2);
            currentInput = String.valueOf(result);
            input.setText(formatNumber(result));

            historyList.add(0, "sqr(" + formatNumber(value) + ") = " + formatNumber(result));
            hasCalculated = true;
        }
    }

    private void reciprocal() {
        if (!currentInput.isEmpty()) {
            double value = Double.parseDouble(currentInput);
            if (value == 0) {
                input.setText("Error: Division by zero");
                return;
            }
            double result = 1 / value;
            currentInput = String.valueOf(result);
            input.setText(formatNumber(result));

            historyList.add(0, "1/(" + formatNumber(value) + ") = " + formatNumber(result));
            hasCalculated = true;
        }
    }

    private void toggleSign() {
        if (!currentInput.isEmpty()) {
            double value = Double.parseDouble(currentInput);
            value = -value;
            currentInput = String.valueOf(value);
            input.setText(formatNumber(value));
        }
    }

    private void appendPi() {
        currentInput = String.valueOf(Math.PI);
        input.setText(formatNumber(Math.PI));
        startNewInput = false;
        hasCalculated = false;
    }

    private void appendE() {
        currentInput = String.valueOf(Math.E);
        input.setText(formatNumber(Math.E));
        startNewInput = false;
        hasCalculated = false;
    }

    private void memorySave() {
        if (!currentInput.isEmpty()) {
            memoryList.add(0, currentInput);
        }
    }

    private void memoryAdd() {
        if (!currentInput.isEmpty() && !memoryList.isEmpty()) {
            double current = Double.parseDouble(currentInput);
            double memory = Double.parseDouble(memoryList.get(0));
            memoryList.set(0, String.valueOf(memory + current));
        } else if (!currentInput.isEmpty()) {
            memoryList.add(0, currentInput);
        }
    }

    private void memorySubtract() {
        if (!currentInput.isEmpty() && !memoryList.isEmpty()) {
            double current = Double.parseDouble(currentInput);
            double memory = Double.parseDouble(memoryList.get(0));
            memoryList.set(0, String.valueOf(memory - current));
        }
    }

    private void memoryRecall() {
        if (!memoryList.isEmpty()) {
            currentInput = memoryList.get(0);
            input.setText(formatNumber(currentInput));
            startNewInput = false;
        }
    }

    private void memoryClear() {
        memoryList.clear();
    }
}