<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
    response.sendRedirect(request.getContextPath() + "/home");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title><c:out value="MohanMart — Multi-Seller E-Commerce Marketplace" /></title>
</head>
<body>
    <p>Redirecting to <a href="<c:out value='${pageContext.request.contextPath}/home' />">MohanMart Marketplace</a>...</p>
    <script src="<c:out value='${pageContext.request.contextPath}/js/chat-widget.js' />"></script>
</body>
</html>
