let controller = ($scope,
                  srv, // this is tribeEndpointsService
                  tribeFilterService, // this is tribeFilterService
                  $timeout,
                  $filter,
                  $log,
                  systemMessagesService, // this is systemMessagesService
                  tribeLinkHeaderService, // this is tribeLinkHeaderService
                  $q,
                  savefunction) => {
    $scope.selected = [];
    $scope.showDiff = false;

    $scope.onHistorySelect = item => {
        $scope.showDiff = false;
        $scope.mergeWidget = undefined;

        if (item.$ui && item.$ui.selected) {
            $scope.selected.push(item);
        } else {
            $scope.selected = $scope.selected.filter(i => i != item);
        }
    };
    $scope.saveMerge = () => {
        // update new operation with the json content
        $scope.ref.operation = JSON.parse($scope['valueA']);

        savefunction();
    };
    $scope.doDiff = () => {
        $scope.showDiff = !$scope.showDiff;

        if ($scope.showDiff) {
            $q.all($scope.selected.map(item => srv.getHistoricItem(item).promise()))
                .then(results => {
                    $scope.ref = results[0]['data'];
                    $timeout(() => $scope.$apply(() => {
                        $scope['valueA'] = JSON.stringify(JSON.parse(JSON.stringify(results[0]['data'])), undefined, 2);
                        $scope['titleA'] = `Rev ${$scope.selected[0]['revisionId']} - ${$filter('date')($scope.selected[0]['timestamp'], 'medium')}`;
                        $scope['valueB'] = JSON.stringify(JSON.parse(JSON.stringify(results[1]['data'])), undefined, 2);
                        $scope['titleB'] = `Rev ${$scope.selected[1]['revisionId']} - ${$filter('date')($scope.selected[1]['timestamp'], 'medium')}`;
                    }));
                });
        }
    };
};

exports.controllerEndpoint = ($scope, srv, tribeFilterService, $timeout, $filter, $log, systemMessagesService, tribeLinkHeaderService, $q) => {
    let savefunction = () => {
        srv.saveEndpoint($scope.endpointLink, {
            // Cannot simply send the endpoint object because it's polluted with errors and expectedValues
            httpMethod: $scope.ref.httpMethod,
            path: $scope.ref.path,
            operation: $scope.ref.operation
        }).then(
            function (saveResponse) {
                $scope.updateEndpoint(saveResponse.data);
                systemMessagesService.info("Saved details! " + saveResponse.status);
            }
        );
    };
    controller.apply({}, [$scope, srv, tribeFilterService, $timeout, $filter, $log, systemMessagesService, tribeLinkHeaderService, $q, savefunction]);
};

exports.controllerApplication = ($scope, srv, tribeFilterService, $timeout, $filter, $log, systemMessagesService, tribeLinkHeaderService, $q) => {
    let savefunction = () => {
        let historic = JSON.parse($scope['valueA']);
        $scope.swagger.info.title = historic['swagger'].info.title;
        $scope.swagger.info.version = historic['swagger'].info.version;
        $scope.swagger.info.description = historic['swagger'].info.description;
        srv.saveApplication($scope.applicationLink, $scope.swagger).then(
            function (saveResponse) {
                systemMessagesService.info("Saved details! " + saveResponse.status);
            }
        );
    };
    controller.apply({}, [$scope, srv, tribeFilterService, $timeout, $filter, $log, systemMessagesService, tribeLinkHeaderService, $q, savefunction]);
};
