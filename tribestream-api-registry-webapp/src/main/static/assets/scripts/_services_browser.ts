///<reference path="../../bower_components/DefinitelyTyped/angularjs/angular.d.ts"/>
///<reference path="../../bower_components/DefinitelyTyped/underscore/underscore.d.ts"/>

module services {
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
