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