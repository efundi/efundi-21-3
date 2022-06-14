package edu.nwu.sakaistudentlink.services;

import java.util.List;
import java.util.Map;

public class SearchResultObject implements java.io.Serializable {

	private static final long serialVersionUID = 7973392390650247561L;

	private List<ModuleOffering> moduleOfferingList;
	private Map<String, String> methodOfDelMap;
	private Map<String, String> presentCatMap;
	private int selectedMetOfDelId;
	private int selectedPresCatId;
	
	public SearchResultObject() {
	}

	public SearchResultObject(List<ModuleOffering> moduleOfferingList, Map<String, String> methodOfDelMap,
			Map<String, String> presentCatMap, int selectedMetOfDelId, int selectedPresCatId) {
		super();
		this.moduleOfferingList = moduleOfferingList;
		this.methodOfDelMap = methodOfDelMap;
		this.presentCatMap = presentCatMap;
		this.selectedMetOfDelId = selectedMetOfDelId;
		this.selectedPresCatId = selectedPresCatId;
	}

	public List<ModuleOffering> getModuleOfferingList() {
		return moduleOfferingList;
	}

	public void setModuleOfferingList(List<ModuleOffering> moduleOfferingList) {
		this.moduleOfferingList = moduleOfferingList;
	}

	public Map<String, String> getMethodOfDelMap() {
		return methodOfDelMap;
	}

	public void setMethodOfDelMap(Map<String, String> methodOfDelMap) {
		this.methodOfDelMap = methodOfDelMap;
	}

	public Map<String, String> getPresentCatMap() {
		return presentCatMap;
	}

	public void setPresentCatMap(Map<String, String> presentCatMap) {
		this.presentCatMap = presentCatMap;
	}

	public int getSelectedMetOfDelId() {
		return selectedMetOfDelId;
	}

	public void setSelectedMetOfDelId(int selectedMetOfDelId) {
		this.selectedMetOfDelId = selectedMetOfDelId;
	}

	public int getSelectedPresCatId() {
		return selectedPresCatId;
	}

	public void setSelectedPresCatId(int selectedPresCatId) {
		this.selectedPresCatId = selectedPresCatId;
	}

}
