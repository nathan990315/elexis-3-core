package ch.elexis.core.ui.locks;

import java.util.Optional;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import ch.elexis.core.constants.ElexisSystemPropertyConstants;
import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.service.LocalLockServiceHolder;
import ch.elexis.core.lock.types.LockInfo;
import ch.elexis.core.lock.types.LockResponse;
import ch.elexis.core.model.Identifiable;
import ch.elexis.core.services.IElexisServerService.ConnectionStatus;
import ch.elexis.core.services.holder.ElexisServerServiceHolder;
import ch.elexis.core.services.holder.StoreToStringServiceHolder;
import ch.elexis.data.PersistentObject;

public class LockStatusDialog extends TitleAreaDialog {
	private CheckboxTableViewer checkboxTableViewer;

	/**
	 * Create the dialog.
	 *
	 * @param parentShell
	 */
	public LockStatusDialog(Shell parentShell) {
		super(parentShell);

	}

	/**
	 * Create contents of the dialog.
	 *
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		setMessage("Overview on the locks currently held by this Elexis instance");
		setTitle("Lock Status");
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		checkboxTableViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.FULL_SELECTION);
		Table table = checkboxTableViewer.getTable();
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		checkboxTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		checkboxTableViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				LockInfo li = (LockInfo) element;
				PersistentObject po = CoreHub.poFactory.createFromString(li.getElementStoreToString());
				String label;
				if (po != null) {
					label = po.getLabel();
				} else {
					Optional<Identifiable> identifiable = StoreToStringServiceHolder.get()
							.loadFromString(li.getElementStoreToString());
					label = (identifiable.isPresent()) ? identifiable.get().getLabel() : "null"; //$NON-NLS-1$
				}
				return li.getElementType() + ": " + label; //$NON-NLS-1$
			}
		});
		checkboxTableViewer.setInput(LocalLockServiceHolder.get().getCopyOfAllHeldLocks());

		Label lblSystemUuid = new Label(container, SWT.NONE);
		lblSystemUuid.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		lblSystemUuid.setText("System UUID: " + LocalLockServiceHolder.get().getSystemUuid()); //$NON-NLS-1$

		Label lblLockStatus = new Label(container, SWT.NONE);
		ConnectionStatus connectinStatus = ElexisServerServiceHolder.get().getConnectionStatus();
		StringBuilder statusString = new StringBuilder();
		statusString.append("Lock-Service: " + connectinStatus.name()); //$NON-NLS-1$
		if (connectinStatus != ConnectionStatus.STANDALONE) {
			statusString
					.append(" @ " + System.getProperty(ElexisSystemPropertyConstants.ELEXIS_SERVER_REST_INTERFACE_URL)); //$NON-NLS-1$
		}
		lblLockStatus.setText(statusString.toString());

		return area;
	}

	@Override
	protected void okPressed() {
		setErrorMessage(null);
		Object[] checkedElements = checkboxTableViewer.getCheckedElements();
		boolean error = false;
		for (Object object : checkedElements) {
			LockInfo lockInfo = (LockInfo) object;
			LockResponse lockResponse = LocalLockServiceHolder.get().releaseLock(lockInfo);
			if (!lockResponse.isOk()) {
				setErrorMessage("Error releasing lock " + lockInfo.getElementStoreToString());
				error = true;
				break;
			}
		}

		if (!error) {
			super.okPressed();
		}
	}

	/**
	 * Create contents of the button bar.
	 *
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}
}
