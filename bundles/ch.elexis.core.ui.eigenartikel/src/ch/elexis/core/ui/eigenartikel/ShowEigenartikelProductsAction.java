package ch.elexis.core.ui.eigenartikel;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.action.Action;

import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.services.holder.ConfigServiceHolder;
import ch.elexis.core.ui.icons.Images;

public class ShowEigenartikelProductsAction extends Action {

	public static final String FILTER_CFG = "ShowEigenartikelProductsAction.showProducts"; //$NON-NLS-1$

	private EigenartikelTreeContentProvider eal;
	private EigenartikelSelector eigenartikelSelector;

	public ShowEigenartikelProductsAction(EigenartikelTreeContentProvider eal,
			EigenartikelSelector eigenartikelSelector) {
		super("Produkte anzeigen", Action.AS_CHECK_BOX);
		this.eal = eal;
		this.eigenartikelSelector = eigenartikelSelector;
		setImageDescriptor(Images.IMG_CARDS.getImageDescriptor());
		setToolTipText(StringUtils.EMPTY);
		setChecked(ConfigServiceHolder.getUser(ShowEigenartikelProductsAction.FILTER_CFG, false));
		execute();
	}

	@Override
	public void run() {
		ConfigServiceHolder.setUser(FILTER_CFG, isChecked());
		execute();
	}

	private void execute() {
		eal.setShowProducts(isChecked());
		eigenartikelSelector.allowArticleRearrangement(isChecked());
	}

}
