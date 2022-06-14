package edu.nwu.sakaistudentlink.services;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ac.za.nwu.academic.dates.dto.AcademicPeriodInfo;
import ac.za.nwu.academic.registration.service.StudentAcademicRegistrationService;
import ac.za.nwu.moduleoffering.dto.ModuleOfferingSearchCriteriaInfo;
import ac.za.nwu.registry.utility.GenericServiceClientFactory;
import assemble.edu.common.dto.ContextInfo;


/**
 * author: Jaco Gillman
 * 
 */
public class CourseManagementWSHandler {

	private SettingsProperties settingsProperties;
	
	private ConnectionManager connectionManager;

    private CourseManagementWSHandler() {
    }

    private static final Logger log = LoggerFactory.getLogger(CourseManagementWSHandler.class);

    private static final String SAKAI_WEB_SERVICE_SUCCESS = "success";

    /**
     * Insert the Sakai Course Management data through a web service call.
     * @param year The year for which to insert the Course Management data.
     * @param module The module for which to insert the Course Management data.
     * @param lecturerUserName The lecturer linked to the specified module.
     * @throws IntegrationException 
     */
    public void insertSakaiCMData(int year, ModuleOffering module, String lecturerUserName) throws IntegrationException {
        try {
            log.info("Get the current Sakai session with Sakai Web Services");
            String sessionId = createSakaiSession();
            if(StringUtils.isEmpty(sessionId)){
            	 log.error("An error occurred while creating a Sakai admin session Id");
            	 return;
            }
            log.info("Insert Course Management data with Sakai Web Services");
            String wsResult = callInsertCMDataWS(sessionId, year, module.getModuleSubjectCode(),
                module.getCanonicalCourseReference(), module.getCourseOfferingReference(year),
                module.getEnrollmentSetReference(year), lecturerUserName,
                getDelimitedStudentUserNames(year, module));
            if (SAKAI_WEB_SERVICE_SUCCESS.equals(wsResult)) {
                updateInsertedDataStatus(module, year, lecturerUserName);
            } else {
           	 	log.error("An error occurred while creating Sakai CM data. Webservice call did not return successful.");
            }
        }
        catch (Exception e) {
            log.error("An error occurred while inserting the Sakai CM data using the Sakai REST Web service.", e);
            IntegrationException ie = new IntegrationException("An error occurred while inserting the Sakai CM data using the Sakai REST Web service. ", e);
			IntegrationError error = new IntegrationError();
			error.setErrorMessage("An error occurred while inserting the Sakai CM data using the Sakai REST Web service. " + e.getMessage());
			ie.addError(error);
			throw ie;
        }
    }

    /**
     * Perform Sakai authentication and return the session id.
     * @return The active Sakai session id.
     * @throws Exception
     */
    private String createSakaiSession() throws Exception {
        String username = settingsProperties.getProperty("sakai.user");
        String password = settingsProperties.getProperty("sakai.password");

		WebClient client = WebClient.create(settingsProperties.getProperty("sakai.rest.address") + "/session");
		client = client.accept("text/xml");
		client.query("_username", username);
		client.query("_password", password);

		Response response = client.post(null);

		int statusCode = response.getStatus();
		if (statusCode == 201) { // CREATED

			String value = IOUtils.toString((InputStream) response.getEntity());
			return value;
		}
		return null;
    }

    /**
     * Insert the Sakai Course Management Data.
     * @param sessionId The active Sakai session id
     * @param courseCode The course code of the module (Example: AFNL)
     * @param canonicalCourseRef The canonical course reference (Example: AFNL 111)
     * @param courseOfferingRef The course offering reference (Example: AFNL 111 P 2011)
     * @param enrollmentSetRef The enrollment set reference (Example: AFNL 111 P 2011 ES)
     * @param lecturerUserName The lecturer name linked to this module.
     * @param studentUserNames A list of student user names linked to this module.
     * @return The result string. Only when the string equals "success" has the call been successful.
     * @throws Exception
     */
    private String callInsertCMDataWS(String sessionId, int year, String courseCode,
            String canonicalCourseRef, String courseOfferingRef, String enrollmentSetRef,
            String lecturerUserName, String studentUserNames) throws Exception {
		WebClient client = WebClient.create(settingsProperties.getProperty("sakai.rest.address") + "/nwu-cm/insert-cm-data");
		client = client.accept("application/json").type("application/json");
		client.query("sakai.session", sessionId);
		client.query("year", "" + year);
		client.query("courseCode", courseCode);
		client.query("canonicalCourseRef", canonicalCourseRef);
		client.query("courseOfferingRef", courseOfferingRef);
		client.query("enrollmentSetRef", enrollmentSetRef);
		client.query("lecturerUserName", lecturerUserName);
		client.query("studentUserNames", studentUserNames);
		Response response = client.post(null);
		int statusCode = response.getStatus();
		String results = null;
		if (statusCode == 200) {
			results = IOUtils.toString((InputStream) response.getEntity());
		}
        return results;
    }

