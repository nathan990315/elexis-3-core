/*******************************************************************************
 * Copyright (c) 2005-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *******************************************************************************/

package ch.elexis.core.ui.contacts.dialogs;

import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ch.elexis.core.constants.StringConstants;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.l10n.Messages;
import ch.elexis.core.ui.icons.ImageSize;
import ch.elexis.core.ui.icons.Images;
import ch.elexis.core.ui.locks.AcquireLockUi;
import ch.elexis.core.ui.locks.ILockHandler;
import ch.elexis.core.ui.util.SWTHelper;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.Person;
import ch.elexis.data.Person.PersonDataException;
import ch.elexis.data.Query;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;
import ch.rgw.tools.TimeTool.TimeFormatException;

public class PatientErfassenDialog extends TitleAreaDialog {
	HashMap<String, String> fld;
	Text tName, tVorname, tGebDat, tStrasse, tPlz, tOrt, tTel;
	Combo cbSex;
	Patient result;

	public Patient getResult() {
		return result;
	}

	public PatientErfassenDialog(final Shell parent, final HashMap<String, String> fields) {
		super(parent);
		fld = fields;
	}

	private String getField(String name) {
		String ret = fld.get(name);
		if (ret == null) {
			ret = StringTool.leer;
		}
		return ret;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		ret.setLayout(new GridLayout(2, false));
		new Label(ret, SWT.NONE).setText(Messages.PatientErfassenDialog_Name); // $NON-NLS-1$
		tName = new Text(ret, SWT.BORDER);
		tName.setText(getField(Patient.FLD_NAME));
		tName.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		new Label(ret, SWT.NONE).setText(Messages.PatientErfassenDialog_firstName); // $NON-NLS-1$
		tVorname = new Text(ret, SWT.BORDER);
		tVorname.setText(getField(Patient.FLD_FIRSTNAME));
		tVorname.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		new Label(ret, SWT.NONE).setText(Messages.PatientErfassenDialog_sex); // $NON-NLS-1$
		cbSex = new Combo(ret, SWT.SINGLE);
		String toolTip = String.format(Messages.Patient_male_female_tooltip, Messages.Patient_male_short,
				Messages.Patient_female_short, Messages.Patient_male_long, Messages.Patient_female_long);
		cbSex.setToolTipText(toolTip);
		cbSex.setItems(new String[] { Messages.Patient_male_short, Messages.Patient_female_short });
		if (StringTool.isNothing(getField(Patient.FLD_SEX))) {
			cbSex.select(0);
		} else {
			cbSex.select(StringTool.isFemale(getField(Patient.FLD_FIRSTNAME)) ? 1 : 0);
		}
		new Label(ret, SWT.NONE).setText(Messages.PatientErfassenDialog_birthDate); // $NON-NLS-1$
		tGebDat = new Text(ret, SWT.BORDER);
		tGebDat.setText(getField(Patient.FLD_DOB));
		tGebDat.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));

		new Label(ret, SWT.NONE).setText(Messages.PatientErfassenDialog_street); // $NON-NLS-1$
		tStrasse = new Text(ret, SWT.BORDER);
		tStrasse.setText(getField(Patient.FLD_STREET));
		tStrasse.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));

		new Label(ret, SWT.NONE).setText(Messages.PatientErfassenDialog_zip); // $NON-NLS-1$
		tPlz = new Text(ret, SWT.BORDER);
		tPlz.setText(getField(Patient.FLD_ZIP));
		tPlz.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));

		new Label(ret, SWT.NONE).setText(Messages.PatientErfassenDialog_city); // $NON-NLS-1$
		tOrt = new Text(ret, SWT.BORDER);
		tOrt.setText(getField(Patient.FLD_PLACE));
		tOrt.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));

		new Label(ret, SWT.NONE).setText(Messages.PatientErfassenDialog_phone); // $NON-NLS-1$
		tTel = new Text(ret, SWT.BORDER);
		tTel.setText(getField(Patient.FLD_PHONE1));
		tTel.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		return ret;
	}

	@Override
	public void create() {
		super.create();
		setMessage(Messages.PatientErfassenDialog_pleaseEnterPersonalia); // $NON-NLS-1$
		setTitle(Messages.PatientErfassenDialog_enterData); // $NON-NLS-1$
		getShell().setText(Messages.PatientErfassenDialog_enterPatient); // $NON-NLS-1$
		setTitleImage(Images.IMG_LOGO.getImage(ImageSize._75x66_TitleDialogIconSize));
	}

	@Override
	protected void okPressed() {
		final String[] ret = new String[8];
		ret[0] = tName.getText();
		ret[1] = tVorname.getText();
		int idx = cbSex.getSelectionIndex();
		if (idx == 1 || cbSex.getText().contentEquals(Messages.Patient_female_short)) {
			ret[2] = Patient.FEMALE; // German w for weiblich = female
		} else if (idx == 0 || cbSex.getText().contentEquals(Messages.Patient_male_short)) {
			ret[2] = Patient.MALE;
		} else if (idx == -1) {
			SWTHelper.showError(Messages.PatientErfassenDialog_Error_Sex,
					Messages.PatientErfassenDialog_Sex_must_be_specified);
			return;
		}
		ret[3] = tGebDat.getText();
		try {
			TimeTool check = null;
			if (!StringTool.isNothing(ret[3])) {
				check = new TimeTool(ret[3], true);
			}
			ret[4] = tStrasse.getText();
			ret[5] = tPlz.getText();
			ret[6] = tOrt.getText();
			ret[7] = tTel.getText();
			Query<Kontakt> qbe = new Query<Kontakt>(Kontakt.class);
			qbe.add("Bezeichnung1", Query.EQUALS, ret[0], true); //$NON-NLS-1$
			qbe.add("Bezeichnung2", Query.EQUALS, ret[1], true); //$NON-NLS-1$
			if (check != null)
				qbe.add(Person.BIRTHDATE, Query.EQUALS, check.toDBString(false), true);
			List<Kontakt> list = qbe.execute();
			if ((list != null) && (!list.isEmpty())) {
				Kontakt k = list.get(0);
				if (k.istPerson()) {
					k.set(Kontakt.FLD_IS_PATIENT, StringConstants.ONE);
					if (MessageDialog.openConfirm(getShell(), Messages.PatientErfassenDialog_personExists,
							Messages.PatientErfassenDialog_personWithThisNameExists) == false) {
						// abort dialog
						super.cancelPressed();
						return;
					}
				}
			}
			result = new Patient(ret[0], ret[1], check, ret[2]);
			AcquireLockUi.aquireAndRun(result, new ILockHandler() {

				@Override
				public void lockFailed() {
					result.delete();
				}

				@Override
				public void lockAcquired() {
					result.set(
							new String[] { Kontakt.FLD_STREET, Kontakt.FLD_ZIP, Kontakt.FLD_PLACE, Kontakt.FLD_PHONE1 },
							new String[] { ret[4], ret[5], ret[6], ret[7] });

					ElexisEventDispatcher.fireSelectionEvent(result);
				}
			});
			super.okPressed();
		} catch (TimeFormatException e) {
			ExHandler.handle(e);
			SWTHelper.showError("Falsches Datumsformat", "Das Geburtsdatum kann nicht interpretiert werden");
			return;
		} catch (PersonDataException pe) {
			ExHandler.handle(pe);
			SWTHelper.showError("Unplausible Angaben", "Bitte überprüfen Sie die Eingabe nochmals: " + pe.getMessage());
		}
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
}
