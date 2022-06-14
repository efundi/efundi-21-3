<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>

<jsp:include page="/WEB-INF/jsp/header.jsp"></jsp:include>
  
  <body>    
    
	<jsp:include page="/WEB-INF/jsp/navBar.jsp"></jsp:include>	
	
	<!-- Main content view -->
    <div class="slink-content" >	
    	
		<div class="row slink-titlebar">
			<div class="col-sm-9 offset-sm-2 mainarea">
				<h2>eFundi Course Link</h2>
			</div>
		</div>		
		
		<div class="row">		
			<!-- main area-->
			<div class="col-sm-9 offset-sm-3 pt-3">
				<!-- a href="${pageContext.request.contextPath}/j_spring_cas_security_check" class="btn btn-primary btn-lg" role="button" aria-disabled="true">Sign in with NWU CAS</a -->
				<!-- a href="${pageContext.request.contextPath}/slinkMain" class="btn btn-primary btn-lg" role="button" aria-disabled="true">Begin eFundi Course Link</a -->
				<a href="${pageContext.request.contextPath}/slinkMain" class="btn btn-primary btn-lg" role="button" aria-disabled="true">Sign in with NWU CAS</a>
			</div>					
		</div>			
		
	</div>
	
  </body>
  
</html>


