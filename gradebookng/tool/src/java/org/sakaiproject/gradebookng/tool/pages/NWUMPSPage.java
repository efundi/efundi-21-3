package org.sakaiproject.gradebookng.tool.pages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.gradebookng.business.util.AssignmentDataProvider;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.sakaiproject.gradebookng.tool.model.GbAssignmentData;
import org.sakaiproject.gradebookng.tool.model.GbModalWindow;
import org.sakaiproject.gradebookng.tool.model.GbStudentInfoData;
import org.sakaiproject.gradebookng.tool.panels.NWUMPSStudentInfoPanel;
import org.sakaiproject.portal.util.PortalUtils;
import org.sakaiproject.service.gradebook.shared.Assignment;

import za.ac.nwu.NWUGradebookPublishUtil;
import za.ac.nwu.NWUGradebookRecord;

/**
 * NWU MPS Page
 *
 * @author Joseph Gillman
 */
public class NWUMPSPage extends BasePage {

	private static final long serialVersionUID = 1L;

	private static final String SAK_PROP_DB_URL = "url@javax.sql.BaseDataSource";
	private static final String SAK_PROP_DB_USERNAME = "username@javax.sql.BaseDataSource";
	private static final String SAK_PROP_DB_PASSWORD = "password@javax.sql.BaseDataSource";

	private Set<GbAssignmentData> selectedAssignments = new HashSet<GbAssignmentData>();
	private Panel assignmentPanel = null;
	private Panel current = null;
	private static NWUGradebookPublishUtil gbUtil = null;

	GbModalWindow modalOutput;

	public NWUMPSPage() {
		
		defaultRoleChecksForInstructorOnlyPage();
		disableLink(this.nwumpsPageLink);
		
		ServerConfigurationService serverConfigService = businessService.getServerConfigService();
		gbUtil = NWUGradebookPublishUtil.getInstance(serverConfigService.getString(SAK_PROP_DB_URL),
				serverConfigService.getString(SAK_PROP_DB_USERNAME), serverConfigService.getString(SAK_PROP_DB_PASSWORD));
		assignmentPanel = new AssignmentPanel("main-panel");
		current = assignmentPanel;

		Form form = new Form("mps-form") {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit() {
				// Publish marks for selected sites
				// System.out.println("Form submit: Send Marks to MPS");
				//
				// List<String> selectedAssignmentIds =
				// selectedAssignments.stream().map(GbAssignmentData::getAssignmentId).collect(Collectors.toList());
				// Map<String, List<String>> sectionUsersMap = businessService.getSectionUsersForCurrentSite();
				// gbUtil.publishGradebookDataToMPS(businessService.getCurrentSiteId(), sectionUsersMap, selectedAssignmentIds);
			}
		};
		form.add(current);

		this.modalOutput = new GbModalWindow("modalOutput");
		this.modalOutput.showUnloadConfirmation(false);
		form.add(this.modalOutput);

		add(form);
	}

	class AjaxCheckBoxPanel extends Panel {
		private static final long serialVersionUID = 1L;

		private AjaxCheckBox field;

		public AjaxCheckBoxPanel(String id, IModel<GbAssignmentData> model) {
			super(id, model);
			field = new AjaxCheckBox("checkBox", newCheckBoxModel(model)) {

				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target) {

					GbAssignmentData selectedAssignment = (GbAssignmentData) getParent().getDefaultModelObject();
					if (getModelObject().booleanValue()) {
						if (!selectedAssignments.contains(selectedAssignment)) {
							selectedAssignments.add(selectedAssignment);
						}
					} else {
						if (selectedAssignments.contains(selectedAssignment)) {
							selectedAssignments.remove(selectedAssignment);
						}
					}
				}
			};			

			GbAssignmentData rowData = (GbAssignmentData) model.getObject();
			
			//Mark checkbox "disabled" if StudentInfoDataList is not empty
			if(rowData.getStudentInfoDataList() != null && !rowData.getStudentInfoDataList().isEmpty()) {
				field.add(new AttributeAppender("disabled", "disabled"));
			}
			
			add(field);
		}

