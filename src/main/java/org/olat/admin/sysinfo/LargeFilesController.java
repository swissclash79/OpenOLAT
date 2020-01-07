package org.olat.admin.sysinfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.olat.NewControllerFactory;
import org.olat.admin.sysinfo.gui.LargeFilesLockedCellRenderer;
import org.olat.admin.sysinfo.gui.LargeFilesNameCellRenderer;
import org.olat.admin.sysinfo.gui.LargeFilesRevisionCellRenderer;
import org.olat.admin.sysinfo.gui.LargeFilesSendMailCellRenderer;
import org.olat.admin.sysinfo.gui.LargeFilesSizeCellRenderer;
import org.olat.admin.sysinfo.gui.LargeFilesTrashedCellRenderer;
import org.olat.admin.sysinfo.model.LargeFilesTableContentRow;
import org.olat.admin.sysinfo.model.LargeFilesTableModel;
import org.olat.admin.sysinfo.model.LargeFilesTableModel.LargeFilesTableColumns;
import org.olat.core.commons.persistence.SortKey;
import org.olat.core.commons.services.vfs.VFSMetadata;
import org.olat.core.commons.services.vfs.VFSRepositoryService;
import org.olat.core.commons.services.vfs.VFSRevision;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableSortOptions;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.ExtendedFlexiTableSearchController;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.text.TextFactory;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CalloutSettings;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.id.Identity;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.ContactMessage;
import org.olat.modules.co.ContactFormController;
import org.springframework.beans.factory.annotation.Autowired;

public class LargeFilesController extends FormBasicController implements ExtendedFlexiTableSearchController {

	public static final String[] TRASHED_KEYS = new String[]{ "trashed", "notTrashed", "both" };
	public static final String[] REVISION_KEYS = new String[]{ "revisions", "files", "both" };
	public static final String[] LOCKED_KEYS = new String[]{ "locked", "notlocked", "both" };

	private final AtomicInteger counter = new AtomicInteger();

	private FlexiTableElement largeFilesTableElement;
	private LargeFilesTableModel largeFilesTableModel;

	private SingleSelection trashedSelection;
	private SingleSelection revisionSelection; 
	private SingleSelection lockedSelection;
	private DateChooser createdAtNewerChooser;
	private DateChooser createdAtOlderChooser;
	private DateChooser editedAtNewerChooser;
	private DateChooser editedAtOlderChooser;
	private DateChooser lockedAtNewerChooser;
	private DateChooser lockedAtOlderChooser;
	private TextElement downloadCountMinEl;
	private TextElement revisionCountMinEl;
	private TextElement maxResultEl;
	private TextElement minSizeEl;
	private FormSubmit searchButton;
	private FormLink resetButton;

	private List<LargeFilesTableContentRow> rows;

	private CloseableModalController cmc;
	private ContactFormController contactCtrl;
	private CloseableCalloutWindowController pathInfoCalloutCtrl;

	@Autowired
	private VFSRepositoryService vfsRepositoryService;