    /**
     * Returns a comma delimited list of all the students linked to the specified module. (per year)
     */
    private String getDelimitedStudentUserNames(int year, ModuleOffering moduleDetail) {
    	
        StringBuilder students = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        
        ModuleOfferingSearchCriteriaInfo searchCriteria = new ModuleOfferingSearchCriteriaInfo();

		AcademicPeriodInfo academicPeriodInfo = new AcademicPeriodInfo();
		academicPeriodInfo.setAcadPeriodtTypeKey("vss.code.AcademicPeriod.YEAR");
		academicPeriodInfo.setAcadPeriodValue(Integer.toString(calendar.get(Calendar.YEAR)));
		ContextInfo contextInfo = new ContextInfo("SOAPUI");
        
        searchCriteria.setAcademicPeriod(academicPeriodInfo);
        searchCriteria.setModuleSubjectCode(moduleDetail.getModuleSubjectCode().toUpperCase());
        searchCriteria.setModuleNumber(moduleDetail.getModuleNumber());       
        searchCriteria.setModuleSite(moduleDetail.getModuleSite());
        searchCriteria.setMethodOfDeliveryTypeKey(moduleDetail.getMethodOfDeliveryCodeParam());   
        searchCriteria.setModeOfDeliveryTypeKey(moduleDetail.getModeOfDeliveryCodeParam());

		try {
			String envTypeKey = settingsProperties.getProperty("ws.student.env.type.key", "/PROD/SAPI-STUDENTACADEMICREGISTRATIONSERVICE/V8");
			String contextInfoUsername = settingsProperties.getProperty("nwu.context.info.username", "sapiappreadprod");
			String contextInfoPassword = settingsProperties.getProperty("nwu.context.info.password", "5p@ssw0rd4pr0dr");
			
			StudentAcademicRegistrationService service = (StudentAcademicRegistrationService) GenericServiceClientFactory
					.getService(envTypeKey, contextInfoUsername, contextInfoPassword, StudentAcademicRegistrationService.class);
			List<String> studentUserNames = service.getStudentAcademicRegistrationByModuleOffering(searchCriteria, contextInfo);			
	        
	        for (int j = 0; j < studentUserNames.size(); j++) {
	        	String studentUserName = studentUserNames.get(j);
	        	students.append(studentUserName);
	            if (j != studentUserNames.size() - 1) {
	                students.append(",");
	            }
			}        
		} catch (Exception e) {
            log.error("Webservice could not request all students for input criteria, see exception logged. ", e);
		} 

        return students.toString();
    }

