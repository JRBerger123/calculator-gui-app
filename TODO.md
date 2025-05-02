# Calculator TODO List

## History and Memory

- [x] Finish memory button logic (M+, M-)
- [ ] Add a button for showing memory and history in condensed view. These buttons should dissapear when in expanded mode
- [ ] Add dedicated clear all memory and history button to its dedicated panel (The MC button technically does part of this, but isn't obvious)
- [x] Only add to history when equals is pressed
- [ ] Create custom button list for history and memory list

## Visual Changes

- [x] Add dark/light mode toggle
- [ ] Change minimum height so that all buttons are always shown
- [ ] Make sure that expressions fit within history and expressionDisplay
- [ ] Scale all text with calculator size
- [ ] Scale side panel with calculator size
- [ ] Remove Input/Result label and replace with a colored border around mainDisplay
- [ ] Change some button icons so they more accurately and clearly show what the button does
- [ ] Create style class(es) for disabled buttons that is applied when buttons cannot be used (e.g., some of the memory buttons when no memory is stored, when error occurs)
- [ ] Show user feedback that text was copy or pasted

## Extra Features

- [x] Change text label above mainDisplay to show input versus result
- [x] Create menu for switching to Scientific Calculator or other calculator types
- [ ] Implement new buttons that were added on the Scientific Calculator
- [ ] Improve number formatting (e.g., thousands separator, trailing zeros, scientific notation)
- [ ] Implement the ability to highlight a portion of the answer for copying or just because
- [x] Implement copy answer feature
- [x] Implement paste input feature
- [x] Add keyboard input support

## Documentation

- [ ] JavaDoc everything
- [ ] Finish README.md for GitHub

## Fix Errors

- [ ] Negative button breaks if used on any other value except the first value
- [ ] Handle mathematical edge cases, such as dividing by 0
- [ ] Implement error handling involving displaying the error type on the mainDisplay
- [ ] Equals does not update expressionDisplay if the previous expressionDisplay includes only a value and equals sign (e.g., 2 =, 4 =)
- [ ] ExpressionDisplay does not clear when a new input starts after equals has been pressed
- [ ] M+ and M- buttons are not implemented
- [x] Change percentage logic to append percent without multiplication if input, and append percent with multiplication if result

## Other

- [ ] Allow nesting of unary operators
- [ ] Refactor methods to be more understandable (e.g., create better names for methods)
- [ ] Create UML Diagram or equivalent showing highlevel of how methods and files connect with each other
- [ ] Compile .jar and .exe files
