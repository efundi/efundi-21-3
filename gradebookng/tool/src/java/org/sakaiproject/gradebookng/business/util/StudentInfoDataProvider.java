package org.sakaiproject.gradebookng.business.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.iterators.EmptyIterator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.sakaiproject.gradebookng.tool.model.GbAssignmentData;
import org.sakaiproject.gradebookng.tool.model.GbStudentInfoData;

/**
 * StudentInfoDataProvider
 *
 * @author Joseph Gillman
 */
public class StudentInfoDataProvider extends SortableDataProvider<GbStudentInfoData, String> {

	private List<GbStudentInfoData> studentInfoList = null;

	public StudentInfoDataProvider(GbAssignmentData assignmentData) {
		studentInfoList = new ArrayList<GbStudentInfoData>();
		
		if(assignmentData != null) {
			for (GbStudentInfoData studentInfoData : assignmentData.getStudentInfoDataList()) {
				studentInfoData.setAssignmentData(assignmentData);
				studentInfoList.add(studentInfoData);
			}
		}
	}

	@Override
	public Iterator iterator(final long first, final long count) {
		if (this.studentInfoList == null) {
			return EmptyIterator.emptyIterator();
		}
		return studentInfoList.listIterator();
	}

	@Override
	public IModel<GbStudentInfoData> model(GbStudentInfoData object) {
		return Model.of((GbStudentInfoData) object);
	}

	@Override
	public long size() {
		if (this.studentInfoList == null) {
			return 0L;
		}
		return this.studentInfoList.size();
	}
}
