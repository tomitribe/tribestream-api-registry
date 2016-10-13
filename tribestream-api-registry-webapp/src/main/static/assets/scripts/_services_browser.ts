module services_browser {
    declare var Base64;

    // wrapper to browser features. useful in unit tests
    angular.module('website-browser', [])

        .factory('Base64', [function () {
            return Base64;
        }])

        .run(function () {
            // placeholder
        });
}
