package za.ac.nwu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ac.za.nwu.academic.dates.dto.AcademicPeriodInfo;
import ac.za.nwu.common.dto.MetaInfo;
import ac.za.nwu.courseoffering.service.CourseOfferingService;
import ac.za.nwu.courseoffering.service.factory.CourseOfferingServiceClientFactory;
import ac.za.nwu.registry.utility.GenericServiceClientFactory;
import ac.za.nwu.utility.ServiceRegistryLookupUtility;
import assemble.edu.common.dto.ContextInfo;
import assemble.edu.exceptions.DoesNotExistException;
import assemble.edu.exceptions.MissingParameterException;
import assemble.edu.exceptions.OperationFailedException;
import assemble.edu.exceptions.PermissionDeniedException;
import nwu.student.assesment.service.crud.StudentAssessmentServiceCRUD;
import nwu.student.assesment.service.crud.factory.StudentAssessmentCRUDServiceClientFactory;

/**
 * 
 * @author Joseph Gillman
 *
 */
public class RepublishNWUGradebookData {

	private static final Logger log = LogManager.getLogger(RepublishNWUGradebookData.class);
	
	private static Connection connection = null;
	private static PropertiesHolder properties = null;
	private static StudentAssessmentServiceCRUD studentAssessmentServiceCRUDService = null;
	private static AcademicPeriodInfo academicPeriodInfo = null;
	private static MetaInfo metaInfo = null;
	private static ContextInfo contextInfo = new ContextInfo("EFUNDI");
	private static CourseOfferingService courseOfferingService = null;

	private final static String NWU_GRDB_RECORDS_SELECT = "SELECT * FROM NWU_GRADEBOOK_DATA WHERE STATUS != \"SUCCESS\" AND RETRY_COUNT < ? AND ((MODIFIED_DATE IS NULL AND CREATED_DATE BETWEEN ? AND ?) OR (MODIFIED_DATE IS NOT NULL AND MODIFIED_DATE BETWEEN ? AND ?)) ORDER BY SITE_TITLE, MODULE, ASSESSMENT_NAME";
		
    public static void main(String args[]) {

    	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        log.info("Start Republishing NWU Gradebook Data at " + dtf.format(LocalDateTime.now()));
        
        try {
            
        	LocalDateTime startLocalDateTime = LocalDateTime.parse(args[0]);
        	LocalDateTime endLocalDateTime = LocalDateTime.parse(args[1]);
        	            
        	properties = new PropertiesHolder();

            try {
//            	connection = DriverManager.getConnection(properties.getUrl(),
//            			properties.getUsername(),
//            			properties.getPassword());
            	
                log.info("Database connection successfully made.");

        		boolean initializeSuccess = initializeWebserviceObjects();
        		if(!initializeSuccess) {
        			log.error("initializeWebserviceObjects was unsuccessful, please see error log");
        			return;
        		}	
        		
                republishGradebookDataToVSS(startLocalDateTime, endLocalDateTime);
                
            } finally  {
                if (connection != null) {
                	connection.close();
                }
            }
        } catch (DateTimeParseException e) {
        	log.error("DateTimeParseException: Please make sure both start & end date paramters has this format: yyyy-MM-ddTHH:mm:ss ie (2021-01-01T12:00:00) ");
        	
        } catch (Exception e) {
        	log.error("Could not publish Gradebook Data to VSS. See error log: " + e);
        }

        log.info("Migration complete! Fisnished at " + dtf.format(LocalDateTime.now()));
    }
    
