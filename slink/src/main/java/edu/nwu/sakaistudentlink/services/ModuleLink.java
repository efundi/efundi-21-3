package edu.nwu.sakaistudentlink.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author Jaco Gillman
 *
 */
public class ModuleLink {

    private static final Logger log = LoggerFactory.getLogger(ModuleLink.class);
    
	private SettingsProperties settingsProperties;

	private ConnectionManager connectionManager;
	
	private CourseManagementWSHandler courseMgmtWSHandler;

	public List<ModuleOffering> getAllModulesLinkedToLecturer(Map<SearchCriteria, String> criteria) throws IntegrationException {
        List<ModuleOffering> linkedModules = new ArrayList<ModuleOffering>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rset = null;
        ModuleOffering moduleOffering = null;
        try {
            conn = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("select m.course_code, m.course_level, m.course_module, m.method_of_del, m.present_cat ");
            sql.append("from CM_YEAR_CAMPUS c, CM_LECTURER l, CM_MODULES m ");
            sql.append("where c.year = ? ");
            sql.append("and c.campus_code = ? ");
            sql.append("and l.year_campus_f_id = c.year_campus_id ");
            sql.append("and l.username = ? ");
            sql.append("and m.lecturer_f_id = l.lecturer_id ");
            sql.append("and m.status <> ? ");

            String methodOfDel = criteria.get(SearchCriteria.METHOD_OF_DEL) == null ? null : criteria.get(SearchCriteria.METHOD_OF_DEL);
            if(methodOfDel != null){
                sql.append("and m.method_of_del = ? ");
            }
            String modeOfDel = criteria.get(SearchCriteria.PRESENT_CAT) == null ? null : criteria.get(SearchCriteria.PRESENT_CAT);
            if(modeOfDel != null){
                sql.append("and m.present_cat = ? ");
            }
            pstmt = conn.prepareStatement(sql.toString());
            int cnt = 1;
                     
            pstmt.setInt(cnt++, getYear(criteria.get(SearchCriteria.YEAR)));
            pstmt.setString(cnt++, criteria.get(SearchCriteria.CAMPUS));
            pstmt.setString(cnt++, criteria.get(SearchCriteria.USER_NAME));
            pstmt.setString(cnt++, ModuleLinkStatus.DELETED.toString());
            if(methodOfDel != null){
                pstmt.setString(cnt++, "vss.code.ENROLCAT." + methodOfDel);
            }   
            if(modeOfDel != null){
                pstmt.setString(cnt++, "vss.code.PRESENTCAT." + modeOfDel);
            }             
            rset = pstmt.executeQuery();
            while (rset.next()) {            	
            	moduleOffering = new ModuleOffering();
				moduleOffering.setModuleSubjectCode(rset.getString(1));
				moduleOffering.setModuleNumber(rset.getString(2) + rset.getString(3));
				moduleOffering.setModuleSite(criteria.get(SearchCriteria.CAMPUS));
				moduleOffering.setMethodOfDeliveryTypeKey(rset.getString(4));
				moduleOffering.setModeOfDeliveryTypeKey(rset.getString(5));
                linkedModules.add(moduleOffering);
            }
        }
        catch (ConnectionNotEstablishedException e) {
			log.error("A SQL Connection could not be established to the Sakai database.", e);
			IntegrationException ie = new IntegrationException("A SQL Connection could not be established to the Sakai database. ", e);
			IntegrationError error = new IntegrationError();
			error.setErrorMessage(e.getMessage());
			ie.addError(error);
			throw ie;        }
        catch (SQLException e) {
			log.error("A SQL Error occurred while performing getAllModulesLinkedToLecturer.", e);
			IntegrationException ie = new IntegrationException("A SQL Error occurred while performing getAllModulesLinkedToLecturer. ", e);
			IntegrationError error = new IntegrationError();
			error.setErrorMessage(e.getMessage());
			ie.addError(error);
			throw ie;
        }
        finally {
            ConnectionManager.close(rset, pstmt, conn);
        }
        return linkedModules;
    }

