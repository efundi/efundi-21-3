package za.ac.nwu;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ac.za.nwu.academic.dates.dto.AcademicPeriodInfo;
import ac.za.nwu.common.dto.MetaInfo;
import ac.za.nwu.courseoffering.service.CourseOfferingService;
import ac.za.nwu.courseoffering.service.factory.CourseOfferingServiceClientFactory;
import ac.za.nwu.moduleoffering.dto.ModuleOfferingInfo;
import ac.za.nwu.moduleoffering.dto.ModuleOfferingSearchCriteriaInfo;
import ac.za.nwu.registry.utility.GenericServiceClientFactory;
import ac.za.nwu.utility.ServiceRegistryLookupUtility;
import assemble.edu.common.dto.ContextInfo;
import assemble.edu.exceptions.DoesNotExistException;
import assemble.edu.exceptions.InvalidParameterException;
import assemble.edu.exceptions.MissingParameterException;
import assemble.edu.exceptions.OperationFailedException;
import assemble.edu.exceptions.PermissionDeniedException;
import nwu.student.assesment.service.StudentAssessmentService;
import nwu.student.assesment.service.crud.StudentAssessmentServiceCRUD;
import nwu.student.assesment.service.dto.ClassGroupCourseCriteriaInfo;
import nwu.student.assesment.service.dto.ClassGroupInfo;
import nwu.student.assesment.service.dto.MaintainStudentResponseWrapper;
import nwu.student.assesment.service.dto.StudentMarkInfo;
import nwu.student.assesment.service.factory.StudentAssessmentServiceClientFactory;
import za.ac.nwu.registry.EtcdRegistryClient;

/**
 * @author Joseph Gillman
 */
public final class NWUGradebookPublishUtil {
	private static final Logger log = LogManager.getLogger(NWUGradebookPublishUtil.class);

	private static NWUGradebookPublishUtil INSTANCE;

	private static PropertiesHolder properties = new PropertiesHolder();
	private static StudentAssessmentService studentAssessmentService = null;
	private static StudentAssessmentServiceCRUD studentAssessmentServiceCRUDService = null;
	private static MetaInfo metaInfo = null;
	private static ContextInfo contextInfo = new ContextInfo("EFUNDI");
	private static CourseOfferingService courseOfferingService = null;
	private Map<Long, List<NWUGradebookRecord>> studentInfoMap = null;
	private static boolean initializeSuccess = false;
	private static String dbUrl, dbUsername, dbPassword;
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

	public final String SUCCESS = "SUCCESS";
	public final String ERROR = "ERROR";

	public final static String SYSTEM_LANGUAGE_TYPE_KEY = "vss.code.LANGUAGE.2";
	public final static String CLASS_GROUP_TYPE_COMPLETE = "vss.code.CLASSGROUPTYPE.V.Complete";
	public final static String CLASS_GROUP_TYPE_VOLLEDIG = "vss.code.CLASSGROUPTYPE.V.Volledig";

	private final static String STUDENT_GRDB_MARKS_SELECT = "SELECT gr.STUDENT_ID, gr.POINTS_EARNED, gr.GRADABLE_OBJECT_ID, gr.DATE_RECORDED, go.NAME, go.POINTS_POSSIBLE, go.DUE_DATE "
			+ " FROM gb_grade_record_t gr JOIN gb_gradable_object_t go ON go.ID = gr.GRADABLE_OBJECT_ID JOIN gb_grade_map_t gm ON gm.GRADEBOOK_ID = go.GRADEBOOK_ID JOIN gb_gradebook_t g ON "
			+ " g.SELECTED_GRADE_MAPPING_ID = gm.ID WHERE go.ID = ? AND g.NAME = ? AND go.DUE_DATE IS NOT NULL AND gr.STUDENT_ID IN (";

	//FOR LOCAL TESTING - NWU DOES NOT HAVE ENTRIES FOR STUDENTS IN sakai_user_id_map
//	private final static String STUDENT_GRDB_MARKS_SELECT = "SELECT su.EID, gr.POINTS_EARNED, gr.GRADABLE_OBJECT_ID, gr.DATE_RECORDED, go.NAME, go.POINTS_POSSIBLE, go.DUE_DATE "
//			+ " FROM gb_grade_record_t gr JOIN gb_gradable_object_t go ON go.ID = gr.GRADABLE_OBJECT_ID JOIN gb_grade_map_t gm ON gm.GRADEBOOK_ID = go.GRADEBOOK_ID JOIN gb_gradebook_t g ON "
//			+ " g.SELECTED_GRADE_MAPPING_ID = gm.ID JOIN sakai_user_id_map su ON su.USER_ID = gr.STUDENT_ID WHERE go.ID = ? AND g.NAME = ? AND go.DUE_DATE IS NOT NULL AND su.EID IN (";
	
//	private final static String STUDENT_GRDB_ALL_MARKS_SELECT = "SELECT gr.STUDENT_ID, gr.POINTS_EARNED, gr.GRADABLE_OBJECT_ID, gr.DATE_RECORDED, go.NAME, go.POINTS_POSSIBLE, go.DUE_DATE "
//			+ " FROM gb_grade_record_t gr JOIN gb_gradable_object_t go ON go.ID = gr.GRADABLE_OBJECT_ID JOIN gb_grade_map_t gm ON gm.GRADEBOOK_ID = go.GRADEBOOK_ID JOIN gb_gradebook_t g ON "
//			+ " g.SELECTED_GRADE_MAPPING_ID = gm.ID WHERE go.ID = ? AND g.NAME = ? AND go.DUE_DATE IS NOT NULL";

	private final static String NWU_GRDB_RECORDS_SELECT = "SELECT * FROM NWU_GRADEBOOK_DATA WHERE SITE_ID = ? AND STUDENT_NUMBER = ? AND GRADABLE_OBJECT_ID = ? AND MODULE = ?";
	private final static String NWU_GRDB_RECORDS_INSERT = "INSERT INTO NWU_GRADEBOOK_DATA (SITE_ID, SITE_TITLE, MODULE, ASSESSMENT_NAME, STUDENT_NUMBER, EVAL_DESCR_ID, GRADE, "
			+ "TOTAL_MARK, GRADABLE_OBJECT_ID, RECORDED_DATE, DUE_DATE, CREATED_DATE, STATUS, RETRY_COUNT) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private final static String NWU_GRDB_RECORDS_GRADE_UPDATE = "UPDATE NWU_GRADEBOOK_DATA SET GRADE = ?, RECORDED_DATE = ?, MODIFIED_DATE = ?, STATUS = ? WHERE ID = ?";
	private final static String NWU_GRDB_RECORDS_STATUS_UPDATE = "UPDATE NWU_GRADEBOOK_DATA SET STATUS = ?, MODIFIED_DATE = ?, DESCRIPTION = ?, RETRY_COUNT = RETRY_COUNT + 1 WHERE SITE_ID = ? AND STUDENT_NUMBER = ? AND MODULE = ?";

	private final static String NWU_GRDB_INFO_SELECT = "SELECT * FROM NWU_GRADEBOOK_DATA WHERE SITE_ID = ? AND GRADABLE_OBJECT_ID = ?";

	private final static String SITE_TITLE_SELECT = "SELECT TITLE FROM sakai_site where SITE_ID = ?";

	private final static String NWU_EVAL_DESCR_SELECT = "SELECT EVAL_DESCR FROM nwu_site_evaluation where SITE_ID = ? AND MODULE = ? AND ASSIGNMENT_ID = ?";
	private final static String NWU_EVAL_DESCRID_SELECT = "SELECT ID FROM nwu_site_evaluation where SITE_ID = ? AND MODULE = ? AND ASSIGNMENT_ID = ? AND EVAL_DESCR = ?";
	private final static String NWU_SITE_EVAL_INSERT = "INSERT INTO nwu_site_evaluation (SITE_ID, MODULE, ASSIGNMENT_ID, EVAL_DESCR) VALUES (?,?,?,?)";

	private final static String SAKAI_USER_ID_SELECT = "SELECT USER_ID FROM SAKAI_USER_ID_MAP WHERE EID = ?";

	/**
	 * @param dbUrl
	 * @param dbUsername
	 * @param dbPassword
	 */
	private NWUGradebookPublishUtil(String dbUrl, String dbUsername, String dbPassword) {
		this.dbUrl = dbUrl;
		this.dbUsername = dbUsername;
		this.dbPassword = dbPassword;
	}

	public static NWUGradebookPublishUtil getInstance(String dbUrl, String dbUsername, String dbPassword) {
		if (INSTANCE == null) {
			INSTANCE = new NWUGradebookPublishUtil(dbUrl, dbUsername, dbPassword);
		}
		return INSTANCE;
	}

