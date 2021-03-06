/*
 * This code is subject to the HIEOS License, Version 1.0
 *
 * Copyright(c) 2011 Vangent, Inc.  All rights reserved.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vangent.hieos.DocViewer.client.view.patient;

import java.util.LinkedHashMap;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.vangent.hieos.DocViewer.client.model.config.ConfigDTO;
import com.vangent.hieos.DocViewer.client.controller.DocViewerController;

/**
 * 
 * @author Bernie Thuman
 * 
 */
public class FindPatientsMainCanvas extends Canvas {
	private final DocViewerController controller;
	private final PatientListCanvas patientListCanvas;
	private final PatientSearchFormCanvas patientSearchFormCanvas;

	/**
	 * @wbp.parser.constructor
	 * @param mainController
	 */
	public FindPatientsMainCanvas(DocViewerController mainController) {
		this.controller = mainController;
		this.patientListCanvas = new PatientListCanvas(controller);
		this.patientSearchFormCanvas = new PatientSearchFormCanvas(controller);
		final DynamicForm optionsForm = this.getOptionsForm();

		// Now add to the layout.
		final VLayout vlayout = new VLayout();
		vlayout.addMember(optionsForm);
		final LayoutSpacer spacer0 = new LayoutSpacer();
		spacer0.setHeight(10);
		vlayout.addMember(spacer0);
		vlayout.addMember(this.patientSearchFormCanvas);

		final HLayout layout = new HLayout();
		layout.addMember(vlayout);
		final LayoutSpacer spacer1 = new LayoutSpacer();
		spacer1.setWidth(8);
		layout.addMember(spacer1);
		layout.addMember(this.patientListCanvas);

		this.addChild(layout);
	}

	/**
	 * 
	 * @return
	 */
	private DynamicForm getOptionsForm() {
		final RadioGroupItem searchModeRadioGroupItem = new RadioGroupItem();
		searchModeRadioGroupItem.setTitle("");
		searchModeRadioGroupItem.setShowTitle(false);

		ConfigDTO cfg = controller.getConfig();
		LinkedHashMap<String, String> searchModeMap = new LinkedHashMap<String, String>();
		searchModeMap.put(ConfigDTO.VAL_SEARCH_MODE_HIE,
				cfg.get(ConfigDTO.KEY_LABEL_HIE_MODE));
		searchModeMap.put(ConfigDTO.VAL_SEARCH_MODE_NHIN_EXCHANGE,
				cfg.get(ConfigDTO.KEY_LABEL_NHIN_MODE));
		searchModeRadioGroupItem.setValueMap(searchModeMap);
		searchModeRadioGroupItem.setDefaultValue(cfg
				.get(ConfigDTO.KEY_SEARCH_MODE));
		searchModeRadioGroupItem.addChangedHandler(new ChangedHandler() {
			@Override
			public void onChanged(ChangedEvent event) {
				String searchMode = (String) event.getValue();
				controller.getConfig().put(ConfigDTO.KEY_SEARCH_MODE, searchMode);
				// SC.say("value = " + searchMode);
			}
		});

		final DynamicForm optionsForm = new DynamicForm();
		optionsForm.setIsGroup(true);
		optionsForm.setHeight(40);
		optionsForm.setWidth(300);
		optionsForm.setNumCols(1);
		optionsForm.setGroupTitle("<b>Search Mode</b>");
		optionsForm.setFields(searchModeRadioGroupItem);

		return optionsForm;
	}

	/**
	 * 
	 * @param gridRecords
	 */
	public void updatePatientList(ListGridRecord[] gridRecords) {
		this.patientListCanvas.update(gridRecords);
	}
}
