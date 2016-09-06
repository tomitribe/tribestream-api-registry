describe('it tests our tribeHtml custom filter', () => {
    let expect = chai.expect;

    var tribeHtml;

    beforeEach((done) => {
        module('website-components', ($provide) => {
            $provide.value('$sce', {
                trustAsHtml: function (input) {
                    return input + '!!';
                }
            });
        });
        inject((function (_$filter_) {
            tribeHtml = _$filter_('tribeHtml');
        }));
        done();
    });

    it('should convert to html accordingly', function () {
        expect(tribeHtml('<i></i>')).to.equal('<i></i>!!');
    });

});

describe('it tests our encodepath filter', () => {
    let expect = chai.expect;
    var filter;

    beforeEach(() => module('website-components'));

    beforeEach(() => inject(function (_$filter_) {
        filter = _$filter_('pathencode');
    }));

    it('should convert root to root', () => {
        expect(filter('/')).to.equal('/');
    });

    it('should convert single path with braces to colon', () => {
        expect(filter('/{foo}')).to.equal('/:foo');
    });

    it('should convert mixed path with braces to colon', () => {
        expect(filter('/{foo}/bar')).to.equal('/:foo/bar');
    });

    it('should convert mixed path with trailing braces to colon', () => {
        expect(filter('/foo/{bar}')).to.equal('/foo/:bar');
    });

});