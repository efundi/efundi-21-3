package za.ac.nwu;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertiesHolder {

    private static final String PROPERTIES_FILENAME = "nwu-gradebook.properties";
    
	private String username;
    private String password;
    private String url;

	private String wsMajorVersion;
    private String wsDatabase;
    private String wsRuntimeEnvironment;
    private String wsUsername;
    private String wsPassword;
    private String wsStudentAssessmentEnvTypeKey;    
    private String wsStudentAssessmentUsername;    
    private String wsStudentAssessmentPassword;
    private String wsStudentAssessmentConnectionTimeout;    
    private String wsStudentAssessmentReceiveTimeout;
    
    private String wsMetaInfoCreateId;
    private String wsMetaInfoAuditFunction;

    private String wsModuleEnvTypeKey;
    private String nwuContextInfoUsername;
    private String nwuContextInfoPassword;
    
    private String nwuRepublishMaxRetry;
    
    public PropertiesHolder() {
        loadFromProperties(PROPERTIES_FILENAME);

        if (username == null || password == null || url == null) {
            throw new RuntimeException("Could not locate your database connection settings!");
        }

        if (wsMajorVersion == null || wsDatabase == null || wsRuntimeEnvironment == null) {
            throw new RuntimeException("Could not locate Webservice settings!");
        }
    }

    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    
    public String getWSMajorVersion() { return wsMajorVersion; }
    public String getWSDatabase() { return wsDatabase; }
    public String getWSRuntimeEnvironment() { return wsRuntimeEnvironment; }
    public String getWSUsername() { return wsUsername; }
    public String getWSPassword() { return wsPassword; }
    public String getWSStudentAssessmentEnvTypeKey() { return wsStudentAssessmentEnvTypeKey; }
    public String getWSStudentAssessmentUsername() { return wsStudentAssessmentUsername; }
    public String getWSStudentAssessmentPassword() { return wsStudentAssessmentPassword; }
    public String getWSStudentAssessmentConnectionTimeout() { return wsStudentAssessmentConnectionTimeout; }
    public String getWSStudentAssessmentReceiveTimeout() { return wsStudentAssessmentReceiveTimeout; }
    public String getWSMetaInfoCreateId() { return wsMetaInfoCreateId; }
    public String getWSMetaInfoAuditFunction() { return wsMetaInfoAuditFunction; }
    public String getWSModuleEnvTypeKey() { return wsModuleEnvTypeKey; }
    public String getNWUContextInfoUsername() { return nwuContextInfoUsername; }
    public String getNWUContextInfoPassword() { return nwuContextInfoPassword; }
    public String getNWURepublishMaxRetry() { return nwuRepublishMaxRetry; }    

    private void loadFromProperties(String filename) {
    	
        Properties properties = new Properties();
    	try (InputStream input = getClass().getClassLoader().getResourceAsStream(filename)) {
            properties.load(input);
//            fh.close();
        } catch (IOException e) {
        	log.info("Failed to read properties from: " + filename);
        }

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String prop = (String) entry.getKey();
            String value = (String) entry.getValue();

            if ("driverClassName@javax.sql.BaseDataSource".equals(prop)) {
                try {
                    Class.forName(value);
                } catch (ClassNotFoundException e) {
                	log.info("*** Failed to load database driver!");
                    throw new RuntimeException(e);
                }
            } else if ("url@javax.sql.BaseDataSource".equals(prop)) {
                this.url = value;
            } else if ("username@javax.sql.BaseDataSource".equals(prop)) {
                this.username = value;
            } else if ("password@javax.sql.BaseDataSource".equals(prop)) {
                this.password = value;
            } else if ("ws.major.version".equals(prop)) {
                this.wsMajorVersion = value;
            } else if ("ws.database".equals(prop)) {
                this.wsDatabase = value;
            } else if ("ws.runtimeEnvironment".equals(prop)) {
                this.wsRuntimeEnvironment = value;
            } else if ("ws.username".equals(prop)) {
                this.wsUsername = value;
            } else if ("ws.password".equals(prop)) {
                this.wsPassword = value;
            } else if ("ws.student.assessment.env.type.key".equals(prop)) {
                this.wsStudentAssessmentEnvTypeKey = value;
            } else if ("ws.student.assessment.username".equals(prop)) {
                this.wsStudentAssessmentUsername = value;
            } else if ("ws.student.assessment.password".equals(prop)) {
                this.wsStudentAssessmentPassword = value;
            } else if ("ws.student.assessment.connectionTimeout".equals(prop)) {
                this.wsStudentAssessmentConnectionTimeout = value;
            } else if ("ws.student.assessment.receiveTimeout".equals(prop)) {
                this.wsStudentAssessmentReceiveTimeout = value;
            } else if ("ws.metaInfo.createId".equals(prop)) {
                this.wsMetaInfoCreateId = value;
            } else if ("ws.metaInfo.auditFunction".equals(prop)) {
                this.wsMetaInfoAuditFunction = value;
            } else if ("ws.module.env.type.key".equals(prop)) {
                this.wsModuleEnvTypeKey = value;
            } else if ("nwu.context.info.username".equals(prop)) {
                this.nwuContextInfoUsername = value;
            } else if ("nwu.context.info.password".equals(prop)) {
                this.nwuContextInfoPassword = value;
            } else if ("nwu.republish.max.retry".equals(prop)) {
                this.nwuRepublishMaxRetry = value;
            }
        }
    }
}
