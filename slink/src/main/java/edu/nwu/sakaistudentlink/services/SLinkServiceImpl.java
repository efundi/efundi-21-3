package edu.nwu.sakaistudentlink.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * author: Jaco Gillman
 * 
 */
public class SLinkServiceImpl implements SLinkService {

	private ModuleSearch moduleSearch;

	private ModuleLink moduleLink;

	@Override
	public List<ModuleOffering> searchModules(Map<SearchCriteria, String> criteria, String username) throws IntegrationException {
		List<ModuleOffering> modules = null;
		String methodOfDeliveryCode = criteria.get(SearchCriteria.METHOD_OF_DEL);
		String presentationCategoryCode = criteria.get(SearchCriteria.PRESENT_CAT);

		List<ModuleOffering> linkedModules = moduleLink.getAllModulesLinkedToLecturer(criteria);
		modules = moduleSearch.findSearchModule(criteria);
		for (ModuleOffering module : modules) {
			if (linkedModules.contains(module)
					&& (methodOfDeliveryCode == null ? true
							: methodOfDeliveryCode.equals(module.getMethodOfDeliveryCode()))
					&& (presentationCategoryCode == null ? true
							: presentationCategoryCode.equals(module.getPresentationCategoryCode()))
					&& StringUtils.isNotBlank(module.getLinkedByLecturer())) {
				module.setLinkedToLecturer(true);
			} else {
				module.setLinkedToLecturer(false);
			}
			if(StringUtils.isNotBlank(module.getLinkedByLecturer()) && !username.equals(module.getLinkedByLecturer())) {
				module.setDisable(true);
			}
		}
		return modules;
	}

	@Autowired
	public void setModuleSearch(ModuleSearch moduleSearch) {
		this.moduleSearch = moduleSearch;
	}

	@Autowired
	public void setModuleLink(ModuleLink moduleLink) {
		this.moduleLink = moduleLink;
	}

	@Override
	public Map<String, String> getMethodOfDelMap(List<ModuleOffering> modules) {
		if(modules.isEmpty()) {
			return null;
		}
		Map<String, String> methodOfDelMap = new HashMap<String, String>();
		for (ModuleOffering moduleOffering : modules) {
			methodOfDelMap.put(moduleOffering.getMethodOfDeliveryCode(), moduleOffering.getMethodOfDeliveryName());
		}
		return methodOfDelMap;
	}

	@Override
	public Map<String, String> getPresentCatMap(List<ModuleOffering> modules) {
		if(modules.isEmpty()) {
			return null;
		}
		Map<String, String> presentCatMap = new HashMap<String, String>();
		for (ModuleOffering moduleOffering : modules) {
			presentCatMap.put(moduleOffering.getPresentationCategoryCode(), moduleOffering.getPresentationCategoryName());
		}
		return presentCatMap;
	}

	@Override
	public void save(List<ModuleOffering> linkedModules, List<ModuleOffering> unlinkedModules, HashMap<SearchCriteria, String> searchCriteria)
			throws IntegrationException {
		if (linkedModules != null && !linkedModules.isEmpty()) {
			moduleLink.linkInstructorToModules(linkedModules, searchCriteria);
		}
		if (unlinkedModules != null && !unlinkedModules.isEmpty()) {
			moduleLink.unlinkInstructorFromModules(unlinkedModules, searchCriteria);
		}
	}

}
