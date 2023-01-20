import java.util.Calendar;
import java.util.List;

import ac.za.nwu.academic.dates.dto.AcademicPeriodInfo;
import ac.za.nwu.academic.registration.service.StudentAcademicRegistrationService;
import ac.za.nwu.moduleoffering.dto.ModuleOfferingSearchCriteriaInfo;
import ac.za.nwu.registry.utility.GenericServiceClientFactory;
import assemble.edu.common.dto.ContextInfo;

public class StudentRegistrationWSTest {

	public static void main(String[] args) {

		testNewWebservice();
		// testOldWebservice();

	}

//	private static void testOldWebservice() {
//		StringBuilder students = new StringBuilder();
//		Calendar calendar = Calendar.getInstance();
//
//		ModuleOfferingSearchCriteriaInfo searchCriteria = new ModuleOfferingSearchCriteriaInfo();
//
//		AcademicPeriodInfo academicPeriodInfo = new AcademicPeriodInfo();
//		academicPeriodInfo.setAcadPeriodtTypeKey("vss.code.AcademicPeriod.Year");
//		academicPeriodInfo.setAcadPeriodValue(Integer.toString(calendar.get(Calendar.YEAR)));
//
//		searchCriteria.setAcademicPeriod(academicPeriodInfo);
//		searchCriteria.setModuleSubjectCode("ALDA");
//		// searchCriteria.setModuleNumber(moduleDetail.getModuleNumber());
//		searchCriteria.setModuleSite("1");
//		// searchCriteria.setMethodOfDeliveryTypeKey(moduleDetail.getMethodOfDeliveryCodeParam());
//		// searchCriteria.setModeOfDeliveryTypeKey(moduleDetail.getModeOfDeliveryCodeParam());
//
//		try {
//			URL wsdlURL = new URL(
//					"http://workflowprd.nwu.ac.za/sapi-vss-v5/StudentAcademicRegistrationService/StudentAcademicRegistrationService?wsdl");
//			StudentAcademicRegistrationService_Service service = new StudentAcademicRegistrationService_Service(wsdlURL);
//			StudentAcademicRegistrationService port = service.getStudentAcademicRegistrationServicePort();
//
//			((BindingProvider) port).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "sapiappreadprod");
//			((BindingProvider) port).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "5p@ssw0rd4pr0dr");
//
//			List<String> studentUserNames = port.getStudentAcademicRegistrationByModuleOffering(searchCriteria, null);
//
//			for (int j = 0; j < studentUserNames.size(); j++) {
//				String studentUserName = studentUserNames.get(j);
//				students.append(studentUserName);
//				if (j != studentUserNames.size() - 1) {
//					students.append(",");
//				}
//			}
//
//			System.out.println(students);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	private static void testNewWebservice() {

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
}
