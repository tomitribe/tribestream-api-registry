var karmaConfig = require('./karma.conf.js');
karmaConfig.autoWatch = true;
karmaConfig.singleRun = false;

module.exports = function (config) {
    karmaConfig.logLevel = config.LOG_INFO;
    config.set(karmaConfig);
};
