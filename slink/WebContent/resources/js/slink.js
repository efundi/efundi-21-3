$(document).ready(function() {

	var url = window.location;		
			
	$("#campusSelectId").focus();
			
	$.jMaskGlobals = {
		translation: {
			'~': {pattern: /[a-zA-Z]/},
			'#': {pattern: /[0-9]/}
		}
	};
			
	$('.module-mask').mask('~~~~###');

	var moduleDataTable = $('#moduleDataTable').DataTable({
		"order": [[ 3, "asc" ]],
		"searching": false,
		'columnDefs': [
			{
		    	"targets": 0, // first column
		        "className": "text-center",
		    }]
	});			
			
	var linkedModuleIds = [];
	var unlinkedModuleIds = [];
			
	$("#becomeUserBtnId").click(function(event) {
		event.preventDefault();

		var usernameId = $('#usernameId').val();       
		if (usernameId.trim() == "") {
            alert("Username is empty, please fill in a valid username value.");
            $('#usernameId').focus();
            return false;
        } 	
        var data = 'j_username=' + encodeURIComponent(usernameId);
        		
        $.ajax({
			async:true,
        	type : "GET",
		    url : contextPath + "/becomeUser", //see "switchUserProcessingFilter" in securityContext.xml, using CAS's switch user filter
			data : data,
		    success: function(data){		
		    	
		    	$("#activeUser").val(data);
		    	$("#usernameId").val('');
		    	clearAll();
		    },
		    error : function(e) {				    	
                alert("User " + usernameId + " not found in staff/instructor directory or something else went wrong.");
		    }
		}); 
	})

	$("#searchBtnId").click(function(event) {
		event.preventDefault();
		
		var campusId = $('#campusSelectId').val();
		if (campusId == 0) {
            alert("Please select a campus");
            $('#campusSelectId').focus();
            return false;
        }

		var moduleStr = $('#moduleTextVal').val();
		if (moduleStr.trim() == "" || moduleStr.length < 4) {
            alert("Please enter a Module subject code with at least 4 characters.");
            $('#moduleTextVal').focus();
            return false;
        } else if (moduleStr.length > 4 && moduleStr.length < 7) {
            alert("Module subject code value is only allowed 4 or 7 characters.");
            $('#moduleTextVal').focus();
            return false;
        }
        
		var metOfDelId = $('#metOfDelSelectId').val();
        var presCatId = $('#presCatSelectId').val();                 
        var data = 'campusId='
                + encodeURIComponent(campusId)
                + '&moduleStr='
                + encodeURIComponent(moduleStr)
                + '&metOfDelId='
                + encodeURIComponent(metOfDelId)
                + '&presCatId='
                + encodeURIComponent(presCatId);

		$.ajax({
			async:true,
			url : contextPath + "/searchCourses",
			type : "GET",
            beforeSend: function(request) {
				$.blockUI({ css: {
                     border: 'none',
                     padding: '15px',
                     backgroundColor: '#000',
                       '-webkit-border-radius': '10px',
                       '-moz-border-radius': '10px',
                     opacity: .5,
                     color: '#fff'
                  }});
			},
			data : data,
			success : function(response) {
				
				clearDataTable();
				
				$.each(response.moduleOfferingList, function(ind, obj){
					
					var checked = '';
					if (obj.linkedToLecturer) {
						checked = 'checked';
					}
					var disabled = '';
					if (obj.disable) {
						disabled = 'disabled';
					}
					var rowNode = moduleDataTable.row.add([
						"<div class='form-check'>  <input type='checkbox'  " + 
							checked + " id='moduleId-" + obj.ksapimotracsid + "' " + disabled + " > </div>",
						obj.linkedByLecturer,
						obj.modulePresentingEngCampusName,
						obj.module,
						obj.methodOfDeliveryName,
						obj.presentationCategoryName						
					]).draw().node();
					if (obj.disable) {
						$( rowNode ).css( 'background-color', '#FEE1E1' );
					}
				});
				
				updateMethodOfDelSelect(response.methodOfDelMap, response.selectedMetOfDelId);
				updatePresentCatSelect(response.presentCatMap, response.selectedPresCatId);
				
				$.unblockUI();
			},
			error : function(response, status, error) {
				$.unblockUI();						
				var errorMessage = JSON.parse(response.responseText)						
				alert(errorMessage.errorMessage);
			}
		});
	})
	
	$("#moduleTextVal").keypress(function(event) {
    	$("#moduleTextVal").val($("#moduleTextVal").val().toUpperCase());
	    if (event.keyCode == 13) {
	        $("#searchBtnId").click();
	    }
	});

	$("#clearBtnId").click(function(event) {
		event.preventDefault();				
		clearAll();				
		$( "#campusSelectId" ).focus();
	})
	
	$('#moduleDataTable').on('click', 'input[type="checkbox"]', function() {
		inputCheckboxHandler($( this ));
	}); 
	
	function inputCheckboxHandler(eventTarget){
		
		var linkIndicator = eventTarget.is(":checked");
		var elementId = eventTarget.attr("id");
        var moduleId = elementId.substring(elementId.indexOf("-") + 1, elementId.length);          
		
        var data = 'linkIndicator='
                + encodeURIComponent(linkIndicator)
                + '&ksapimotracsid='
                + encodeURIComponent(moduleId);

        $.ajax({
			async:true,
            type : "POST",
			url : contextPath + "/updateLinkedLists",
            data : data,
            success : function(response) {
               // do something ... console.log?
            },
            error : function(e) {
               alert('An error occured : ' + e);
            }
        });		 	
	}
	
	function updateMethodOfDelSelect(methodOfDelMap, selectedMetOfDelId) {

        clearMetOfDelSelect();
		
		if($.isEmptyObject(methodOfDelMap)) {
			$('#metOfDelSelectId').prop('disabled', true);
		} else {
			$('#metOfDelSelectId').prop('disabled', false);
			$.each(methodOfDelMap, function(key, value) {
				$('#metOfDelSelectId').append($("<option/>", {
					value: key,
			        text: value
			    }));
			});
		}
		
		$('#metOfDelSelectId').val(selectedMetOfDelId);
	}
	
	function updatePresentCatSelect(presentCatMap, selectedPresCatId) {				

        clearPresCatSelect();
		
		if($.isEmptyObject(presentCatMap)) {
			
			$('#presCatSelectId').prop('disabled', true);
		} else {
			
			$('#presCatSelectId').prop('disabled', false);
			
			$.each(presentCatMap, function(key, value) {
				$('#presCatSelectId').append($("<option/>", {
					value: key,
			        text: value
			    }));
			});
		}
		
		$('#presCatSelectId').val(selectedPresCatId);
	}
	
	$("#saveBtnId").click(function(event) {
		event.preventDefault();	

        $.ajax({
			async:true,
            beforeSend: function(request) {
				$.blockUI({ css: {
                     border: 'none',
                     padding: '15px',
                     backgroundColor: '#000',
                       '-webkit-border-radius': '10px',
                       '-moz-border-radius': '10px',
                     opacity: .5,
                     color: '#fff'
                  }});
			},
            type : "GET",
			url : contextPath + "/save",
            success : function(response) {

				$.unblockUI();				
                alert('You have successfully linked/unlinked your courses.');
            	clearAll();		
            },
            error : function(response, status, error) {
				$.unblockUI();						
				var errorMessage = JSON.parse(response.responseText)						
				alert(errorMessage.errorMessage);
			}
        });
        						
	})	
	
	function clearAll() {

        $('#usernameId').val('');       				

		$('#campusSelectId').val('0');
        $('#moduleTextVal').val('');         
        
        clearMetOfDelSelect();
        $('#metOfDelSelectId').prop('disabled', true);
        
        clearPresCatSelect();
        $('#presCatSelectId').prop('disabled', true);
        
        clearDataTable();
	}

	function clearMetOfDelSelect() {

		$('#metOfDelSelectId').empty();
		$('#metOfDelSelectId').append("<option value=\"0\">- All Presentation Categories -</option>");
	}
	
	function clearPresCatSelect() {

		$('#presCatSelectId').empty();
		$('#presCatSelectId').append("<option value=\"0\">- All Presentation Categories -</option>");
	}
	
	function clearDataTable() {
        moduleDataTable.clear().draw();				
	}
	
});