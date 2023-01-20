import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeSet;

import ac.za.nwu.academic.dates.dto.AcademicPeriodInfo;
import ac.za.nwu.academic.registration.service.StudentAcademicRegistrationService;
import ac.za.nwu.courseoffering.service.CourseOfferingService;
import ac.za.nwu.courseoffering.service.factory.CourseOfferingServiceClientFactory;
import ac.za.nwu.moduleoffering.dto.ModuleOfferingInfo;
import ac.za.nwu.moduleoffering.dto.ModuleOfferingSearchCriteriaInfo;
import ac.za.nwu.registry.utility.GenericServiceClientFactory;
import assemble.edu.common.dto.ContextInfo;
import assemble.edu.exceptions.DoesNotExistException;
import assemble.edu.exceptions.InvalidParameterException;
import assemble.edu.exceptions.MissingParameterException;
import assemble.edu.exceptions.OperationFailedException;
import assemble.edu.exceptions.PermissionDeniedException;
import edu.nwu.sakaistudentlink.services.ModuleOffering;

public class StudentRegistrationWSTest {

	public static void main(String[] args) {
		
//		testStudentAcademicRegistrationService();
		testCourseOfferingService();
	
	}

	private static void testStudentAcademicRegistrationService() {

		try {

			StringBuilder students = new StringBuilder();
			Calendar calendar = Calendar.getInstance();

//			String webserviceUrl = "http://workflow7tst.nwu.ac.za/sapi-vss-v8-v_test/StudentAcademicRegistrationService/StudentAcademicRegistrationService?wsdll";
//			String webserviceUrl = "/PROD/SAPI-STUDENTACADEMICREGISTRATIONSERVICE/V7";			

			String webserviceUrl = "/TEST/SAPI-STUDENTACADEMICREGISTRATIONSERVICE/V8/V_TEST";
				
			String contextInfoUsername = "sapiappreadtest";
			String contextInfoPassword = "sp@ssw0rd";
			
			ModuleOfferingSearchCriteriaInfo searchCriteria = new ModuleOfferingSearchCriteriaInfo();

			AcademicPeriodInfo academicPeriodInfo = new AcademicPeriodInfo();
			academicPeriodInfo.setAcadPeriodtTypeKey("vss.code.AcademicPeriod.Year");
			academicPeriodInfo.setAcadPeriodValue(Integer.toString(calendar.get(Calendar.YEAR)));
			ContextInfo contextInfo = new ContextInfo("SOAPUI");

			searchCriteria.setAcademicPeriod(academicPeriodInfo);
			searchCriteria.setModuleSubjectCode("ALDA");
			// searchCriteria.setModuleNumber(moduleDetail.getModuleNumber());
			searchCriteria.setModuleSite("1");
			// searchCriteria.setMethodOfDeliveryTypeKey(moduleDetail.getMethodOfDeliveryCodeParam());
			// searchCriteria.setModeOfDeliveryTypeKey(moduleDetail.getModeOfDeliveryCodeParam());
			
			StudentAcademicRegistrationService service = (StudentAcademicRegistrationService) GenericServiceClientFactory
					.getService(webserviceUrl, contextInfoUsername, contextInfoPassword,
							StudentAcademicRegistrationService.class);
			List<String> studentUserNames = service.getStudentAcademicRegistrationByModuleOffering(searchCriteria, contextInfo);

//			URL wsdlURL = new URL(
//					"http://workflowprd.nwu.ac.za/sapi-vss-v5/StudentAcademicRegistrationService/StudentAcademicRegistrationService?wsdl");
//			StudentAcademicRegistrationService_Service service = new StudentAcademicRegistrationService_Service(wsdlURL);
//			StudentAcademicRegistrationService port = service.getStudentAcademicRegistrationServicePort();
//
//			((BindingProvider) port).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "sapiappreadprod");
//			((BindingProvider) port).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "5p@ssw0rd4pr0dr");
//
//			List<String> studentUserNames = port.getStudentAcademicRegistrationByModuleOffering(searchCriteria, null);

			for (int j = 0; j < studentUserNames.size(); j++) {
				String studentUserName = studentUserNames.get(j);
				students.append(studentUserName);
				if (j != studentUserNames.size() - 1) {
					students.append(",");
				}
			}

			System.out.println(students);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void testCourseOfferingService() {
		List<ModuleOffering> modules = new ArrayList<ModuleOffering>();
		TreeSet<ModuleOffering> moduleSet = new TreeSet<ModuleOffering>();
		Calendar calendar = Calendar.getInstance();

		String webserviceUrl = "/TEST/CURRICULUM-DELIVERY-COURSEOFFERINGSERVICE/V3/V_TEST";
			
		String contextInfoUsername = "sapiappreadtest";
		String contextInfoPassword = "sp@ssw0rd";
		
		ModuleOfferingSearchCriteriaInfo searchCriteria = new ModuleOfferingSearchCriteriaInfo();

		AcademicPeriodInfo academicPeriodInfo = new AcademicPeriodInfo();
		academicPeriodInfo.setAcadPeriodtTypeKey("vss.code.AcademicPeriod.Year");
		academicPeriodInfo.setAcadPeriodValue(Integer.toString(calendar.get(Calendar.YEAR)));
		ContextInfo contextInfo = new ContextInfo("SOAPUI");

		searchCriteria.setAcademicPeriod(academicPeriodInfo);

		searchCriteria.setModuleSubjectCode("ALDA");
		searchCriteria.setModuleSite("1");
//		
//		String courseCode = criteria.get(SearchCriteria.MODULE_SUBJECT_CODE);
//		if (courseCode != null) {
//			searchCriteria.setModuleSubjectCode(criteria.get(SearchCriteria.MODULE_SUBJECT_CODE));
//		}
//		String moduleNumber = criteria.get(SearchCriteria.MODULE_NUMBER);
//		if (moduleNumber != null) {
//			searchCriteria.setModuleNumber(moduleNumber);
//		}
//		searchCriteria.setModuleSite(criteria.get(SearchCriteria.CAMPUS));
//		String methodOfDel = criteria.get(SearchCriteria.METHOD_OF_DEL) == null ? null
//				: criteria.get(SearchCriteria.METHOD_OF_DEL);
//		if (methodOfDel != null) {
//			searchCriteria.setMethodOfDeliveryTypeKey("vss.code.ENROLCAT." + methodOfDel);
//		}
//		String modeOfDel = criteria.get(SearchCriteria.PRESENT_CAT) == null ? null : criteria.get(SearchCriteria.PRESENT_CAT);
//		if (modeOfDel != null) {
//			searchCriteria.setModeOfDeliveryTypeKey("vss.code.PRESENTCAT." + modeOfDel);
//		}

		CourseOfferingService courseOfferingService = null;
		try {
			courseOfferingService = (CourseOfferingService) CourseOfferingServiceClientFactory
					.getCourseOfferingService(webserviceUrl, contextInfoUsername, contextInfoPassword);
			List<ModuleOfferingInfo> moduleOfferingList = courseOfferingService
					.getModuleOfferingBySearchCriteria(searchCriteria, "vss.code.LANGUAGE.2", contextInfo);
			if (moduleOfferingList != null) {

				System.out.println(moduleOfferingList);
			}
			
		} catch (PermissionDeniedException e) {
//			log.error("User does not have permission to access the service.", e);
		} catch (DoesNotExistException e) {
//			log.error("Unable to find service wsdl url from provided key.", e);
		} catch (MissingParameterException e) {
//			log.error("Error invoking service, missing parameter.", e);
		} catch (OperationFailedException e) {
//			log.error("Unable to create a client instance from the provided details.", e);
		} catch (InvalidParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
}