	/**
	 * Get studentInfo for assignmentIds
	 * @param sectionUsersMap 
	 * 
	 * @param string
	 * @param assignmentIds
	 * @return
	 */
	public Map<Long, List<NWUGradebookRecord>> getStudentInfoMap(String siteId, Map<String, List<String>> sectionUsersMap, List<Long> assignmentIds) {
		
		Connection connection = null;
		PreparedStatement studentGBMarksInfoPrepStmt = null;
		ResultSet studentGBMarksInfoResultSet = null;
		List<NWUGradebookRecord> studentInfoList = null;
		NWUGradebookRecord gradebookRecord = null;

		try {

			connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
			connection.setAutoCommit(true);
			log.info("Database connection successfully made.");
			
			// Check if any new grades exist and create new entry in NWU_GRADEBOOK_DATA if it does
			confirmAllStudentGrades(connection, siteId, sectionUsersMap, assignmentIds);			
			
			studentInfoMap = new HashMap<Long, List<NWUGradebookRecord>>();
			for (Long assignmentId : assignmentIds) {

				// # Get matching data for students from NWU_GRADEBOOK_DATA
				studentGBMarksInfoPrepStmt = connection.prepareStatement(NWU_GRDB_INFO_SELECT);
				studentGBMarksInfoPrepStmt.setString(1, siteId);
				studentGBMarksInfoPrepStmt.setLong(2, assignmentId);
				studentGBMarksInfoResultSet = studentGBMarksInfoPrepStmt.executeQuery();

				studentInfoList = new ArrayList<NWUGradebookRecord>();

				while (studentGBMarksInfoResultSet.next()) {
					gradebookRecord = new NWUGradebookRecord();
					gradebookRecord.setId(studentGBMarksInfoResultSet.getLong("ID"));
					gradebookRecord.setSiteId(studentGBMarksInfoResultSet.getString("SITE_ID"));
					gradebookRecord.setSiteTitle(studentGBMarksInfoResultSet.getString("SITE_TITLE"));
					gradebookRecord.setModule(studentGBMarksInfoResultSet.getString("MODULE"));
					gradebookRecord.setAssessmentName(studentGBMarksInfoResultSet.getString("ASSESSMENT_NAME"));
					gradebookRecord.setStudentNumber(studentGBMarksInfoResultSet.getString("STUDENT_NUMBER"));
					gradebookRecord.setEvalDescrId(studentGBMarksInfoResultSet.getInt("EVAL_DESCR_ID"));
					gradebookRecord.setGrade(studentGBMarksInfoResultSet.getDouble("GRADE"));
					gradebookRecord.setTotalMark(studentGBMarksInfoResultSet.getDouble("TOTAL_MARK"));
					gradebookRecord.setGradableObjectId(studentGBMarksInfoResultSet.getLong("GRADABLE_OBJECT_ID"));
					gradebookRecord.setRecordedDate(studentGBMarksInfoResultSet.getTimestamp("RECORDED_DATE").toLocalDateTime());
					gradebookRecord.setDueDate(studentGBMarksInfoResultSet.getTimestamp("DUE_DATE").toLocalDateTime());
					gradebookRecord.setCreatedDate(studentGBMarksInfoResultSet.getTimestamp("CREATED_DATE").toLocalDateTime());
					Timestamp modifiedDate = studentGBMarksInfoResultSet.getTimestamp("MODIFIED_DATE");
					gradebookRecord.setModifiedDate(modifiedDate != null ? studentGBMarksInfoResultSet.getTimestamp("MODIFIED_DATE").toLocalDateTime() : null);
					gradebookRecord.setStatus(studentGBMarksInfoResultSet.getString("STATUS"));
					gradebookRecord.setRetryCount(studentGBMarksInfoResultSet.getInt("RETRY_COUNT"));
					gradebookRecord.setDescription(studentGBMarksInfoResultSet.getString("DESCRIPTION"));
					studentInfoList.add(gradebookRecord);
				}
				if (!studentInfoList.isEmpty()) {
					studentInfoMap.put(assignmentId, studentInfoList);
				}
			}

		} catch (Exception e) {
			log.error("Could not get student info, see error log for siteId: " + siteId + "; assignmentIds: " + assignmentIds, e);
		} finally {

			try {
				if (studentGBMarksInfoPrepStmt != null && !studentGBMarksInfoPrepStmt.isClosed()) {
					studentGBMarksInfoPrepStmt.close();
				}
				if (studentGBMarksInfoResultSet != null && !studentGBMarksInfoResultSet.isClosed()) {
					studentGBMarksInfoResultSet.close();
				}
				connection.close();
			} catch (SQLException e) {
				log.error("Could not get student info, see error log for siteId: " + siteId + "; assignmentIds: " + assignmentIds,
						e);
			}
		}
		return studentInfoMap;
	}

	/**
	 * 
	 * @param connection 
	 * @param siteId
	 * @param sectionUsersMap 
	 * @param assignmentIds
	 */
	private void confirmAllStudentGrades(Connection connection, String siteId, Map<String, List<String>> sectionUsersMap, List<Long> assignmentIds) {

		PreparedStatement studentGradebookMarksPrepStmt = null;
		PreparedStatement nwuGradebookRecordsSelectPrepStmt = null;

		ResultSet studentGradebookMarksResultSet = null;
		ResultSet nwuGradebookRecordsSelectResultSet = null;

		try {
			String module = null, siteTitle = null, studentNumber, assessmentName = null, evalDescr = null, evalShortDescr = null;
			List<String> studentNumbersForModule = null;
			double grade, total = 0.0;
			int evalDescrId, gradableObjectId;
			LocalDateTime dueDate = null;
			LocalDateTime recordedDate = null;

			siteTitle = getSiteTitle(connection, siteId);
			
			for (Map.Entry<String, List<String>> moduleEntry : sectionUsersMap.entrySet()) {

				module = (String) moduleEntry.getKey();
				studentNumbersForModule = moduleEntry.getValue();
				
				for (Long assignmentId : assignmentIds) {
					
					// # Get all student numbers and their grades for siteId and the date recorded between start and end date
					StringBuilder sbSql = new StringBuilder();
					sbSql.append(STUDENT_GRDB_MARKS_SELECT);		
					
					for (int i = 0; i < studentNumbersForModule.size(); i++) {
						if (i > 0)
							sbSql.append(",");
						sbSql.append(" ?");
					}
					sbSql.append(" ) ");
					studentGradebookMarksPrepStmt = connection.prepareStatement(sbSql.toString());

					int counter = 1;
					int assignmentIdInt = Long.valueOf(assignmentId).intValue();
					studentGradebookMarksPrepStmt.setInt(counter++, assignmentIdInt);
					studentGradebookMarksPrepStmt.setString(counter++, siteId);
					for (int i = 0; i < studentNumbersForModule.size(); i++) {
//						studentGradebookMarksPrepStmt.setString(counter++, getUUIDFromStudentNumber(connection, studentNumbersForModule.get(i)));
						studentGradebookMarksPrepStmt.setString(counter++, studentNumbersForModule.get(i));
					}
					studentGradebookMarksResultSet = studentGradebookMarksPrepStmt.executeQuery();

					if (studentGradebookMarksResultSet.next() == false) {
						log.info("No Grades found, see error log for siteId: " + siteId + "; assignmentIds: "
								+ assignmentIds);
						log.info("ResultSet in empty: " + sbSql.toString() + "; studentNumbersForModule: "
								+ studentNumbersForModule);
					} else {

						do {						
							studentNumber = studentGradebookMarksResultSet.getString("STUDENT_ID");
							grade = studentGradebookMarksResultSet.getDouble("POINTS_EARNED");
							recordedDate = studentGradebookMarksResultSet.getTimestamp("DATE_RECORDED").toLocalDateTime();
							assessmentName = studentGradebookMarksResultSet.getString("NAME");

							evalShortDescr = getEvalShortDesc(connection, siteId, module, assignmentIdInt);
							evalDescrId = getEvalDescId(connection, siteId, module, assignmentIdInt, evalShortDescr);
							
							evalDescr = getEvaluationDesc(assessmentName);

							total = studentGradebookMarksResultSet.getDouble("POINTS_POSSIBLE");
							dueDate = studentGradebookMarksResultSet.getTimestamp("DUE_DATE").toLocalDateTime();
							gradableObjectId = studentGradebookMarksResultSet.getInt("GRADABLE_OBJECT_ID");

							// # Get matching data for students from NWU_GRADEBOOK_DATA
							nwuGradebookRecordsSelectPrepStmt = connection.prepareStatement(NWU_GRDB_RECORDS_SELECT);
							nwuGradebookRecordsSelectPrepStmt.setString(1, siteId);
							nwuGradebookRecordsSelectPrepStmt.setString(2, studentNumber);
							nwuGradebookRecordsSelectPrepStmt.setInt(3, gradableObjectId);
							nwuGradebookRecordsSelectPrepStmt.setString(4, module);
							// nwuGradebookRecordsSelectPrepStmt.setInt(5, evalDescrId);
							nwuGradebookRecordsSelectResultSet = nwuGradebookRecordsSelectPrepStmt.executeQuery();

							if (nwuGradebookRecordsSelectResultSet.next()) {
								do {
									int id = nwuGradebookRecordsSelectResultSet.getInt("ID");
									double existingGrade = nwuGradebookRecordsSelectResultSet.getDouble("GRADE");
									LocalDateTime existingRecordedDate = nwuGradebookRecordsSelectResultSet
											.getTimestamp("RECORDED_DATE").toLocalDateTime();

									// # If the grade and recordedDate differ, update NWU_GRADEBOOK_DATA record and add to map for
									// WS
									if (existingGrade != grade
											|| (existingRecordedDate != null && !existingRecordedDate.isEqual(recordedDate))) {

										updateNWUGradebookData(connection, studentNumber, grade, recordedDate, id);
									}
								} while (nwuGradebookRecordsSelectResultSet.next());
							} else {

								// # If the record does not exist in NWU_GRADEBOOK_DATA, insert new with status STATUS_NEW
								insertNWUGradebookData(connection, siteId, siteTitle, studentNumber, assessmentName, grade,
										total, evalDescrId, dueDate, recordedDate, gradableObjectId, module);
							}

						} while (studentGradebookMarksResultSet.next());
					}
				}
			}			
			
		} catch (Exception e) {
			log.error("Grades could not be updated, see error log for siteId: " + siteId + "; assignmentIds: "
					+ assignmentIds, e);
		} finally {

			try {
				if (studentGradebookMarksResultSet != null && !studentGradebookMarksResultSet.isClosed()) {
					studentGradebookMarksResultSet.close();
				}
				if (nwuGradebookRecordsSelectResultSet != null && !nwuGradebookRecordsSelectResultSet.isClosed()) {
					nwuGradebookRecordsSelectResultSet.close();
				}
				if (studentGradebookMarksPrepStmt != null && !studentGradebookMarksPrepStmt.isClosed()) {
					studentGradebookMarksPrepStmt.close();
				}
				if (nwuGradebookRecordsSelectPrepStmt != null && !nwuGradebookRecordsSelectPrepStmt.isClosed()) {
					nwuGradebookRecordsSelectPrepStmt.close();
				}
			} catch (SQLException e) {
				log.error("Grades could not be updated, see error log for siteId: " + siteId + "; assignmentIds: "
						+ assignmentIds, e);
			}
		}

	}

