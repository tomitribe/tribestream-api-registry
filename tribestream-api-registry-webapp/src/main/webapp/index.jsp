<!DOCTYPE html>
<html id="ng-app" data-ng-app="tribe-main"><%@ page contentType="text/html;charset=UTF-8" language="java" %>
<head><title>tribestream registry</title>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge;"/>
    <script type="text/javascript">
        // doc base
        (function () {
            var contextPath = '<%=request.getContextPath()%>';
            var result = '';
            if (document.location.href === '<%=request.getRequestURL()%>') {
                if (document.location.port) {
                    result = "//" + document.location.hostname + ":" + document.location.port + contextPath + "/";
                } else {
                    result = "//" + document.location.hostname + contextPath + "/";
                }
            } else {
                var reqUrl = '<%=request.getRequestURL()%>'
                        .replace(/^http:/, '')
                        .replace(/^https:/, '')
                        .replace(/^\/\//, '')
                        .replace(/^[^\/]*/, '')
                        .replace(new RegExp('^' + contextPath, "i"), '');
                var baseUrl = document.location.pathname.replace(new RegExp(reqUrl + '$', 'i'), '');
                if (document.location.port) {
                    result = "//" + document.location.hostname + ":" + document.location.port + baseUrl + "/";
                } else {
                    result = "//" + document.location.hostname + baseUrl + "/";
                }
            }
            document.write("<base href='" + window.location.protocol + result + "' />");
        }());
    </script>
    <link rel="stylesheet" href="app/third-party/styles/_.css"/>
    <link rel="stylesheet" href="app/styles/_.css"/>
    <link rel="icon" href="app/images/favicon.png">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width">
</head>
<body>
<!--[if lt IE 9]>
<p class="browsehappy">You are using an <strong>outdated</strong> browser. Please <a href="http://outdatedbrowser.com/">upgrade your browser</a> to improve your experience.</p>
<![endif]-->
<div data-app-closable-messages></div>
<div data-ng-view class="app-body">
    <div class="app-loading"></div>
</div>
<script type="text/javascript" src="app/third-party/_1.js"></script>
<script type="text/javascript" src="app/third-party/_2.js"></script>
<script type="text/javascript" src="app/third-party/_3.js"></script>
<script type="text/javascript" src="app/scripts/_.js"></script>
<script type="text/javascript" src="app/scripts/_templates.js"></script>
</body>
</html>