	public LargeFilesController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl, "large_files");
		initForm(ureq);
		updateModel();
	}

	public void updateModel() {
		rows = new ArrayList<>();
		int maxResults = 100, downloadCountMin = 0, minSize = 0;
		Long revisionsCountMin = new Long(0);

		if(StringHelper.containsNonWhitespace(maxResultEl.getValue())) {
			maxResults = Integer.parseInt(maxResultEl.getValue());
		}
		if(StringHelper.containsNonWhitespace(minSizeEl.getValue())) {
			minSize = Integer.parseInt(minSizeEl.getValue());
		}
		if(StringHelper.containsNonWhitespace(downloadCountMinEl.getValue())) {
			downloadCountMin = Integer.parseInt(downloadCountMinEl.getValue());
		}
		if(StringHelper.containsNonWhitespace(revisionCountMinEl.getValue())) {
			revisionsCountMin = Long.parseLong(revisionCountMinEl.getValue());
		}

		if(revisionSelection.getSelectedKey() != REVISION_KEYS[0]) {
			List<VFSMetadata> files = vfsRepositoryService.getLargestFiles(maxResults, 
					createdAtNewerChooser.getDate(), createdAtOlderChooser.getDate(),
					editedAtNewerChooser.getDate(), editedAtOlderChooser.getDate(),
					lockedAtNewerChooser.getDate(), lockedAtOlderChooser.getDate(),
					trashedSelection.getSelectedKey(), revisionSelection.getSelectedKey(), lockedSelection.getSelectedKey(),
					downloadCountMin, revisionsCountMin, minSize);

			for(VFSMetadata file:files) {
				LargeFilesTableContentRow contentRow = new LargeFilesTableContentRow(file);

				String[] path = contentRow.getPath().split("/");

				StringBuilder sb = new StringBuilder(path[0]);
				if(path.length > 1) {
					sb.append("/").append(path[1]);
					if(path.length > 2) {
						sb.append("/...");
					}
				}

				FormLink pathInfo = uifactory.addFormLink("pathinfo_" + counter.incrementAndGet() , "pathInfo", sb.toString(), null, null, Link.NONTRANSLATED);
				pathInfo.setUserObject(contentRow);
				contentRow.setPathInfo(pathInfo);
				rows.add(contentRow);
			}
		}

		if(revisionSelection.getSelectedKey() != REVISION_KEYS[1]) {
			List<VFSRevision> revisions = vfsRepositoryService.getLargestRevisions(maxResults, 
					createdAtNewerChooser.getDate(), createdAtOlderChooser.getDate(),
					editedAtNewerChooser.getDate(), editedAtOlderChooser.getDate(),
					lockedAtNewerChooser.getDate(), lockedAtOlderChooser.getDate(),
					trashedSelection.getSelectedKey(), revisionSelection.getSelectedKey(), lockedSelection.getSelectedKey(),
					downloadCountMin, revisionsCountMin, minSize);

			for(VFSRevision revision:revisions) {
				LargeFilesTableContentRow contentRow = new LargeFilesTableContentRow(revision);

				String[] path = contentRow.getPath().split("/");

				StringBuilder sb = new StringBuilder(path[0]);
				if(path.length > 1) {
					sb.append("/").append(path[1]);
					if(path.length > 2) {
						sb.append("/...");
					}
				}
				
				FormLink pathInfo = uifactory.addFormLink("pathinfo_" + counter.incrementAndGet() , "pathInfo", sb.toString(), null, null, Link.NONTRANSLATED);
				pathInfo.setUserObject(contentRow);
				contentRow.setPathInfo(pathInfo);
				rows.add(contentRow);
			}
		}

		Collections.sort(rows, (row1,row2) -> {
			return row2.getSize().intValue() - row1.getSize().intValue();
		});

		if(maxResults != 0 && maxResults < rows.size()) {
			rows = rows.subList(0, maxResults);
		}

		largeFilesTableModel.setObjects(rows);
		largeFilesTableElement.reset(true, true, true);
	}

	private void resetForm() {
		createdAtNewerChooser.reset();
		createdAtOlderChooser.reset();
		lockedAtNewerChooser.reset();
		lockedAtOlderChooser.reset();
		editedAtNewerChooser.reset();
		editedAtOlderChooser.reset();
		revisionCountMinEl.reset();
		downloadCountMinEl.reset();
		trashedSelection.reset();
		lockedSelection.reset();
		revisionSelection.reset();
		maxResultEl.reset();
		minSizeEl.reset();

		updateModel();
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		FormLayoutContainer largefFilesTitle = FormLayoutContainer.createVerticalFormLayout("largeFilesTitle", getTranslator());
		formLayout.add(largefFilesTitle);
		largefFilesTitle.setFormTitle(translate("largefiles.title"));

		FormLayoutContainer leftContainer = FormLayoutContainer.createDefaultFormLayout_6_6("filter_left", getTranslator());
		leftContainer.setRootForm(mainForm);
		formLayout.add(leftContainer);

		FormLayoutContainer rightContainer = FormLayoutContainer.createDefaultFormLayout_6_6("filter_right", getTranslator());
		leftContainer.setRootForm(mainForm);
		formLayout.add(rightContainer);

		FormLayoutContainer filterButtonLayout = FormLayoutContainer.createButtonLayout("filter_buttons", getTranslator());
		leftContainer.setRootForm(mainForm);
		formLayout.add(filterButtonLayout);

		// Left part of the filter
		createdAtNewerChooser = uifactory.addDateChooser("largefiles.filter.created.newer", "largefiles.filter.created.newer", null, leftContainer);
		editedAtNewerChooser = uifactory.addDateChooser("largefiles.filter.edited.newer", "largefiles.filter.edited.newer", null, leftContainer);
		lockedAtNewerChooser = uifactory.addDateChooser("largefiles.filter.locked.newer", null, leftContainer);
		revisionCountMinEl = uifactory.addTextElement("largefiles.filter.revision.count.min", 4, "", leftContainer);
		downloadCountMinEl = uifactory.addTextElement("largefiles.filter.download.count.min", 8, "", leftContainer);
		maxResultEl = uifactory.addTextElement("largefiles.filter.results.max", 5, "100", leftContainer);
		minSizeEl = uifactory.addTextElement("largefiles.filter.size.min", 18, "", leftContainer);


		// Right part of the filter
		createdAtOlderChooser = uifactory.addDateChooser("largefiles.filter.created.older", "largefiles.filter.created.older", null, rightContainer);
		editedAtOlderChooser = uifactory.addDateChooser("largefiles.filter.edited.older", "largefiles.filter.edited.older", null, rightContainer);
		lockedAtOlderChooser = uifactory.addDateChooser("largefiles.filter.locked.older", null, rightContainer);


		String[] trashedValues = new String[] {
				translate("largefiles.filter.trashed.only"),
				translate("largefiles.filter.trashed.not"),
				translate("largefiles.filter.trashed.both")
		};
		trashedSelection = uifactory.addRadiosHorizontal("largefiles.filter.trashed", "largefiles.filter.trashed", rightContainer, TRASHED_KEYS, trashedValues);
		trashedSelection.select(TRASHED_KEYS[2], true);

		String[] revisionValues = new String[] {
				translate("largefiles.filter.revision.only"),
				translate("largefiles.filter.revision.not"),
				translate("largefiles.filter.revision.both")
		};
		revisionSelection = uifactory.addRadiosHorizontal("largefiles.filter.revision", "largefiles.filter.revision", rightContainer, REVISION_KEYS, revisionValues);
		revisionSelection.select(REVISION_KEYS[2], true);

		String[] lockedValues = new String[] {
				translate("largefiles.filter.locked.only"),
				translate("largefiles.filter.locked.not"),
				translate("largefiles.filter.locked.both")
		};
		lockedSelection = uifactory.addRadiosHorizontal("largefiles.filter.locked", rightContainer, LOCKED_KEYS, lockedValues);
		lockedSelection.select(LOCKED_KEYS[2], true);

		// Filter buttons
		searchButton = uifactory.addFormSubmitButton("largefiles.filter.button.search", filterButtonLayout);
		resetButton = uifactory.addFormLink("largefiles.filter.button.reset", filterButtonLayout, Link.BUTTON);

		// Tabled
		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		DefaultFlexiColumnModel column;

		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.key));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.uuid));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, LargeFilesTableColumns.name, new LargeFilesNameCellRenderer()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, LargeFilesTableColumns.size, new LargeFilesSizeCellRenderer()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, LargeFilesTableColumns.path));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.age));

		column = new DefaultFlexiColumnModel(false, LargeFilesTableColumns.trashed, new LargeFilesTrashedCellRenderer());
		column.setIconHeader(CSSHelper.getIconCssClassFor(CSSHelper.CSS_CLASS_TRASHED));
		columnsModel.addFlexiColumnModel(column);

		column = new DefaultFlexiColumnModel(false, LargeFilesTableColumns.revision, new LargeFilesRevisionCellRenderer());
		column.setIconHeader(CSSHelper.getIconCssClassFor(CSSHelper.CSS_CLASS_REVISION));
		columnsModel.addFlexiColumnModel(column);

		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.revisionNr));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.revisionComment));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.fileCategory));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.fileType));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.downloadCount));

		DefaultFlexiColumnModel sendMail = new DefaultFlexiColumnModel(false, LargeFilesTableColumns.sendMail, "sendMail", new LargeFilesSendMailCellRenderer());
		sendMail.setIconHeader(CSSHelper.getIconCssClassFor(CSSHelper.CSS_CLASS_MAIL));
		columnsModel.addFlexiColumnModel(sendMail);

		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.author, "selectAuthor"));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.license));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.language));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.source));

		column = new DefaultFlexiColumnModel(false, LargeFilesTableColumns.locked, new LargeFilesLockedCellRenderer());
		column.setIconHeader(CSSHelper.getIconCssClassFor(CSSHelper.CSS_CLASS_LOCKED));
		columnsModel.addFlexiColumnModel(column);

		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.lockedAt));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.lockedBy, "selectLockedBy"));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.creator));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.publisher));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.pubDate));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.createdAt));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, LargeFilesTableColumns.lastModifiedAt));

		largeFilesTableModel = new LargeFilesTableModel(columnsModel, getLocale());
		largeFilesTableElement = uifactory.addTableElement(getWindowControl(), "large_files", largeFilesTableModel, getTranslator(), formLayout);

		FlexiTableSortOptions sortOptions = new FlexiTableSortOptions();
		sortOptions.setDefaultOrderBy(new SortKey(LargeFilesTableColumns.size.name(), false));
		sortOptions.setFromColumnModel(true);
		largeFilesTableElement.setSortSettings(sortOptions);
		largeFilesTableElement.setAndLoadPersistedPreferences(ureq, "admin-large-files-list");	
		largeFilesTableElement.setSearchEnabled(false);
		largeFilesTableElement.setExportEnabled(true);
	}

	@Override
	protected void doDispose() {

	}

	@Override
	protected void formOK(UserRequest ureq) {
		updateModel();
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source == largeFilesTableElement) {
			if(event instanceof SelectionEvent) {
				SelectionEvent te = (SelectionEvent) event;
				String cmd = te.getCommand();
				LargeFilesTableContentRow contentRow = largeFilesTableModel.getObject(te.getIndex());
				if("selectAuthor".equals(cmd)) {
					if (contentRow.getAuthor() != null) {
						openUser(ureq, contentRow.getAuthor().getKey());
					}
				} else if("selectLockedBy".equals(cmd)) {
					if (contentRow.getLockedBy() != null) {
						openUser(ureq, contentRow.getLockedBy().getKey());
					}
				} else if("sendMail".equals(cmd)) {
					if (contentRow.getAuthor() != null) {
						contactUser(ureq, contentRow.getAuthor());
					}
				}
			}
		} else if(source == resetButton) {
			resetForm();
		} else if(source instanceof FormLink) {
			FormLink link = (FormLink) source;

			if("pathInfo".equals(link.getCmd())) {
				removeAsListenerAndDispose(pathInfoCalloutCtrl);

				LargeFilesTableContentRow row = (LargeFilesTableContentRow) link.getUserObject();
				System.out.println(link.getUserObject());

				CalloutSettings settings = new CalloutSettings(false);

				pathInfoCalloutCtrl = new CloseableCalloutWindowController(ureq, getWindowControl(),
						TextFactory.createTextComponentFromString("pathInfo", row.getPath(), "", true, null), link.getFormDispatchId(), "", true, "", settings);
				listenTo(pathInfoCalloutCtrl);
				pathInfoCalloutCtrl.activate();
			}

		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void event(UserRequest  ureq, Controller source, Event event) {
		if (source == cmc) {
			cleanUp();
		} else if (source == contactCtrl) {
			cmc.deactivate();
			cleanUp();
		} 
		super.event(ureq, source, event);
	}

	private void cleanUp() {
		removeAsListenerAndDispose(cmc);
		removeAsListenerAndDispose(contactCtrl);
		removeAsListenerAndDispose(pathInfoCalloutCtrl);
		cmc = null;
		contactCtrl = null;
		pathInfoCalloutCtrl = null;
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOK = super.validateFormLogic(ureq);

		if(maxResultEl.getValue() != "") {
			try {
				if(Integer.parseInt(maxResultEl.getValue()) <= 0) {
					maxResultEl.setErrorKey("largefiles.filter.error.small", null);
					allOK &= false;
				}
			} catch (Exception e) {
				maxResultEl.setErrorKey("largefiles.filter.error.letter", null);
				allOK &= false;
			}
		}

		if(minSizeEl.getValue() != "") {
			try {
				if(Integer.parseInt(minSizeEl.getValue()) <= 0) {
					minSizeEl.setErrorKey("largefiles.filter.error.small", null);
					allOK &= false;
				}
			} catch (Exception e) {
				minSizeEl.setErrorKey("largefiles.filter.error.letter", null);
				allOK &= false;
			}
		}

		if(downloadCountMinEl.getValue() != "") {
			try {
				if(Integer.parseInt(downloadCountMinEl.getValue()) <= 0) {
					downloadCountMinEl.setErrorKey("largefiles.filter.error.small", null);
					allOK &= false;
				}
			} catch (Exception e) {
				downloadCountMinEl.setErrorKey("largefiles.filter.error.letter", null);
				allOK &= false;
			}
		}

		if(revisionCountMinEl.getValue() != "") {
			try {
				if(Integer.parseInt(revisionCountMinEl.getValue()) <= 0) {
					revisionCountMinEl.setErrorKey("largefiles.filter.error.small", null);
					allOK &= false;
				}
			} catch (Exception e) {
				revisionCountMinEl.setErrorKey("largefiles.filter.error.letter", null);
				allOK &= false;
			}
		}

		return allOK;
	}

	private void contactUser(UserRequest ureq, Identity user) {
		removeAsListenerAndDispose(cmc);

		ContactMessage cmsg = new ContactMessage(getIdentity());
		String fullName = user.getUser().getFirstName() + " " + user.getUser().getLastName();
		ContactList contactList = new ContactList(fullName);
		contactList.add(user);
		cmsg.addEmailTo(contactList);
		cmsg.setSubject("Too large files in your personal folder");

		String bodyStart = translate("largefiles.mail.start", new String[] {user.getUser().getFirstName() + user.getUser().getLastName()});
		String bodyFiles = "<ul>";
		String bodyEnd = translate("largefiles.mail.end");

		for(LargeFilesTableContentRow row:rows) {
			if (row.getAuthor() == user) {
				bodyFiles += "<li><b>" + Formatter.formatBytes(row.getSize()) + "</b>  -  " +row.getName() + "</li>";
			}
		}
		bodyFiles += "</ul>";

		cmsg.setBodyText(bodyStart + bodyFiles + bodyEnd);
		contactCtrl = new ContactFormController(ureq, getWindowControl(), true, false, false, cmsg, null);
		listenTo(contactCtrl);
		cmc = new CloseableModalController(getWindowControl(), "close", contactCtrl.getInitialComponent());
		cmc.activate();
		listenTo(cmc);
	}

	@Override
	public void setEnabled(boolean enable) {
		// Nothing do to here
	}

	@Override
	public List<String> getConditionalQueries() {
		return Collections.emptyList();
	}

	private void openUser(UserRequest ureq, Long userKey) {
		NewControllerFactory.getInstance().launch("[UserAdminSite:0][usearch:0][table:0][Identity:" + userKey.toString() + "]", ureq, getWindowControl());
	}
}