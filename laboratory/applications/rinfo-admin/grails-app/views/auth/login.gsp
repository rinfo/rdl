<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="sv" lang="sv">
<head>
<title>Logga in - Rinfo</title>
<link rel="stylesheet" href="${createLinkTo(dir:'css',file:'screen.css')}" />
<link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}" />
<link rel="shortcut icon" href="${createLinkTo(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
</head>
<body>
<div class="bottom">

<g:if test="${flash.message}">
<div class="message">${flash.message}</div>
</g:if>

<g:form action="signIn">
<input type="hidden" name="targetUri" value="${targetUri}" />

<div class="login panel">
<h1>Välkommen till Rinfo administration</h1>
<p><label for="username">Användarnamn:</label>
<input type="text" id="username" name="username" value="${username}" /></p>

<p><label for="password">Lösenord:</label>
<input type="password" id="password" name="password" value="" /></p>

<p class="actions"><input type="submit" value="Logga in" /></p>
</div>
</g:form>
</div>
</body>
</html>
