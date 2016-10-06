<!DOCTYPE html>
<%
    boolean testing = false;
    if(request.getParameter("test") != null) {
        testing = Boolean.parseBoolean(request.getParameter("test").toUpperCase());
    }
%>
<html <% if(!testing) { %> id="ng-app" data-ng-app="tribe-main" <% } %> ><%@ page contentType="text/html;charset=UTF-8" language="java" %>
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
    <link rel="stylesheet" href="<%=request.getContextPath()%>/app/third-party/styles/_.css"/>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/app/styles/_.css"/>
    <% if(testing) { %>
        <link rel="stylesheet" href="<%=request.getContextPath()%>/app/third-party/styles/_tests.css"/>
    <% } %>
    <link rel="icon" href="<%=request.getContextPath()%>/app/images/favicon.png">
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
<% if(testing) { %>
    <script type="text/javascript" src="<%=request.getContextPath()%>/app/third-party/_tests_1.js"></script>
    <script type="text/javascript">
        mocha.setup({
            "ui": "bdd",
            "reporter": "html"
        });
    </script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/app/third-party/_tests_2.js"></script>
<% } else { %>
    <script type="text/javascript" src="<%=request.getContextPath()%>/app/third-party/_.js"></script>
<% } %>
<script type="text/javascript" src="<%=request.getContextPath()%>/app/scripts/_.js"></script>
<% if(testing) { %>
    <div id="mocha"></div>
    <script type="text/javascript" src="<%=request.getContextPath()%>/app/scripts/_tests.js"></script>
    <script type="text/javascript">
        mocha.run();
    </script>
<% } else { %>
<script type="text/javascript" src="<%=request.getContextPath()%>/app/scripts/_templates.js"></script>
<% } %>
</body>
</html>
