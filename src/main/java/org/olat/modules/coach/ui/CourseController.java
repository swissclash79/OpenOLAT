/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.coach.ui;

import java.util.ArrayList;
import java.util.List;

import org.olat.NewControllerFactory;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.table.BooleanColumnDescriptor;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.CustomRenderColumnDescriptor;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableDataModel;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.text.TextComponent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.StringHelper;
import org.olat.core.util.resource.OresHelper;
import org.olat.modules.coach.CoachingService;
import org.olat.modules.coach.model.CourseStatEntry;
import org.olat.modules.coach.model.EfficiencyStatementEntry;
import org.olat.repository.RepositoryEntry;

import org.olat.modules.coach.ui.EfficiencyStatementEntryTableDataModel.Columns;
import org.olat.modules.coach.ui.ToolbarController.Position;

/**
 * 
 * Description:<br>
 * Overview of all students under the scrutiny of a coach.
 * 
 * <P>
 * Initial Date:  8 févr. 2012 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class CourseController extends BasicController implements Activateable2 {
	
	private final Link backLink, next, previous;
	private final Link nextCourse, previousCourse;
	private final Link openCourse;
	private final TextComponent detailsCmp, detailsCourseCmp;
	private final TableController tableCtr;
	private final VelocityContainer mainVC;
	private final VelocityContainer courseDetailsVC;
	
	private CloseableModalController cmc;
	private ContactController contactCtrl;
	private final ToolbarController toolbar;
	private EfficiencyStatementDetailsController statementCtrl;
	
	private boolean hasChanged = false;
	
	private final RepositoryEntry course;
	private final CourseStatEntry courseStat;
	private final CoachingService coachingService;
	
	public CourseController(UserRequest ureq, WindowControl wControl, RepositoryEntry course, CourseStatEntry courseStat, int index, int numOfCourses) {
		super(ureq, wControl);
		
		this.course = course;
		this.courseStat = courseStat;
		coachingService = CoreSpringFactory.getImpl(CoachingService.class);

		TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(translate("error.no.found"));
		tableConfig.setDownloadOffered(true);
		tableConfig.setPreferencesOffered(true, "courseController");
		
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), null, null, null, null, true, getTranslator());
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("student.name", Columns.studentName.ordinal(), "select", getLocale()));
		tableCtr.addColumnDescriptor(false, new DefaultColumnDescriptor("table.header.course.name", Columns.repoName.ordinal(), "select", getLocale()));
		tableCtr.addColumnDescriptor(new BooleanColumnDescriptor("table.header.passed", Columns.passed.ordinal(), translate("passed.true"), translate("passed.false")));

		tableCtr.addColumnDescriptor(new CustomRenderColumnDescriptor("table.header.score", Columns.score.ordinal(), "select", getLocale(),
				ColumnDescriptor.ALIGNMENT_RIGHT, new ScoreCellRenderer()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("table.header.lastScoreDate", Columns.lastModification.ordinal(), "select", getLocale()));
		listenTo(tableCtr);
		
		List<EfficiencyStatementEntry> entries = loadModel();
		
		mainVC = createVelocityContainer("course");
		
		courseDetailsVC = createVelocityContainer("course_details");
		courseDetailsVC.contextPut("courseName", StringHelper.escapeHtml(course.getDisplayname()));

		toolbar = new ToolbarController(ureq, wControl, getTranslator());
		listenTo(toolbar);

		backLink = toolbar.addToolbarLink("back", this, Position.left);
		backLink.setCustomEnabledLinkCSS("b_link_back");

		previous = toolbar.addToolbarLink("previous", this, Position.center);
		previous.setCustomEnabledLinkCSS("b_with_small_icon_left   b_move_left_icon");
		previous.setEnabled(entries.size() > 1);
		
		detailsCmp = toolbar.addToolbarText("", this, Position.center);
		
		next = toolbar.addToolbarLink("next", this, Position.center);
		next.setCustomEnabledLinkCSS("b_with_small_icon_right   b_move_right_icon");
		next.setEnabled(entries.size() > 1);
		
		//courses next,previous
		previousCourse = toolbar.addToolbarLink("previous.course", this, Position.center);
		previousCourse.setCustomEnabledLinkCSS("b_with_small_icon_left  b_move_left_icon");
		previousCourse.setEnabled(numOfCourses > 1);
		
		detailsCourseCmp = toolbar.addToolbarText("details.course", "", this, Position.center);
		detailsCourseCmp.setText(translate("students.details", new String[]{
				StringHelper.escapeHtml(course.getDisplayname()),
				Integer.toString(index + 1), Integer.toString(numOfCourses)
		}));
		nextCourse = toolbar.addToolbarLink("next.course", this, Position.center);
		nextCourse.setCustomEnabledLinkCSS("b_with_small_icon_right   b_move_right_icon");
		nextCourse.setEnabled(numOfCourses > 1);
		
		openCourse = LinkFactory.createButton("open", courseDetailsVC, this);
		openCourse.setCustomEnabledLinkCSS("b_link_left_icon b_link_course");
		courseDetailsVC.put("open", openCourse);
		
		mainVC.put("toolbar", toolbar.getInitialComponent());
		mainVC.put("courseTable", tableCtr.getInitialComponent());
		mainVC.put("courseDetails", courseDetailsVC);
		setDetailsToolbarVisible(false);
		putInitialPanel(mainVC);
	}
	
	public CourseStatEntry getEntry() {
		return courseStat;
	}
	
	public List<EfficiencyStatementEntry> loadModel() {
		List<EfficiencyStatementEntry> entries = coachingService.getCourse(getIdentity(), course, 0, -1);
		TableDataModel<EfficiencyStatementEntry> model = new EfficiencyStatementEntryTableDataModel(entries);
		tableCtr.setTableDataModel(model);
		return entries;
	}
	
	private void reloadModel() {
		if(hasChanged) {
			loadModel();
			hasChanged = false;
		}
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if (source == next) {
			nextEntry(ureq);
		} else if (source == previous) {
			previousEntry(ureq);
		} else if (source == backLink) {
			back(ureq);
		} else if (source == openCourse) {
			openCourse(ureq);
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(source == tableCtr) {
			if(event instanceof TableEvent) {
				TableEvent e = (TableEvent) event;
				if("select".equals(e.getActionId())) {
					EfficiencyStatementEntry entry = (EfficiencyStatementEntry)tableCtr.getTableDataModel().getObject(e.getRowId());
					selectDetails(ureq, entry);
				}
			}
		} else if (source == statementCtrl) {
			if(event == Event.CHANGED_EVENT) {
				hasChanged = true;
				fireEvent(ureq, Event.CHANGED_EVENT);
			} else {
				removeDetails(ureq);
			}
		} else if (source == cmc) {
			removeAsListenerAndDispose(cmc);
			removeAsListenerAndDispose(contactCtrl);
			cmc = null;
			contactCtrl = null;
		} else if (source == toolbar) {
			if("back".equals(event.getCommand())) {
				reloadModel();
				back(ureq);
			} else if("next".equals(event.getCommand())) {
				nextEntry(ureq);
			} else if("previous".equals(event.getCommand())) {
				previousEntry(ureq);
			} else if("contact.link".equals(event.getCommand())) {
				contact(ureq);
			} else if ("next.course".equals(event.getCommand())) {
				fireEvent(ureq, event);
			} else if ("previous.course".equals(event.getCommand())) {
				fireEvent(ureq, event);
			}
		}
		super.event(ureq, source, event);
	}

	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty()) return;
		
		ContextEntry ce = entries.get(0);
		OLATResourceable ores = ce.getOLATResourceable();
		if("Identity".equals(ores.getResourceableTypeName())) {
			Long identityKey = ores.getResourceableId();
			for(int i=tableCtr.getRowCount(); i-->0; ) {
				EfficiencyStatementEntry entry = (EfficiencyStatementEntry)tableCtr.getTableDataModel().getObject(i);
				if(identityKey.equals(entry.getStudentKey())) {
					selectDetails(ureq, entry);
					statementCtrl.activate(ureq, entries.subList(1, entries.size()), ce.getTransientState());
					break;
				}
			}
		}
	}

	private void setDetailsToolbarVisible(boolean visible) {
		next.setVisible(visible);
		previous.setVisible(visible);
		detailsCmp.setVisible(visible);

		nextCourse.setVisible(!visible);
		previousCourse.setVisible(!visible);
		detailsCourseCmp.setVisible(!visible);
	}
	
	private void contact(UserRequest ureq) {
		removeAsListenerAndDispose(cmc);
		if(statementCtrl != null) {
			contactCtrl = new ContactController(ureq, getWindowControl());
			cmc = new CloseableModalController(getWindowControl(), translate("close"), contactCtrl.getInitialComponent());
			cmc.activate();
			listenTo(cmc);
		}
	}
	
	private void back(UserRequest ureq) {
		if(statementCtrl == null) {
			fireEvent(ureq, Event.BACK_EVENT);
		} else {
			removeDetails(ureq);
		}
	}
	
	private void previousEntry(UserRequest ureq) {
		EfficiencyStatementEntry currentEntry = statementCtrl.getEntry();
		int previousIndex = tableCtr.getIndexOfSortedObject(currentEntry) - 1;
		if(previousIndex < 0 || previousIndex >= tableCtr.getRowCount()) {
			previousIndex = tableCtr.getRowCount() - 1;
		}
		EfficiencyStatementEntry previousEntry = (EfficiencyStatementEntry)tableCtr.getSortedObjectAt(previousIndex);
		selectDetails(ureq, previousEntry);
	}
	
	private void nextEntry(UserRequest ureq) {
		EfficiencyStatementEntry currentEntry = statementCtrl.getEntry();
		int nextIndex = tableCtr.getIndexOfSortedObject(currentEntry) + 1;
		if(nextIndex < 0 || nextIndex >= tableCtr.getRowCount()) {
			nextIndex = 0;
		}
		EfficiencyStatementEntry nextEntry = (EfficiencyStatementEntry)tableCtr.getSortedObjectAt(nextIndex);
		selectDetails(ureq, nextEntry);
	}
	
	private void removeDetails(UserRequest ureq) {
		mainVC.remove(statementCtrl.getInitialComponent());	
		removeAsListenerAndDispose(statementCtrl);
		statementCtrl = null;
		setDetailsToolbarVisible(false);
		addToHistory(ureq);
	}
	
	private void selectDetails(UserRequest ureq,  EfficiencyStatementEntry entry) {
		removeAsListenerAndDispose(statementCtrl);

		OLATResourceable ores = OresHelper.createOLATResourceableInstance(Identity.class, entry.getStudentKey());
		WindowControl bwControl = addToHistory(ureq, ores, null);
		statementCtrl = new EfficiencyStatementDetailsController(ureq, bwControl, entry, null);
		listenTo(statementCtrl);
		mainVC.put("efficiencyDetails", statementCtrl.getInitialComponent());
		
		int index = tableCtr.getIndexOfSortedObject(entry) + 1;
		String details = translate("students.details", new String[] {
				StringHelper.escapeHtml(entry.getStudentFullName()),
				String.valueOf(index), String.valueOf(tableCtr.getRowCount())
		});
		detailsCmp.setText(details);
		setDetailsToolbarVisible(true);
	}
	
	private void openCourse(UserRequest ureq) {
		List<ContextEntry> ces = new ArrayList<ContextEntry>(1);
		
		OLATResourceable ores = OresHelper.createOLATResourceableInstance("RepositoryEntry", courseStat.getRepoKey());
		ces.add(BusinessControlFactory.getInstance().createContextEntry(ores));

		BusinessControl bc = BusinessControlFactory.getInstance().createFromContextEntries(ces);
	  WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(bc, getWindowControl());
		NewControllerFactory.getInstance().launch(ureq, bwControl);
	}
}
