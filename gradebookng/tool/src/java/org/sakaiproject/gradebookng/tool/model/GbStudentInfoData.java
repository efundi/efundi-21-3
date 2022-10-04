package org.sakaiproject.gradebookng.tool.model;

import org.apache.wicket.util.io.IClusterable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * GbStudentInfoData
 *
 * @author Joseph Gillman
 */
@Data
public class GbStudentInfoData implements IClusterable {

	private static final long serialVersionUID = 1L;

	private boolean selected;
	
	private String userId;

	private String status;

	private String errorMessage;
	
	private int retryCount;
	
	@EqualsAndHashCode.Exclude
    @ToString.Exclude
	private GbAssignmentData assignmentData;
}