	/**
	 * @param siteId
	 * @param sectionUsersMap
	 * @param assignmentIds
	 * @throws SQLException
	 */
	public String publishGradebookDataToMPS(String siteId, Map<String, List<String>> sectionUsersMap,
			List<String> assignmentIds) {

		if (!initializeSuccess) {
			initializeSuccess = initializeWebserviceObjects();
			if (!initializeSuccess) {
				log.error("initializeWebserviceObjects was unsuccessful, please see error log");
				return ERROR;
			}
		}

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		log.info("Start Publishing NWU Gradebook Data at " + dtf.format(LocalDateTime.now()));

		Connection connection = null;
		PreparedStatement studentGradebookMarksPrepStmt = null;
		PreparedStatement nwuGradebookRecordsSelectPrepStmt = null;

		ResultSet studentGradebookMarksResultSet = null;
		ResultSet nwuGradebookRecordsSelectResultSet = null;
		
		AcademicPeriodInfo academicPeriodInfo = null;

		try {
			connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
			connection.setAutoCommit(true);
			log.info("Database connection successfully made.");
			
			String module = null, siteTitle = null, studentNumber, assessmentName = null, evalDescr = null, evalShortDescr = null, status = null;
			List<String> studentNumbersForModule = null;
			HashMap<Integer, Double> studentGradeMap = null;
			double grade, total = 0.0;
			int evalDescrId, gradableObjectId;
			LocalDateTime dueDate = null;
			LocalDateTime recordedDate = null;
			List<String> moduleValues = null;

			siteTitle = getSiteTitle(connection, siteId);

			LocalDate now = LocalDate.now();
			ZoneId defaultZoneId = ZoneId.systemDefault();			

			for (Map.Entry<String, List<String>> moduleEntry : sectionUsersMap.entrySet()) {

				module = (String) moduleEntry.getKey();
				studentNumbersForModule = moduleEntry.getValue();
				
				String year = module.substring(module.length() - 4);
				
				academicPeriodInfo = new AcademicPeriodInfo();
				academicPeriodInfo.setAcadPeriodtTypeKey("vss.code.AcademicPeriod.YEAR");
				academicPeriodInfo.setAcadPeriodValue(year);

				LocalDate firstDayOfYear = now.with(TemporalAdjusters.firstDayOfYear()).withYear(Integer.parseInt(year));
				Date startDate = Date.from(firstDayOfYear.atStartOfDay(defaultZoneId).toInstant());

				LocalDate lastDayOfYear = now.with(TemporalAdjusters.lastDayOfYear()).withYear(Integer.parseInt(year));
				Date endDate = Date.from(lastDayOfYear.atStartOfDay(defaultZoneId).toInstant());

				for (String assignmentId : assignmentIds) {

					assessmentName = null;
					evalDescr = null;
					// # Get all student numbers and their grades for siteId and the date recorded between start and end date
					StringBuilder sbSql = new StringBuilder();
					sbSql.append(STUDENT_GRDB_MARKS_SELECT);

					for (int i = 0; i < studentNumbersForModule.size(); i++) {
						if (i > 0)
							sbSql.append(",");
						sbSql.append(" ?");
					}
					sbSql.append(" ) ");
					studentGradebookMarksPrepStmt = connection.prepareStatement(sbSql.toString());

					int counter = 1;
					int assignmentIdInt = Integer.parseInt(assignmentId);
					studentGradebookMarksPrepStmt.setInt(counter++, assignmentIdInt);
					studentGradebookMarksPrepStmt.setString(counter++, siteId);

					for (int i = 0; i < studentNumbersForModule.size(); i++) {
//						studentGradebookMarksPrepStmt.setString(counter++, getUUIDFromStudentNumber(connection, studentNumbersForModule.get(i)));
						studentGradebookMarksPrepStmt.setString(counter++, studentNumbersForModule.get(i));
					}
					studentGradebookMarksResultSet = studentGradebookMarksPrepStmt.executeQuery();

					studentGradeMap = new HashMap<>();
					if (studentGradebookMarksResultSet.next() == false) {
						log.info("No Grades found and published to MPS, see error log for siteId: " + siteId + "; assignmentIds: "
								+ assignmentIds);
						log.info("ResultSet in empty: " + sbSql.toString() + "; studentNumbersForModule: "
								+ studentNumbersForModule);
					} else {

						do {
							studentNumber = studentGradebookMarksResultSet.getString("STUDENT_ID");
							grade = studentGradebookMarksResultSet.getDouble("POINTS_EARNED");
							recordedDate = studentGradebookMarksResultSet.getTimestamp("DATE_RECORDED").toLocalDateTime();
							assessmentName = studentGradebookMarksResultSet.getString("NAME");

							evalShortDescr = getEvalShortDesc(connection, siteId, module, assignmentIdInt);
							evalDescrId = getEvalDescId(connection, siteId, module, assignmentIdInt, evalShortDescr);
							
							evalDescr = getEvaluationDesc(assessmentName);

							total = studentGradebookMarksResultSet.getDouble("POINTS_POSSIBLE");
							dueDate = studentGradebookMarksResultSet.getTimestamp("DUE_DATE").toLocalDateTime();
							gradableObjectId = studentGradebookMarksResultSet.getInt("GRADABLE_OBJECT_ID");

							// # Get matching data for students from NWU_GRADEBOOK_DATA
							nwuGradebookRecordsSelectPrepStmt = connection.prepareStatement(NWU_GRDB_RECORDS_SELECT);
							nwuGradebookRecordsSelectPrepStmt.setString(1, siteId);
							nwuGradebookRecordsSelectPrepStmt.setString(2, studentNumber);
							nwuGradebookRecordsSelectPrepStmt.setInt(3, gradableObjectId);
							nwuGradebookRecordsSelectPrepStmt.setString(4, module);
							// nwuGradebookRecordsSelectPrepStmt.setInt(5, evalDescrId);
							nwuGradebookRecordsSelectResultSet = nwuGradebookRecordsSelectPrepStmt.executeQuery();
							
							if (nwuGradebookRecordsSelectResultSet.next()) {
								status = nwuGradebookRecordsSelectResultSet.getString("STATUS");
								if(status != null && (status.equals(NWUGradebookRecord.STATUS_NEW) || status.equals(NWUGradebookRecord.STATUS_UPDATED))) {
									studentGradeMap.put(Integer.parseInt(studentNumber), grade);

									log.debug("Inserted/Updated NWU_GRADEBOOK_DATA - siteId = " + siteId + ", module = " + module + ", assessmentName = "
											+ assessmentName + ", studentNumber = " + studentNumber + ", grade = " + grade);
								}
							}
							
						} while (studentGradebookMarksResultSet.next());
					}

					if (!studentGradeMap.isEmpty()) {

						moduleValues = Collections.list(new StringTokenizer(module, " ")).stream().map(token -> (String) token)
								.collect(Collectors.toList());

						// # If the INSERT was successful and studentGradeMap not empty, send student grades/data via Webservice
						// StudentAssessmentServiceCRUD
						publishGrades(connection, siteId, module, moduleValues, studentGradeMap, siteTitle, evalDescr, evalShortDescr, total,
								dueDate, recordedDate, startDate, endDate, academicPeriodInfo);
					}
				}

				log.info("Publishing NWU Gradebook Data complete! Finished at " + dtf.format(LocalDateTime.now()));
			}

		} catch (Exception e) {
			log.error("Grades could not be published to MPS, see error log for siteId: " + siteId + "; assignmentIds: "
					+ assignmentIds, e);
			return ERROR;
		} finally {

			try {
				if (studentGradebookMarksResultSet != null && !studentGradebookMarksResultSet.isClosed()) {
					studentGradebookMarksResultSet.close();
				}
				if (nwuGradebookRecordsSelectResultSet != null && !nwuGradebookRecordsSelectResultSet.isClosed()) {
					nwuGradebookRecordsSelectResultSet.close();
				}
				if (studentGradebookMarksPrepStmt != null && !studentGradebookMarksPrepStmt.isClosed()) {
					studentGradebookMarksPrepStmt.close();
				}
				if (nwuGradebookRecordsSelectPrepStmt != null && !nwuGradebookRecordsSelectPrepStmt.isClosed()) {
					nwuGradebookRecordsSelectPrepStmt.close();
				}
				connection.close();
			} catch (SQLException e) {
				log.error("Grades could not be published to MPS, see error log for siteId: " + siteId + "; assignmentIds: "
						+ assignmentIds, e);
				return ERROR;
			}
		}
		return SUCCESS;
	}

	/**
	 * @param connection
	 * @param studentNumber
	 * @return
	 */
	private static String getUUIDFromStudentNumber(Connection connection, String studentNumber) {
		PreparedStatement prepStmt = null;
		try {
			prepStmt = connection.prepareStatement(SAKAI_USER_ID_SELECT);
			prepStmt.setString(1, studentNumber);
			ResultSet resultSet = prepStmt.executeQuery();
			if (resultSet.next()) {
				return resultSet.getString("USER_ID");
			} else {
				log.error("User with StudentNumber " + studentNumber + " not found in SAKAI_USER_ID_MAP");
			}

		} catch (SQLException e) {
			log.error("User with StudentNumber " + studentNumber + " not found in SAKAI_USER_ID_MAP");
		} finally {
			try {
				if (prepStmt != null && !prepStmt.isClosed()) {
					prepStmt.close();
				}
			} catch (SQLException e) {
				log.error("User with StudentNumber " + studentNumber + " not found in SAKAI_USER_ID_MAP");
			}
		}
		return null;
	}

