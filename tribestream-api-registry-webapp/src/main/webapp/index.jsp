<!DOCTYPE html>
<html id="ng-app" data-ng-app="tribe-main">
<%@ page contentType="text/html;charset=UTF-8" language="java" session="false" %>
<head>
    <title>tribestream registry</title>
    <script type="text/javascript">
        // doc base
        (function () {
            var getHrefList = function(hrefStr) {
                var hrefArr = hrefStr.split('/');
                hrefArr.shift(); // remove protocol
                hrefArr.shift(); // remove "//"
                hrefArr.shift(); // remove host and port (if any)
                return hrefArr;
            };
            var getNext = function(list) {
                if(!list.length) {
                    return null;
                }
                var last = list.pop();
                if(!last) {
                    last = getNext(list);
                }
                return last;
            };
            var getCleanDocLocation = function() {
                var result = document.location.href;
                if(document.location.hash) {
                    result =  result.substring(0, result.length - document.location.hash.length);
                }
                if(document.location.search) {
                    result =  result.substring(0, result.length - document.location.search.length);
                }
                return result;
            };
            var baseUrl = (function() {
                var docHref = getCleanDocLocation();
                var remoteHref = getHrefList('<%=request.getRequestURL()%>');
                var localHref = getHrefList(docHref);
                if('<%=request.getRequestURL()%>' === docHref || remoteHref.join('/') === localHref.join('/')) {
                    var ctxPath = '<%=request.getContextPath()%>/';
                    return ctxPath === '/' ? '/' : ctxPath;
                }
                var result = [];
                while(remoteHref.length) {
                    var lastRemote = getNext(remoteHref);
                    var lastLocal = getNext(localHref);
                    if(lastRemote !== lastLocal && lastLocal) {
                        result.push(lastLocal);
                        break;
                    }

                }
                result.unshift(document.location.host);
                return document.location.protocol + '//' + result.join('/') + '/';
            }());
            document.open();
            document.write("<base href='" + baseUrl + "' />");
            document.close();
        }());
    </script>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body>
<!--[if lt IE 9]>
<p class="browsehappy">You are using an <strong>outdated</strong> browser. Please <a href="http://outdatedbrowser.com/">upgrade your browser</a> to improve your experience.</p>
<![endif]-->
<div data-app-closable-messages></div>
<div data-ng-view class="app-body">
    <div class="app-loading"></div>
</div>
<!-- Use this instead of HtmlWebpackPlugin in order to include the charset  -->
<script type="text/javascript" src="app/polyfills.js" charset="utf-8"></script>
<script type="text/javascript" src="app/vendor.js" charset="utf-8"></script>
<script type="text/javascript" src="app/app.js" charset="utf-8"></script>
</body>
</html>
