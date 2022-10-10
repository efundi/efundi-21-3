package org.sakaiproject.gradebookng.tool.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.util.io.IClusterable;

import lombok.Data;

/**
 * GbAssignmentData
 *
 * @author Joseph Gillman
 */
@Data
public class GbAssignmentData implements IClusterable {
	
	private static final long serialVersionUID = 1L;

	private boolean selected;

	private String assignmentId;

	private String assignmentName;
	
	private List<GbStudentInfoData> studentInfoDataList = new ArrayList<GbStudentInfoData>();
}