	/**
	 * @param siteId
	 * @param sectionUsersMap
	 * @param assignmentId
	 * @param selectedStudentInfoIds
	 * @throws SQLException
	 */
	public String republishGradebookDataToMPS(String siteId, Map<String, List<String>> sectionUsersMap, String assignmentId,
			List<String> selectedStudentInfoIds) {

		if (!initializeSuccess) {
			initializeSuccess = initializeWebserviceObjects();
			if (!initializeSuccess) {
				log.error("initializeWebserviceObjects was unsuccessful, please see error log");
				return ERROR;
			}
		}

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		log.info("Start Republishing NWU Gradebook Data at " + dtf.format(LocalDateTime.now()));

		Connection connection = null;
		PreparedStatement studentGradebookMarksPrepStmt = null;
		PreparedStatement nwuGradebookRecordsSelectPrepStmt = null;

		ResultSet studentGradebookMarksResultSet = null;
		ResultSet nwuGradebookRecordsSelectResultSet = null;
		
		AcademicPeriodInfo academicPeriodInfo = null;

		try {
			connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
			connection.setAutoCommit(true);
			log.info("Database connection successfully made.");
			
			String module = null, siteTitle = null, studentNumber, assessmentName = null, evalDescr = null, evalShortDescr = null, status = null;
			List<String> studentNumbersForModule = null;
			List<String> selectedStudentNumbersForModule = null;
			HashMap<Integer, Double> studentGradeMap = null;
			double grade, total = 0.0;
			int evalDescrId, gradableObjectId;
			LocalDateTime dueDate = null;
			LocalDateTime recordedDate = null;
			List<String> moduleValues = null;

			siteTitle = getSiteTitle(connection, siteId);

			LocalDate now = LocalDate.now();
			ZoneId defaultZoneId = ZoneId.systemDefault();

			for (Map.Entry<String, List<String>> moduleEntry : sectionUsersMap.entrySet()) {
				
				module = (String) moduleEntry.getKey();
				studentNumbersForModule = moduleEntry.getValue();
				selectedStudentNumbersForModule = new ArrayList<String>();				

				String year = module.substring(module.length() - 4);
				
				academicPeriodInfo = new AcademicPeriodInfo();
				academicPeriodInfo.setAcadPeriodtTypeKey("vss.code.AcademicPeriod.YEAR");
				academicPeriodInfo.setAcadPeriodValue(year);

				LocalDate firstDayOfYear = now.with(TemporalAdjusters.firstDayOfYear()).withYear(Integer.parseInt(year));
				Date startDate = Date.from(firstDayOfYear.atStartOfDay(defaultZoneId).toInstant());

				LocalDate lastDayOfYear = now.with(TemporalAdjusters.lastDayOfYear()).withYear(Integer.parseInt(year));
				Date endDate = Date.from(lastDayOfYear.atStartOfDay(defaultZoneId).toInstant());
								
				getStudentNumbersForModule(selectedStudentNumbersForModule, selectedStudentInfoIds, studentNumbersForModule);
				if (selectedStudentNumbersForModule == null || selectedStudentNumbersForModule.isEmpty() )
					continue;

				assessmentName = null;
				
				// # Get all student numbers and their grades for siteId and the date recorded between start and end date
				StringBuilder sbSql = new StringBuilder();
				sbSql.append(STUDENT_GRDB_MARKS_SELECT);

				for (int i = 0; i < selectedStudentNumbersForModule.size(); i++) {
					if (i > 0)
						sbSql.append(",");
					sbSql.append(" ?");
				}
				sbSql.append(" ) ");
				studentGradebookMarksPrepStmt = connection.prepareStatement(sbSql.toString());

				int counter = 1;
				int assignmentIdInt = Integer.parseInt(assignmentId);
				studentGradebookMarksPrepStmt.setInt(counter++, assignmentIdInt);
				studentGradebookMarksPrepStmt.setString(counter++, siteId);

				for (int i = 0; i < selectedStudentNumbersForModule.size(); i++) {
//					studentGradebookMarksPrepStmt.setString(counter++, getUUIDFromStudentNumber(connection, selectedStudentNumbersForModule.get(i)));
					studentGradebookMarksPrepStmt.setString(counter++, selectedStudentNumbersForModule.get(i));
				}
				studentGradebookMarksResultSet = studentGradebookMarksPrepStmt.executeQuery();

				studentGradeMap = new HashMap<>();
				if (studentGradebookMarksResultSet.next() == false) {
					log.info("No Grades found and republished to MPS, see error log for siteId: " + siteId + "; assignmentId: "
							+ assignmentId);
					log.info("ResultSet in empty: " + sbSql.toString() + "; studentNumbersForModule: "
							+ studentNumbersForModule);
				} else {

					do {
						studentNumber = studentGradebookMarksResultSet.getString("STUDENT_ID");
						grade = studentGradebookMarksResultSet.getDouble("POINTS_EARNED");
						recordedDate = studentGradebookMarksResultSet.getTimestamp("DATE_RECORDED").toLocalDateTime();
						assessmentName = studentGradebookMarksResultSet.getString("NAME");

						evalShortDescr = getEvalShortDesc(connection, siteId, module, assignmentIdInt);
						evalDescrId = getEvalDescId(connection, siteId, module, assignmentIdInt, evalShortDescr);
						evalDescr = getEvaluationDesc(assessmentName);

						total = studentGradebookMarksResultSet.getDouble("POINTS_POSSIBLE");
						dueDate = studentGradebookMarksResultSet.getTimestamp("DUE_DATE").toLocalDateTime();
						gradableObjectId = studentGradebookMarksResultSet.getInt("GRADABLE_OBJECT_ID");

						// # Get matching data for students from NWU_GRADEBOOK_DATA
						nwuGradebookRecordsSelectPrepStmt = connection.prepareStatement(NWU_GRDB_RECORDS_SELECT);
						nwuGradebookRecordsSelectPrepStmt.setString(1, siteId);
						nwuGradebookRecordsSelectPrepStmt.setString(2, studentNumber);
						nwuGradebookRecordsSelectPrepStmt.setInt(3, gradableObjectId);
						nwuGradebookRecordsSelectPrepStmt.setString(4, module);
						// nwuGradebookRecordsSelectPrepStmt.setInt(5, evalDescrId);
						nwuGradebookRecordsSelectResultSet = nwuGradebookRecordsSelectPrepStmt.executeQuery();
						
						if (nwuGradebookRecordsSelectResultSet.next()) {
							status = nwuGradebookRecordsSelectResultSet.getString("STATUS");
							if(status != null && (status.equals(NWUGradebookRecord.STATUS_NEW) || status.equals(NWUGradebookRecord.STATUS_UPDATED))) {
								studentGradeMap.put(Integer.parseInt(studentNumber), grade);

								log.debug("Inserted/Updated NWU_GRADEBOOK_DATA - siteId = " + siteId + ", module = " + module + ", assessmentName = "
										+ assessmentName + ", studentNumber = " + studentNumber + ", grade = " + grade);
							}
						}

					} while (studentGradebookMarksResultSet.next());
				}

				if (!studentGradeMap.isEmpty()) {

					moduleValues = Collections.list(new StringTokenizer(module, " ")).stream().map(token -> (String) token)
							.collect(Collectors.toList());

					// # If the INSERT was successful and studentGradeMap not empty, send student grades/data via Webservice
					// StudentAssessmentServiceCRUD
					republishGrades(connection, siteId, module, moduleValues, studentGradeMap, siteTitle, evalDescr, evalShortDescr, total,
							dueDate, recordedDate, startDate, endDate, academicPeriodInfo);
				}

				log.info("Republishing NWU Gradebook Data complete! Finished at " + dtf.format(LocalDateTime.now()));
			}

		} catch (Exception e) {
			log.error("Grades could not be republished to MPS, see error log for siteId: " + siteId + "; assignmentId: "
					+ assignmentId, e);
			return ERROR;
		} finally {

			try {
				if (studentGradebookMarksResultSet != null && !studentGradebookMarksResultSet.isClosed()) {
					studentGradebookMarksResultSet.close();
				}
				if (nwuGradebookRecordsSelectResultSet != null && !nwuGradebookRecordsSelectResultSet.isClosed()) {
					nwuGradebookRecordsSelectResultSet.close();
				}

				if (studentGradebookMarksPrepStmt != null && !studentGradebookMarksPrepStmt.isClosed()) {
					studentGradebookMarksPrepStmt.close();
				}
				if (nwuGradebookRecordsSelectPrepStmt != null && !nwuGradebookRecordsSelectPrepStmt.isClosed()) {
					nwuGradebookRecordsSelectPrepStmt.close();
				}
				connection.close();
			} catch (SQLException e) {
				log.error("Grades could not be republished to MPS, see error log for siteId: " + siteId + "; assignmentId: "
						+ assignmentId, e);
				return ERROR;
			}
		}
		return SUCCESS;
	}

	/**
	 * @param selectedStudentNumbersForModule
	 * @param selectedStudentInfoIds
	 * @param studentNumbersForModule
	 * @return
	 */
	private void getStudentNumbersForModule(List<String> selectedStudentNumbersForModule, List<String> selectedStudentInfoIds,
			List<String> studentNumbersForModule) {

		if (selectedStudentInfoIds == null || selectedStudentInfoIds.isEmpty() || studentNumbersForModule == null
				|| studentNumbersForModule.isEmpty()) {
			return;
		}

		for (String studentInfoId : selectedStudentInfoIds) {
			if (studentNumbersForModule.contains(studentInfoId)) {
				selectedStudentNumbersForModule.add(studentInfoId);
			}
		}
	}

	/**
	 * Generate Evaluation Descr from Assessment title, use first 38 chars and append "-E"
	 * 
	 * @param assessmentName
	 * @return
	 */
	private String getEvaluationDesc(String assessmentName) {
		String evaluationDesc = null;
		if (assessmentName != null && assessmentName.length() > 38) {
			evaluationDesc = assessmentName.substring(0, 38);
		} else {
			evaluationDesc = assessmentName;
		}
		return evaluationDesc + "-E";
	}

