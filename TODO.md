# Calculator TODO List

## History and Memory

- [ ] Finish memory button logic (M+, M-)
- [ ] Add a button for showing memory and history in condensed view. These buttons should dissapear when in expanded mode
- [ ] Add dedicated clear all memory and history button to its dedicated panel (The MC button technically does part of this, but isn't obvious)
- [x] Only add to history when equals is pressed
- [ ] Create custom button list for history and memory list

## Visual Changes

- [ ] Add dark/light mode toggle
- [ ] Change minimum height so that all buttons are always shown
- [ ] Make sure that expressions fit within history and expressionDisplay
- [ ] Scale button text with calculator size
- [ ] Scale side panel with calculator size
- [ ] Remove Input/Result label and replace with a colored border around mainDisplay

## Extra Features

- [x] Change text label above mainDisplay to show input versus result
- [ ] Create menu for switching to Scientific Calculator or other calculator types
- [ ] Implement new buttons on Scientific Calculator
- [ ] Improve number formatting (e.g., thousands separator, trailing zeros, scientific notation)
- [ ] Implement the ability to highlight answer
- [x] Implement copy answer feature
- [x] Implement paste input feature
- [x] Add keyboard input support

## Documentation

- [ ] JavaDoc everything
- [ ] Finish README.md for GitHub

## Fix Errors

- [ ] Negative button breaks if used on any other value except the first value
- [ ] Handle mathematical edge cases, such as dividing by 0
- [ ] Equals does not update expressionDisplay if the previous expressionDisplay includes only a value and equals sign (e.g., 2 =, 4 =)
- [ ] ExpressionDisplay does not clear when a new input starts after equals has been pressed
- [ ] M+ and M- buttons are not implemented
- [ ] Change percentage logic to append percent without multiplication if input, and append percent with multiplication if result

## Other

- [ ] Allow nesting of unary operators
- [ ] Refactor methods to be more understandable
- [ ] Compile .jar and .exe files
