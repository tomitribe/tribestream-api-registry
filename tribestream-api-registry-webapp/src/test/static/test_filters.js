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

describe('it tests our encodepath filter', function() {

    var $filter;

    beforeEach(function () {
        module('website-components');
        inject(function (_$filter_) {
            $filter = _$filter_;
        });
    });


    it('should convert root to root', function () {
        var filter = $filter('pathencode');
        expect(filter('/')).toBe('/');
    });

    it('should convert single path with braces to colon', function() {
        var filter = $filter('pathencode');
        expect(filter('/{foo}')).toBe('/:foo');
    });

    it('should convert mixed path with braces to colon', function() {
        var filter = $filter('pathencode');
        expect(filter('/{foo}/bar')).toBe('/:foo/bar');
    });

    it('should convert mixed path with trailing braces to colon', function() {
        var filter = $filter('pathencode');
        expect(filter('/foo/{bar}')).toBe('/foo/:bar');
    });

});