	/**
	 * @param connection 
	 * @param siteId
	 * @param module
	 * @param moduleValues
	 * @param studentGradeMap
	 * @param siteTitle
	 * @param evalDescr
	 * @param evalShortDescr
	 * @param total
	 * @param dueDate
	 * @param recordedDate
	 * @param endDate
	 * @param startDate
	 * @param academicPeriodInfo
	 */
	private static void publishGrades(Connection connection, String siteId, String module, List<String> moduleValues,
			HashMap<Integer, Double> studentGradeMap, String siteTitle, String evalDescr, String evalShortDescr, double total,
			LocalDateTime dueDate, LocalDateTime recordedDate, Date startDate, Date endDate, AcademicPeriodInfo academicPeriodInfo) {

		log.info("publishGrades start");
		log.info("		siteId = " + siteId);
		log.info("		module = " + module);
		log.info("		studentGradeMap = " + studentGradeMap);
		log.info("		evalDescr = " + evalDescr);
		log.info("		evalShortDescr = " + evalShortDescr);
		log.info("		total = " + total);
		log.info("		dueDate = " + dueDate);
		log.info("		recordedDate = " + recordedDate);

		String strValue = moduleValues.get(2);
		int indexOf = strValue.indexOf("-");
		String enrolmentCategoryTypeKey = "vss.code.ENROLCAT." + strValue.substring(0, indexOf);
		String modeOfDeliveryTypeKey = "vss.code.PRESENTCAT." + strValue.substring(indexOf + 1);

		String moduleSite = Campus.getNumber(moduleValues.get(3));
		ModuleOfferingInfo moduleOfferingInfo = getModuleOfferingInfo(academicPeriodInfo, moduleValues.get(0),
				moduleValues.get(1), moduleSite, enrolmentCategoryTypeKey, modeOfDeliveryTypeKey, contextInfo);

		if (moduleOfferingInfo == null) {
			log.error("Grades could not be published, see error log for siteTitle: " + siteTitle + "; evalDescr: " + evalDescr);
			log.error("Could not find ModuleOfferingInfo, see error log for subjectCode: " + moduleValues.get(0)
					+ "; moduleNumber: " + moduleValues.get(1) + "; moduleSite: " + moduleSite + "; enrolmentCategoryTypeKey: "
					+ enrolmentCategoryTypeKey + "; modeOfDeliveryTypeKey: " + modeOfDeliveryTypeKey);
			return;
		}
		String moduleCode = moduleValues.get(0) + moduleValues.get(1);
		String classGroupDescr = getClassGroupDescription(startDate, endDate, modeOfDeliveryTypeKey, enrolmentCategoryTypeKey,
				moduleCode, moduleSite);
		if (classGroupDescr == null) {
			log.error("Grades could not be published, see error log for siteTitle: " + siteTitle + "; evalDescr: " + evalDescr);
			log.error("Could not get getClassGroupDescription, see error log for subjectCode: " + moduleValues.get(0)
					+ "; moduleNumber: " + moduleValues.get(1) + "; moduleSite: " + moduleSite + "; enrolmentCategoryTypeKey: "
					+ enrolmentCategoryTypeKey + "; modeOfDeliveryTypeKey: " + modeOfDeliveryTypeKey + "; moduleSite: "
					+ moduleSite);
			return;
		}

		StudentMarkInfo studentMarkInfo = new StudentMarkInfo();
		studentMarkInfo.setModuleSubjectCode(moduleValues.get(0));
		studentMarkInfo.setModuleNumber(moduleValues.get(1));
		studentMarkInfo.setAcademicPeriod(academicPeriodInfo);
		studentMarkInfo.setEnrolmentCategoryTypeKey(enrolmentCategoryTypeKey);
		studentMarkInfo.setModeOfDeliveryTypeKey(modeOfDeliveryTypeKey);
		studentMarkInfo.setTermTypeKey(moduleOfferingInfo.getTermTypeKey());
		studentMarkInfo.setModuleOrgEnt(Integer.parseInt(moduleOfferingInfo.getModuleOrgEnt()));
		studentMarkInfo.setModuleSite(Integer.parseInt(moduleSite));
		studentMarkInfo.setClassGroupDescription(classGroupDescr);
		studentMarkInfo.setLanguageTypeKey(SYSTEM_LANGUAGE_TYPE_KEY);
		studentMarkInfo.setEvaluationDesc(evalDescr);
		studentMarkInfo.setEvaluationShortDesc(evalShortDescr);
		studentMarkInfo.setEvaluationCutOffDate(Date.from(dueDate.atZone(ZoneId.systemDefault()).toInstant()));
		studentMarkInfo.setEvaluationMarkOutOff((int) total);
		studentMarkInfo.setEvaluationNoOfSubmissions(1);
		studentMarkInfo.setEvaluationSubminimum(0);
		studentMarkInfo.setEvaluationAssessmentDateTime(Date.from(recordedDate.atZone(ZoneId.systemDefault()).toInstant()));
		studentMarkInfo.setEvaluationIsRequiredForExam(false);
		studentMarkInfo.setStudentAndMark(studentGradeMap);
		studentMarkInfo.setMetaInfo(metaInfo);

		try {
			logMaintainStudentMarkData(studentGradeMap, studentMarkInfo, academicPeriodInfo);
			MaintainStudentResponseWrapper result = studentAssessmentServiceCRUDService.maintainStudentMark(studentMarkInfo,
					contextInfo);

			HashMap<String, String> maintainStudentResponse = result.getMaintainStudentResponse();
			if (maintainStudentResponse == null) {
				log.error("Response from publishGrades is empty for siteId: " + siteId + "; siteTitle: " + siteTitle
						+ "; module: " + module + "; evalDescr: " + evalDescr + "; studentGradeMap: " + studentGradeMap);

				updateNWUGradebookRecordsWithStatus(connection, siteId, module, studentGradeMap, NWUGradebookRecord.STATUS_FAIL,
						"MaintainStudentResponse is null");
			} else {
				updateNWUGradebookRecords(connection, siteId, module, maintainStudentResponse);
			}

		} catch (DoesNotExistException | InvalidParameterException | MissingParameterException | OperationFailedException
				| PermissionDeniedException e) {
			log.error("Grades could not be published, see error log for siteTitle: " + siteTitle + "; evalDescr: " + evalDescr,
					e);

			updateNWUGradebookRecordsWithStatus(connection, siteId, module, studentGradeMap, NWUGradebookRecord.STATUS_FAIL, e.getMessage());
		} catch (Exception e) {
			log.error("Grades could not be published, see error log for siteTitle: " + siteTitle + "; evalDescr: " + evalDescr,
					e);

			updateNWUGradebookRecordsWithStatus(connection, siteId, module, studentGradeMap, NWUGradebookRecord.STATUS_FAIL, e.getMessage());
		}

		log.info("publishGrades end");
	}

	/**
	 * 
	 * @param studentGradeMap
	 * @param studentMarkInfo
	 * @param academicPeriodInfo
	 */
	private static void logMaintainStudentMarkData(HashMap<Integer, Double> studentGradeMap, StudentMarkInfo studentMarkInfo, AcademicPeriodInfo academicPeriodInfo) {
		log.info("MaintainStudentMark :: AcadPeriodValue: " + academicPeriodInfo.getAcadPeriodValue()
		 + "; ModuleSubjectCode: " + studentMarkInfo.getModuleSubjectCode() + "; ModuleNumber: " + studentMarkInfo.getModuleNumber()
		 + "; EnrolmentCategoryTypeKey: " + studentMarkInfo.getEnrolmentCategoryTypeKey() + "; ModeOfDeliveryTypeKey: " + studentMarkInfo.getModeOfDeliveryTypeKey()
		 + "; TermTypeKey: " + studentMarkInfo.getTermTypeKey() + "; ModuleOrgEnt: " + studentMarkInfo.getModuleOrgEnt()
		 + "; ModuleSite: " + studentMarkInfo.getModuleSite() + "; ClassGroupDescription: " + studentMarkInfo.getClassGroupDescription()
		 + "; EvaluationDesc: " + studentMarkInfo.getEvaluationDesc() + "; EvaluationShortDesc: " + studentMarkInfo.getEvaluationShortDesc()
		 + "; EvaluationCutOffDate: " + studentMarkInfo.getEvaluationCutOffDate() + "; EvaluationMarkOutOff: " + studentMarkInfo.getEvaluationMarkOutOff()
		 + "; EvaluationAssessmentDateTime: " + studentMarkInfo.getEvaluationAssessmentDateTime());
		
		StringBuilder studentGradeMapStr = new StringBuilder("MaintainStudentMark :: StudentGradeMap : ");
		for (Entry<Integer, Double> entry : studentGradeMap.entrySet()) {
			studentGradeMapStr.append("(").append(entry.getKey()).append(",").append(entry.getValue()).append(") ");
		}

		log.info(studentGradeMapStr);
	}

	/**
	 * @param startDate
	 * @param endDate
	 * @param modeOfDeliveryTypeKey
	 * @param enrolmentCategoryTypeKey
	 * @param moduleCode
	 * @param moduleSite
	 * @return
	 */
	private static String getClassGroupDescription(Date startDate, Date endDate, String modeOfDeliveryTypeKey,
			String enrolmentCategoryTypeKey, String moduleCode, String moduleSite) {

		ClassGroupCourseCriteriaInfo info = new ClassGroupCourseCriteriaInfo();
		info.setStartDate(startDate);
		info.setEndDate(endDate);
		info.setPresentationCategoryTypeKey(modeOfDeliveryTypeKey);
		info.setEnrolmentCategoryTypeKey(enrolmentCategoryTypeKey);
		info.setModuleCode(moduleCode);
		info.setSite(Integer.parseInt(moduleSite) * -1);

		try {
			List<ClassGroupInfo> result = studentAssessmentService.searchClassGroupsByCourseCriteria(info,
					SYSTEM_LANGUAGE_TYPE_KEY, contextInfo);

			if (result == null || result.isEmpty()) {
				log.info("getClassGroupDescription result is empty");
				return null;
			}

			for (ClassGroupInfo classGroupInfo : result) {
				if (classGroupInfo.getClassGroupTypeKey() != null
						&& (classGroupInfo.getClassGroupTypeKey().equals(CLASS_GROUP_TYPE_COMPLETE)
								|| classGroupInfo.getClassGroupTypeKey().equals(CLASS_GROUP_TYPE_VOLLEDIG))) {
					log.info("Found ClassGroupDescription, for startDate: " + startDate + "; endDate: " + endDate
							+ "; moduleCode: " + moduleCode + "; moduleSite: " + moduleSite + "; enrolmentCategoryTypeKey: "
							+ enrolmentCategoryTypeKey + "; modeOfDeliveryTypeKey: " + modeOfDeliveryTypeKey
							+ "; classGroupDescription: " + classGroupInfo.getClassGroupDescription());
					return classGroupInfo.getClassGroupDescription();
				}
			}

		} catch (DoesNotExistException | InvalidParameterException | MissingParameterException | OperationFailedException
				| PermissionDeniedException e) {
			log.error("Could not find 	, see error log for startDate: " + startDate + "; endDate: " + endDate
					+ "; moduleCode: " + moduleCode + "; moduleSite: " + moduleSite + "; enrolmentCategoryTypeKey: "
					+ enrolmentCategoryTypeKey + "; modeOfDeliveryTypeKey: " + modeOfDeliveryTypeKey);

		} catch (Exception e) {
			log.error("Could not find ClassGroupDescription, see error log for startDate: " + startDate + "; endDate: " + endDate
					+ "; moduleCode: " + moduleCode + "; moduleSite: " + moduleSite + "; enrolmentCategoryTypeKey: "
					+ enrolmentCategoryTypeKey + "; modeOfDeliveryTypeKey: " + modeOfDeliveryTypeKey);
		}
		log.info("Could not find ClassGroupDescription, see error log for startDate: " + startDate + "; endDate: " + endDate
				+ "; moduleCode: " + moduleCode + "; moduleSite: " + moduleSite + "; enrolmentCategoryTypeKey: "
				+ enrolmentCategoryTypeKey + "; modeOfDeliveryTypeKey: " + modeOfDeliveryTypeKey);
		return null;
	}

