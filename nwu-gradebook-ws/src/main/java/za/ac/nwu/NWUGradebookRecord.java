package za.ac.nwu;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class NWUGradebookRecord {

	private static final long serialVersionUID = 1L;

	public static final String STATUS_NEW = "NEW";
	public static final String STATUS_UPDATED = "UPDATED";
	public static final String STATUS_SUCCESS = "SUCCESS";
	public static final String STATUS_FAIL = "FAILED";

	private Long id;

	@EqualsAndHashCode.Include
    private String siteId;

    private String siteTitle;

	@EqualsAndHashCode.Include
    private String module;

	@EqualsAndHashCode.Include
    private String assessmentName;
    
//	@EqualsAndHashCode.Include
    private String studentNumber;

    private int evalDescrId;

//	@EqualsAndHashCode.Include
    private double grade;
    private double totalMark;

//	@EqualsAndHashCode.Include
    private Long gradableObjectId;

    private LocalDateTime recordedDate;
    private LocalDateTime dueDate;
    private LocalDateTime createdDate;    
    private LocalDateTime modifiedDate;

//	@EqualsAndHashCode.Include
    private String status;
	
    private int retryCount;
    
    private String description;
}
