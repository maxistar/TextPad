## ADDED Requirements

### Requirement: Persian is available as an interface language
The app SHALL offer Persian (Farsi) as a selectable interface language and SHALL load Persian string resources when the user selects it.

#### Scenario: User selects Persian
- **WHEN** the user chooses `فارسی` in the language setting
- **THEN** the app applies the `fa` locale and displays the Persian translations available in its resources

### Requirement: Persian interface supports right-to-left presentation
When Persian is selected, the app SHALL present the settings and editor screens with right-to-left text and layout direction appropriate for Persian.

#### Scenario: Persian settings and editor screens
- **WHEN** the app is configured to use Persian on a supported Android device
- **THEN** settings labels and editor screen content render in Persian with correct right-to-left direction and no clipped or incorrectly ordered controls
