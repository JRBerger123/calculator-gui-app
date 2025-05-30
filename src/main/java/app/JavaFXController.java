package app;

import java.text.DecimalFormat;
import java.util.Optional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Tooltip;

/**
 * JavaFXController class handles the user interface and logic for the calculator application.
 * It manages the calculator's state, input handling, and expression evaluation using a JavaScript engine.
 * The class is responsible for updating the UI components based on user interactions and calculations.
 * It also provides functionality for memory operations, history tracking, and context menu management.
 * 
 * @author Brandon Berger,
 * @version 1.0
 * @since 2025.05.02
 * @see <a href="https://github.com/JRBerger123/calculator-gui-app">GitHub Repository</a>
 * @see <a href="https://github.com/JRBerger123">Brandon Berger's GitHub</a>
 * 
 */
public class JavaFXController {

    /**
     * The root layout of the calculator application.
     * This is the main container for all UI components and is used to manage the layout and responsiveness of the calculator.
     */
    @FXML private AnchorPane root;

    /**
     * JavaFX UI components for the main display of the calculator.
     */
    @FXML private Label mainDisplay;

    /**
     * JavaFX UI components for the expression display of the calculator.
     */
    @FXML private Label expressionDisplay;

    /**
     * JavaFX UI components for the display type label (Input/Result).
     * This label indicates whether the current display is showing an input or a result.
     * It is updated dynamically based on the calculator's state.
     */
    @FXML private Label displayTypeLabel;

    /**
     * Button for clearing all entries in the calculator.
     */
    @FXML private Button togglePercentButton;

    /**
     * Button for opening context menu for additional options.
     * This button is used to toggle the visibility of the context menu.
     * The context menu is hidden by default and can be shown by clicking this button.
     */
    @FXML private Button contextMenuButton;

    /**
     * GUI element for the context menu.
     * This menu provides additional options and settings for the calculator, such as changing themes or switching between calculator modes.
     */
    @FXML private VBox contextMenu;

    /**
     * GUI element for the side panel of the calculator.
     * This panel can be used to display additional information, such as history or memory values.
     */
    @FXML private VBox sidePanel;

    /**
     * The main calculator layout that contains the buttons and displays.
     * This layout is used to organize the calculator's buttons and displays in a user-friendly manner.
     * Required so that the calculator can expand dynamically vertically.
     */
    @FXML private VBox calculatorRoot;

    /**
     * Button for evaulating equations and showing results.
     */
    @FXML private Button equalsButton;

    /**
     * Button for setting the side panel to show list of saved values in history.
     */
    @FXML private Button historyButton;

    /**
     * Button for setting the side panel to show list of saved values in memory.
     */
    @FXML private Button memoryButton;

    /**
     * ListView for displaying the history or memory of calculations.
     * Element that allows for the showing of history or memory list in a stack data structure.
     */
    @FXML private ListView<String> sidePanelListView;

    /**
     * Tooltip for the clear history/memory button.
     * This tooltip provides additional information about the button's function when hovered over.
     */
    @FXML private Tooltip clearTooltip;

    /**
     * Flag to indicate if the calculator is in dark mode.
     * This is used to toggle the theme of the calculator UI.
     */
    private boolean darkMode = true;

    /**
     * List of history entries for the calculator.
     * This is used to keep track of previous calculations and results.
     * TODO: To be replaced with a more advanced data structure in the future.
     */
    private final ObservableList<String> historyList = FXCollections.observableArrayList();

    /**
     * List of memory values for the calculator.
     * This is used to store values that can be recalled later.
     * TODO: To be replaced with a more advanced data structure in the future.
     */
    private final ObservableList<String> memoryList = FXCollections.observableArrayList();

    /**
     * DecimalFormat for formatting numbers in decimal notation.
     */
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##########");

    /**
     * DecimalFormat for scientific notation with 6 decimal places.
     */
    private final DecimalFormat scientificFormat = new DecimalFormat("0.######E0");

    /**
     * Full expression shown in expressionDisplay above the mainDisplay.
     */
    private final StringBuilder expressionBuilder = new StringBuilder();
    
    /**
     * Current input shown in mainDisplay
     */
    private final StringBuilder currentInputBuilder = new StringBuilder();