	/**
	 * @param connection 
	 * @param siteId
	 * @param module
	 * @param moduleValues
	 * @param studentGradeMap
	 * @param siteTitle
	 * @param evalDescr
	 * @param evalShortDescr
	 * @param total
	 * @param dueDate
	 * @param recordedDate
	 * @param endDate
	 * @param startDate
	 * @param academicPeriodInfo
	 */
	private void republishGrades(Connection connection, String siteId, String module, List<String> moduleValues, HashMap<Integer, Double> studentGradeMap,
			String siteTitle, String evalDescr, String evalShortDescr, double total, LocalDateTime dueDate,
			LocalDateTime recordedDate, Date startDate, Date endDate, AcademicPeriodInfo academicPeriodInfo) {
		
		log.info("republishGrades start");
		log.info("		siteId = " + siteId);
		log.info("		module = " + module);
		log.info("		studentGradeMap = " + studentGradeMap);
		log.info("		evalDescr = " + evalDescr);
		log.info("		evalShortDescr = " + evalShortDescr);
		log.info("		total = " + total);
		log.info("		dueDate = " + dueDate);
		log.info("		recordedDate = " + recordedDate);

		String strValue = moduleValues.get(2);
		int indexOf = strValue.indexOf("-");
		String enrolmentCategoryTypeKey = "vss.code.ENROLCAT." + strValue.substring(0, indexOf);
		String modeOfDeliveryTypeKey = "vss.code.PRESENTCAT." + strValue.substring(indexOf + 1);

		String moduleSite = Campus.getNumber(moduleValues.get(3));
		ModuleOfferingInfo moduleOfferingInfo = getModuleOfferingInfo(academicPeriodInfo, moduleValues.get(0),
				moduleValues.get(1), moduleSite, enrolmentCategoryTypeKey, modeOfDeliveryTypeKey, contextInfo);

		if (moduleOfferingInfo == null) {
			log.error("Grades could not be republished, see error log for siteTitle: " + siteTitle + "; evalDescr: " + evalDescr);
			log.error("Could not find ModuleOfferingInfo, see error log for subjectCode: " + moduleValues.get(0)
					+ "; moduleNumber: " + moduleValues.get(1) + "; moduleSite: " + moduleSite + "; enrolmentCategoryTypeKey: "
					+ enrolmentCategoryTypeKey + "; modeOfDeliveryTypeKey: " + modeOfDeliveryTypeKey);
			return;
		}
		String moduleCode = moduleValues.get(0) + moduleValues.get(1);
		String classGroupDescr = getClassGroupDescription(startDate, endDate, modeOfDeliveryTypeKey, enrolmentCategoryTypeKey,
				moduleCode, moduleSite);
		if (classGroupDescr == null) {
			log.error("Grades could not be republished, see error log for siteTitle: " + siteTitle + "; evalDescr: " + evalDescr);
			log.error("Could not get getClassGroupDescription, see error log for subjectCode: " + moduleValues.get(0)
					+ "; moduleNumber: " + moduleValues.get(1) + "; moduleSite: " + moduleSite + "; enrolmentCategoryTypeKey: "
					+ enrolmentCategoryTypeKey + "; modeOfDeliveryTypeKey: " + modeOfDeliveryTypeKey + "; moduleSite: "
					+ moduleSite);
			return;
		}

		StudentMarkInfo studentMarkInfo = new StudentMarkInfo();
		studentMarkInfo.setModuleSubjectCode(moduleValues.get(0));
		studentMarkInfo.setModuleNumber(moduleValues.get(1));
		studentMarkInfo.setAcademicPeriod(academicPeriodInfo);
		studentMarkInfo.setEnrolmentCategoryTypeKey(enrolmentCategoryTypeKey);
		studentMarkInfo.setModeOfDeliveryTypeKey(modeOfDeliveryTypeKey);
		studentMarkInfo.setTermTypeKey(moduleOfferingInfo.getTermTypeKey());
		studentMarkInfo.setModuleOrgEnt(Integer.parseInt(moduleOfferingInfo.getModuleOrgEnt()));
		studentMarkInfo.setModuleSite(Integer.parseInt(moduleSite));
		studentMarkInfo.setClassGroupDescription(classGroupDescr);
		studentMarkInfo.setLanguageTypeKey(SYSTEM_LANGUAGE_TYPE_KEY);
		studentMarkInfo.setEvaluationDesc(evalDescr);
		studentMarkInfo.setEvaluationShortDesc(evalShortDescr);
		studentMarkInfo.setEvaluationCutOffDate(Date.from(dueDate.atZone(ZoneId.systemDefault()).toInstant()));
		studentMarkInfo.setEvaluationMarkOutOff((int) total);
		studentMarkInfo.setEvaluationNoOfSubmissions(1);
		studentMarkInfo.setEvaluationSubminimum(0);
		studentMarkInfo.setEvaluationAssessmentDateTime(Date.from(recordedDate.atZone(ZoneId.systemDefault()).toInstant()));
		studentMarkInfo.setEvaluationIsRequiredForExam(false);
		studentMarkInfo.setStudentAndMark(studentGradeMap);
		studentMarkInfo.setMetaInfo(metaInfo);

		try {
			logMaintainStudentMarkData(studentGradeMap, studentMarkInfo, academicPeriodInfo);
			MaintainStudentResponseWrapper result = studentAssessmentServiceCRUDService.maintainStudentMark(studentMarkInfo,
					contextInfo);

			HashMap<String, String> maintainStudentResponse = result.getMaintainStudentResponse();
			if (maintainStudentResponse == null) {
				log.error("Response from republishGrades is empty for siteId: " + siteId + "; siteTitle: " + siteTitle
						+ "; module: " + module + "; evalDescr: " + evalDescr + "; studentGradeMap: " + studentGradeMap);

				updateNWUGradebookRecordsWithStatus(connection, siteId, module, studentGradeMap, NWUGradebookRecord.STATUS_FAIL,
						"MaintainStudentResponse is null");
			} else {
				updateNWUGradebookRecords(connection, siteId, module, maintainStudentResponse);
			}

		} catch (DoesNotExistException | InvalidParameterException | MissingParameterException | OperationFailedException
				| PermissionDeniedException e) {
			log.error("Grades could not be republished, see error log for siteTitle: " + siteTitle + "; evalDescr: " + evalDescr,
					e);

			updateNWUGradebookRecordsWithStatus(connection, siteId, module, studentGradeMap, NWUGradebookRecord.STATUS_FAIL, e.getMessage());
		} catch (Exception e) {
			log.error("Grades could not be republished, see error log for siteTitle: " + siteTitle + "; evalDescr: " + evalDescr,
					e);

			updateNWUGradebookRecordsWithStatus(connection, siteId, module, studentGradeMap, NWUGradebookRecord.STATUS_FAIL, e.getMessage());
		}

		log.info("republishGrades end");
	}

	/**
	 * @param connection 
	 * @param siteId
	 * @param module
	 * @param maintainStudentResponse
	 */
	private static void updateNWUGradebookRecords(Connection connection, String siteId, String module, HashMap<String, String> maintainStudentResponse) {

		PreparedStatement nwuGradebookRecordsUpdateStatusPrepStmt = null;

		for (Entry<String, String> entry : maintainStudentResponse.entrySet()) {
			String studentNumber = entry.getKey();
			String resultValue = entry.getValue();
			boolean success = false;
			log.debug("updateNWUGradebookRecords - studentNumber: " + studentNumber + " resultValue: " + resultValue);

			// # IF WS update was successful, update status in new table to DONE, else update status to FAIL / RETRY
			if (resultValue != null && resultValue.equals("Create or Update of student mark successful")) {
				success = true;
			}

			try {
				nwuGradebookRecordsUpdateStatusPrepStmt = connection.prepareStatement(NWU_GRDB_RECORDS_STATUS_UPDATE);

				// if WS success / else status = FAIL with description
				if (success) {
					nwuGradebookRecordsUpdateStatusPrepStmt.setString(1, NWUGradebookRecord.STATUS_SUCCESS);
					nwuGradebookRecordsUpdateStatusPrepStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
					nwuGradebookRecordsUpdateStatusPrepStmt.setString(3, null);
				} else {
					nwuGradebookRecordsUpdateStatusPrepStmt.setString(1, NWUGradebookRecord.STATUS_FAIL);
					nwuGradebookRecordsUpdateStatusPrepStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
					nwuGradebookRecordsUpdateStatusPrepStmt.setString(3, resultValue);
				}

				nwuGradebookRecordsUpdateStatusPrepStmt.setString(4, siteId);
				nwuGradebookRecordsUpdateStatusPrepStmt.setString(5, studentNumber);
				nwuGradebookRecordsUpdateStatusPrepStmt.setString(6, module);

				// execute the preparedstatement
				nwuGradebookRecordsUpdateStatusPrepStmt.executeUpdate();

				log.debug("Published NWU Student data & updated NWU_GRADEBOOK_DATA - siteId = " + siteId + ", module = " + module
						+ ", studentNumber = " + studentNumber);
			} catch (SQLException e) {
				log.error("Could not update NWU Gradebook data for: siteId: " + siteId + "; studentNumber: " + studentNumber
						+ "; module: " + module, e);
			} finally {

				try {
					if (nwuGradebookRecordsUpdateStatusPrepStmt != null && !nwuGradebookRecordsUpdateStatusPrepStmt.isClosed()) {
						nwuGradebookRecordsUpdateStatusPrepStmt.close();
					}
				} catch (SQLException e) {
					log.error("Could not be update MPS Gradebook Records, see error log for siteId: " + siteId + "; module: "
							+ module, e);
				}
			}
		}
	}

	/**
	 * @param connection 
	 * @param siteId
	 * @param module
	 * @param studentGradeMap
	 * @param status
	 * @param description
	 */
	private static void updateNWUGradebookRecordsWithStatus(Connection connection, String siteId, String module, HashMap<Integer, Double> studentGradeMap,
			String status, String description) {
		PreparedStatement nwuGradebookRecordsUpdateStatusPrepStmt = null;
		Integer studentNumber = null;
		try {
			for (Entry<Integer, Double> entry : studentGradeMap.entrySet()) {
				studentNumber = entry.getKey();

				nwuGradebookRecordsUpdateStatusPrepStmt = connection.prepareStatement(NWU_GRDB_RECORDS_STATUS_UPDATE);
				nwuGradebookRecordsUpdateStatusPrepStmt.setString(1, status);
				nwuGradebookRecordsUpdateStatusPrepStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
				nwuGradebookRecordsUpdateStatusPrepStmt.setString(3, description);
				nwuGradebookRecordsUpdateStatusPrepStmt.setString(4, siteId);
				nwuGradebookRecordsUpdateStatusPrepStmt.setString(5, Integer.toString(studentNumber));
				nwuGradebookRecordsUpdateStatusPrepStmt.setString(6, module);

				// execute the preparedstatement
				nwuGradebookRecordsUpdateStatusPrepStmt.executeUpdate();
			}
			log.debug("Published NWU Student data & updated NWU_GRADEBOOK_DATA - siteId = " + siteId + ", module = " + module
					+ ", studentNumber = " + studentNumber);
		} catch (SQLException e) {
			log.error("Could not update NWU Gradebook data for: siteId: " + siteId + "; studentNumber: " + studentNumber
					+ "; module: " + module, e);
		} finally {

			try {
				if (nwuGradebookRecordsUpdateStatusPrepStmt != null && !nwuGradebookRecordsUpdateStatusPrepStmt.isClosed()) {
					nwuGradebookRecordsUpdateStatusPrepStmt.close();
				}
			} catch (SQLException e) {
				log.error(
						"Could not be update MPS Gradebook Records, see error log for siteId: " + siteId + "; module: " + module,
						e);
			}
		}
	}

