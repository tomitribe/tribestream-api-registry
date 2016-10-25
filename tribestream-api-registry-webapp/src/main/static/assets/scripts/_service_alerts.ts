angular.module('tribe-alerts', [])

    .factory('tribeErrorHandlerService', ['systemMessagesService', '$log', function (systemMessagesService, $log) {
        return {
            ensureErrorHandler: function (originalCallback) {
                if (originalCallback) {
                    return originalCallback;
                }
                return function (evn) {
                    if (!evn.statusText || evn.statusText === '') {
                        systemMessagesService.error('There is something wrong with your connection.');
                        $log.error(evn);
                    }
                    systemMessagesService.error(evn.statusText);
                };
            }
        };
    }])

    .factory('systemMessagesService', [function () {
        var listeners = [];
        var messages = [];
        var addMessage = function (msgType, msgText, timeout) {
            var msgObj = {
                msgType: msgType,
                msgText: msgText,
                msgTimeout: timeout
            };
            triggerUpdateEvent(msgObj);
        };
        var triggerUpdateEvent = function (newMessage?) {
            if (newMessage) {
                messages.push(newMessage);
            }
            var cloneMessages = _.clone(messages);
            _.each(listeners, function (listener) {
                listener.onMessage(cloneMessages, newMessage);
            });
        };
        return {
            addListener: function (listener) {
                listeners = _.union([listener], listeners);
            },
            removeListener: function (listener) {
                listeners = _.reject(listeners, function (item) {
                    return item === listener;
                });
            },
            getMessages: function () {
                return _.clone(messages);
            },
            info: function (msg, timeout) {
                addMessage('info', msg, timeout);
            },
            warn: function (msg, timeout) {
                addMessage('warn', msg, timeout);
            },
            error: function (msg, timeout) {
                addMessage('error', msg, timeout);
            },
            setMessageRead: function (msg) {
                msg['read'] = true;
                triggerUpdateEvent();
            }
        };
    }])

    .run(function () {
        // placeholder
    });