		protected IModel<Boolean> newCheckBoxModel(IModel<GbAssignmentData> model) {
			return new PropertyModel<Boolean>(model, "selected");
		}

		public AjaxCheckBoxPanel(String id) {
			this(id, new Model<GbAssignmentData>());
		}

		public CheckBox getField() {
			return field;
		}
	}

	class AssignmentPanel extends Panel {
		private static final long serialVersionUID = 1L;

		/**
		 * @param id
		 *            component id
		 */
		public AssignmentPanel(String id) {
			super(id);

			// get the list of Assignments
			final List<Assignment> assignments = businessService.getGradebookAssignments();
			List<Long> assignmentIds = assignments.stream().map(Assignment::getId).collect(Collectors.toList());
			
			Map<Long, List<NWUGradebookRecord>> studentInfoMap = gbUtil.getStudentInfoMap(businessService.getCurrentSiteId(), assignmentIds);

			AssignmentDataProvider assignmentDataProvider = new AssignmentDataProvider(assignments, studentInfoMap);
			AjaxFallbackDefaultDataTable assignmentsTable = new AjaxFallbackDefaultDataTable<>("assignments-table", getColumns(),
					assignmentDataProvider, 100);
			assignmentsTable.addBottomToolbar(new NoRecordsToolbar(assignmentsTable));

			final SendMarksButton sendMarks = new SendMarksButton("sendMarks");
			sendMarks.setDefaultFormProcessing(false);
			sendMarks.setOutputMarkupId(true);

			add(sendMarks);
			add(assignmentsTable);
		}
	}

	@SuppressWarnings("unchecked")
	private List<IColumn<GbAssignmentData, String>> getColumns() {

		List<IColumn<GbAssignmentData, String>> columns = Lists.newArrayList();
		columns.add(new AbstractColumn<GbAssignmentData, String>(new Model<String>("Select")) {
			@Override
			public void populateItem(Item<ICellPopulator<GbAssignmentData>> cellItem, String componentId,
					IModel<GbAssignmentData> rowModel) {
				cellItem.add(new AjaxCheckBoxPanel(componentId, rowModel));
				cellItem.add(new AttributeModifier("style", "display: table-cell; text-align: center;"));
			}
		});
		columns.add(new PropertyColumn<>(Model.of("Test Name"), "assignmentName"));
		columns.add(new AbstractColumn<GbAssignmentData, String>(Model.of("Detail from MPS")) {
			@Override
			public void populateItem(Item<ICellPopulator<GbAssignmentData>> cellItem, String componentId,
					IModel<GbAssignmentData> rowModel) {				
				GbAssignmentData rowData = (GbAssignmentData) rowModel.getObject();
				if(rowData.getStudentInfoDataList() != null && !rowData.getStudentInfoDataList().isEmpty()) {					
					cellItem.add(new LinkPanel(componentId, rowModel, true));
				} else {			
					cellItem.add(new LinkPanel(componentId, rowModel, false));
				}
				
				cellItem.add(new AttributeModifier("style", "display: table-cell; text-align: center;"));
			}
		});
		return columns;
	}

	private class SendMarksButton extends GbAjaxButton {

		public SendMarksButton(final String id) {
			super(id);
		}

		public SendMarksButton(final String id, final Form<?> form) {
			super(id, form);
		}

		@Override
		public void onSubmit(final AjaxRequestTarget target, final Form form) {
//			final GbModalWindow window = getModalOutputWindow();
//			window.setTitle("Sending marks to MPS.");
//			window.setContent(new Label(modalOutput.getContentId(), "Marks being send to MPS. Please wait"));
//			window.setComponentToReturnFocusTo(this);
//
////			window.setWidthUnit("%");
////			window.setInitialWidth(75);
////			window.setPositionAtTop(true);
//			window.setMaskType(MaskType.SEMI_TRANSPARENT);
//			
//			target.appendJavaScript(String.format("setTimeout(function() { $('#%s').modal('hide'); location.reload(true); }, 10000);", window.getMarkupId()));
//
//			window.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
//				public boolean onCloseButtonClicked(AjaxRequestTarget target) {
//					target.appendJavaScript("alert('You can\\'t close this modal window using close button."
//							+ " The window will close after marks has been send to MPS.');");
//					return false;
//				}
//			});
//
//			window.show(target);
			

			List<String> selectedAssignmentIds = selectedAssignments.stream().map(GbAssignmentData::getAssignmentId)
					.collect(Collectors.toList());
			if(selectedAssignmentIds == null || selectedAssignmentIds.isEmpty()) return;
			Map<String, List<String>> sectionUsersMap = businessService.getSectionUsersForCurrentSite();
			String status = gbUtil.publishGradebookDataToMPS(businessService.getCurrentSiteId(), sectionUsersMap,
					selectedAssignmentIds);

			target.appendJavaScript("$.unblockUI();");
			target.appendJavaScript("location.reload();");
			
//			if(status == null || status.equals(gbUtil.SUCCESS) || status.equals(gbUtil.ERROR)) {
//				target.appendJavaScript("$.unblockUI();");
//				target.appendJavaScript("location.reload();");
//			}
		}

		@Override
		protected void onAfterSubmit(AjaxRequestTarget target, Form form) {
//			try {
//				Thread.sleep(10000); // 1000 milliseconds is one second.
//
//			} catch (InterruptedException ex) {
//				Thread.currentThread().interrupt();
//			}
//
//			modalOutput.close(target);
//
//			// refresh
//			setResponsePage(NWUMPSPage.class);

//
//			List<String> selectedAssignmentIds = selectedAssignments.stream().map(GbAssignmentData::getAssignmentId)
//					.collect(Collectors.toList());
//			if(selectedAssignmentIds == null || selectedAssignmentIds.isEmpty()) return;
//			Map<String, List<String>> sectionUsersMap = businessService.getSectionUsersForCurrentSite();
//			gbUtil.publishGradebookDataToMPS(businessService.getCurrentSiteId(), sectionUsersMap,
//					selectedAssignmentIds);
		}

		@Override
		protected void onError(AjaxRequestTarget target, Form<?> form) {

			target.appendJavaScript("$.unblockUI();");
			target.appendJavaScript("location.reload();");
		}
	}

	class LinkPanel extends Panel {
		private static final long serialVersionUID = 1L;

		/**
		 * @param id
		 *            component id
		 * @param model
		 *            model for contact
		 */
		public LinkPanel(String id, IModel<GbAssignmentData> model, boolean disable) {
			super(id, model);
			
			AjaxLink ajaxLink = new AjaxLink<Void>("assignment-info") {
				
				@Override
				public void onClick(AjaxRequestTarget target) {
					GbAssignmentData assignmentData = (GbAssignmentData) getParent().getDefaultModelObject();
					
					Map<String, List<String>> sectionUsersMap = businessService.getSectionUsersForCurrentSite();			
					List<NWUGradebookRecord> studentInfoList = gbUtil.getStudentInfoList(businessService.getCurrentSiteId(), sectionUsersMap,
							Long.valueOf(assignmentData.getAssignmentId()));
					
					assignmentData.setStudentInfoDataList(convertToGbStudentInfoDataList(studentInfoList));					
					
					// System.out.println((GbAssignmentData) getParent().getDefaultModelObject());
					NWUMPSStudentInfoPanel studentInfoPanel = new NWUMPSStudentInfoPanel("main-panel", gbUtil, assignmentData);
					studentInfoPanel.setOutputMarkupId(true);
					current.replaceWith(studentInfoPanel);
					target.add(studentInfoPanel);
					current = studentInfoPanel;
					// setResponsePage(new NWUMPSStudentInfoPanel();
				}
			};
			
			ajaxLink.setEnabled(disable);
			
			add(ajaxLink);
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
	}

	public GbModalWindow getModalOutputWindow() {
		return this.modalOutput;
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(NWUMPSPage.class, "repeater.css")));
		
		response.render(JavaScriptHeaderItem.forUrl("/library/webjars/jquery-blockui/2.65/jquery.blockUI.js"));
		
		final String version = PortalUtils.getCDNQuery();
		response.render(JavaScriptHeaderItem.forUrl(String.format("/gradebookng-tool/scripts/gradebook-nwu-mps.js%s", version)));

	}
}
