package ch.elexis.core.model.verrechnet;

public class Constants {
	public static final String VAT_SCALE = "vat_scale";
	/** mark something (Verrechnet) as medical indicated, value true / false */
	public static final String FLD_EXT_INDICATED = "indicated";
	/** the prescription ID of this if it is an article */
	public static final String FLD_EXT_PRESC_ID = "prescriptionId";
	/** mark a Verrechnet that its price was changed , value true / false */
	public static final String FLD_EXT_CHANGEDPRICE = "changedPrice";

	public static final String FLD_EXT_PFLICHTLEISTUNG = "obligation";

	public static final String FLD_EXT_SIDE = "Seite";
	public static final String SIDE_L = "l";
	public static final String SIDE_R = "r";
	public static final String LEFT = "left";
	public static final String RIGHT = "right";
}
