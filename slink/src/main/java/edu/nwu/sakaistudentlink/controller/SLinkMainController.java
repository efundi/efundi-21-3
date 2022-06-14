package edu.nwu.sakaistudentlink.controller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.ldap.userdetails.InetOrgPerson;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import edu.nwu.sakaistudentlink.services.IntegrationException;
import edu.nwu.sakaistudentlink.services.ModuleOffering;
import edu.nwu.sakaistudentlink.services.SLinkService;
import edu.nwu.sakaistudentlink.services.SearchCriteria;
import edu.nwu.sakaistudentlink.services.SearchResultObject;

/**
 * author: Jaco Gillman
 * 
 */
@Controller
@RequestMapping("/")
@Configuration
@PropertySource("classpath:slink.properties")
public class SLinkMainController {

	@Autowired
	private SLinkService sLinkService;

	@RequestMapping(value = "/slinkMain", method = RequestMethod.GET)
	public ModelAndView slinkMain() throws IntegrationException {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (!auth.isAuthenticated()) {
			return new ModelAndView("index");
		}		

		Map<String, Object> slinkDataMap = new HashMap<String, Object>();

		Calendar calendar = Calendar.getInstance();
		slinkDataMap.put("currentYear", calendar.get(Calendar.YEAR));

		InetOrgPerson principal = (InetOrgPerson) auth.getPrincipal();
		slinkDataMap.put("activeUser", principal.getInitials() + " " + principal.getSn() + " (" + principal.getUsername() + ")");

		return new ModelAndView("slinkMain", slinkDataMap);
	}

