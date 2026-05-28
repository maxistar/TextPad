Feature: Save a text file

  Scenario: Saving a new text file persists its contents
    Given the TextPad app is open and ready for editing
    When I enter the text "TextPad E2E smoke text"
    And I save the text as a generated test file
    Then the editor remains ready after saving
    And the saved test file contains "TextPad E2E smoke text"
