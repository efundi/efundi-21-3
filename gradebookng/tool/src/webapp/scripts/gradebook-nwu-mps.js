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