    /**
     * Change 'Inserted' status to 'Done' for the Module record.
     */
    private void updateInsertedDataStatus(ModuleOffering moduleDetail, int year,
            String lecturerUserName) throws Exception {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE CM_MODULES ");
            sql.append("SET STATUS = ? ");
            sql.append("WHERE STATUS = ? ");
            sql.append("AND lecturer_f_id = ");
            sql.append("(SELECT l.lecturer_id ");
            sql.append("FROM CM_LECTURER l, ");
            sql.append("CM_YEAR_CAMPUS c ");
            sql.append("WHERE l.year_campus_f_id = c.year_campus_id ");
            sql.append("AND c.year = ? ");
            sql.append("AND c.campus_code = ? ");
            sql.append("AND l.username = ?) ");
            sql.append("AND course_code = ? ");
            sql.append("AND course_level = ? ");
            sql.append("AND course_module = ? ");
            sql.append("AND method_of_del = ? ");
            sql.append("AND present_cat = ?");
            pstmt = connection.prepareStatement(sql.toString());
            pstmt.setString(1, ModuleLinkStatus.DONE.toString());
            pstmt.setString(2, ModuleLinkStatus.INSERTED.toString());
            pstmt.setInt(3, year);
            pstmt.setString(4, moduleDetail.getModuleSite());
            pstmt.setString(5, lecturerUserName);
            pstmt.setString(6, moduleDetail.getModuleSubjectCode());
            pstmt.setString(7, moduleDetail.getCourseLevel());
            pstmt.setString(8, moduleDetail.getCourseModule());
            pstmt.setString(9, moduleDetail.getMethodOfDeliveryCodeParam());
            pstmt.setString(10, moduleDetail.getModeOfDeliveryCodeParam());
            pstmt.executeUpdate();
        }
        catch (ConnectionNotEstablishedException e) {
            log.error("A SQL Connection could not be established while performing updateInsertedDataStatus", e);
            throw new ConnectionNotEstablishedException("A SQL Connection could not be established while performing updateInsertedDataStatus", e);
        }
        catch (SQLException e) {
            log.error("SQL error occurred while performing updateInsertedDataStatus", e);
            throw new SQLException("SQL error occurred while performing updateInsertedDataStatus", e);
        }
        finally {
            ConnectionManager.close(null, pstmt, connection);
        }
    }

    /**
     * Delete the Sakai Course Management data through a web service call.
     * @param year The year for which to delete the Sakai Course Management data.
     * @param modules The module for which to delete the Sakai Course Management data.
     * @param lecturerUserName The lecturer user name from which this module will be unlinked.
     * @throws IntegrationException 
     */
    public void deleteSakaiCMData(int year, List<ModuleOffering> modules, String lecturerUserName) throws IntegrationException {

        for (ModuleOffering module : modules) {
            try {
                log.info("Get the current Sakai session with Sakai Web Services");
                String sessionId = createSakaiSession();
                if(StringUtils.isEmpty(sessionId)){
                	 log.error(
                             "An error occurred while creating a Sakai admin session Id");
                	 return;
                }
                log.info("Delete Course Management data with Sakai Web Services");
                boolean courseSetExists = isCourseSetExists(module);
                boolean canonicalCourseExists = isCanonicalCourseExists(module);
                boolean courseOfferingExists = isCourseOfferingExists(module, year);
                boolean onlyDeletedModulesExist = isOnlyDeletedModulesExist(year);
                String wsResult = callDeleteCMDataWS(sessionId, year, module.getModuleSubjectCode(),
                    module.getCanonicalCourseReference(), module.getCourseOfferingReference(year),
                    module.getEnrollmentSetReference(year), lecturerUserName,
                    getDelimitedStudentUserNames(year, module), courseSetExists,
                    canonicalCourseExists, courseOfferingExists, onlyDeletedModulesExist);
                if (SAKAI_WEB_SERVICE_SUCCESS.equals(wsResult)) {
                    deleteDeletedDataStatus(year);
                }
            }
            catch (Exception e) {
                log.error("An error occurred while deleting the Sakai CM data using the Sakai REST Web service.", e);
                IntegrationException ie = new IntegrationException("An error occurred while inserting the Sakai CM data using the Sakai REST Web service. ", e);
    			IntegrationError error = new IntegrationError();
    			error.setErrorMessage("An error occurred while deleting the Sakai CM data using the Sakai REST Web service. " + e.getMessage());
    			ie.addError(error);
    			throw ie;
            }
        }
    }

    /**
     * Returns a boolean whether a non deleted Course Set exists. (in any year)
     */
    private boolean isCourseSetExists(ModuleOffering module) throws Exception {
        boolean exists = false;
        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet rset = null;
        try {
            connection = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("select count(*) ");
            sql.append("from CM_MODULES m ");
            sql.append("where m.course_code = ? ");
            sql.append("and m.status <> ? ");
            pstmt = connection.prepareStatement(sql.toString());
            pstmt.setString(1, module.getModuleSubjectCode());
            pstmt.setString(2, ModuleLinkStatus.DELETED.toString());
            rset = pstmt.executeQuery();
            if (rset.next()) {
                exists = rset.getInt(1) > 0;
            }
        }
        catch (ConnectionNotEstablishedException e) {
            log.error(
                "A SQL Connection could not be established while performing isCourseSetExists", e);
            throw new ConnectionNotEstablishedException("A SQL Connection could not be established while performing isCourseSetExists", e);
        }
        catch (SQLException e) {
            log.error("SQL error occurred while performing isCourseSetExists", e);
            throw new SQLException("SQL error occurred while performing isCourseSetExists", e);
        }
        finally {
            ConnectionManager.close(rset, pstmt, connection);
        }
        return exists;
    }

    /**
     * Returns a boolean to indicate whether a non deleted CanonicalCourse exists. (in any year)
     */
    private boolean isCanonicalCourseExists(ModuleOffering module) throws Exception {
        boolean exists = false;
        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet rset = null;
        try {
            connection = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("select count(*) ");
            sql.append("from CM_MODULES m ");
            sql.append("where m.course_code = ? ");
            sql.append("and m.course_level = ? ");
            sql.append("and m.course_module = ? ");
            sql.append("and m.status <> ? ");
            pstmt = connection.prepareStatement(sql.toString());
            pstmt.setString(1, module.getModuleSubjectCode());
            pstmt.setString(2, module.getCourseLevel());
            pstmt.setString(3, module.getCourseModule());
            pstmt.setString(4, ModuleLinkStatus.DELETED.toString());
            rset = pstmt.executeQuery();
            if (rset.next()) {
                exists = rset.getInt(1) > 0;
            }
        }
        catch (ConnectionNotEstablishedException e) {
            log.error(
                "A SQL Connection could not be established while performing isCanonicalCourseExists", e);
            throw new ConnectionNotEstablishedException("A SQL Connection could not be established while performing isCanonicalCourseExists", e);
        }
        catch (SQLException e) {
            log.error("SQL error occurred while performing isCanonicalCourseExists", e);
            throw new SQLException("SQL error occurred while performing isCanonicalCourseExists", e);
        }
        finally {
            ConnectionManager.close(rset, pstmt, connection);
        }
        return exists;
    }

    /**
     * Returns a boolean indicating whether this course offering exists for another lecturer.
     */
    private boolean isCourseOfferingExists(ModuleOffering module, int year) throws Exception {
        boolean exists = false;
        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet rset = null;
        try {
            connection = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT COUNT(*) ");
            sql.append("FROM CM_MODULES m, CM_LECTURER l, CM_YEAR_CAMPUS c ");
            sql.append("WHERE m.status <> ? ");
            sql.append("AND m.course_code = ? ");
            sql.append("AND m.course_level = ? ");
            sql.append("AND m.course_module = ? ");
            sql.append("AND  m.lecturer_f_id = l.lecturer_id ");
            sql.append("AND l.year_campus_f_id = c.year_campus_id ");
            sql.append("AND c.year = ? ");
            sql.append("AND c.campus_code = ?");
            sql.append("AND method_of_del = ? ");
            sql.append("AND present_cat = ?");
            pstmt = connection.prepareStatement(sql.toString());
            pstmt.setString(1, ModuleLinkStatus.DELETED.toString());
            pstmt.setString(2, module.getModuleSubjectCode());
            pstmt.setString(3, module.getCourseLevel());
            pstmt.setString(4, module.getCourseModule());
            pstmt.setInt(5, year);
            pstmt.setString(6, module.getModuleSite());
            pstmt.setString(7, module.getMethodOfDeliveryCodeParam());
            pstmt.setString(8, module.getModeOfDeliveryCodeParam());     
            rset = pstmt.executeQuery();
            if (rset.next()) {
                exists = rset.getInt(1) > 0;
            }
        }
        catch (ConnectionNotEstablishedException e) {
            log.error(
                "A SQL Connection could not be established while performing isCourseOfferingExists", e);
            throw new ConnectionNotEstablishedException("A SQL Connection could not be established while performing isCourseOfferingExists", e);
        }
        catch (SQLException e) {
            log.error("SQL error occurred while performing isCourseOfferingExists", e);
            throw new SQLException("SQL error occurred while performing isCourseOfferingExists", e);
        }
        finally {
            ConnectionManager.close(rset, pstmt, connection);
        }
        return exists;
    }

    /**
     * Returns a boolean indicating whether only modules with 'deleted' status exists for the specified year.
     */
    private boolean isOnlyDeletedModulesExist(int year) throws Exception {
        boolean onlyDeletedModulesExist = false;
        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet rset = null;
        try {
            connection = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT COUNT(*) ");
            sql.append("FROM CM_MODULES m, CM_LECTURER l, CM_YEAR_CAMPUS c ");
            sql.append("WHERE m.status <> ? ");
            sql.append("and m.lecturer_f_id = l.lecturer_id ");
            sql.append("and l.year_campus_f_id = c.year_campus_id ");
            sql.append("and c.year = ? ");
            pstmt = connection.prepareStatement(sql.toString());
            pstmt.setString(1, ModuleLinkStatus.DELETED.toString());
            pstmt.setInt(2, year);
            rset = pstmt.executeQuery();
            if (rset.next()) {
                onlyDeletedModulesExist = rset.getInt(1) == 0;
            }
        }
        catch (ConnectionNotEstablishedException e) {
            log.error(
                "A SQL Connection could not be established while performing isOnlyDeletedModulesExist", e);
            throw new ConnectionNotEstablishedException("A SQL Connection could not be established while performing isOnlyDeletedModulesExist", e);
        }
        catch (SQLException e) {
            log.error("SQL error occurred while performing isOnlyDeletedModulesExist", e);
            throw new SQLException("SQL error occurred while performing isOnlyDeletedModulesExist", e);
        }
        finally {
            ConnectionManager.close(rset, pstmt, connection);
        }
        return onlyDeletedModulesExist;
    }

    /**
     * Delete the Sakai Course Management Data.
     * @param sessionId The active Sakai session id
     * @param year The year for which to delete the course management data.
     * @param courseCode The course code of the module (Example: AFNL)
     * @param canonicalCourseRef The canonical course reference (Example: AFNL 111)
     * @param courseOfferingRef The course offering reference (Example: AFNL 111 P 2011)
     * @param enrollmentSetRef The enrollment set reference (Example: AFNL 111 P 2011 ES)
     * @param lecturerUserName The lecturer name linked to this module.
     * @param studentUserNames A list of student user names linked to this module.
     * @param lecturerLinkedToCourseSet A boolean indicating whether the lecturer is linked to a Course Set (any year)
     * @param courseSetExists A boolean indicating whether this Course Set exists (any year)
     * @param canonicalCourseExists A boolean indicating whether this Canonical Course exists (any year)
     * @param courseOfferingExists A boolean indicating whether this Course Offering exists for other lecturers. 
     * @param onlyDeletedModulesExist A boolean indicating whether only modules with a deleted status exist for the year.
     * @return The result string. Only when the string equals "success" has the call been successful.
     * @throws Exception
     */
    private String callDeleteCMDataWS(String sessionId, int year, String courseCode,
            String canonicalCourseRef, String courseOfferingRef, String enrollmentSetRef,
            String lecturerUserName, String studentUserNames, boolean courseSetExists,
            boolean canonicalCourseExists, boolean courseOfferingExists,
            boolean onlyDeletedModulesExist) throws Exception {
		WebClient client = WebClient.create(settingsProperties.getProperty("sakai.rest.address") + "/nwu-cm/delete-cm-data");
		client = client.accept("application/json").type("application/json");
		client.query("sakai.session", sessionId);
		client.query("year", "" + year);
		client.query("courseCode", courseCode);
		client.query("canonicalCourseRef", canonicalCourseRef);
		client.query("courseOfferingRef", courseOfferingRef);
		client.query("enrollmentSetRef", enrollmentSetRef);
		client.query("lecturerUserName", lecturerUserName);
		client.query("studentUserNames", studentUserNames);
		client.query("courseSetExists", courseSetExists ? "true" : "false");
		client.query("canonicalCourseExists", canonicalCourseExists ? "true" : "false");
		client.query("courseOfferingExists", courseOfferingExists ? "true" : "false");
		client.query("onlyDeletedModulesExist", onlyDeletedModulesExist ? "true" : "false");
		Response response = client.post(null);
		int statusCode = response.getStatus();
		String results = null;
		if (statusCode == 200) {
			results = IOUtils.toString((InputStream) response.getEntity());
		}
        return results;
    }

    /**
     * Remove the module records with status equal to 'deleted'.
     */
    private void deleteDeletedDataStatus(int year) throws Exception {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("delete from CM_MODULES ");
            sql.append("where status = ? ");
            sql.append("and lecturer_f_id in ");
            sql.append("(select l.lecturer_id ");
            sql.append("from CM_LECTURER l, ");
            sql.append("CM_YEAR_CAMPUS c ");
            sql.append("where l.year_campus_f_id = c.year_campus_id ");
            sql.append("and c.year = ?)");
            pstmt = connection.prepareStatement(sql.toString());
            pstmt.setString(1, ModuleLinkStatus.DELETED.toString());
            pstmt.setInt(2, year);
            pstmt.executeUpdate();
        }
        catch (ConnectionNotEstablishedException e) {
            log.error("A SQL Connection could not be established while performing deleteDeletedDataStatus", e);
            throw new ConnectionNotEstablishedException("A SQL Connection could not be established while performing deleteDeletedDataStatus", e);
        }
        catch (SQLException e) {
            log.error("SQL error occurred while performing deleteDeletedDataStatus", e);
            throw new SQLException("SQL error occurred while performing deleteDeletedDataStatus", e);
        }
        finally {
            ConnectionManager.close(null, pstmt, connection);
        }
    }	

	@Autowired
	public void setSettingsProperties(SettingsProperties settingsProperties) {
		this.settingsProperties = settingsProperties;
	}
	
	@Autowired
	public void setConnectionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}
}