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

import java.util.Locale;

import org.olat.core.gui.components.table.CustomCssCellRenderer;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.translator.Translator;
import org.olat.modules.coach.ui.ProgressValue;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class ProgressRenderer extends CustomCssCellRenderer {
	
	private final boolean neutral;
	private final Translator translator;
	
	public ProgressRenderer(boolean neutral, Translator translator) {
		this.neutral = neutral;
		this.translator = translator;
	}

	@Override
	protected String getCssClass(Object val) {
		return neutral ? "o_eff_statement_progress_neutral" : "o_eff_statement_rg";
	}

	@Override
	public void render(StringOutput sb, Renderer renderer, Object val, Locale locale, int alignment, String action) {
		if(renderer == null) {
			ProgressValue progress = (ProgressValue)val;
			int green = progress.getGreen();
			int total = progress.getTotal();
			sb.append(green).append(" / ").append(total);
		} else {
			super.render(sb, renderer, val, locale, alignment, action);
		}
	}

	@Override
	protected String getCellValue(Object val) {
		StringBuilder sb = new StringBuilder();
		
		if(val instanceof ProgressValue) {
			ProgressValue progress = (ProgressValue)val;
			int green = Math.round(100.0f * ((float)progress.getGreen() / (float)progress.getTotal()));
			String[] values = new String[]{ Integer.toString(progress.getGreen()), Integer.toString(progress.getTotal()) };
			String tooltip = translator.translate("tooltip.of", values);
			sb.append("<div class='o_eff_statement_progress' ext:qtip='").append(tooltip).append("'>")
			  .append("<div class='o_eff_statement_solved' style='width: ").append(green).append("%;'/>")
			  .append("&#160;")
			  .append("</div>")
			  .append("</div>");
		}
		return sb.toString();
	}

	@Override
	protected String getHoverText(Object val) {
		return null;
	}
}
