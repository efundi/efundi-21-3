<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>

<jsp:include page="header.jsp"></jsp:include>

<body>
	<jsp:include page="/WEB-INF/jsp/navBar.jsp"></jsp:include>

	<div class="container-fluid">

		<div class="row">
			<div class="col-sm-3 offset-sm-1">
				<h3>eFundi Course Link</h3>
			</div>
			<div class="col-sm-1 offset-sm-3">
				<label for="activeUser" class="col-form-label">Active User:
				</label>
			</div>
			<div class="col-sm-4">
				<input type="text" readonly class="form-control-plaintext"
					id="activeUser" value=" ${ activeUser }">
			</div>
		</div>

		<div class="row">
			<div class="col-sm-3 offset-sm-7">
				<div class="input-group mb-3">
					<input type="text" class="form-control" placeholder="Username"
						aria-label="Username" aria-describedby="basic-addon2" id="usernameId">
					<div class="input-group-append">
						<button class="btn btn-secondary" type="button" id="becomeUserBtnId">Become User</button>
					</div>	      			 		 
				</div>
			</div>
			<div class="">
				<button class="btn btn-secondary mr-2" type="button" id="saveBtnId">Save</button>
			</div>
			<div class="">
				<a href="${pageContext.request.contextPath}/logout" class="btn btn-secondary" role="button">Logout</a>
			</div>
		</div>		

		<hr>

		<div class="row">
			<div class="input-group">
				<div class="input-group-prepend">
					<span class="input-group-text">Year: ${ currentYear }</span>
				</div>
				<div class="input-group-prepend">
					<label class="input-group-text" for="inputCampusSelect">Campus</label>
				</div>
				<select class="custom-select" id="campusSelectId">
					<option value="0" selected>- Select Campus -</option>
					<option value="1">Potchefstroom</option>
					<option value="2">Vaal Triangle</option>
					<option value="9">Mafikeng</option>
				</select>

				<div class="input-group-prepend">
					<span class="input-group-text" id="moduleId">Module</span>
				</div>
				<input type="text" class="form-control module-mask" placeholder="Course Code"
					aria-label="Course Code" aria-describedby="moduleId" id="moduleTextVal">

				<div class="input-group-prepend">
					<label class="input-group-text" for="metOfDelSelectId">Method of Delivery</label>
				</div>
				<select class="custom-select input-group"
					id="metOfDelSelectId" disabled>
					<option value="0" selected>- All Method of Deliveries -</option>
				</select>

				<div class="input-group-prepend">
					<label class="input-group-text input-group"
						for="presCatSelectId">Presentation Category</label>
				</div>
				<select class="custom-select" id="presCatSelectId"
					disabled>
					<option value="0" selected>- All Presentation Categories -</option>
				</select>

			</div>
		</div>

		<hr>

		<div class="row justify-content-md-center">
			<div class="pr-2">
				<button class="btn btn-secondary" type="button" id="searchBtnId">Search</button>
			</div>
			<div class="pl-2">
				<button class="btn btn-secondary" type="button" id="clearBtnId">Clear</button>
			</div>
		</div>

		<hr>

		<div class="panel panel-default">
			<div class="panel-body">
				<table class="table table-hover" id="moduleDataTable">
					<thead>
						<tr>
							<th class="col-md-1">Link Instructor</th>
							<th class="col-md-1">Linked by Instructor</th>
							<th class="col-md-1">Campus</th>
							<th class="col-md-1">Module</th>
							<th class="col-md-2">Method of Delivery</th>
							<th class="col-md-2">Presentation Category</th>
						</tr>
					</thead>					
				</table>
			</div>
		</div>
		
		<hr>
		
	</div>
</body>

</html>
