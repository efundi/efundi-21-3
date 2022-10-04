package org.sakaiproject.gradebookng.tool.panels;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.MaskType;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.sakaiproject.gradebookng.business.util.StudentInfoDataProvider;
import org.sakaiproject.gradebookng.tool.component.GbAjaxButton;
import org.sakaiproject.gradebookng.tool.model.GbAssignmentData;
import org.sakaiproject.gradebookng.tool.model.GbModalWindow;
import org.sakaiproject.gradebookng.tool.model.GbStudentInfoData;
import org.sakaiproject.gradebookng.tool.pages.NWUMPSPage;

import za.ac.nwu.NWUGradebookPublishUtil;
import za.ac.nwu.NWUGradebookRecord;

/**
 * NWUMPSStudentInfoPanel
 *
 * @author Joseph Gillman
 *
 */
public class NWUMPSStudentInfoPanel extends BasePanel {

	private static final String NWU_ITHELP_URL = "nwu.ithelp.url";
	private static final String REPUBLISH_MAX_RETRY = "nwu.republish.max.retry";

	private static final long serialVersionUID = 1L;
	private Set<GbStudentInfoData> selectedStudentInfos = new HashSet<GbStudentInfoData>();
	private static NWUGradebookPublishUtil gbUtil = null;
	private GbAssignmentData assignmentData;
	
	GbModalWindow modalOutput;

	public NWUMPSStudentInfoPanel(String id) {
		super(id);
		addPanelComponents();
	}
	
	public NWUMPSStudentInfoPanel(String id, NWUGradebookPublishUtil gbUtil, GbAssignmentData assignmentData) {
		super(id);
		this.gbUtil = gbUtil;
		setAssignmentData(assignmentData);
		addPanelComponents();
	}

	private void addPanelComponents() {
		
		String helpUrl = serverConfigService.getString(NWU_ITHELP_URL, "http://ithelp.nwu.ac.za");		
		add(new ExternalLink("itHelpURL_link", helpUrl) {

            private static final long serialVersionUID = -8010560272317354356L;

            @Override
            protected void onComponentTag(ComponentTag tag)
            {
                super.onComponentTag(tag);
                tag.put("target", "_blank");
            }
		});
		
		final ResubmitButton resubmitButton = new ResubmitButton("student-info-resubmit");
		resubmitButton.setDefaultFormProcessing(false);
		resubmitButton.setOutputMarkupId(true);
		add(resubmitButton);

		add(new Link<Void>("student-info-close") {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick() {
				setResponsePage(NWUMPSPage.class);
			}
		});
		
		CheckBox selectAll = new CheckBox("select-all");
		add(selectAll);

		StudentInfoDataProvider studentInfoDataProvider = new StudentInfoDataProvider(getAssignmentData());
		AjaxFallbackDefaultDataTable studentInfoTable = new AjaxFallbackDefaultDataTable<>("student-info-table", getColumns(), studentInfoDataProvider, 5000);
		studentInfoTable.addBottomToolbar(new NoRecordsToolbar(studentInfoTable));
		add(studentInfoTable);

		this.modalOutput = new GbModalWindow("modalOutputStudentInfo");
		this.modalOutput.showUnloadConfirmation(false);
		add(this.modalOutput);
	}

	@SuppressWarnings("unchecked")
	private List<IColumn<GbStudentInfoData, String>> getColumns() {

		List<IColumn<GbStudentInfoData, String>> columns = Lists.newArrayList();
		columns.add(new AbstractColumn<GbStudentInfoData, String>(new Model<String>("Select")) {
			@Override
			public void populateItem(Item<ICellPopulator<GbStudentInfoData>> cellItem, String componentId,
					IModel<GbStudentInfoData> rowModel) {
				cellItem.add(new AjaxCheckBoxPanel(componentId, rowModel));
				cellItem.add(new AttributeModifier("style", "display: table-cell; text-align: center;"));
			}
		});
        columns.add(new PropertyColumn<>(Model.of("UserId"), "userId"));
        columns.add(new PropertyColumn<>(Model.of("Status"), "status"));
        columns.add(new PropertyColumn<>(Model.of("Error Message"), "errorMessage"));
		return columns;
	}
	