    /**
     * Keeps track of the actual JavaScript expression for evaluation.
     * This is used in the background (not shown to the user) to keep the expression in syntax that JavaScript engine can understand.
     */
    private final StringBuilder jsExpressionBuilder = new StringBuilder();

    /**
     * JavaScript engine for evaluating expressions.
     * This uses GraalVM JavaScript engine if available, otherwise falls back to generic JavaScript engine.
     */
    private ScriptEngine engine;
    
    /**
     * Flag to indicate if a new input is being started.
     * This is used to determine if the current input in mainDisplay should be cleared when a new number is entered.
     */
    private boolean startNewInput = true;

    /**
     * Flag to indicate if an operation was just performed.
     */
    private boolean hasJustPerformedOperation = false;
    
    /**
     * Pending unary operation type. This is used to track the current unary operation being applied.
     */
    private String pendingUnaryOperation = null;

    /**
     * List of pending operation closings. This is used to track the number of closing parentheses needed for unary operations.
     */
    private int unclosedParenthesesCount = 0;

    /**
     * Flag to indicate if the percent cycle is complete.
     * This is used to track if the percent operation has been applied and needs to be converted back to decimal.
     * This is used to prevent continuous dividing of the value by 100 when the percent button is pressed multiple times.
     */
    private boolean isPercentFormatActive = false;

    /**
     * Flag to indicate if the context menu is currently visible.
     * This is used to toggle the visibility of the context menu when the menu button is clicked.
     */
    private boolean isContextMenuVisible = false;

    /**
     * Threshold for responsive layout. If the window width exceeds this value, the side panel is shown.
     * If the window width is less than this value, the side panel is hidden.
     */
    private static final double RESPONSIVE_THRESHOLD = 555.0;

    /**
     * Initializes the JavaFX controller.
     * Sets up the JavaScript engine, initializes the calculator state, configures the UI, and sets up Key Event Handler.
     */
    @FXML
    public void initialize() {
        try {
            this.engine = new ScriptEngineManager().getEngineByName("graal.js");

            // Checks if GraalVM JavaScript engine initialized sucessfully, if not, tries to fall back to generic JavaScript engine
            // The generic JavaScript engine was removed in Java 15+, so this is a fallback for older versions
            if (this.engine == null) {
                System.err.println("GraalVM JavaScript engine not found, trying generic JavaScript");
                this.engine = new ScriptEngineManager().getEngineByName("JavaScript");
                
                // Checks if the generic JavaScript engine initialized successfully, if not, exits the application
                if (this.engine == null) {
                    System.err.println("No JavaScript engine found! Application will exit.");
                    Platform.exit();
                    return;
                }
            }
            
            // Print the name of the JavaScript engine for debugging purposes
            System.out.println("JavaScript engine created successfully: " + engine.getClass().getName() + "\n");
            
            // Set text for the percent button is required as % is used as a special character in JavaFX and cannot be set directly in FXML
            togglePercentButton.setText("%");

            resetCalculator();

            // Calculator defaults to showing the history panel over the memory panel
            showHistoryPanel();

            // Sets up the calculator layout to be responsive to window size changes, as sidePanel is hidden by default
            setupResponsiveLayout();

            // Event handler for key presses
            root.setOnKeyPressed(this::handleKeyPress);

            // Add clipboard functionality
            root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                // TODO: Test edge cases for copying values as input
                // Handle Ctrl+C for copy
                if (event.isControlDown() && event.getCode() == KeyCode.C) {
                    // Get the text to copy (from main display)
                    String textToCopy = mainDisplay.getText();
                    
                    // Get system clipboard
                    final Clipboard clipboard = Clipboard.getSystemClipboard();
                    final ClipboardContent content = new ClipboardContent();
                    
                    // Set clipboard content
                    content.putString(textToCopy);
                    clipboard.setContent(content);
                    
                    // TODO: Provide visual feedback that copy occured
                    
                    event.consume();
                }

                // TODO: Test edge cases for pasting values as input
                // Handle Ctrl+V for paste
                if (event.isControlDown() && event.getCode() == KeyCode.V) {
                    // Get content from clipboard
                    final Clipboard clipboard = Clipboard.getSystemClipboard();
                    String clipboardText = clipboard.getString();
                    
                    // Validate the clipboard content
                    if (clipboardText != null && isValidCalculatorInput(clipboardText)) {
                        // For valid numeric content, append to current input
                        for (char c : clipboardText.toCharArray()) {
                            // Process each character as if it were typed
                            if (Character.isDigit(c) || c == '.') {
                                appendToInput(String.valueOf(c));
                            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                                processOperatorInput(String.valueOf(c));
                            }
                            // Ignore other characters
                        }
                    }
                    
                    // TODO: Provide visual feedback that paste occured

                    event.consume();
                }
            });