    /**
     * 
     * @param startLocalDateTime
     * @param endLocalDateTime
     * @throws SQLException
     */
    private static void republishGradebookDataToVSS(LocalDateTime startLocalDateTime, LocalDateTime endLocalDateTime) throws SQLException {

		PreparedStatement nwuGradebookRecordsSelectPrepStmt = null;
		ResultSet nwuGradebookRecordsSelectResultSet = null;
		
		String maxRetry = properties.getNWURepublishMaxRetry();
		
		// #1 Get all NWU_GRADEBOOK_DATA records with STATUS not equal SUCCESS, RETRY_COUNT is less than allowed max, and dates between startDate and endDate
		nwuGradebookRecordsSelectPrepStmt = connection.prepareStatement(NWU_GRDB_RECORDS_SELECT);
		nwuGradebookRecordsSelectPrepStmt.setInt(1, Integer.parseInt(maxRetry));
		nwuGradebookRecordsSelectPrepStmt.setTimestamp(2, Timestamp.valueOf(startLocalDateTime));
		nwuGradebookRecordsSelectPrepStmt.setTimestamp(3, Timestamp.valueOf(endLocalDateTime));
		nwuGradebookRecordsSelectPrepStmt.setTimestamp(4, Timestamp.valueOf(startLocalDateTime));
		nwuGradebookRecordsSelectPrepStmt.setTimestamp(5, Timestamp.valueOf(endLocalDateTime));
		nwuGradebookRecordsSelectResultSet = nwuGradebookRecordsSelectPrepStmt.executeQuery();
		
		int id;
		String siteId = "", siteTitle = "", module = "", assessmentName = "", studentNumber = "", evalDescr = "", status = "", description = "";
		HashMap<Integer, Double> studentGradeMap = null;
		double grade, total = 0.0;
		int evalDescrId;
		int gradableObjectId;
		int maxRetryCount;
		LocalDateTime dueDate = null;
		LocalDateTime recordedDate = null;
		LocalDateTime createdDate = null;
		LocalDateTime modifiedDate = null;
		NWUGradebookRecord nwuGradebookRecord = null;
		
		while (nwuGradebookRecordsSelectResultSet.next()) {
			
			nwuGradebookRecord = new NWUGradebookRecord();
			
			id = nwuGradebookRecordsSelectResultSet.getInt("ID");			
			String tempSiteId = nwuGradebookRecordsSelectResultSet.getString("SITE_ID");
			String tempSiteTitle = nwuGradebookRecordsSelectResultSet.getString("SITE_TITLE");
			String tempModule = nwuGradebookRecordsSelectResultSet.getString("MODULE");
			String tempAssessmentName = nwuGradebookRecordsSelectResultSet.getString("ASSESSMENT_NAME");
			String tempStudentNumber = nwuGradebookRecordsSelectResultSet.getString("STUDENT_NUMBER");
			evalDescrId = nwuGradebookRecordsSelectResultSet.getInt("EVAL_DESCR_ID");
			grade = nwuGradebookRecordsSelectResultSet.getDouble("GRADE");
			total = nwuGradebookRecordsSelectResultSet.getDouble("TOTAL_MARK");
			gradableObjectId = nwuGradebookRecordsSelectResultSet.getInt("GRADABLE_OBJECT_ID");
			recordedDate = nwuGradebookRecordsSelectResultSet.getTimestamp("RECORDED_DATE").toLocalDateTime();
			dueDate = nwuGradebookRecordsSelectResultSet.getTimestamp("DUE_DATE").toLocalDateTime();
			createdDate = nwuGradebookRecordsSelectResultSet.getTimestamp("CREATED_DATE").toLocalDateTime();
			modifiedDate = nwuGradebookRecordsSelectResultSet.getTimestamp("MODIFIED_DATE").toLocalDateTime();
			status = nwuGradebookRecordsSelectResultSet.getString("STATUS");
			maxRetryCount = nwuGradebookRecordsSelectResultSet.getInt("RETRY_COUNT");
			description = nwuGradebookRecordsSelectResultSet.getString("DESCRIPTION");
			

			
			if(nwuGradebookRecordsSelectResultSet.isLast()) {
				studentGradeMap.put(Integer.parseInt(studentNumber), grade);
				
				// publishGrades
			} else {
				
				// compare with previous row - if same siteId, module & assessmentName, add student to Map
				if(siteId.equals("") && module.equals("") && assessmentName.equals("")) { // first row, add to map and set temp values

					studentGradeMap = new HashMap<>();
					studentGradeMap.put(Integer.parseInt(studentNumber), grade);
					siteId = tempSiteId;
					module = tempModule;
					assessmentName = tempAssessmentName;				
				}
				else if(!siteId.equals(tempSiteId) || !module.equals(tempModule) || !assessmentName.equals(tempAssessmentName)) { // If any of these values differ from previous, send previous student list

					siteId = tempSiteId;
					module = tempModule;
					assessmentName = tempAssessmentName;

					studentGradeMap = new HashMap<>();
					studentGradeMap.put(Integer.parseInt(studentNumber), grade);

				} else if (siteId.equals(tempSiteId) && module.equals(tempModule) && assessmentName.equals(tempAssessmentName)) { // first row or new site
					
					studentGradeMap.put(Integer.parseInt(studentNumber), grade);
					
				}
			}
			
			
			
			
//			nwuGradebookRecordsInsertPrepStmt.setString(1, siteId);
//			nwuGradebookRecordsInsertPrepStmt.setString(2, siteTitle);
//			nwuGradebookRecordsInsertPrepStmt.setString(3, module);
//			nwuGradebookRecordsInsertPrepStmt.setString(4, assessmentName);					
//			nwuGradebookRecordsInsertPrepStmt.setString(5, studentNumber);
//			nwuGradebookRecordsInsertPrepStmt.setInt(6, evalDescrId);
//			nwuGradebookRecordsInsertPrepStmt.setDouble(7, grade);
//			nwuGradebookRecordsInsertPrepStmt.setDouble(8, total);
//			nwuGradebookRecordsInsertPrepStmt.setInt(9, gradableObjectId);
//			nwuGradebookRecordsInsertPrepStmt.setTimestamp(10, Timestamp.valueOf(recordedDate));
//			nwuGradebookRecordsInsertPrepStmt.setTimestamp(11, Timestamp.valueOf(dueDate));	
//			nwuGradebookRecordsInsertPrepStmt.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));							
//			nwuGradebookRecordsInsertPrepStmt.setString(13, NWUGradebookRecord.STATUS_NEW);
//			nwuGradebookRecordsInsertPrepStmt.setInt(14, 0);
//			
//			
//			siteId, siteTitle, studentNumber, assessmentName, studentGradeMap, grade, total, evalDescrId, dueDate, recordedDate, gradableObjectId, module
		}
	}

