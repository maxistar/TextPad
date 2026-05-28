module.exports = {
  default: {
    require: [
      'dist/src/world/CustomWorld.js',
      'dist/src/support/hooks.js',
      'dist/src/steps/**/*.js',
    ],
    format: ['progress', 'html:artifacts/cucumber-report.html'],
    parallel: 1,
  },
}
