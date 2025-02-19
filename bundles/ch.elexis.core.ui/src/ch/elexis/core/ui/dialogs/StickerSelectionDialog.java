/*******************************************************************************
 * Copyright (c) 2008-2011, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *
 *******************************************************************************/

package ch.elexis.core.ui.dialogs;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;

import ch.elexis.core.model.ISticker;
import ch.elexis.core.services.IStickerService;
import ch.elexis.core.services.holder.StickerServiceHolder;
import ch.elexis.core.ui.util.CoreUiUtil;
import ch.elexis.core.ui.util.SWTHelper;

public class StickerSelectionDialog extends TitleAreaDialog {
	private TableViewer viewer;
	private StickerViewerComparator comparator;

	private List<ISticker> allStickers;
	private List<ISticker> selectedStickers;

	private String message;

	private IStickerService stickerService = StickerServiceHolder.get();

	public StickerSelectionDialog(Shell shell, String message, Class<?> clazz) {
		super(shell);
		this.message = message;
		allStickers = stickerService.getStickersForClass(clazz);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		ret.setLayout(new GridLayout());
		Label lbl = new Label(ret, SWT.WRAP);
		lbl.setText(Messages.AssignStickerDialog_PleaseConfirm); // $NON-NLS-1$

		viewer = new TableViewer(ret, SWT.CHECK | SWT.FULL_SELECTION);
		viewer.getTable().setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		viewer.getTable().setHeaderVisible(true);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(allStickers);
		createColumns();
		comparator = new StickerViewerComparator();
		viewer.setComparator(comparator);

		return ret;
	}

	private void createColumns() {
		// first column - label
		TableViewerColumn col = new TableViewerColumn(viewer, SWT.NONE);
		col.getColumn().setText(Messages.AssignStickerDialog_StickerName);
		col.getColumn().setWidth(300);
		col.getColumn().addSelectionListener(getSelectionAdapter(col.getColumn(), 0));
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				ISticker s = (ISticker) element;
				return s.getLabel();
			}

			@Override
			public Color getBackground(Object element) {
				ISticker s = (ISticker) element;
				return CoreUiUtil.getColorForString(s.getBackground());
			}

			@Override
			public Color getForeground(Object element) {
				ISticker s = (ISticker) element;
				return CoreUiUtil.getColorForString(s.getForeground());
			}
		});

		// second column - value
		col = new TableViewerColumn(viewer, SWT.NONE);
		col.getColumn().setText(Messages.AssignStickerDialog_StickerWert);
		col.getColumn().setWidth(50);
		col.getColumn().addSelectionListener(getSelectionAdapter(col.getColumn(), 1));
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				ISticker s = (ISticker) element;
				return s.getImportance() + StringUtils.EMPTY;
			}

			@Override
			public Color getBackground(Object element) {
				ISticker s = (ISticker) element;
				return CoreUiUtil.getColorForString(s.getBackground());
			}

			@Override
			public Color getForeground(Object element) {
				ISticker s = (ISticker) element;
				return CoreUiUtil.getColorForString(s.getForeground());
			}
		});
	}

	private SelectionAdapter getSelectionAdapter(final TableColumn column, final int index) {
		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				comparator.setColumn(index);
				viewer.getTable().setSortDirection(comparator.getDirection());
				viewer.getTable().setSortColumn(column);
				viewer.refresh();
			}
		};
		return selectionAdapter;
	}

	@Override
	public void create() {
		super.create();
		setTitle("Sticker"); //$NON-NLS-1$
		if (message != null) {
			setMessage(message);
		}
		getShell().setText("Elexis Sticker"); //$NON-NLS-1$
	}

	public List<ISticker> getSelection() {
		return selectedStickers;
	}

	@Override
	protected void okPressed() {
		selectedStickers = Stream.of(viewer.getTable().getItems()).filter(it -> it.getChecked())
				.map(it -> (ISticker) it.getData()).collect(Collectors.toList());
		super.okPressed();
	}

	class StickerViewerComparator extends ViewerComparator {
		private int propertyIndex;
		private boolean direction = true;
		private ISticker s1;
		private ISticker s2;

		public StickerViewerComparator() {
			this.propertyIndex = 0;
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof ISticker && e2 instanceof ISticker) {
				s1 = (ISticker) e1;
				s2 = (ISticker) e2;
				int rc = 0;

				switch (propertyIndex) {
				case 0:
					String label1 = StringUtils.defaultString(s1.getLabel()).toLowerCase();
					String label2 = StringUtils.defaultString(s2.getLabel()).toLowerCase();
					rc = label1.compareTo(label2);
					break;
				case 1:
					Integer wert1 = s1.getImportance();
					Integer wert2 = s2.getImportance();
					rc = wert1.compareTo(wert2);
					break;
				default:
					break;
				}

				// If descending order, flip the direction
				if (direction) {
					rc = -rc;
				}
				return rc;
			}
			return 0;
		}

		/**
		 * for sort direction
		 *
		 * @return SWT.DOWN or SWT.UP
		 */
		public int getDirection() {
			return direction ? SWT.DOWN : SWT.UP;
		}

		public void setColumn(int column) {
			if (column == this.propertyIndex) {
				// Same column as last sort; toggle the direction
				direction = !direction;
			} else {
				// New column; do an ascending sort
				this.propertyIndex = column;
				direction = true;
			}
		}

	}

}
