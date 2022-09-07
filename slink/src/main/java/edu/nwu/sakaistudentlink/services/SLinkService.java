package edu.nwu.sakaistudentlink.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author: Jaco Gillman
 * 
 */
public interface SLinkService {

    public List<ModuleOffering> searchModules(Map<SearchCriteria, String> criteria, String username)
            throws IntegrationException;

	public Map<String, String> getMethodOfDelMap(List<ModuleOffering> modules);

	public Map<String, String> getPresentCatMap(List<ModuleOffering> modules);

	public void save(List<ModuleOffering> linkedModules, List<ModuleOffering> unlinkedModules, HashMap<SearchCriteria, String> searchCriteria) throws IntegrationException;
}
