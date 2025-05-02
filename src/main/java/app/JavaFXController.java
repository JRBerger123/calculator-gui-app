package app;

import java.text.DecimalFormat;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class JavaFXController {

    @FXML private Label mainDisplay;
    @FXML private Label expressionDisplay;
    @FXML private Label displayType;
    @FXML private Button percentButton;
    @FXML private AnchorPane root;
    @FXML private VBox sidePanel;
    @FXML private VBox calculatorRoot;
    @FXML private Button historyButton;
    @FXML private Button memoryButton;
    @FXML private ListView<String> historyMemoryListView;

    private final ObservableList<String> historyList = FXCollections.observableArrayList();
    private final ObservableList<String> memoryList = FXCollections.observableArrayList();

    private final DecimalFormat df = new DecimalFormat("#.##########");
    private final DecimalFormat scientificDF = new DecimalFormat("0.######E0");

    // Full expression that shows in expressionDisplay
    private final StringBuilder expressionBuilder = new StringBuilder();
    
    // Current input that shows in mainDisplay
    private final StringBuilder currentInput = new StringBuilder();

    // Keeps track of the actual JavaScript expression for evaluation
    private final StringBuilder jsExpressionBuilder = new StringBuilder();
    
    // JavaScript engine for evaluating expressions
    // This uses GraalVM JavaScript engine if available, otherwise falls back to generic JavaScript engine
    private ScriptEngine engine;
    
    // Tracks the state of the calculator
    private boolean startNewInput = true;
    private boolean operationJustPerformed = false;
    
    // Track pending unary operations. Allows for nested operations (e.g., sqrt(sqrt(4)))
    private String pendingUnaryOperation = null;
    private int pendingOperationClosings = 0;

    private static final double RESPONSIVE_THRESHOLD = 555.0;

    @FXML
    public void initialize() {
        try {
            this.engine = new ScriptEngineManager().getEngineByName("graal.js");
            if (this.engine == null) {
                System.err.println("GraalVM JavaScript engine not found, trying generic JavaScript");
                this.engine = new ScriptEngineManager().getEngineByName("JavaScript");
                
                if (this.engine == null) {
                    System.err.println("No JavaScript engine found! Application will exit.");
                    Platform.exit();
                    return;
                }
            }
            
            System.out.println("JavaScript engine created successfully: " + engine.getClass().getName());
            
            percentButton.setText("%");
            resetCalculator();
            showHistoryPanel();
            setupResponsiveLayout();
        } catch (Exception e) {
            System.err.println("Error initializing JavaFXController: " + e.getMessage());
            Platform.exit();
        }
    }

    private void setupResponsiveLayout() {
        sidePanel.managedProperty().bind(sidePanel.visibleProperty());
        sidePanel.setVisible(false);

        root.sceneProperty().addListener((obsS, oldScene, newScene) -> {
            if (newScene != null) {
                boolean shouldShow = newScene.getWidth() > RESPONSIVE_THRESHOLD;
                sidePanel.setVisible(shouldShow);
                adjustCalculatorLayout(shouldShow);

                newScene.widthProperty().addListener((obsW, oldW, newW) -> {
                    boolean showPanel = newW.doubleValue() > RESPONSIVE_THRESHOLD;
                    sidePanel.setVisible(showPanel);
                    adjustCalculatorLayout(showPanel);
                });
            }
        });
    }

    private void adjustCalculatorLayout(boolean sidePanelVisible) {
        AnchorPane.setRightAnchor(calculatorRoot, sidePanelVisible ? 245.0 : 0.0);
    }

    /**
     * Handles left button clicks on elements in the calculator.
     * Each button is associated with an ID, and the corresponding action is performed.
     * 
     * @param event The ActionEvent triggered by the button click
     */
    @SuppressWarnings("unused")
    @FXML
    private void handleLeftClick(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String id = clickedButton.getId();

        switch (id) {
            case "cButton" -> clear();
            case "ceButton" -> clearEntry();
            case "backspaceButton" -> backspace();
            case "equalsButton" -> evaluateExpression();

            case "plusButton" -> handleOperator("+");
            case "minusButton" -> handleOperator("-");
            case "multiplyButton" -> handleOperator("*");
            case "divideButton" -> handleOperator("/");
            
            case "decimalButton" -> appendToInput(".");

            case "zeroButton" -> appendToInput("0");
            case "oneButton" -> appendToInput("1");
            case "twoButton" -> appendToInput("2");
            case "threeButton" -> appendToInput("3");
            case "fourButton" -> appendToInput("4");
            case "fiveButton" -> appendToInput("5");
            case "sixButton" -> appendToInput("6");
            case "sevenButton" -> appendToInput("7");
            case "eightButton" -> appendToInput("8");
            case "nineButton" -> appendToInput("9");

            case "percentButton" -> togglePercentDisplay();
            case "squareButton" -> applyUnaryOperation("square");
            case "squareRootButton" -> applyUnaryOperation("sqrt");
            case "reciprocalButton" -> applyUnaryOperation("reciprocal");
            case "negateButton" -> applyUnaryOperation("negate");

            case "historyButton" -> showHistoryPanel();
            case "memoryButton" -> showMemoryPanel();
            case "mcButton" -> memoryList.clear();
            case "mrButton" -> recallMemory();
            case "msButton" -> memoryList.add(0, mainDisplay.getText());

            default -> System.err.println("Unhandled button ID: " + id);
        }
    }

    private void appendToInput(String value) {
        // If an operation was just performed or we're starting a new input,
        // clear the current input
        if (startNewInput || operationJustPerformed) {
            currentInput.setLength(0);
            startNewInput = false;
            operationJustPerformed = false;

            // Reset the display type to "Input" when starting a new input
            displayType.setText("Input");
            displayType.setStyle("-fx-text-fill: rgba(229, 245, 0, 0.6);");
        }
        
        // Handle special case for decimal point
        if (value.equals(".") && currentInput.toString().contains(".")) {
            return; // Prevent multiple decimal points
        }
        
        // Handle special case for zero at the beginning
        if (currentInput.toString().equals("0") && !value.equals(".")) {
            currentInput.setLength(0);
        }
        
        // Append the value to the current input
        currentInput.append(value);
        
        // Update the main display with the current input
        mainDisplay.setText(currentInput.toString());
    }
    
    /**
     * Handles operator input (+, -, *, /) and performs the appropriate operation.
     * Converts percentage values to decimal before performing operations.
     * 
     * @param operator The operator to apply
     */
    private void handleOperator(String operator) {
        // Convert percentage to decimal if needed
        if (mainDisplay.getText().endsWith("%")) {
            try {
                double value = parseDisplayValue(mainDisplay.getText());
                String decimalStr = formatNumber(value);
                mainDisplay.setText(decimalStr);
                currentInput.setLength(0);
                currentInput.append(decimalStr);
            } catch (NumberFormatException e) {
                mainDisplay.setText("Error");
                System.err.println("Error converting percentage: " + e.getMessage());
                return;
            }
        }
        
        // If there's a current input, add it to the expression
        if (!operationJustPerformed) {
            if (currentInput.length() > 0) {
                expressionBuilder.append(currentInput);
                jsExpressionBuilder.append(currentInput);
            } else {
                // If no current input, append 0 before operator
                expressionBuilder.append("0");
                jsExpressionBuilder.append("0");
            }
        }
        
        // Close any pending operations
        while (pendingOperationClosings > 0) {
            expressionBuilder.append(")");
            jsExpressionBuilder.append(")");
            pendingOperationClosings--;
        }
        pendingUnaryOperation = null;
        
        // If the last character is an operator, replace it
        if (expressionBuilder.length() > 0) {
            char lastChar = expressionBuilder.charAt(expressionBuilder.length() - 1);
            if (lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/') {
                expressionBuilder.setLength(expressionBuilder.length() - 1);
                jsExpressionBuilder.setLength(jsExpressionBuilder.length() - 1);
            }
        }
        
        // Append the operator to the expression
        expressionBuilder.append(operator);
        jsExpressionBuilder.append(operator);
        
        // Update the expression display
        expressionDisplay.setText(expressionBuilder.toString());
        
        // Evaluate the expression so far and show the result in the main display
        evaluatePartialExpression();
        
        // Mark that an operation was just performed
        operationJustPerformed = true;

        // Update the display type to "Result" to indicate the main display shows a result
        displayType.setText("Result");
        displayType.setStyle("-fx-text-fill: rgba(0, 255, 0, 0.6);");
    }

    /**
     * Evaluates the partial expression so far and updates the main display with the result.
     * Ensures all percentage values are properly converted to decimals.
     */
    private void evaluatePartialExpression() {
        // Only evaluate if there's something to evaluate
        if (jsExpressionBuilder.length() > 0) {
            try {
                String tempExpression = jsExpressionBuilder.toString();

                // Create a temporary expression without the trailing operator
                if (tempExpression.endsWith("+") || tempExpression.endsWith("-") || 
                    tempExpression.endsWith("*") || tempExpression.endsWith("/")) {
                    tempExpression = tempExpression.substring(0, tempExpression.length() - 1);
                }
                
                // Only evaluate if there's a valid expression
                if (!tempExpression.isEmpty()) {
                    Object result = engine.eval(tempExpression);
                    String resultStr = formatNumber(Double.parseDouble(result.toString()));
                    mainDisplay.setText(resultStr);
                    
                    // Store the result as the current input for the next operation
                    currentInput.setLength(0);
                    currentInput.append(resultStr);

                    // Update the display type to "Result" to indicate the main display shows a result
                    displayType.setText("Result");
                    displayType.setStyle("-fx-text-fill: rgba(0, 255, 0, 0.6);");
                }
            } catch (ScriptException | NumberFormatException e) {
                // If there's an error, don't update the display
                System.err.println("Partial expression error: " + e.getMessage());
            }
        }
    }

    /**
     * Evaluates the complete expression and updates the display with the final result.
     * Ensures all percentage values are properly converted to decimals.
     */
    private void evaluateExpression() {
        // Convert percentage to decimal if needed
        if (mainDisplay.getText().endsWith("%")) {
            try {
                double value = parseDisplayValue(mainDisplay.getText());
                String decimalStr = formatNumber(value);
                mainDisplay.setText(decimalStr);
                currentInput.setLength(0);
                currentInput.append(decimalStr);
            } catch (NumberFormatException e) {
                mainDisplay.setText("Error");
                System.err.println("Error converting percentage: " + e.getMessage());
                return;
            }
        }
        
        // If there's a current input and an operation wasn't just performed,
        // add it to the expression
        if (currentInput.length() > 0 && !operationJustPerformed) {
            expressionBuilder.append(currentInput);
            jsExpressionBuilder.append(currentInput);
        }
        
        // Close any pending operations
        while (pendingOperationClosings > 0) {
            expressionBuilder.append(")");
            jsExpressionBuilder.append(")");
            pendingOperationClosings--;
        }
        pendingUnaryOperation = null;
        
        // Only evaluate if there's an expression
        if (jsExpressionBuilder.length() > 0) {
            try {
                String jsExpressionStr = jsExpressionBuilder.toString();
                String displayExpressionStr = expressionBuilder.toString();
                
                // Check if the expression ends with an operator and remove it
                if (jsExpressionStr.endsWith("+") || jsExpressionStr.endsWith("-") || 
                    jsExpressionStr.endsWith("*") || jsExpressionStr.endsWith("/")) {
                    jsExpressionStr = jsExpressionStr.substring(0, jsExpressionStr.length() - 1);
                    displayExpressionStr = displayExpressionStr.substring(0, displayExpressionStr.length() - 1);
                    expressionBuilder.setLength(displayExpressionStr.length());
                    jsExpressionBuilder.setLength(jsExpressionStr.length());
                }
                
                // Use jsExpressionStr for evaluation
                Object result = engine.eval(jsExpressionStr);
                String resultStr = formatNumber(Double.parseDouble(result.toString()));
                
                // Add to history
                historyList.add(0, displayExpressionStr + " = " + resultStr);
                
                // Update displays
                mainDisplay.setText(resultStr);
                if (!expressionDisplay.getText().endsWith(" =")) {
                    expressionDisplay.setText(displayExpressionStr + " =");
                }

                // Update the display type to "Result" to indicate the main display shows a result
                displayType.setText("Result");
                displayType.setStyle("-fx-text-fill: rgba(0, 255, 0, 0.6);");
                
                // Reset state
                expressionBuilder.setLength(0);
                jsExpressionBuilder.setLength(0);
                currentInput.setLength(0);
                currentInput.append(resultStr);
                startNewInput = true;
                operationJustPerformed = false;
                pendingUnaryOperation = null;
                pendingOperationClosings = 0;
            } catch (ScriptException | NumberFormatException e) {
                mainDisplay.setText("Error");
                System.err.println("Expression error: " + e.getMessage());
                resetCalculator();
            }
        }
    }

    private void clear() {
        resetCalculator();
    }
    
    private void resetCalculator() {
        expressionBuilder.setLength(0);
        jsExpressionBuilder.setLength(0);
        currentInput.setLength(0);
        mainDisplay.setText("0");
        expressionDisplay.setText("");
        startNewInput = true;
        operationJustPerformed = false;
        pendingUnaryOperation = null;
        pendingOperationClosings = 0;

        // Reset the display type to "Input" when starting a new input
        displayType.setText("Input");
        displayType.setStyle("-fx-text-fill: rgba(229, 245, 0, 0.6);");
    }

    private void clearEntry() {
        // Clear only the current entry/input
        currentInput.setLength(0);
        mainDisplay.setText("0");
        startNewInput = true;
    }

    private void backspace() {
        // Only allow backspace on the current input, not on results
        if (!startNewInput && !operationJustPerformed && currentInput.length() > 0) {
            currentInput.deleteCharAt(currentInput.length() - 1);
            mainDisplay.setText(currentInput.length() > 0 ? currentInput.toString() : "0");
        }
    }

    /**
     * Applies a unary operation (square, square root, reciprocal, negate) to the current value.
     * Converts percentage values to decimal before performing operations.
     * 
     * @param type The type of unary operation to apply
     */
    private void applyUnaryOperation(String type) {
        try {
            // Check if the last character in the expression is an operator
            boolean afterOperator = false;
            if (expressionBuilder.length() > 0) {
                char lastChar = expressionBuilder.charAt(expressionBuilder.length() - 1);
                if (lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/') {
                    afterOperator = true;
                }
            }
            
            // Convert percentage to decimal if needed
            if (mainDisplay.getText().endsWith("%") && !type.equals("percent")) {
                double value = parseDisplayValue(mainDisplay.getText());
                String decimalStr = formatNumber(value);
                mainDisplay.setText(decimalStr);
                currentInput.setLength(0);
                currentInput.append(decimalStr);
            }
            
            // Get the value from the main display, handling percent signs
            double value = parseDisplayValue(mainDisplay.getText());
            String valueStr = formatNumber(value);
            
            // Create display and JS representations for the operation
            String operationPrefix, operationSuffix;
            String jsOperationPrefix, jsOperationSuffix;
            
            switch (type) {
                case "percent" -> {
                    operationPrefix = "";
                    operationSuffix = "%";
                    jsOperationPrefix = "";
                    jsOperationSuffix = " / 100";
                }
                case "square" -> {
                    operationPrefix = "";
                    operationSuffix = "\u00B2";
                    jsOperationPrefix = "Math.pow(";
                    jsOperationSuffix = ", 2)";
                }
                case "sqrt" -> {
                    operationPrefix = "\u221A(";
                    operationSuffix = ")";
                    jsOperationPrefix = "Math.sqrt(";
                    jsOperationSuffix = ")";
                }
                case "reciprocal" -> {
                    operationPrefix = "1/(";
                    operationSuffix = ")";
                    jsOperationPrefix = "1/(";
                    jsOperationSuffix = ")";
                }
                case "negate" -> {
                    operationPrefix = "-(";
                    operationSuffix = ")";
                    jsOperationPrefix = "-(";
                    jsOperationSuffix = ")";
                }
                default -> {
                    operationPrefix = "";
                    operationSuffix = "";
                    jsOperationPrefix = "";
                    jsOperationSuffix = "";
                }
            }
            
            // Handle the operation differently based on context
            if (afterOperator) {
                // After an operator, we append the operation but don't evaluate yet
                
                // Update the expression display with the operation prefix
                expressionBuilder.append(operationPrefix);
                jsExpressionBuilder.append(jsOperationPrefix);
                
                // Update the expression display
                expressionDisplay.setText(expressionBuilder.toString());
                
                // We don't modify the main display - it should still show the previous result
                
                // Set flags to prepare for the next input
                startNewInput = true;
                operationJustPerformed = false; // Allow next digit input to replace display
                
                // Reset the display type to "Input" when starting a new input
                displayType.setText("Input");
                displayType.setStyle("-fx-text-fill: rgba(229, 245, 0, 0.6);");
                
                // Store operation info for later completion
                pendingUnaryOperation = type;
                pendingOperationClosings++;
                
                return;
            } else if (pendingUnaryOperation != null && operationJustPerformed) {
                // We're nesting operations (e.g., sqrt(sqrt(...)))
                
                // Update the expression display with the operation prefix
                expressionBuilder.append(operationPrefix);
                jsExpressionBuilder.append(jsOperationPrefix);
                
                // Update the expression display
                expressionDisplay.setText(expressionBuilder.toString());
                
                // We don't modify the main display - it should still show the previous result
                
                // Set flags to prepare for the next input
                startNewInput = true;
                operationJustPerformed = false; // Allow next digit input to replace display
                
                // Reset the display type to "Input" when starting a new input
                displayType.setText("Input");
                displayType.setStyle("-fx-text-fill: rgba(229, 245, 0, 0.6);");
                
                // Store operation info for later completion
                pendingUnaryOperation = type;
                pendingOperationClosings++;
                
                return;
            }
            
            // For direct operations (not after operator or nested), proceed normally
            // Apply the operation using JavaScript engine
            double result;
            try {
                // Construct the full JavaScript expression
                String jsExpression = jsOperationPrefix + valueStr + jsOperationSuffix;
                Object evalResult = engine.eval(jsExpression);
                result = Double.parseDouble(evalResult.toString());
            } catch (ScriptException e) {
                System.err.println("Error evaluating operation: " + e.getMessage());
                mainDisplay.setText("Error");
                return;
            }
            
            // Format and display the result
            String formatted = formatNumber(result);
            mainDisplay.setText(formatted);
            
            // Update the display type to "Result" to indicate the main display shows a result
            displayType.setText("Result");
            displayType.setStyle("-fx-text-fill: rgba(0, 255, 0, 0.6);");
            
            // Create the display representation
            String operationDisplay = operationPrefix + valueStr + operationSuffix;
            String jsExpression = jsOperationPrefix + valueStr + jsOperationSuffix;
            
            // Update the expression display and jsExpressionBuilder
            if (expressionBuilder.length() > 0) {
                // Check if the last character is an operator
                char lastChar = expressionBuilder.charAt(expressionBuilder.length() - 1);
                if (lastChar == '+' || lastChar == '-' || lastChar == '*' || lastChar == '/') {
                    // If the last character is an operator, append the operation display
                    expressionBuilder.append(operationDisplay);
                    jsExpressionBuilder.append(jsExpression);
                } else {
                    // Otherwise, replace the last term with the operation
                    int lastOperatorIndex = Math.max(
                        expressionBuilder.lastIndexOf("+"),
                        Math.max(
                            expressionBuilder.lastIndexOf("-"),
                            Math.max(
                                expressionBuilder.lastIndexOf("*"),
                                expressionBuilder.lastIndexOf("/")
                            )
                        )
                    );
                    
                    if (lastOperatorIndex >= 0) {
                        // If there's an operator, replace everything after it
                        expressionBuilder.replace(lastOperatorIndex + 1, expressionBuilder.length(), operationDisplay);
                        
                        // Do the same for jsExpressionBuilder
                        int jsLastOpIndex = Math.max(
                            jsExpressionBuilder.lastIndexOf("+"),
                            Math.max(
                                jsExpressionBuilder.lastIndexOf("-"),
                                Math.max(
                                    jsExpressionBuilder.lastIndexOf("*"),
                                    jsExpressionBuilder.lastIndexOf("/")
                                )
                            )
                        );
                        if (jsLastOpIndex >= 0) {
                            jsExpressionBuilder.replace(jsLastOpIndex + 1, jsExpressionBuilder.length(), jsExpression);
                        }
                    } else {
                        // No operator, replace the entire expression
                        expressionBuilder.replace(0, expressionBuilder.length(), operationDisplay);
                        jsExpressionBuilder.setLength(0);
                        jsExpressionBuilder.append(jsExpression);
                    }
                }
            } else {
                // If there's no existing expression, set it to the operation display
                expressionBuilder.append(operationDisplay);
                jsExpressionBuilder.setLength(0);
                jsExpressionBuilder.append(jsExpression);
            }
            
            // Update the expression display (except for percent toggle)
            if (!type.equals("percent")) {
                expressionDisplay.setText(expressionBuilder.toString());
            }
            
            // Update current input with the result
            currentInput.setLength(0);
            currentInput.append(formatted);
            
            // Create a more descriptive history entry
            if (!type.equals("percent")) {  // Don't add percent toggle to history
                String historyEntry;
                switch (type) {
                    case "square" -> historyEntry = valueStr + "\u00B2 = " + formatted;
                    case "sqrt" -> historyEntry = "sqrt(" + valueStr + ") = " + formatted;
                    case "reciprocal" -> historyEntry = "1/(" + valueStr + ") = " + formatted;
                    case "negate" -> historyEntry = "negate(" + valueStr + ") = " + formatted;
                    default -> historyEntry = valueStr + " = " + formatted;
                }
                
                // Add to history
                historyList.add(0, historyEntry);
            }
            
            // Mark that we should start a new input next
            startNewInput = true;
            operationJustPerformed = true;
            
        } catch (NumberFormatException e) {
            mainDisplay.setText("Error");
            System.err.println("Error in unary operation: " + e.getMessage());
        }
    }

    private void recallMemory() {
        if (!memoryList.isEmpty()) {
            String memoryValue = memoryList.get(0);
            mainDisplay.setText(memoryValue);
            
            // Update current input with recalled value
            currentInput.setLength(0);
            currentInput.append(memoryValue);
            
            // Mark that we should continue with this input
            startNewInput = false;
            operationJustPerformed = false;

            // Reset the display type to "Input" when starting a new input
            displayType.setText("Input");
            displayType.setStyle("-fx-text-fill: rgba(229, 245, 0, 0.6);");
        }
    }

    private void showHistoryPanel() {
        historyMemoryListView.setItems(historyList);
        historyButton.setStyle("-fx-background-color: #27c0c5");
        memoryButton.setStyle("-fx-background-color: #bdbdbd");
    }

    private void showMemoryPanel() {
        historyMemoryListView.setItems(memoryList);
        historyButton.setStyle("-fx-background-color: #bdbdbd");
        memoryButton.setStyle("-fx-background-color: #27c0c5");
    }

    /**
     * Formats a double value, converting it to an integer if it's sufficiently close to an integer.
     * This handles cases like sqrt(2)^2 where the result should be exactly 2 but might be 2.0000000000001
     * due to floating-point precision issues.
     * 
     * @param value The double value to format
     * @return A string representation, either as an integer or a formatted double
     */
    private String formatNumber(double value) {
        // Check if the value is very close to an integer
        double roundedValue = Math.round(value);
        if (Math.abs(value - roundedValue) < 1E-10) {
            return String.format("%d", (long)roundedValue);
        }
        
        // Handle scientific notation for very large or small numbers
        if (Math.abs(value) < 0.0000001 || Math.abs(value) > 10000000) {
            return scientificDF.format(value);
        }
        
        // Use decimal formatter for regular numbers
        return df.format(value);
    }

    /**
     * Toggles between percentage and decimal representation in the main display.
     * If the current value doesn't have a percent sign, multiplies by 100 and adds "%".
     * If the current value already has a percent sign, divides by 100 and removes "%".
     * This operation only affects the main display and doesn't modify the expression display.
     */
    private void togglePercentDisplay() {
        String currentText = mainDisplay.getText();
        
        try {
            if (currentText.endsWith("%")) {
                // Convert from percentage to decimal
                String valueWithoutPercent = currentText.substring(0, currentText.length() - 1);
                double value = Double.parseDouble(valueWithoutPercent);
                double decimalValue = value / 100.0;
                
                // Format and display the decimal value
                String formatted = formatNumber(decimalValue);
                mainDisplay.setText(formatted);
                
                // Update current input with the decimal value
                currentInput.setLength(0);
                currentInput.append(formatted);
            } else {
                // Convert from decimal to percentage
                double value = Double.parseDouble(currentText);
                double percentValue = value * 100.0;
                
                // Format the percentage value (without using formatNumber to preserve all digits)
                String formatted;
                if (percentValue == (long) percentValue) {
                    formatted = String.format("%d%%", (long) percentValue);
                } else {
                    // Use the decimal formatter but handle the percent sign manually
                    formatted = df.format(percentValue) + "%";
                }
                
                mainDisplay.setText(formatted);
                
                // Update current input with the percentage value (including % sign)
                currentInput.setLength(0);
                currentInput.append(formatted);
            }
            
            // Mark that we should start a new input next if another number is pressed
            startNewInput = true;
            operationJustPerformed = false;
        } catch (NumberFormatException e) {
            mainDisplay.setText("Error");
            System.err.println("Error in percent toggle: " + e.getMessage());
        }
    }

    /**
     * Converts a display string to a numeric value, handling percent signs.
     * If the string ends with a percent sign, converts it to its decimal equivalent.
     * 
     * @param displayValue The string value from the display
     * @return The numeric value represented by the string
     * @throws NumberFormatException if the string cannot be parsed as a number
     */
    private double parseDisplayValue(String displayValue) throws NumberFormatException {
        if (displayValue.endsWith("%")) {
            String valueWithoutPercent = displayValue.substring(0, displayValue.length() - 1);
            double value = Double.parseDouble(valueWithoutPercent);
            return value / 100.0;
        } else {
            return Double.parseDouble(displayValue);
        }
    }
}