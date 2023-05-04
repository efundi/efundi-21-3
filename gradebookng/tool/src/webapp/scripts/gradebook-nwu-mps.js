/**************************************************************************************
 *                    Gradebook NWU MPS Javascript                                      
 *************************************************************************************/

function selectAllCheckboxes() {
	if(document.getElementById('select-all').checked) { //selecting all
	    $(':checkbox').each(function() {
	      if(!this.name.includes('select-all') && !this.checked && !this.disabled) {
	        this.click();
	      }
	    });
	} else {  //deselecting all
	    $(':checkbox').each(function() {
	      if(!this.name.includes('select-all') && this.checked && !this.disabled) {
	        this.click();
	      }
	    });
	}
}

function lockScreen() {

	$.blockUI({
		css: {
			border: 'none',
			padding: '15px',
			backgroundColor: '#000',
			'-webkit-border-radius': '10px',
			'-moz-border-radius': '10px',
			opacity: .5,
			color: '#fff'
		}
	});
}