            // Close the context menu if clicking elsewhere
            root.setOnMouseClicked(event -> {
                if (!contextMenu.isHover() && !contextMenuButton.isHover()) {
                    contextMenu.setVisible(false);
                    isContextMenuVisible = false;
                }
            });

            // Required for Enter button to act as equals button
            // Waits for the JavaFX application thread to be ready before setting focus
            // Equivalent to setting the equals button tab order to highest priority
            Platform.runLater(() -> {
                equalsButton.requestFocus();
                equalsButton.setDefaultButton(true);
            });
        } catch (Exception e) {
            System.err.println("Error initializing JavaFXController: " + e.getMessage());
            Platform.exit();
        }
    }

    /**
     * Sets up the responsive layout for the calculator.
     * If the window width exceeds the threshold, the side panel is shown.
     */
    private void setupResponsiveLayout() {
        sidePanel.managedProperty().bind(sidePanel.visibleProperty());
        sidePanel.setVisible(false);

        root.sceneProperty().addListener((obsS, oldScene, newScene) -> {
            if (newScene != null) {
                boolean shouldShow = newScene.getWidth() > RESPONSIVE_THRESHOLD;
                sidePanel.setVisible(shouldShow);
                setSidePanelVisibility(shouldShow);

                // Add listener to the scene width property to adjust the side panel visibility based on scene (window) width
                newScene.widthProperty().addListener((obsW, oldW, newW) -> {
                    boolean showPanel = newW.doubleValue() > RESPONSIVE_THRESHOLD;
                    sidePanel.setVisible(showPanel);
                    setSidePanelVisibility(showPanel);
                });
            }
        });
    }

    /**
     * Adjusts the layout of the calculator based on whether the side panel should be visible or not.
     * @param sidePanelVisible True if the side panel should be visible, false otherwise.
     */
    private void setSidePanelVisibility(boolean sidePanelVisible) {
        // Sets the exact width of the side panel when visible versus hidden
        AnchorPane.setRightAnchor(calculatorRoot, sidePanelVisible ? 245.0 : 0.0);
    }

    /**
     * Validates if the given string is valid calculator input.
     * Accepts numeric values, decimal points, and basic operators.
     * 
     * @param input The string to validate
     * @return true if the input is valid for the calculator, false otherwise
     */
    private boolean isValidCalculatorInput(String input) {
        // Allow only digits, decimal points, and basic operators
        return input.matches("[0-9.+\\-*/\\s]*");
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

            case "plusButton" -> processOperatorInput("+");
            case "minusButton" -> processOperatorInput("-");
            case "multiplyButton" -> processOperatorInput("*");
            case "divideButton" -> processOperatorInput("/");
            
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

            case "togglePercentButton" -> togglePercentFormat();
            case "squareButton" -> applyUnaryOperation("square");
            case "squareRootButton" -> applyUnaryOperation("sqrt");
            case "reciprocalButton" -> applyUnaryOperation("reciprocal");
            case "negateButton" -> applyUnaryOperation("negate");

            case "historyButton" -> showHistoryPanel();
            case "memoryButton" -> showMemoryPanel();
            case "mcButton" -> memoryList.clear();
            case "mrButton" -> recallMemory();
            case "msButton" -> memoryList.add(0, mainDisplay.getText());

            case "contextMenuButton" -> handleMenuToggle();

            default -> System.err.println("Unhandled button ID: " + id);
        }
    }

    // TODO: add functionality to allow other keyboard layouts
    /**
     * Handles key presses for keyboard input.
     * Assumes keyboard layout is US-QWERTY.
     * Maps keys to calculator functions and performs the corresponding action.
     * Properly handles shift-modified keys for symbols like +, *, etc.
     * 
     * @param event The KeyEvent triggered by the key press
     */
    private void handleKeyPress(KeyEvent event) {
        KeyCode code = event.getCode();
        boolean shiftDown = event.isShiftDown();
        
        // Handle digit keys (not affected by shift)
        if (code.isDigitKey() && !shiftDown) {
            String name = code.toString(); // e.g., "DIGIT0", "NUMPAD3"
            if (name.startsWith("DIGIT") || name.startsWith("NUMPAD")) {
                String digit = name.replaceAll("[^0-9]", ""); // extract the digit
                appendToInput(digit);
                event.consume();
                return;
            }
        }
        
        
        // Handle keys that behave differently with shift
        switch (code) {
            case DIGIT8 -> {
                if (shiftDown) {
                    // * (asterisk) - multiply
                    processOperatorInput("*");
                    event.consume();
                }
            }
            case EQUALS -> {
                if (shiftDown) {
                    // + (plus sign on equals key with shift)
                    processOperatorInput("+");
                    event.consume();
                } else {
                    // = (equals sign without shift)
                    evaluateExpression();
                    event.consume();
                }
            }
            case PLUS -> {
                // + (dedicated plus key)
                processOperatorInput("+");
                event.consume();
            }
            case MINUS -> {
                // - (minus/hyphen)
                processOperatorInput("-");
                event.consume();
            }
            case SLASH -> {
                // / (forward slash)
                processOperatorInput("/");
                event.consume();
            }
            case DIGIT5 -> {
                if (shiftDown) {
                    // % (percent sign, SHIFT+5)
                    togglePercentFormat();
                    event.consume();
                }
            }
            case DIGIT6 -> {
                if (shiftDown) {
                    // ^ (caret, SHIFT+6)
                    // TODO: Handle exponentiation in Scientific mode
                    event.consume();
                }
            }
            case DIGIT9 -> {
                if (shiftDown) {
                    // ( (left parenthesis)
                    // TODO: Handle parentheses in Scientific mode
                    event.consume();
                }
            }
            case DIGIT0 -> {
                if (shiftDown) {
                    // ) (right parenthesis)
                    // TODO: Handle parentheses in Scientific mode
                    event.consume();
                }
            }
            case R -> {
                // r/R key for reciprocal
                applyUnaryOperation("reciprocal");
                event.consume();
            }
            case S -> {
                // s/S key for square
                applyUnaryOperation("square");
                event.consume();
            }
            case Q -> {
                // q/Q key for square root
                applyUnaryOperation("sqrt");
                event.consume();
            }
            case ENTER -> {
                // Enter key for equals
                evaluateExpression();
                event.consume();
            }
            case BACK_SPACE -> {
                // Backspace for delete
                backspace();
                event.consume();
            }
            case DELETE -> {
                // Delete key for clear entry
                clearEntry();
                event.consume();
            }
            case ESCAPE -> {
                // Escape for clear all
                clear();
                event.consume();
            }
            case PERIOD, DECIMAL -> {
                // Period/decimal point
                appendToInput(".");
                event.consume();
            }
            default -> {
                // No action for unhandled keys
            }
        }
        
        // Process remaining text input for special characters
        // This is needed because some keyboard layouts produce different character codes
        String keyText = event.getText();
        if (!keyText.isEmpty() && !event.isConsumed()) {
            switch (keyText) {
                case "+" -> { processOperatorInput("+"); event.consume(); }
                case "-" -> { processOperatorInput("-"); event.consume(); }
                case "*" -> { processOperatorInput("*"); event.consume(); }
                case "/" -> { processOperatorInput("/"); event.consume(); }
                case "=" -> { evaluateExpression(); event.consume(); }
                case "%" -> { togglePercentFormat(); event.consume(); }
                case "." -> { appendToInput("."); event.consume(); }
            }
        }
    }

    /**
     * Appends a value to the current input in the main display.
     * @param value The value to append (e.g., a digit or decimal point)
     */
    private void appendToInput(String value) {
        // If an operation was just performed or we're starting a new input,
        // clear the current input
        if (startNewInput && !hasJustPerformedOperation) {
            expressionBuilder.setLength(0);
            expressionDisplay.setText("");
        }
        if (startNewInput || hasJustPerformedOperation) {
            currentInputBuilder.setLength(0);
            startNewInput = false;
            hasJustPerformedOperation = false;
            resetPercentCycle();

            // Set the display type to "Input" as mainDisplay is now showing an input
            setDisplayTypeLabel(true);
        }
        
        // Handle special case for decimal point
        if (value.equals(".") && currentInputBuilder.toString().contains(".")) {
            return; // Prevent multiple decimal points
        }
        
        // Handle special case for zero at the beginning
        if (currentInputBuilder.toString().equals("0") && !value.equals(".")) {
            currentInputBuilder.setLength(0);
        }
        
        // Append the value to the current input
        currentInputBuilder.append(value);
        
        // Update the main display with the current input
        mainDisplay.setText(currentInputBuilder.toString());
    }
    
    /**
     * Handles operator input (+, -, *, /) and performs the appropriate operation.
     * Converts percentage values to decimal before performing operations.
     * 
     * @param operator The operator to apply
     */
    private void processOperatorInput(String operator) {
        // Convert percentage to decimal if needed
        if (mainDisplay.getText().endsWith("%")) {
            try {
                double value = parseDisplayValue(mainDisplay.getText());
                String decimalStr = formatNumber(value);
                mainDisplay.setText(decimalStr);
                currentInputBuilder.setLength(0);
                currentInputBuilder.append(decimalStr);
            } catch (NumberFormatException e) {
                mainDisplay.setText("Error");
                System.err.println("Error converting percentage: " + e.getMessage());
                return;
            }
        }
        
        // If there's a current input, add it to the expression
        if (!hasJustPerformedOperation) {
            if (currentInputBuilder.length() > 0) {
                expressionBuilder.append(currentInputBuilder);
                jsExpressionBuilder.append(currentInputBuilder);
            } else {
                // If no current input, append 0 before operator
                expressionBuilder.append("0");
                jsExpressionBuilder.append("0");
            }
        }
        
        // Close any pending operations
        while (unclosedParenthesesCount > 0) {
            expressionBuilder.append(")");
            jsExpressionBuilder.append(")");
            unclosedParenthesesCount--;
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
        expressionBuilder.append(" ").append(operator).append(" ");
        jsExpressionBuilder.append(operator);
        
        // Update the expression display
        expressionDisplay.setText(expressionBuilder.toString());
        
        // Evaluate the expression so far and show the result in the main display
        evaluateAndUpdateDisplay();
        
        // Mark that an operation was just performed
        hasJustPerformedOperation = true;

        // Update the display type to "Result" to indicate the main display shows a result
        setDisplayTypeLabel(false);
    }

    /**
     * Evaluates the partial expression so far and updates the main display with the result.
     * Ensures all percentage values are properly converted to decimals.
     */
    private void evaluateAndUpdateDisplay() {
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
                    tempExpression = normalizeExpression(tempExpression);

                    Object result = engine.eval(tempExpression);
                    String resultStr = formatNumber(Double.parseDouble(result.toString()));

                    mainDisplay.setText(resultStr);
                    
                    // Store the result as the current input for the next operation
                    currentInputBuilder.setLength(0);
                    currentInputBuilder.append(resultStr);

                    // Update the display type to "Result" to indicate the main display shows a result
                    setDisplayTypeLabel(false);
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
            togglePercentFormat();
        }
        
        // If there's a current input and an operation wasn't just performed,
        // add it to the expression
        if (currentInputBuilder.length() > 0 && !hasJustPerformedOperation) {
            expressionBuilder.append(currentInputBuilder);
            jsExpressionBuilder.append(currentInputBuilder);
        }
        
        // Close any pending operations
        while (unclosedParenthesesCount > 0) {
            expressionBuilder.append(")");
            jsExpressionBuilder.append(")");
            unclosedParenthesesCount--;
        }

        // Reset pending unary operation
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

                // Normalize the expression to fix syntax issues like double negatives
                jsExpressionStr = normalizeExpression(jsExpressionStr);
                
                // Use jsExpressionStr for evaluation
                Object result = engine.eval(jsExpressionStr);
                String resultStr = formatNumber(Double.parseDouble(result.toString()));
                
                // Add to history
                historyList.add(0, displayExpressionStr + " = " + resultStr);
                
                // Show the result in the main display
                mainDisplay.setText(resultStr);
                if (!expressionDisplay.getText().endsWith(" =")) {
                    expressionDisplay.setText(displayExpressionStr + " =");
                }

                // Update the display type to "Result" to indicate the main display shows a result
                setDisplayTypeLabel(false);
                
                // Reset state
                expressionBuilder.setLength(0);
                jsExpressionBuilder.setLength(0);
                currentInputBuilder.setLength(0);
                currentInputBuilder.append(resultStr);
                startNewInput = true;
                hasJustPerformedOperation = false;
                pendingUnaryOperation = null;
                unclosedParenthesesCount = 0;
            } catch (ScriptException | NumberFormatException e) {
                mainDisplay.setText("Error");
                System.err.println("Expression error: " + e.getMessage());
                resetCalculator();
            }
        }
    }

    /**
     * Normalizes an expression to ensure it's compatible with JavaScript evaluation.
     * Specifically handles cases like double negatives (--) which cause syntax errors.
     * 
     * @param expression The expression to normalize
     * @return A JavaScript-compatible expression
     */
    private String normalizeExpression(String expression) {
        // Replace double negatives with a plus
        String normalized = expression.replaceAll("--", "+");
        
        // Replace combinations like "+-" or "-+" with a single minus
        normalized = normalized.replaceAll("\\+-", "-");
        normalized = normalized.replaceAll("-\\+", "-");
        
        return normalized;
    }

    /**
     * Clears the calculator state and resets the displays.
     * Alias for resetCalculator() method.
     */
    private void clear() {
        resetCalculator();
    }
    
    /**
     * Resets the calculator state and clears all displays.
     */
    private void resetCalculator() {
        expressionBuilder.setLength(0);
        jsExpressionBuilder.setLength(0);
        currentInputBuilder.setLength(0);
        mainDisplay.setText("0");
        expressionDisplay.setText("");
        startNewInput = true;
        hasJustPerformedOperation = false;
        pendingUnaryOperation = null;
        unclosedParenthesesCount = 0;

        // Set the display type to "Input" as mainDisplay is now showing an input
        setDisplayTypeLabel(true);
    }

    /**
     * Clears only the current entry/input in the calculator.
     */
    private void clearEntry() {
        // Clear only the current entry/input
        currentInputBuilder.setLength(0);
        mainDisplay.setText("0");
        startNewInput = true;
    }

    /**
     * Allows the user to delete the last character of the current input and updates mainDisplay.
     * Only allows backspace on the current input, not on results.
     */
    private void backspace() {
        // Only allow backspace on the current input, not on results
        if (!startNewInput && !hasJustPerformedOperation && currentInputBuilder.length() > 0) {
            currentInputBuilder.deleteCharAt(currentInputBuilder.length() - 1);
            mainDisplay.setText(currentInputBuilder.length() > 0 ? currentInputBuilder.toString() : "0");
        }
    }

    /**
     * Applies a unary operation (square, square root, reciprocal, negate) to the current value.
     * Converts percentage values to decimal before performing operations.
     * Standard Calculator does not allow nesting of unary or binary operations as there isn't a way to close parantheses.
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
                togglePercentFormat();
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
                hasJustPerformedOperation = false; // Allow next digit input to replace display
                
                // Set the display type to "Input" as mainDisplay is now showing an input
                setDisplayTypeLabel(true);
                
                // Store operation info for later completion
                pendingUnaryOperation = type;
                unclosedParenthesesCount++;
                
                return;
            } else if (pendingUnaryOperation != null && hasJustPerformedOperation) {
                // We're nesting operations (e.g., sqrt(sqrt(...)))
                
                // Update the expression display with the operation prefix
                expressionBuilder.append(operationPrefix);
                jsExpressionBuilder.append(jsOperationPrefix);
                
                // Update the expression display
                expressionDisplay.setText(expressionBuilder.toString());
                
                // We don't modify the main display - it should still show the previous result
                
                // Set flags to prepare for the next input
                startNewInput = true;
                hasJustPerformedOperation = false; // Allow next digit input to replace display
                
                // Set the display type to "Input" as mainDisplay is now showing an input
                setDisplayTypeLabel(true);
                
                // Store operation info for later completion
                pendingUnaryOperation = type;
                unclosedParenthesesCount++;
                
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
            
            setDisplayTypeLabel(false);
            
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
            currentInputBuilder.setLength(0);
            currentInputBuilder.append(formatted);
            
            // Mark that we should start a new input next
            startNewInput = true;
            hasJustPerformedOperation = true;
            
        } catch (NumberFormatException e) {
            mainDisplay.setText("Error");
            System.err.println("Error in unary operation: " + e.getMessage());
        }
    }

    /**
     * Recalls the first value from memory and updates the main display.
     * If the memory list is empty, does nothing.
     */
    private void recallMemory() {
        if (!memoryList.isEmpty()) {
            String memoryValue = memoryList.get(0);
            mainDisplay.setText(memoryValue);
            
            // Update current input with recalled value
            currentInputBuilder.setLength(0);
            currentInputBuilder.append(memoryValue);
            
            // Mark that we should continue with this input
            startNewInput = false;
            hasJustPerformedOperation = false;

            // Set the display type to "Input" as mainDisplay is now showing an input
            setDisplayTypeLabel(true);
        }
    }

    /**
     * Sets the list in the side panel to the history list and updates the button styles to indicate the active list.
     */
    private void showHistoryPanel() {
        sidePanelListView.setItems(historyList);
        historyButton.setStyle("-fx-background-color: #27c0c5");
        memoryButton.setStyle("-fx-background-color: #bdbdbd");
    }

    /**
     * Sets the list in the side panel to the memory list and updates the button styles to indicate the active list.
     */
    private void showMemoryPanel() {
        sidePanelListView.setItems(memoryList);
        historyButton.setStyle("-fx-background-color: #bdbdbd");
        memoryButton.setStyle("-fx-background-color: #27c0c5");
    }

    // TODO: Test edge cases for formatting numbers
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
            return scientificFormat.format(value);
        }
        
        // Use decimal formatter for regular numbers
        return decimalFormat.format(value);
    }

    /**
     * Handles percentage operations with context-aware behavior.
     * When applied to input: Simply appends % symbol (without multiplication)
     * When applied to result: Multiplies by 100 and adds % symbol
     * Allows toggling between percentage and decimal representation multiple times.
     */
    private void togglePercentFormat() {
        String currentText = mainDisplay.getText();
        String displayState = displayTypeLabel.getText();
        
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
                currentInputBuilder.setLength(0);
                currentInputBuilder.append(formatted);
                
                // When converting from percent to decimal, startNewInput should be false
                startNewInput = false;
                hasJustPerformedOperation = false;
                
                // Mark that this value has been through percent conversion
                isPercentFormatActive = true;
            } else {
                // Convert from decimal to percentage
                double value = Double.parseDouble(currentText);
                double percentValue;
                
                // If in input mode and not previously percented, use direct percentage
                // Otherwise multiply by 100 for proper percentage representation
                if ("Input".equals(displayState) && !isPercentFormatActive) {
                    percentValue = value;  // Direct percentage without multiplication
                } else {
                    percentValue = value * 100.0;  // Standard percentage conversion
                }
                
                // Format the percentage value
                String formatted;
                if (percentValue == (long) percentValue) {
                    formatted = String.format("%d%%", (long) percentValue);
                } else {
                    formatted = decimalFormat.format(percentValue) + "%";
                }
                
                mainDisplay.setText(formatted);
                
                // Update current input with the percentage value
                currentInputBuilder.setLength(0);
                currentInputBuilder.append(formatted);
                
                // When converting to percent, startNewInput should be true
                startNewInput = true;
                hasJustPerformedOperation = false;
            }
        } catch (NumberFormatException e) {
            mainDisplay.setText("Error");
            System.err.println("Error in percent toggle: " + e.getMessage());
        }
    }

    // Resets the percent cycle state (applying percent and de-applying percent)
    // Add this to any method that changes the input or starts a new calculation
    private void resetPercentCycle() {
        isPercentFormatActive = false;
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

    /**
     * Handles the toggle of the context menu visibility.
     * This method is called when the menu button is clicked.
     */
    @FXML
    private void handleMenuToggle() {
        isContextMenuVisible = !isContextMenuVisible;
        contextMenu.setVisible(isContextMenuVisible);
    }

    /**
     * Loads the standard calculator layout.
     * This is the default layout and is loaded when the application starts.
     */
    @FXML private void loadStandard() { swapLayout("/app/CalculatorLayout.fxml"); }

    /**
     * Loads the scientific calculator layout.
     * This method is not yet implemented and currently prints a message to the console.
     */
    @FXML private void loadScientific() { swapLayout("/app/ScientificCalculatorLayout.fxml"); }

    private void swapLayout(String fxmlName) {
        /*   
        try {
            Parent newRoot = FXMLLoader.load(
                    getClass().getResource(fxmlName)
            );
            Scene scene = root.getScene();
            scene.setRoot(newRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        // Cannot swap layout yet, as not all elements in the FXML file are implemented
        System.out.println("Layout swapping not supported yet");
    }

    /**
     * Toggles between light and dark theme for the calculator.
     * Uses CSS class-based approach to switch themes defined in SCSS.
     */
    @FXML
    private void handleThemeToggle() {
        Scene scene = root.getScene();
        ObservableList<String> sheets = scene.getStylesheets();
        sheets.clear();
        if (darkMode) {
            sheets.add(getClass().getResource("/styles/style-light.css")
                    .toExternalForm());
        } else {
            sheets.add(getClass().getResource("/styles/style.css")
                    .toExternalForm());
        }
        darkMode = !darkMode;
    }

    /**
     * Adds the current displayed value to memory.
     * If the memory list is empty, initializes it with the current value.
     */
    @FXML
    private void memoryAdd() {
        try {
            double displayed = parseDisplayValue(mainDisplay.getText());
            double mem = memoryList.isEmpty() ? 0 : parseDisplayValue(memoryList.get(0));
            double updated = mem + displayed;

            String fmt = formatNumber(updated);
            if (memoryList.isEmpty()) {
                memoryList.add(0, fmt);
            } else {
                memoryList.set(0, fmt);
            }           
        } catch (Exception e) {
            System.err.println("Error trying to add to memory: " + e.getMessage());
        }
    }

    /**
     * Subtracts the current displayed value from memory.
     * If the memory list is empty, initializes it with the negative of the current value.
     */
    @FXML
    private void memorySubtract() {
        try {
            double displayed = parseDisplayValue(mainDisplay.getText());
            double mem = memoryList.isEmpty() ? 0 : parseDisplayValue(memoryList.get(0));
            double updated = mem - displayed;
            String fmt = formatNumber(updated);
            
            if (memoryList.isEmpty()) { 
                memoryList.add(0, fmt);
            } else {
                memoryList.set(0, fmt);
            }
        } catch (Exception e) {
            System.err.println("Error trying to add to memory: " + e.getMessage());
        }
    }

    @FXML
    private void handleMemoryAdd(ActionEvent event){
        memoryAdd();
    }

    @FXML
    private void handleMemoryMinus(ActionEvent event){
        memorySubtract();
    }

    /**
     * Sets the display type label to indicate whether the main display is showing an input or a result.
     * This is used to provide visual feedback to the user about the current state of the calculator.
     * Used in percent toggle.
     */
    private void setDisplayTypeLabel(boolean isInput) {
        if (isInput) {
            // Update the display type to "Input" to indicate the main display shows an input
            displayTypeLabel.setText("Input");
            displayTypeLabel.setStyle("-fx-text-fill: rgba(238, 255, 0, 0.6);");
        } else {
            // Update the display type to "Result" to indicate the main display shows a result
            displayTypeLabel.setText("Result");
            displayTypeLabel.setStyle("-fx-text-fill: rgba(0, 245, 20, 0.6);");
        }
    }

    /**
     * Handles the clear button action for the history/memory panel.
     * Clears either history list or memory list based on which panel is currently active.
     * Updates the tooltip to reflect the current context.
     */
    @FXML
    private void handleClearHistoryMemory() {
        // Get the current panel state
        boolean isHistoryActive = sidePanelListView.getItems() == historyList;

        if (isHistoryActive) {
            // Clear history list
            if (!historyList.isEmpty()) {
                historyList.clear();
            }
        } else {
            // Clear memory list
            if (!memoryList.isEmpty()) {
                memoryList.clear();
            }
        }
    }

    /**
     * Updates the clear button tooltip based on the active panel.
     * Call this method when switching between history and memory panels.
     */
    private void updateClearButtonTooltip() {
        boolean isHistoryActive = sidePanelListView.getItems() == historyList;
        clearTooltip.setText(isHistoryActive ? "Clear History" : "Clear Memory");
    }
}