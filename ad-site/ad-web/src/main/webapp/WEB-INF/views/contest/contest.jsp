<%@ page language="java" pageEncoding="UTF-8"
	contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib prefix="sec"
	uri="http://www.springframework.org/security/tags"%>
<c:if test="${contest.state == 'FINISHED' }">
	<article class="center-article-wrapper ui-corner-all contest-info">
		<h2 class="clearfix color-imp bigger-font">Konkurs został
			zakończony</h2>
		<br /> <br /> <span class="clearfix color-imp bigger-font">Uczestnicy</span>
		<br />
		<c:choose>
			<c:when test="${contest.type == 'ANSWER_QUESTION' }">
				<c:forEach items="${contest.answers }" var="answer">
					<a href="${pageContext.request.contextPath}/user/${answer.user.id}">${answer.user.displayName
						}</a>
				</c:forEach>
			</c:when>
			<c:otherwise>
				<c:forEach items="${contest.ads }" var="ad">
					<a
						href="${pageContext.request.contextPath}/user/${answer.user.id}">${answer.user.displayName
						}</a>
				</c:forEach>
			</c:otherwise>
		</c:choose>
	</article>
</c:if>
<article class="center-article-wrapper ui-corner-all clearfix">

	<img src="${contest.imageUrl}" class="contest-image" />
	<div class="image-right">
		<h2 class="color-imp">${contest.name }</h2>
		<div class="contest-description">${contest.description}</div>
	</div>
</article>
<article class="center-article-wrapper ui-corner-all contest-info">
	<span class="clearfix color-imp bigger-font">Informacje
		dodatkowe</span>

	<sec:authorize access="!hasRole('ROLE_USER')">
		<span class="clearfix color-imp full-width">Zaloguj się się aby
			wziąć udział w konkursie</span>
	</sec:authorize>

	<span class="clearfix full-width">Data zakończenia konkursu <span
		class="color-imp"><fmt:formatDate
				value="${contest.finishDate }" pattern="yy-mm-dd" /> o <fmt:formatDate
				value="${contest.finishDate }" pattern="hh:mm" /></span>
	</span> <span class="clearfix full-width">Wyniki zostaną podane <span
		class="color-imp"><fmt:formatDate
				value="${contest.scoresDate }" pattern="yy-mm-dd" /> o <fmt:formatDate
				value="${contest.scoresDate }" pattern="hh:mm" /></span></span> <span
		class="color-imp clearfix full-width">O wygranej dowiesz się na
		swoim koncie spotnik.pl</span>
</article>
<sec:authorize access="hasRole('ROLE_USER')">
	<c:if
		test="${contest.state != 'FINISHED' and !hasPosted }">
			<div class="center-article-wrapper ui-corner-all">
			<c:choose>
				<c:when test="${contest.type == 'ANSWER_QUESTION' }">
					<span class="bigger-font color-imp clearfix full-width"
						style="text-align: center">Wyślij odpowiedź</span>
					<form method="POST" class="form"
						action="${pageContext.request.contextPath}/contest/${contest.id }/answer">
						<textarea name="answer"></textarea>
						<input type="submit" class="ui-corner-all button-green"
							value="Wyślij" />
					</form>
				</c:when>
				<c:otherwise>
					<a
						href="${pageContext.request.contextPath}/contest/${contest.id}/ad/register"><button
							class="ui-corner-all button-green full-width">Dodaj
							reklamę</button></a>
				</c:otherwise>
			</c:choose>
		</div>
	</c:if>
</sec:authorize>
