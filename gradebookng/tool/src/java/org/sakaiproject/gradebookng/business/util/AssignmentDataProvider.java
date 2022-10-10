package org.sakaiproject.gradebookng.business.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.iterators.EmptyIterator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.sakaiproject.gradebookng.tool.model.GbAssignmentData;
import org.sakaiproject.gradebookng.tool.model.GbStudentInfoData;
import org.sakaiproject.service.gradebook.shared.Assignment;

import za.ac.nwu.NWUGradebookRecord;

/**
 * @author dev
 */
public class AssignmentDataProvider extends SortableDataProvider<GbAssignmentData, String> {

	private static final long serialVersionUID = 1L;
	
	private List<GbAssignmentData> assignmentList = null;

	public AssignmentDataProvider(List<Assignment> assignments, Map<Long, List<NWUGradebookRecord>> studentInfoMap) {
		// The default sorting
//		setSort("name", SortOrder.ASCENDING);
		
		Date now = new Date();
        Instant nowInstant = now.toInstant();
        LocalDateTime nowLocalDateTime = LocalDateTime.ofInstant(nowInstant, ZoneId.systemDefault());
				
		GbAssignmentData gbAssignmentData = null;
		assignmentList = new ArrayList<GbAssignmentData>();
		for (Assignment assignment : assignments) {
			Date dueDate = assignment.getDueDate();
			if (dueDate == null) {
				continue;
			}
			Instant dueDateInstant = dueDate.toInstant();
	        LocalDateTime dueDateLocalDateTime = LocalDateTime.ofInstant(dueDateInstant, ZoneId.systemDefault());
			
			if (dueDateLocalDateTime.isBefore(nowLocalDateTime)) {
				gbAssignmentData = new GbAssignmentData();
				gbAssignmentData.setAssignmentId(Long.toString(assignment.getId()));
				gbAssignmentData.setAssignmentName(assignment.getName());
				
				if(studentInfoMap.get(assignment.getId()) == null) {
					gbAssignmentData.setStudentInfoDataList(new ArrayList<GbStudentInfoData>());
				} else {
					gbAssignmentData.setStudentInfoDataList(convertToGbStudentInfoDataList(studentInfoMap.get(assignment.getId())));
				}
				
				assignmentList.add(gbAssignmentData);
			}
		}
	}

	/**
	 * 
	 * @param gradebookRecordList
	 * @return
	 */
	private List<GbStudentInfoData> convertToGbStudentInfoDataList(List<NWUGradebookRecord> gradebookRecordList) {
		List<GbStudentInfoData> studentInfoDataList = new ArrayList<GbStudentInfoData>();
		GbStudentInfoData studentInfoData = null;
		for (NWUGradebookRecord nwuGradebookRecord : gradebookRecordList) {
			studentInfoData = new GbStudentInfoData();
			studentInfoData.setUserId(nwuGradebookRecord.getStudentNumber());
			studentInfoData.setStatus(nwuGradebookRecord.getStatus());
			studentInfoData.setErrorMessage(nwuGradebookRecord.getDescription());
			studentInfoData.setRetryCount(nwuGradebookRecord.getRetryCount());			
			studentInfoDataList.add(studentInfoData);
		}
		return studentInfoDataList;
	}

	@Override
	public Iterator iterator(final long first, final long count) {
		if (this.assignmentList == null) {
			return EmptyIterator.emptyIterator();
		}
		return assignmentList.listIterator();
	}

	@Override
	public IModel<GbAssignmentData> model(GbAssignmentData object) {
		return Model.of((GbAssignmentData) object);
	}

	@Override
	public long size() {
		if (this.assignmentList == null) {
			return 0L;
		}
		return this.assignmentList.size();
	}
}