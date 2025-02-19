/*******************************************************************************
 * Copyright (c) 2005-2009, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *
 *******************************************************************************/
package ch.elexis.core.ui.stock.dialogs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.slf4j.LoggerFactory;

import ch.elexis.core.importer.div.importers.ExcelWrapper;
import ch.elexis.core.model.IArticle;
import ch.elexis.core.model.ICodeElement;
import ch.elexis.core.model.IStock;
import ch.elexis.core.model.IStockEntry;
import ch.elexis.core.services.ICodeElementService.CodeElementTyp;
import ch.elexis.core.services.ICodeElementServiceContribution;
import ch.elexis.core.services.holder.CodeElementServiceHolder;
import ch.elexis.core.services.holder.CoreModelServiceHolder;
import ch.elexis.core.services.holder.LocalLockServiceHolder;
import ch.elexis.core.services.holder.StockServiceHolder;
import ch.elexis.core.services.holder.StoreToStringServiceHolder;
import ch.elexis.core.ui.icons.Images;
import ch.elexis.core.ui.util.SWTHelper;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class ImportArticleDialog extends TitleAreaDialog {

	private ComboViewer comboStockType;
	private Text tFilePath;
	private StringBuffer reportBuilder = null;
	private Link reportLink;

	public ImportArticleDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		reportBuilder = null;
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayout(new GridLayout(3, false));
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		new Label(ret, SWT.NONE).setText("Import in Lager");

		comboStockType = new ComboViewer(ret, SWT.BORDER | SWT.READ_ONLY);
		comboStockType.setContentProvider(ArrayContentProvider.getInstance());
		comboStockType.setInput(StockServiceHolder.get().getAllStocks(false));
		comboStockType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof StructuredSelection) {
					if (!selection.isEmpty()) {
						Object o = ((StructuredSelection) selection).getFirstElement();
					}
				}
			}
		});
		comboStockType.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IStock) {
					IStock stock = (IStock) element;
					return stock.getLabel();
				}
				return super.getText(element);
			}
		});
		comboStockType.getCombo().setLayoutData(SWTHelper.getFillGridData(2, false, 1, false));

		comboStockType.setSelection(new StructuredSelection(StockServiceHolder.get().getDefaultStock()));

		new Label(ret, SWT.NONE).setText("Quelldatei auswählen");

		tFilePath = new Text(ret, SWT.BORDER);
		tFilePath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		tFilePath.setText(StringUtils.EMPTY);
		Button btnBrowse = new Button(ret, SWT.NONE);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setFilterExtensions(new String[] { "*.xls" }); //$NON-NLS-1$
				String selected = fd.open();
				tFilePath.setText(selected);
			}
		});
		btnBrowse.setText("auswählen..");

		reportLink = new Link(ret, SWT.NONE);
		reportLink.setText(StringUtils.EMPTY);
		reportLink.setVisible(false);
		reportLink.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true, 3, 1));
		// Event handling when users click on links.
		reportLink.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
				fd.setFilterExtensions(new String[] { "*.csv" }); //$NON-NLS-1$
				fd.setFileName("report.csv"); //$NON-NLS-1$
				String path = fd.open();
				if (path != null) {
					try {
						FileUtils.writeStringToFile(new File(path), reportBuilder.toString(), "UTF-8"); //$NON-NLS-1$
					} catch (IOException e1) {
						MessageDialog.openError(getShell(), "Report Error",
								"Report konnte nicht gespeichert werden.\n\n" + e1.getMessage());
						LoggerFactory.getLogger(ImportArticleDialog.class).error("report save error", e1); //$NON-NLS-1$
					}
				}
			}

		});

		return ret;
	}

	@Override
	public void create() {
		super.create();
		setTitle("Artikel Importieren"); //$NON-NLS-1$
		setMessage("Bitte wählen Sie die Quelle aus, aus dem Sie die Artikel importieren möchten."); //$NON-NLS-1$
		getShell().setText("Artikel Import"); //$NON-NLS-1$
		getShell().setImage(Images.IMG_PILL.getImage());
	}

	@Override
	protected void okPressed() {
		reportLink.setVisible(false);
		reportBuilder = new StringBuffer();
		doImport();
		if (reportBuilder.length() > 0) {
			reportLink
					.setText(" <a href=\"\">Import Report vom " + new TimeTool().toString(TimeTool.FULL_GER) + "</a>");
			reportLink.setVisible(true);
		}
	}

	private void doImport() {

		StringBuffer buf = new StringBuffer();

		// check for store availability

		// check for stock availability
		StructuredSelection iSelection = (StructuredSelection) comboStockType.getSelection();
		if (iSelection.isEmpty()) {
			buf.append("Bitte wählen Sie ein Lager aus.");
		} else {
			final IStock stock = (IStock) iSelection.getFirstElement();

			// check src file
			String path = tFilePath.getText();
			if (path != null && !path.isEmpty() && path.toLowerCase().endsWith("xls")) { //$NON-NLS-1$

				try (FileInputStream is = new FileInputStream(tFilePath.getText())) {
					ExcelWrapper xl = new ExcelWrapper();
					if (xl.load(is, 0)) {
						xl.setFieldTypes(new Class[] { Integer.class, String.class, String.class, String.class,
								String.class, String.class, Integer.class, String.class, String.class, String.class });
						MessageDialog dialog = new MessageDialog(getShell(), "Datenimport", null,
								"Wie sollen die Datenbestände importiert werden ?", MessageDialog.QUESTION, 0,
								"Datenbestand 'exakt' importieren", "Datenbestand 'aufaddieren'");
						int ret = dialog.open();
						if (ret >= 0) {
							runImport(buf, stock, xl, ret == 0);
						}
						return;
					}
				} catch (IOException e) {
					MessageDialog.openError(getShell(), "Import error",
							"Import fehlgeschlagen.\nDatei nicht importierbar: " + path);
					LoggerFactory.getLogger(ImportArticleDialog.class).error("cannot import file at " + path, e); //$NON-NLS-1$
				}
			} else {
				buf.append("Die Quelldatei ist ungültig. Bitte überprüfen Sie diese Datei.\n" + path);
			}
		}

		if (buf.length() > 0) {
			MessageDialog.openInformation(getShell(), "Import Ergebnis", buf.toString());
		} else {
			MessageDialog.openWarning(getShell(), "Import Ergebnis",
					"Import nicht möglich.\nÜberprüfen Sie das Log-File.");
		}
	}

	private void runImport(StringBuffer buf, final IStock stock, ExcelWrapper xl, boolean overrideStockEntries) {
		ProgressMonitorDialog progress = new ProgressMonitorDialog(getShell());
		try {
			progress.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {

					int importCount = 0;
					int articleNotFoundByGtin = 0;
					int articleNotFoundInStock = 0;
					int unexpectedErrors = 0;
					int lastRow = xl.getLastRow();
					int firstRow = xl.getFirstRow() + 1; // header offset
					monitor.beginTask("Artikel in Lager Import", 100);

					for (int i = firstRow; i <= lastRow; i++) {
						Optional<? extends IArticle> opArticle = Optional.empty();
						List<String> row = xl.getRow(i);
						String stockCount = row.get(0);
						String articleName = row.get(1);
						String gtin = row.get(6);
						String stockMin = null;
						String stockMax = null;
						if (row.size() > 8) {
							stockMin = row.get(8);
						}
						if (row.size() > 9) {
							stockMax = row.get(9);
						}

						// search for article
						opArticle = findArticleByGtin(gtin);

						if (opArticle.isPresent()) {
							String articleStoreToString = StoreToStringServiceHolder.get()
									.storeToString(opArticle.get()).get();
							// check if article is present in stock
							IStockEntry stockEntry = StockServiceHolder.get().findStockEntryForArticleInStock(stock,

									articleStoreToString);

							String result = "MODIFY"; //$NON-NLS-1$
							if (stockEntry == null) {
								stockEntry = StockServiceHolder.get().storeArticleInStock(stock, articleStoreToString);
								result = "ADDITION"; //$NON-NLS-1$
							}

							if (stockEntry instanceof IStockEntry) {
								if (LocalLockServiceHolder.get().acquireLock(stockEntry).isOk()) {
									// do import
									stockEntry.setCurrentStock(overrideStockEntries
											? StringTool.parseSafeInt(stockCount)
											: (StringTool.parseSafeInt(stockCount) + stockEntry.getCurrentStock()));
									if (stockMin != null) {
										stockEntry.setMinimumStock(StringTool.parseSafeInt(stockMin));
									}
									if (stockMax != null) {
										stockEntry.setMaximumStock(StringTool.parseSafeInt(stockMax));
									}
									importCount++;
									addToReport("OK " + result + " '" + stock.getLabel() + "'", articleName, gtin); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									CoreModelServiceHolder.get().save(stockEntry);
									LocalLockServiceHolder.get().releaseLock(stockEntry);
								} else {
									addToReport("NO LOCK", articleName, gtin); //$NON-NLS-1$
									unexpectedErrors++;
								}
							} else {
								addToReport("Not in Stock '" + stock.getLabel() + "'", articleName, gtin); //$NON-NLS-1$ //$NON-NLS-2$
								articleNotFoundInStock++;
							}
						} else {
							articleNotFoundByGtin++;
							addToReport("Not found by GTIN", articleName, gtin); //$NON-NLS-1$
						}

						monitor.worked(1);
						if (monitor.isCanceled()) {
							buf.append("Der Import wurde durch den Benutzer abgebrochen.");
							break;
						}
					}

					buf.append(lastRow);
					buf.append(" Artikel gelesen.");
					buf.append(StringUtils.LF);
					buf.append(StringUtils.LF);
					buf.append(importCount);
					buf.append(" Artikel erfolgreich nach Lager '");
					buf.append(stock.getLabel());
					buf.append("' importiert.");
					buf.append(StringUtils.LF);
					if (articleNotFoundInStock > 0) {
						buf.append(StringUtils.LF);
						buf.append(articleNotFoundInStock);
						buf.append(" Artikel nicht im Lager '");
						buf.append(stock.getLabel());
						buf.append("' vorhanden.");
					}

					if (articleNotFoundByGtin > 0) {
						buf.append(StringUtils.LF);
						buf.append(articleNotFoundByGtin);
						buf.append(" Artikel nicht in der Datenbank gefunden.");
					}
					if (unexpectedErrors > 0) {
						buf.append(StringUtils.LF);
						buf.append(unexpectedErrors);
						buf.append(" Artikel konnten nicht verarbeitet werden.");
					}
				}

			});
		} catch (InvocationTargetException | InterruptedException e) {
			LoggerFactory.getLogger(ImportArticleDialog.class).warn("Exception during article to lager import.", e); //$NON-NLS-1$
		}
	}

	private void addToReport(String col1, String col2, String col3) {
		if (reportBuilder != null) {
			reportBuilder.append(col1);
			reportBuilder.append(";"); //$NON-NLS-1$
			reportBuilder.append(col2);
			reportBuilder.append(";"); //$NON-NLS-1$
			reportBuilder.append(col3);
			reportBuilder.append(StringUtils.LF);
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		Button okBtn = super.getButton(IDialogConstants.OK_ID);
		if (okBtn != null) {
			okBtn.setText("Import");
		}

		Button closeBtn = super.getButton(IDialogConstants.CANCEL_ID);
		if (closeBtn != null) {
			closeBtn.setText("Schließen");
		}
	}

	private Optional<IArticle> findArticleByGtin(String scanCode) {
		List<ICodeElementServiceContribution> articleContributions = CodeElementServiceHolder.get()
				.getContributionsByTyp(CodeElementTyp.ARTICLE);
		for (ICodeElementServiceContribution contribution : articleContributions) {
			Optional<ICodeElement> loadFromCode = contribution.loadFromCode(scanCode);
			if (loadFromCode.isPresent()) {
				if (loadFromCode.get() instanceof IArticle) {
					return loadFromCode.map(IArticle.class::cast);
				} else {
					LoggerFactory.getLogger(getClass()).warn(
							"Found article for gtin [{}] but is not castable to IArticle [{}]", scanCode, //$NON-NLS-1$
							loadFromCode.get().getClass().getName());
				}
			}
		}
		return Optional.empty();
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
}