	/**
	 * @param academicPeriodInfo
	 * @param subjectCode
	 * @param moduleNumber
	 * @param moduleSite
	 * @param enrolmentCategoryTypeKey
	 * @param modeOfDeliveryTypeKey
	 * @param contextInfo
	 * @return
	 */
	private static ModuleOfferingInfo getModuleOfferingInfo(AcademicPeriodInfo academicPeriodInfo, String subjectCode,
			String moduleNumber, String moduleSite, String enrolmentCategoryTypeKey, String modeOfDeliveryTypeKey,
			ContextInfo contextInfo) {

		ModuleOfferingSearchCriteriaInfo searchCriteria = new ModuleOfferingSearchCriteriaInfo();
		searchCriteria.setAcademicPeriod(academicPeriodInfo);
		searchCriteria.setModuleSubjectCode(subjectCode);
		searchCriteria.setModuleNumber(moduleNumber);
		searchCriteria.setModuleSite("-" + moduleSite);
		searchCriteria.setMethodOfDeliveryTypeKey(enrolmentCategoryTypeKey);
		searchCriteria.setModeOfDeliveryTypeKey(modeOfDeliveryTypeKey);

		try {
			List<ModuleOfferingInfo> moduleOfferingList = courseOfferingService.getModuleOfferingBySearchCriteria(searchCriteria,
					"vss.code.LANGUAGE.2", contextInfo);

			if (moduleOfferingList != null && !moduleOfferingList.isEmpty()) {
				return moduleOfferingList.get(0);
			}

		} catch (DoesNotExistException | InvalidParameterException | MissingParameterException | OperationFailedException
				| PermissionDeniedException e) {
			log.error("Could not find ModuleOfferingInfo, see error log for AcadPeriodValue: " + academicPeriodInfo.getAcadPeriodValue() + "; subjectCode: " + subjectCode + "; moduleNumber: "
					+ moduleNumber + "; moduleSite: " + moduleSite + "; enrolmentCategoryTypeKey: " + enrolmentCategoryTypeKey
					+ "; modeOfDeliveryTypeKey: " + modeOfDeliveryTypeKey, e);
		} catch (Exception e) {
			log.error("Could not find ModuleOfferingInfo, see error log for AcadPeriodValue: " + academicPeriodInfo.getAcadPeriodValue() + "; subjectCode: " + subjectCode + "; moduleNumber: "
					+ moduleNumber + "; moduleSite: " + moduleSite + "; enrolmentCategoryTypeKey: " + enrolmentCategoryTypeKey
					+ "; modeOfDeliveryTypeKey: " + modeOfDeliveryTypeKey, e);
		}

		return null;
	}

	/**
	 * @param connection
	 * @param studentNumber
	 * @param grade
	 * @param recordedDate
	 * @param id
	 */
	private static void updateNWUGradebookData(Connection connection, String studentNumber, HashMap<Integer, Double> studentGradeMap, double grade,
			LocalDateTime recordedDate, int id) {

		PreparedStatement nwuGradebookRecordsUpdatePrepStmt = null;
		try {
			nwuGradebookRecordsUpdatePrepStmt = connection.prepareStatement(NWU_GRDB_RECORDS_GRADE_UPDATE);
			nwuGradebookRecordsUpdatePrepStmt.setDouble(1, grade);
			nwuGradebookRecordsUpdatePrepStmt.setTimestamp(2, Timestamp.valueOf(recordedDate));
			nwuGradebookRecordsUpdatePrepStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
			nwuGradebookRecordsUpdatePrepStmt.setString(4, NWUGradebookRecord.STATUS_UPDATED);
			nwuGradebookRecordsUpdatePrepStmt.setInt(5, id);

			// execute the preparedstatement
			int count = nwuGradebookRecordsUpdatePrepStmt.executeUpdate();

			if (count > 0) {
				// If Updated successfully, add student and grade data to Map
				studentGradeMap.put(Integer.parseInt(studentNumber), grade);

				log.debug("Updated NWU_GRADEBOOK_DATA - ID = " + id + ", GRADE = " + grade);
			}
		} catch (SQLException e) {
			log.error("Could not update NWU Gradebook data: ", e);
		} finally {

			try {
				if (nwuGradebookRecordsUpdatePrepStmt != null && !nwuGradebookRecordsUpdatePrepStmt.isClosed()) {
					nwuGradebookRecordsUpdatePrepStmt.close();
				}
			} catch (SQLException e) {
				log.error("Could not be update MPS Gradebook Data, see error log for studentNumber: " + studentNumber, e);
			}
		}
	}
	
	/**
	 * @param connection
	 * @param studentNumber
	 * @param grade
	 * @param recordedDate
	 * @param id
	 */
	private static void updateNWUGradebookData(Connection connection, String studentNumber, double grade,
			LocalDateTime recordedDate, int id) {

		PreparedStatement nwuGradebookRecordsUpdatePrepStmt = null;
		try {
			nwuGradebookRecordsUpdatePrepStmt = connection.prepareStatement(NWU_GRDB_RECORDS_GRADE_UPDATE);
			nwuGradebookRecordsUpdatePrepStmt.setDouble(1, grade);
			nwuGradebookRecordsUpdatePrepStmt.setTimestamp(2, Timestamp.valueOf(recordedDate));
			nwuGradebookRecordsUpdatePrepStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
			nwuGradebookRecordsUpdatePrepStmt.setString(4, NWUGradebookRecord.STATUS_UPDATED);
			nwuGradebookRecordsUpdatePrepStmt.setInt(5, id);

			// execute the preparedstatement
			nwuGradebookRecordsUpdatePrepStmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Could not update NWU Gradebook data: ", e);
		} finally {

			try {
				if (nwuGradebookRecordsUpdatePrepStmt != null && !nwuGradebookRecordsUpdatePrepStmt.isClosed()) {
					nwuGradebookRecordsUpdatePrepStmt.close();
				}
			} catch (SQLException e) {
				log.error("Could not be update MPS Gradebook Data, see error log for studentNumber: " + studentNumber, e);
			}
		}
	}

	/**
	 * @param connection
	 * @param siteId
	 * @param siteTitle
	 * @param studentNumber
	 * @param assessmentName
	 * @param studentGradeMap
	 * @param grade
	 * @param total
	 * @param evalDescrId
	 * @param dueDate
	 * @param recordedDate
	 * @param gradableObjectId
	 * @param module
	 */
	private static void insertNWUGradebookData(Connection connection, String siteId, String siteTitle, String studentNumber, String assessmentName,
			HashMap<Integer, Double> studentGradeMap, double grade, double total, int evalDescrId, LocalDateTime dueDate,
			LocalDateTime recordedDate, int gradableObjectId, String module) {

		PreparedStatement nwuGradebookRecordsInsertPrepStmt = null;
		try {
			nwuGradebookRecordsInsertPrepStmt = connection.prepareStatement(NWU_GRDB_RECORDS_INSERT);

			nwuGradebookRecordsInsertPrepStmt.setString(1, siteId);
			nwuGradebookRecordsInsertPrepStmt.setString(2, siteTitle);
			nwuGradebookRecordsInsertPrepStmt.setString(3, module);
			nwuGradebookRecordsInsertPrepStmt.setString(4, assessmentName);
			nwuGradebookRecordsInsertPrepStmt.setString(5, studentNumber);
			nwuGradebookRecordsInsertPrepStmt.setInt(6, evalDescrId);
			nwuGradebookRecordsInsertPrepStmt.setDouble(7, grade);
			nwuGradebookRecordsInsertPrepStmt.setDouble(8, total);
			nwuGradebookRecordsInsertPrepStmt.setInt(9, gradableObjectId);
			nwuGradebookRecordsInsertPrepStmt.setTimestamp(10, Timestamp.valueOf(recordedDate));
			nwuGradebookRecordsInsertPrepStmt.setTimestamp(11, Timestamp.valueOf(dueDate));
			nwuGradebookRecordsInsertPrepStmt.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
			nwuGradebookRecordsInsertPrepStmt.setString(13, NWUGradebookRecord.STATUS_NEW);
			nwuGradebookRecordsInsertPrepStmt.setInt(14, 0);

			// execute the preparedstatement
			int count = nwuGradebookRecordsInsertPrepStmt.executeUpdate();
			if (count > 0) {
				// If inserted successfully, add student and grade data to Map
				studentGradeMap.put(Integer.parseInt(studentNumber), grade);

				log.debug("Inserted NWU_GRADEBOOK_DATA - siteId = " + siteId + ", module = " + module + ", assessmentName = "
						+ assessmentName + ", studentNumber = " + studentNumber + ", grade = " + grade);
			}
		} catch (SQLException e) {
			log.error("Could not insert NWU Gradebook data for: siteId: " + siteId + "; module: " + module + "; studentNumber: "
					+ studentNumber + "; assessmentName: " + assessmentName, e);
		} finally {

			try {
				if (nwuGradebookRecordsInsertPrepStmt != null && !nwuGradebookRecordsInsertPrepStmt.isClosed()) {
					nwuGradebookRecordsInsertPrepStmt.close();
				}
			} catch (SQLException e) {
				log.error("Could not insert NWU Gradebook data for: siteId: " + siteId + "; module: " + module
						+ "; studentNumber: " + studentNumber + "; assessmentName: " + assessmentName, e);
			}
		}
	}
	
