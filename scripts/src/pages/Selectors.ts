export type Selector =
  | { kind: 'accessibilityId'; value: string }
  | { kind: 'className'; value: string }
  | { kind: 'id'; value: string }
  | { kind: 'xpath'; value: string }
  | { kind: 'text'; value: string }

const id = (value: string): Selector => ({ kind: 'id', value })
const text = (value: string): Selector => ({ kind: 'text', value })
const xpath = (value: string): Selector => ({ kind: 'xpath', value })

export const selectors = {
  editorInput: id('com.maxistar.textpad:id/editText1'),
  overflowMenu: { kind: 'accessibilityId', value: 'More options' } as Selector,
  saveAction: xpath(
    '//*[@resource-id="com.maxistar.textpad:id/title" and @text="Save"]',
  ),
  androidFileNameInput: { kind: 'className', value: 'android.widget.EditText' } as Selector,
  androidConfirmButton: id('android:id/button1'),
  legacyFileNameInput: id('com.maxistar.textpad:id/fdEditTextFile'),
  legacyCreateButton: id('com.maxistar.textpad:id/fdButtonCreate'),
  fileWrittenToast: text('File written'),
}