	/**
	 * 
	 * @return
	 */
	private static boolean initializeWebserviceObjects() {
		if(academicPeriodInfo == null) {
			Calendar calendar = Calendar.getInstance();	
			academicPeriodInfo = new AcademicPeriodInfo();
			academicPeriodInfo.setAcadPeriodtTypeKey("vss.code.AcademicPeriod.YEAR");
			academicPeriodInfo.setAcadPeriodValue(Integer.toString(calendar.get(Calendar.YEAR)));
		}
		
		if (metaInfo == null) {
			metaInfo = new MetaInfo();
			metaInfo.setCreateId(properties.getWSMetaInfoCreateId());
			metaInfo.setAuditFunction(properties.getWSMetaInfoAuditFunction());
		}
		
		if(studentAssessmentServiceCRUDService == null) {
			String studentServiceLookupKey = ServiceRegistryLookupUtility.getServiceRegistryLookupKey(properties.getWSRuntimeEnvironment(),
	                StudentAssessmentCRUDServiceClientFactory.STUDENTASSESSMENTCRUDSERVICE, properties.getWSMajorVersion(), properties.getWSDatabase());

			try {
				studentAssessmentServiceCRUDService = (StudentAssessmentServiceCRUD) GenericServiceClientFactory.getService(
				        studentServiceLookupKey, properties.getWSUsername(), properties.getWSPassword(), StudentAssessmentServiceCRUD.class);
			} catch (PermissionDeniedException e) {
				log.error("initializeWebserviceObjects - Initializing StudentAssessmentServiceCRUD failed with PermissionDeniedException: ", e);
				return false;
			} catch (DoesNotExistException e) {
				log.error("initializeWebserviceObjects - Initializing StudentAssessmentServiceCRUD failed with DoesNotExistException: ", e);
				return false;
			} catch (MissingParameterException e) {
				log.error("initializeWebserviceObjects - Initializing StudentAssessmentServiceCRUD failed with MissingParameterException: ", e);
			} catch (OperationFailedException e) {
				log.error("initializeWebserviceObjects - Initializing StudentAssessmentServiceCRUD failed with OperationFailedException: ", e);
				return false;
			}
		}
		
		if (courseOfferingService == null) {
			try {
				courseOfferingService = (CourseOfferingService) CourseOfferingServiceClientFactory
						.getCourseOfferingService(properties.getWSModuleEnvTypeKey(), properties.getNWUContextInfoUsername(), properties.getNWUContextInfoPassword());
			} catch (PermissionDeniedException e) {
				log.error("initializeWebserviceObjects - Initializing CourseOfferingService failed with PermissionDeniedException: ", e);
				return false;
			} catch (DoesNotExistException e) {
				log.error("initializeWebserviceObjects - Initializing CourseOfferingService failed with DoesNotExistException: ", e);
				return false;
			} catch (MissingParameterException e) {
				log.error("initializeWebserviceObjects - Initializing CourseOfferingService failed with MissingParameterException: ", e);
				return false;
			} catch (OperationFailedException e) {
				log.error("initializeWebserviceObjects - Initializing CourseOfferingService failed with OperationFailedException: ", e);
				return false;
			}
		}

        log.info("Initializing Webservice Objects was successful.");
		return true;
	}
}
