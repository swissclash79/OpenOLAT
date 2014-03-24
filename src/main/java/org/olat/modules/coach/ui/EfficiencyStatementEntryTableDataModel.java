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

import org.olat.core.gui.components.table.TableDataModel;
import org.olat.course.assessment.UserEfficiencyStatement;
import org.olat.modules.coach.model.EfficiencyStatementEntry;
import org.olat.modules.coach.ui.ProgressValue;
import org.olat.repository.RepositoryEntry;

/**
 * 
 * Description:<br>
 * 
 * <P>
 * Initial Date:  8 févr. 2012 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class EfficiencyStatementEntryTableDataModel implements TableDataModel<EfficiencyStatementEntry> {
	
	private List<EfficiencyStatementEntry> group;
	
	public EfficiencyStatementEntryTableDataModel(List<EfficiencyStatementEntry> group) {
		this.group = group;
	}

	@Override
	public int getColumnCount() {
		return 5;
	}

	@Override
	public int getRowCount() {
		return group == null ? 0 : group.size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		EfficiencyStatementEntry entry = group.get(row);
		switch(Columns.getValueAt(col)) {
			case studentName: {
				return entry.getStudentFullName();
			}
			case repoName: {
				RepositoryEntry re = entry.getCourse();
				return re.getDisplayname();
			}
			case score: {
				UserEfficiencyStatement s = entry.getUserEfficencyStatement();
				return s == null ? null : s.getScore();
			}
			case passed: {
				UserEfficiencyStatement s = entry.getUserEfficencyStatement();
				return s == null ? null : s.getPassed();
			}
			case progress: {
				UserEfficiencyStatement s = entry.getUserEfficencyStatement();
				if(s == null || s.getTotalNodes() == null) {
					ProgressValue val = new ProgressValue();
					val.setTotal(100);
					val.setGreen(0);
					return val;
				}
				
				ProgressValue val = new ProgressValue();
				val.setTotal(s.getTotalNodes().intValue());
				val.setGreen(s.getAttemptedNodes() == null ? 0 : s.getAttemptedNodes().intValue());
				return val;
			}
			case lastModification: {
				UserEfficiencyStatement s = entry.getUserEfficencyStatement();
				return s == null ? null : s.getLastModified();
			}
		}
		return null;
	}

	@Override
	public EfficiencyStatementEntry getObject(int row) {
		return group.get(row);
	}
	
	public int indexOf(EfficiencyStatementEntry obj) {
		return group.indexOf(obj);
	}

	@Override
	public void setObjects(List<EfficiencyStatementEntry> objects) {
		group = objects;
	}

	@Override
	public EfficiencyStatementEntryTableDataModel createCopyWithEmptyList() {
		return new EfficiencyStatementEntryTableDataModel(new ArrayList<EfficiencyStatementEntry>());
	}
	
	public static enum Columns {
		studentName,
		repoName,
		score,
		passed,
		progress,
		lastModification,
		;

		public static Columns getValueAt(int ordinal) {
			if(ordinal >= 0 && ordinal < values().length) {
				return values()[ordinal];
			}
			return studentName;
		}
	}
}
