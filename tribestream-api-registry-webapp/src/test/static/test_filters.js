describe('it tests our tribeHtml custom filter', function () {

    var mockSce = {
        trustAsHtml: function (input) {
            return input + '!!';
        }
    };
    var $filter;

    beforeEach(function () {
        module('website-components', function ($provide) {
            $provide.value('$sce', mockSce);
        });

        inject(function (_$filter_) {
            $filter = _$filter_;
        });
    });

    it('should convert to html accordingly', function () {
        var filter = $filter('tribeHtml');
        expect(filter('<i></i>')).toBe('<i></i>!!');
    });

});