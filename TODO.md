# Calculator TODO List

## History and Memory

- [ ] Finish memory button logic (M+, M-)
- [ ] Show memory and history in condensed view
- [ ] Add clear all memory and history button
- [x] Only add to history when equals is pressed
- [ ] Create custom button list for history and memory list

## Visual Changes

- [ ] Add dark mode toggle
- [ ] Change minimum height so that all buttons are always shown
- [ ] Make sure that expressions fit within history and expressionDisplay
- [ ] Scale button text with calculator size
- [ ] Scale side panel with calculator size
- [ ] Change Input and Result label to a colored border around mainDisplay

## Extra Features

- [x] Change text label above mainDisplay to show input versus result
- [ ] Implement Scientific Calculator
- [ ] Create menu for switching to Scientific Calculator or other calculator types
- [ ] Improve number formatting (e.g., thousands separator, trailing zeros, scientific notation)
- [ ] Add keyboard input support

## Documentation

- [ ] JavaDoc everything
- [ ] Finish README.md for GitHub

## Fix Errors

- [ ] Negative button breaks if used on any other value except the first value
- [ ] Handle mathematical edge cases, such as dividing by 0
- [ ] Equals does not update expressionDisplay if the previous expressionDisplay includes only a value and equals sign (e.g., 2 =, 4 =)
- [ ] ExpressionDisplay does not clear when a new input starts after equals has been pressed
- [ ] M+ and M- buttons are not implemented

## Other

- [ ] Allow nesting of unary operators
- [ ] Refactor methods to be more understandable
- [ ] Compile .jar and .exe files