    public void linkInstructorToModules(List<ModuleOffering> modules,
            Map<SearchCriteria, String> criteria) throws IntegrationException {
        final int year = getYear(criteria.get(SearchCriteria.YEAR));
        final String campusNumber = criteria.get(SearchCriteria.CAMPUS);
        final String userName = criteria.get(SearchCriteria.USER_NAME);      
        int yearCampusId;
		try {
			yearCampusId = getYearCampusId(year, campusNumber);

	        if (yearCampusId == 0) {// year_campus doesn't exists
	            insertYearCampus(year, campusNumber);
	            yearCampusId = getYearCampusId(year, campusNumber);
	        }
	        int lecturerId = getLecturerId(userName, year, campusNumber);
	        if (lecturerId == 0) {//lecturer doesn't exist
	            insertLecturer(yearCampusId, userName);
	            lecturerId = getLecturerId(userName, year, campusNumber);
	        }
	        for (ModuleOffering module : modules) {
	            insertModule(lecturerId, module);
	            if ("webservice".equals(settingsProperties.getProperty("sakai.cm.insertion.type",
	                "webservice"))) {
	            	courseMgmtWSHandler.insertSakaiCMData(year, module, userName);
	            }
	        }
		} catch (ConnectionNotEstablishedException e) {
			log.error("A SQL Connection could not be established to the Sakai database.", e);
			IntegrationException ie = new IntegrationException("A SQL Connection could not be established to the Sakai database. ", e);
			IntegrationError error = new IntegrationError();
			error.setErrorMessage(e.getMessage());
			ie.addError(error);
			throw ie;
		}
    }

    public void unlinkInstructorFromModules(List<ModuleOffering> modules,
            Map<SearchCriteria, String> criteria) throws IntegrationException {
        final int year = getYear(criteria.get(SearchCriteria.YEAR));
        final String userName = criteria.get(SearchCriteria.USER_NAME);
        int lecturerId;
		try {
			lecturerId = getLecturerId(userName, year, criteria.get(SearchCriteria.CAMPUS));

	        for (ModuleOffering module : modules) {
	            //If the module was set to 'Done' status - update it.
	            int recordsUpdated = updateModuleLink(module, lecturerId);
	            if (recordsUpdated == 0) {
	                //If the module's status is 'Inserted', delete the record.
	                deleteModuleLink(module, lecturerId);
	            }
	        }
	        //the following must be done in a separate 'for loop' since its dependent on the status settings of all the modules
	        if ("webservice".equals(settingsProperties.getProperty("sakai.cm.insertion.type",
	            "webservice"))) {
	        	courseMgmtWSHandler.deleteSakaiCMData(year, modules, userName);
	        }
		} catch (ConnectionNotEstablishedException e) {
			log.error("A SQL Connection could not be established to the Sakai database.", e);
			IntegrationException ie = new IntegrationException("A SQL Connection could not be established to the Sakai database. ", e);
			IntegrationError error = new IntegrationError();
			error.setErrorMessage(e.getMessage());
			ie.addError(error);
			throw ie;
		}
    }