	@RequestMapping(value = "/getUserDetail", method = RequestMethod.GET)
	public @ResponseBody String becomeUser(HttpSession session) throws IntegrationException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		InetOrgPerson principal = (InetOrgPerson) auth.getPrincipal();
		return principal.getInitials() + " " + principal.getSn() + " (" + principal.getUsername() + ")";
	}

	@RequestMapping(value = "/searchCourses", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public SearchResultObject searchCourses(@RequestParam("campusId") String campusId,
			@RequestParam("moduleStr") String moduleStr, @RequestParam("metOfDelId") String metOfDelId,
			@RequestParam("presCatId") String presCatId, HttpSession session) throws IntegrationException {

		HashMap<SearchCriteria, String> criteria = new HashMap<SearchCriteria, String>();
		removeSessionAttributes(session);

		Calendar calendar = Calendar.getInstance();
		criteria.put(SearchCriteria.YEAR, String.valueOf(calendar.get(Calendar.YEAR)));
		criteria.put(SearchCriteria.CAMPUS, campusId);

		if (!moduleStr.isEmpty()) {			
			criteria.put(SearchCriteria.MODULE_SUBJECT_CODE, moduleStr.substring(0, 4));
			if(moduleStr.length() > 4) {
				criteria.put(SearchCriteria.MODULE_NUMBER, moduleStr.substring(4, 7));
			}
		}

		if (!"0".equals(metOfDelId)) {
			criteria.put(SearchCriteria.METHOD_OF_DEL, metOfDelId);
		}
		if (!"0".equals(presCatId)) {
			criteria.put(SearchCriteria.PRESENT_CAT, presCatId);
		}

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		InetOrgPerson principal = (InetOrgPerson) auth.getPrincipal();
		String username = principal.getUsername();
		criteria.put(SearchCriteria.USER_NAME, username);
		
		List<ModuleOffering> moduleOfferings = sLinkService.searchModules(criteria, username);
		SearchResultObject searchResultObject = new SearchResultObject(moduleOfferings, 
				sLinkService.getMethodOfDelMap(moduleOfferings), sLinkService.getPresentCatMap(moduleOfferings), Integer.parseInt(metOfDelId), Integer.parseInt(presCatId));		
		session.setAttribute("moduleOfferings", moduleOfferings);	
		session.setAttribute("searchCriteria", criteria);

		return searchResultObject;
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/updateLinkedLists", method = RequestMethod.POST)
	public @ResponseBody String updateLinkedLists(@RequestParam("linkIndicator") String linkIndicator, @RequestParam("ksapimotracsid") String ksapimotracsid, HttpSession session) throws IntegrationException {
		
		List<ModuleOffering> moduleOfferings = (List<ModuleOffering>) session.getAttribute("moduleOfferings");		
		List<ModuleOffering> linkedModules = (List<ModuleOffering>) session.getAttribute("linkedModules");
		if(linkedModules == null) {
			linkedModules = new ArrayList<>();
		}
		List<ModuleOffering> unlinkedModules = (List<ModuleOffering>) session.getAttribute("unlinkedModules");
		if(unlinkedModules == null) {
			unlinkedModules = new ArrayList<>();
		}

		ModuleOffering moduleOffering = null;
		for (ModuleOffering moduleOfferingObj : moduleOfferings) {
			if(moduleOfferingObj.getKsapimotracsid().equals(ksapimotracsid)) {
				moduleOffering = moduleOfferingObj;
				break;
			}
		}
		
		if(moduleOffering != null) {

			if (Boolean.parseBoolean(linkIndicator)) {
	            if (moduleOffering.isLinkedToLecturer()) {
	                unlinkedModules.remove(moduleOffering);
	            }
	            else {
	                linkedModules.add(moduleOffering);
	            }
	        }
	        //The check box is OFF
	        else {
	            if (moduleOffering.isLinkedToLecturer()) {
	                unlinkedModules.add(moduleOffering);
	            }
	            else {
	                linkedModules.remove(moduleOffering);
	            }
	        }
			
			session.setAttribute("linkedModules", linkedModules);		
			session.setAttribute("unlinkedModules", unlinkedModules);
		}		
		
		return "success";
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/save", method = RequestMethod.GET)
	public @ResponseBody String save(HttpServletResponse response, HttpSession session) throws IntegrationException {
		
		List<ModuleOffering> linkedModules = (List<ModuleOffering>) session.getAttribute("linkedModules");
		List<ModuleOffering> unlinkedModules = (List<ModuleOffering>) session.getAttribute("unlinkedModules");
		HashMap<SearchCriteria, String> searchCriteria = (HashMap<SearchCriteria, String>) session.getAttribute("searchCriteria");		
		
		// save
        sLinkService.save(linkedModules, unlinkedModules, searchCriteria);	
        removeSessionAttributes(session);

		// redirect to /j_spring_security_logout	
		
//		response.addHeader("Access-Control-Allow-Origin", "*");
//		response.addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
		
        return "success";
	}
	

	@RequestMapping(value = "/logout", method = RequestMethod.GET)
	public String logout(HttpServletResponse response, HttpSession session) throws IntegrationException {
		
//		List<ModuleOffering> linkedModules = (List<ModuleOffering>) session.getAttribute("linkedModules");
//		List<ModuleOffering> unlinkedModules = (List<ModuleOffering>) session.getAttribute("unlinkedModules");
//		HashMap<SearchCriteria, String> searchCriteria = (HashMap<SearchCriteria, String>) session.getAttribute("searchCriteria");		
//		
//		// save & logout
//        sLinkService.save(linkedModules, unlinkedModules, searchCriteria);	
//        removeSessionAttributes(session);

		// redirect to /j_spring_security_logout	
		
//		response.addHeader("Access-Control-Allow-Origin", "*");
//		response.addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
		
        return "redirect:/j_spring_cas_security_logout";
	}
	
	/**
	 * 
	 * @param session
	 */
	private void removeSessionAttributes(HttpSession session) {
		session.removeAttribute("moduleOfferings");
		session.removeAttribute("linkedModules");
		session.removeAttribute("unlinkedModules");		
		session.removeAttribute("searchCriteria");
	}
}
