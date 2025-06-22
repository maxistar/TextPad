const { remote } = require('webdriverio');

const capabilities = {
  platformName: 'Android',
  'appium:automationName': 'UiAutomator2',
//  'appium:deviceName': 'emulator-5554',
  'appium:appPackage': 'com.maxistar.textpad',
  'appium:appActivity': '.activities.EditorActivity',
  'appium:autoGrantPermissions': true,
};

const wdOpts = {
  hostname: process.env.APPIUM_HOST || 'localhost',
  port: parseInt(process.env.APPIUM_PORT, 10) || 4723,
  logLevel: 'info',
  capabilities,
};

async function runTest() {
const driver = await remote(wdOpts);
const el1 = await driver.$("id:com.maxistar.textpad:id/editText1");
await el1.addValue("some text");
const el2 = await driver.$("accessibility id:More options");
await el2.click();
const el3 = await driver.$("xpath://android.widget.TextView[@resource-id=\"com.maxistar.textpad:id/title\" and @text=\"Save\"]");
await el3.click();
const el4 = await driver.$("class name:android.widget.EditText");
await el4.clearValue();
await el4.addValue("somefilename.txt");
const el5 = await driver.$("id:android:id/button1");
await el5.click();

}

runTest().catch(console.error);