    private int getYearCampusId(int year, String campusCode) throws ConnectionNotEstablishedException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rset = null;
        int yearCampusId = 0;
        try {
            conn = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("select year_campus_id ");
            sql.append("from CM_YEAR_CAMPUS ");
            sql.append("where year = ? ");
            sql.append("and campus_code = ?");
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setInt(1, year);
            pstmt.setString(2, campusCode);
            rset = pstmt.executeQuery();
            while (rset.next()) {
                yearCampusId = rset.getInt(1);
            }
        }
        catch (ConnectionNotEstablishedException e) {
            log.error("A SQL Connection could not be established while retrieving campus", e);
            throw new ConnectionNotEstablishedException(
            	"A SQL Connection could not be established while retrieving campus.", e);
        }
        catch (SQLException e) {
            log.error("A SQL Error occurred while retrieving campus data", e);
            throw new ConnectionNotEstablishedException(
                    "A SQL Error occurred while retrieving campus data", e);
        }
        finally {
            ConnectionManager.close(rset, pstmt, conn);
        }
        return yearCampusId;
    }

    private void insertYearCampus(int year, String campusCode) throws ConnectionNotEstablishedException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("insert into ");
            sql.append("CM_YEAR_CAMPUS (year, campus_code) ");
            sql.append("values (?, ?)");
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setInt(1, year);
            pstmt.setString(2, campusCode);
            pstmt.executeUpdate();
        }
        catch (ConnectionNotEstablishedException e) {
            log.error(
                "A SQL Connection could not be established while inserting campus data", e);
            throw new ConnectionNotEstablishedException(
                    "A SQL Connection could not be established while inserting campus data", e);
        }
        catch (SQLException e) {
            log.error("A SQL Error occurred while performing insertYearCampus", e);
            throw new ConnectionNotEstablishedException(
                    "A SQL Error occurred while inserting campus data.", e);
        }
        finally {
            ConnectionManager.close(null, pstmt, conn);
        }
    }

    private int getLecturerId(String userName, int year, String campusCode) throws ConnectionNotEstablishedException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rset = null;
        int lecturerId = 0;
        try {
            conn = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("select l.lecturer_id ");
            sql.append("from CM_LECTURER l, ");
            sql.append("CM_YEAR_CAMPUS c ");
            sql.append("where l.year_campus_f_id = c.year_campus_id ");
            sql.append("and l.username = ? ");
            sql.append("and c.year = ? ");
            sql.append("and c.campus_code = ? ");
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setString(1, userName);
            pstmt.setInt(2, year);
            pstmt.setString(3, campusCode);
            rset = pstmt.executeQuery();
            while (rset.next()) {
                lecturerId = rset.getInt(1);
            }
        }
        catch (ConnectionNotEstablishedException e) {
            log.error("A SQL Connection could not be established while performing getLecturerId", e);
            throw new ConnectionNotEstablishedException(
                    "A SQL Connection could not be established while retrieving Lecturer.", e);
        }
        catch (SQLException e) {
            log.error("SQL Error occurred while retrieving Lecturer", e);
            throw new ConnectionNotEstablishedException(
                    "SQL Error occurred while retrieving Lecturer.", e);
        }
        finally {
            ConnectionManager.close(rset, pstmt, conn);
        }
        return lecturerId;
    }

    private void insertLecturer(int campusId, String userName) throws ConnectionNotEstablishedException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("insert into ");
            sql.append("CM_LECTURER (year_campus_f_id, username) ");
            sql.append("values (?, ?)");
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setInt(1, campusId);
            pstmt.setString(2, userName);
            pstmt.executeUpdate();
        }
        catch (ConnectionNotEstablishedException e) {
            log.error("A SQL Connection could not be established while performing insertLecturer",
                e);
            throw new ConnectionNotEstablishedException(
                    "A SQL Connection could not be established while inserting Lecturer.", e);
        }
        catch (SQLException e) {
            log.error("A SQL Error occurred while performing insertLecturer", e);
            throw new ConnectionNotEstablishedException(
                    "SQL Error occurred while inserting Lecturer.", e);
        }
        finally {
            ConnectionManager.close(null, pstmt, conn);
        }
    }

    private void insertModule(int lecturerId, ModuleOffering module) throws ConnectionNotEstablishedException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("insert into ");
            sql.append("CM_MODULES (lecturer_f_id, course_code, course_level, course_module, status, method_of_del, present_cat)");
            sql.append("values (?, ?, ?, ?, ?, ?, ?)");
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setInt(1, lecturerId);
            pstmt.setString(2, module.getModuleSubjectCode());
            pstmt.setString(3, module.getCourseLevel());
            pstmt.setString(4, module.getCourseModule());
            pstmt.setString(5, ModuleLinkStatus.INSERTED.toString());
            pstmt.setString(6, module.getMethodOfDeliveryCodeParam());
            pstmt.setString(7, module.getModeOfDeliveryCodeParam());
            pstmt.executeUpdate();
        }
        catch (ConnectionNotEstablishedException e) {
            log.error("A SQL Connection could not be established while performing insertModule", e);
            throw new ConnectionNotEstablishedException(
                    "A SQL Connection could not be established while inserting module.", e);
        }
        catch (SQLException e) {
            log.error("A SQL Error occurred while performing insertModule", e);
            throw new ConnectionNotEstablishedException(
                    "SQL Error occurred while while inserting module.", e);
        }
        finally {
            ConnectionManager.close(null, pstmt, conn);
        }
    }

    private int updateModuleLink(ModuleOffering module, int lecturerId) throws ConnectionNotEstablishedException {
        int recordsUpdated = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("update CM_MODULES ");
            sql.append("set status = ? ");
            sql.append("where course_code = ? ");
            sql.append("and course_level = ? ");
            sql.append("and course_module = ? ");
            sql.append("and lecturer_f_id = ? ");
            sql.append("and status = ?");
            sql.append("and method_of_del = ? ");
            sql.append("and present_cat = ?");
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setString(1, ModuleLinkStatus.DELETED.toString());
            pstmt.setString(2, module.getModuleSubjectCode());
            pstmt.setString(3, module.getCourseLevel());
            pstmt.setString(4, module.getCourseModule());
            pstmt.setInt(5, lecturerId);
            pstmt.setString(6, ModuleLinkStatus.DONE.toString());
            pstmt.setString(7, module.getMethodOfDeliveryCodeParam());
            pstmt.setString(8, module.getModeOfDeliveryCodeParam());
            recordsUpdated = pstmt.executeUpdate();
        }
        catch (ConnectionNotEstablishedException e) {
            log.error(
                "A SQL Connection could not be established while updating module link", e);
            throw new ConnectionNotEstablishedException(
                    "A SQL Connection could not be established while updating module link.", e);
        }
        catch (SQLException e) {
            log.error("A SQL Error occurred while updating module link", e);
            throw new ConnectionNotEstablishedException(
                    "SQL Error occurred while updating module link.", e);
        }
        finally {
            ConnectionManager.close(null, pstmt, conn);
        }
        return recordsUpdated;
    }

    private void deleteModuleLink(ModuleOffering module, int lecturerId) throws ConnectionNotEstablishedException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = connectionManager.getCourseManagementConnection();
            StringBuilder sql = new StringBuilder();
            sql.append("delete from CM_MODULES ");
            sql.append("where course_code = ? ");
            sql.append("and course_level = ? ");
            sql.append("and course_module = ? ");
            sql.append("and lecturer_f_id = ? ");
            sql.append("and status = ? ");
            sql.append("and method_of_del = ? ");
            sql.append("and present_cat = ?");
            pstmt = conn.prepareStatement(sql.toString());
            pstmt.setString(1, module.getModuleSubjectCode());
            pstmt.setString(2, module.getCourseLevel());
            pstmt.setString(3, module.getCourseModule());
            pstmt.setInt(4, lecturerId);
            pstmt.setString(5, ModuleLinkStatus.INSERTED.toString());
            pstmt.setString(6, module.getMethodOfDeliveryCodeParam());
            pstmt.setString(7, module.getModeOfDeliveryCodeParam());
            pstmt.executeUpdate();
        }
        catch (ConnectionNotEstablishedException e) {
            log.error(
                "A SQL Connection could not be established while deleting module link", e);
            throw new ConnectionNotEstablishedException(
                    "A SQL Connection could not be established while deleting module link.", e);
        }
        catch (SQLException e) {
            log.error("A SQL Error occurred while deleting module link", e);
            throw new ConnectionNotEstablishedException(
                    "SQL Error occurred while deleting module link.", e);
        }
        finally {
            ConnectionManager.close(null, pstmt, conn);
        }
    }

    private int getYear(String year) {
        return Integer.parseInt(year);
    }

	@Autowired
	public void setSettingsProperties(SettingsProperties settingsProperties) {
		this.settingsProperties = settingsProperties;
	}

	@Autowired
    public void setConnectionManager(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Autowired
	public void setCourseMgmtWSHandler(CourseManagementWSHandler courseMgmtWSHandler) {
		this.courseMgmtWSHandler = courseMgmtWSHandler;
	}
}