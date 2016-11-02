Error.stackTraceLimit = Infinity;

require("./assets/scripts/polyfills.ts");
require("./assets/scripts/vendor.ts");
require("./bower_components/angular-mocks/angular-mocks.js");

var appContext = require.context('./assets', true, /\.spec\.ts/);

appContext.keys().forEach(appContext);