	/**
	 * @param connection
	 * @param siteId
	 * @param siteTitle
	 * @param studentNumber
	 * @param assessmentName
	 * @param grade
	 * @param total
	 * @param evalDescrId
	 * @param dueDate
	 * @param recordedDate
	 * @param gradableObjectId
	 * @param module
	 */
	private static void insertNWUGradebookData(Connection connection, String siteId, String siteTitle, String studentNumber, String assessmentName,
			double grade, double total, int evalDescrId, LocalDateTime dueDate,
			LocalDateTime recordedDate, int gradableObjectId, String module) {

		PreparedStatement nwuGradebookRecordsInsertPrepStmt = null;
		try {
			nwuGradebookRecordsInsertPrepStmt = connection.prepareStatement(NWU_GRDB_RECORDS_INSERT);

			nwuGradebookRecordsInsertPrepStmt.setString(1, siteId);
			nwuGradebookRecordsInsertPrepStmt.setString(2, siteTitle);
			nwuGradebookRecordsInsertPrepStmt.setString(3, module);
			nwuGradebookRecordsInsertPrepStmt.setString(4, assessmentName);
			nwuGradebookRecordsInsertPrepStmt.setString(5, studentNumber);
			nwuGradebookRecordsInsertPrepStmt.setInt(6, evalDescrId);
			nwuGradebookRecordsInsertPrepStmt.setDouble(7, grade);
			nwuGradebookRecordsInsertPrepStmt.setDouble(8, total);
			nwuGradebookRecordsInsertPrepStmt.setInt(9, gradableObjectId);
			nwuGradebookRecordsInsertPrepStmt.setTimestamp(10, Timestamp.valueOf(recordedDate));
			nwuGradebookRecordsInsertPrepStmt.setTimestamp(11, Timestamp.valueOf(dueDate));
			nwuGradebookRecordsInsertPrepStmt.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
			nwuGradebookRecordsInsertPrepStmt.setString(13, NWUGradebookRecord.STATUS_NEW);
			nwuGradebookRecordsInsertPrepStmt.setInt(14, 0);

			// execute the preparedstatement
			nwuGradebookRecordsInsertPrepStmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Could not insert NWU Gradebook data for: siteId: " + siteId + "; module: " + module + "; studentNumber: "
					+ studentNumber + "; assessmentName: " + assessmentName, e);
		} finally {

			try {
				if (nwuGradebookRecordsInsertPrepStmt != null && !nwuGradebookRecordsInsertPrepStmt.isClosed()) {
					nwuGradebookRecordsInsertPrepStmt.close();
				}
			} catch (SQLException e) {
				log.error("Could not insert NWU Gradebook data for: siteId: " + siteId + "; module: " + module
						+ "; studentNumber: " + studentNumber + "; assessmentName: " + assessmentName, e);
			}
		}
	}

	/**
	 * @return
	 */
	private static boolean initializeWebserviceObjects() {
		
		if (metaInfo == null) {
			metaInfo = new MetaInfo();
			metaInfo.setCreateId(properties.getWSMetaInfoCreateId());
			metaInfo.setAuditFunction(properties.getWSMetaInfoAuditFunction());
		}

		if (studentAssessmentService == null) {
			String studentServiceLookupKey = ServiceRegistryLookupUtility.getServiceRegistryLookupKey(
					properties.getWSRuntimeEnvironment(), StudentAssessmentServiceClientFactory.STUDENTASSESSMENTSERVICE,
					properties.getWSMajorVersion(), properties.getWSDatabase());

			try {
				studentAssessmentService = (StudentAssessmentService) GenericServiceClientFactory.getService(
						studentServiceLookupKey, properties.getWSUsername(), properties.getWSPassword(),
						StudentAssessmentService.class);
			} catch (PermissionDeniedException e) {
				log.error(
						"initializeWebserviceObjects - Initializing StudentAssessmentService failed with PermissionDeniedException: ",
						e);
				return false;
			} catch (DoesNotExistException e) {
				log.error(
						"initializeWebserviceObjects - Initializing StudentAssessmentService failed with DoesNotExistException: ",
						e);
				return false;
			} catch (MissingParameterException e) {
				log.error(
						"initializeWebserviceObjects - Initializing StudentAssessmentService failed with MissingParameterException: ",
						e);
			} catch (OperationFailedException e) {
				log.error(
						"initializeWebserviceObjects - Initializing StudentAssessmentService failed with OperationFailedException: ",
						e);
				return false;
			}
		}

		if (studentAssessmentServiceCRUDService == null) {

	        String wsdlURL = null;
	        EtcdRegistryClient wsRegistry = new EtcdRegistryClient();
	        try {
	            wsdlURL = wsRegistry.getWSEndpoint(properties.getWSStudentAssessmentEnvTypeKey());
	        } catch (DoesNotExistException | MissingParameterException | NoSuchAlgorithmException | KeyManagementException |
	                OperationFailedException e) {
	            log.error("initializeWebserviceObjects - Initializing StudentAssessmentServiceCRUD failed - Unable to get soap wsdl from etcd properties.");
	        }

	        WebService webService = StudentAssessmentServiceCRUD.class.getAnnotation(WebService.class);
	        if (wsdlURL != null) {
	            try {
	            	studentAssessmentServiceCRUDService =
	                        (StudentAssessmentServiceCRUD)
	                                setupServicePort(new URL(wsdlURL), webService.targetNamespace(), webService.name(),
	                                        StudentAssessmentServiceCRUD.class, wsdlURL);
	            } catch (PermissionDeniedException | MalformedURLException e) {
	                log.error("Unable to create service client for StudentAssessmentServiceCRUD.");
	            }
	        }
		}

		if (courseOfferingService == null) {
			try {
				courseOfferingService = (CourseOfferingService) CourseOfferingServiceClientFactory.getCourseOfferingService(
						properties.getWSModuleEnvTypeKey(), properties.getNWUContextInfoUsername(),
						properties.getNWUContextInfoPassword());
			} catch (PermissionDeniedException e) {
				log.error(
						"initializeWebserviceObjects - Initializing CourseOfferingService failed with PermissionDeniedException: ",
						e);
				return false;
			} catch (DoesNotExistException e) {
				log.error("initializeWebserviceObjects - Initializing CourseOfferingService failed with DoesNotExistException: ",
						e);
				return false;
			} catch (MissingParameterException e) {
				log.error(
						"initializeWebserviceObjects - Initializing CourseOfferingService failed with MissingParameterException: ",
						e);
				return false;
			} catch (OperationFailedException e) {
				log.error(
						"initializeWebserviceObjects - Initializing CourseOfferingService failed with OperationFailedException: ",
						e);
				return false;
			}
		}

		log.info("Initializing Webservice Objects was successful.");
		return true;
	}
	
	 public static Object setupServicePort(URL url, String nameSpace, String serviceName,
             Class interfaceClass, String serviceWSDL) throws PermissionDeniedException {

		 QName qname = new QName(nameSpace, serviceName);
		 Service createdService = Service.create(url, qname);

		 Object servicePort = createdService.getPort(interfaceClass);
		 BindingProvider prov = (BindingProvider) servicePort;
		 prov.getRequestContext().put("javax.xml.ws.security.auth.username", properties.getWSStudentAssessmentUsername());
		 prov.getRequestContext().put("javax.xml.ws.security.auth.password", properties.getWSStudentAssessmentPassword());
		 prov.getRequestContext().put("javax.xml.ws.client.connectionTimeout", properties.getWSStudentAssessmentConnectionTimeout());
		 prov.getRequestContext().put("javax.xml.ws.client.receiveTimeout", properties.getWSStudentAssessmentReceiveTimeout());

		 return servicePort;
	 }

	/**
	 * @param connection
	 * @param siteId
	 * @return
	 * @throws SQLException
	 */
	private static String getSiteTitle(Connection connection, String siteId) throws SQLException {

		PreparedStatement siteTitlePrepStmt = connection.prepareStatement(SITE_TITLE_SELECT);
		siteTitlePrepStmt.setString(1, siteId);
		ResultSet siteTitleResultSet = siteTitlePrepStmt.executeQuery();
		if (siteTitleResultSet.next()) {
			return siteTitleResultSet.getString("TITLE");
		}
		return null;
	}

	/**
	 * @param connection
	 * @param siteId
	 * @param module
	 * @param assignmentId 
	 * @return
	 */
	private static String getEvalShortDesc(Connection connection, String siteId, String module, int assignmentId) {
		String evalShortDesc = null;
		PreparedStatement prepStmt = null;
		try {
			prepStmt = connection.prepareStatement(NWU_EVAL_DESCR_SELECT);
			prepStmt.setString(1, siteId);
			prepStmt.setString(2, module);
			prepStmt.setInt(3, assignmentId);
			ResultSet resultSet = prepStmt.executeQuery();
			if (resultSet.next()) {
				evalShortDesc = resultSet.getString("EVAL_DESCR");
			}
		} catch (SQLException e) {
			log.error("Exception evaluation code for: siteId: " + siteId + "; module: " + module + "; assignmentId: " + assignmentId, e);
		} finally {
			try {
				if (prepStmt != null && !prepStmt.isClosed()) {
					prepStmt.close();
				}
			} catch (SQLException e) {
				log.error("Exception evaluation code for: siteId: " + siteId + "; module: " + module + "; assignmentId: " + assignmentId, e);
			}
		}
		// if it does not exist, generate one
		if (evalShortDesc == null) {
			evalShortDesc = generateEvalShortDesc(connection, siteId, module, assignmentId);
		}
		return evalShortDesc;
	}

	/**
	 * @param connection
	 * @param siteId
	 * @param module
	 * @param assignmentId
	 * @param evalDescr
	 * @return
	 * @throws SQLException
	 */
	private static int getEvalDescId(Connection connection, String siteId, String module, int assignmentId, String evalDescr) throws SQLException {
		PreparedStatement prepStmt = null;
		try {
			prepStmt = connection.prepareStatement(NWU_EVAL_DESCRID_SELECT);
			prepStmt.setString(1, siteId);
			prepStmt.setString(2, module);
			prepStmt.setInt(3, assignmentId);
			prepStmt.setString(4, evalDescr);
			ResultSet resultSet = prepStmt.executeQuery();
			if (resultSet.next()) {
				return resultSet.getInt("ID");
			}
		} catch (SQLException e) {
			log.error("Exception evaluation code for: siteId: " + siteId + "; module: " + module + "; assignmentId: " + assignmentId, e);
		} finally {
			try {
				if (prepStmt != null && !prepStmt.isClosed()) {
					prepStmt.close();
				}
			} catch (SQLException e) {
				log.error("Exception evaluation code for: siteId: " + siteId + "; module: " + module + "; assignmentId: " + assignmentId, e);
			}
		}
		return 0;
	}

	/**
	 * @param connection
	 * @param siteId
	 * @param module
	 * @param assignmentId
	 * @return
	 */
	private static String generateEvalShortDesc(Connection connection, String siteId, String module, int assignmentId) {
		String randomStr = RandomStringUtils.random(5, true, true);
		PreparedStatement prepStmt = null;
		try {
			prepStmt = connection.prepareStatement(NWU_SITE_EVAL_INSERT);

			prepStmt.setString(1, siteId);
			prepStmt.setString(2, module);
			prepStmt.setInt(3, assignmentId);
			prepStmt.setString(4, randomStr);

			// execute the preparedstatement
			int count = prepStmt.executeUpdate();
			if (count > 0) {
				return randomStr;
			}
		} catch (SQLException e) {
			log.error("Could not insert random evaluation code for: siteId: " + siteId + "; module: " + module + "; assignmentId: " + assignmentId + "; randomStr: "
					+ randomStr, e);
		} finally {
			try {
				if (prepStmt != null && !prepStmt.isClosed()) {
					prepStmt.close();
				}
			} catch (SQLException e) {
				log.error("Could not insert random evaluation code for: siteId: " + siteId + "; module: " + module + "; assignmentId: " + assignmentId
						+ "; randomStr: " + randomStr, e);
			}
		}
		return "ERROR";
	}
}
