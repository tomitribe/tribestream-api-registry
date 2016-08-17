describe('it tests our custom password strength directive', function () {

    var $compile;
    var $rootScope;

    beforeEach(function () {
        module('website-components');
        module('tribe-main');

        inject(function (_$compile_) {
            $compile = _$compile_;
        });

        inject(function (_$rootScope_) {
            $rootScope = _$rootScope_;
        });
    });

    it('tests the tribeEditableBlock component', function () {
        $rootScope.myContent = "Lalala lilili.";
        var element = $compile([
            '<div data-tribe-editable-block data-content="myContent"></div>'
        ].join(''))($rootScope);
        $rootScope.$digest();
        expect(element.find('textarea').val()).toBe("Lalala lilili.");
        $rootScope.$apply(function () {
            $rootScope.myContent = "Lelele.";
        });
        expect(element.find('textarea').val()).toBe("Lelele.");
        expect(element.hasClass('editing')).toBe(false);
        element.triggerHandler('click');
        expect(element.hasClass('editing')).toBe(true);
        element.find('textarea').blur();
        expect(element.hasClass('editing')).toBe(false);
    });

});