	class AjaxCheckBoxPanel extends Panel {
		private static final long serialVersionUID = 1L;

		private AjaxCheckBox field;

		public AjaxCheckBoxPanel(String id, IModel<GbStudentInfoData> model) {
			super(id, model);
			field = new AjaxCheckBox("checkBox", newCheckBoxModel(model)) {

				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target) {

					GbStudentInfoData selectedStudentInfo = (GbStudentInfoData) getParent().getDefaultModelObject();
					if (getModelObject().booleanValue()) {
						if (!selectedStudentInfos.contains(selectedStudentInfo)) {
							selectedStudentInfos.add(selectedStudentInfo);
						}
					} else {
						if (selectedStudentInfos.contains(selectedStudentInfo)) {
							selectedStudentInfos.remove(selectedStudentInfo);
						}
					}
				}
			};
			
			GbStudentInfoData rowData = (GbStudentInfoData) model.getObject();
			
			//Mark checkbox "disabled" is status is "SUCCESS" and repubish retrycount is 5 or more
			if(rowData.getStatus() != null && rowData.getStatus().equals(NWUGradebookRecord.STATUS_SUCCESS) && 
					Integer.compare(rowData.getRetryCount(), Integer.parseInt(serverConfigService.getString(REPUBLISH_MAX_RETRY, "5"))) >= 0) {
				field.add(new AttributeAppender("disabled", "disabled"));
			}
			add(field);
		}

		protected IModel<Boolean> newCheckBoxModel(IModel<GbStudentInfoData> model) {
			return new PropertyModel<Boolean>(model, "selected");
		}

		public AjaxCheckBoxPanel(String id) {
			this(id, new Model<GbStudentInfoData>());
		}

		public CheckBox getField() {
			return field;
		}
	}
	
	private class ResubmitButton extends GbAjaxButton {

		public ResubmitButton(final String id) {
			super(id);
		}

		public ResubmitButton(final String id, final Form<?> form) {
			super(id, form);
		}

		@Override
		public void onSubmit(final AjaxRequestTarget target, final Form form) {
			final GbModalWindow window = getModalOutputWindow();
			window.setTitle("Resubmit marks to MPS.");
			window.setContent(new Label(modalOutput.getContentId(), "Marks being send to MPS. Please wait"));
			window.setComponentToReturnFocusTo(this);
			window.setMaskType(MaskType.SEMI_TRANSPARENT);			
			target.appendJavaScript(String.format("setTimeout(function() { $('#%s').modal('hide'); location.reload(true); }, 10000);", window.getMarkupId()));

			window.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
				public boolean onCloseButtonClicked(AjaxRequestTarget target) {
					target.appendJavaScript("alert('You can\\'t close this modal window using close button."
							+ " The window will close after marks has been send to MPS.');");
					return false;
				}
			});
			window.show(target);
		}

		@Override
		protected void onAfterSubmit(AjaxRequestTarget target, Form form) {
			
			List<String> selectedStudentInfoIds = selectedStudentInfos.stream().map(GbStudentInfoData::getUserId)
					.collect(Collectors.toList());
			if(selectedStudentInfoIds == null || selectedStudentInfoIds.isEmpty()) return;
			Map<String, List<String>> sectionUsersMap = businessService.getSectionUsersForCurrentSite();
			gbUtil.republishGradebookDataToMPS(businessService.getCurrentSiteId(), sectionUsersMap, getAssignmentData().getAssignmentId(),
					selectedStudentInfoIds);
		}

		@Override
		protected void onError(AjaxRequestTarget target, Form<?> form) {
		}
	}
	
	public GbAssignmentData getAssignmentData() {
		return assignmentData;
	}

	public void setAssignmentData(GbAssignmentData assignmentData) {
		this.assignmentData = assignmentData;
	}

	public GbModalWindow getModalOutputWindow() {
		return this.modalOutput;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(
			CssHeaderItem.forReference(new CssResourceReference(NWUMPSPage.class, "repeater.css")));
	}
}
