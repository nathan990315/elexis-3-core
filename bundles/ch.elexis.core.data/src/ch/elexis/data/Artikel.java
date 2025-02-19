/*******************************************************************************
 * Copyright (c) 2005-2016, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 	  MEDEVIT - major refactorings due to multi-stock implementation
 *******************************************************************************/
package ch.elexis.data;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.interfaces.IArticle;
import ch.elexis.core.data.interfaces.IFall;
import ch.elexis.core.data.service.CoreModelServiceHolder;
import ch.rgw.tools.Money;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class Artikel extends VerrechenbarAdapter implements IArticle {

	public static final String TABLENAME = "ARTIKEL";

	public static final String FLD_EAN = "EAN";
	public static final String FLD_CODECLASS = "Codeclass";
	public static final String FLD_KLASSE = "Klasse";
	public static final String XID_PHARMACODE = "www.xid.ch/id/pharmacode/ch";
	public static final String FLD_SUB_ID = "SubID";
	public static final String ARTIKEL = "Artikel";
	public static final String FLD_PHARMACODE = "Pharmacode";
	public static final String FLD_EXTID = "ExtID";
	public static final String VERKAUFSEINHEIT = "Verkaufseinheit";
	public static final String VERPACKUNGSEINHEIT = "Verpackungseinheit";
	public static final String FLD_VK_PREIS = "VK_Preis";
	public static final String FLD_EK_PREIS = "EK_Preis";
	public static final String EIGENNAME = "Eigenname";
	public static final String FLD_TYP = "Typ";
	public static final String FLD_NAME = "Name";
	public static final String FLD_ATC_CODE = "ATC_code";

	public static final Pattern NAME_VE_PATTERN = Pattern.compile(".+ ([0-9]+) Stk.*");

	static {
		addMapping(TABLENAME, FLD_NAME, FLD_EK_PREIS, FLD_VK_PREIS, FLD_TYP, FLD_EXTINFO, FLD_EAN, FLD_SUB_ID,
				EIGENNAME + "=Name_intern", FLD_CODECLASS, FLD_KLASSE, FLD_ATC_CODE, FLD_EXTID);
		Xid.localRegisterXIDDomainIfNotExists(XID_PHARMACODE, FLD_PHARMACODE, Xid.ASSIGNMENT_REGIONAL);
	}

	@Override
	protected String getTableName() {
		return TABLENAME;
	}

	public String getXidDomain() {
		return XID_PHARMACODE;
	}

	/**
	 * This implementation of PersistentObject#load is special in that it tries to
	 * load the actual appropriate subclass
	 */
	public static Artikel load(final String id) {
		if (id == null) {
			return null;
		}
		Artikel ret = new Artikel(id);
		if (!ret.exists()) {
			return ret;
		}
		String clazz = ret.get(FLD_KLASSE);
		if (!StringTool.isNothing(clazz)) {
			try {
				ret = (Artikel) CoreHub.poFactory.createFromString(clazz + "::" + id);
			} catch (Exception ex) {
				log.error("Fehlerhafter Leistungscode " + clazz + "::" + id);
			}
		}
		return ret;
	}

	/**
	 * Einen neuen Artikel mit vorgegebenen Parametern erstellen
	 *
	 * @param Name
	 * @param Typ
	 */
	public Artikel(final String Name, final String Typ) {
		create(null);
		set(new String[] { FLD_NAME, FLD_TYP }, new String[] { Name, Typ });
	}

	public Artikel(final String Name, final String Typ, final String subid) {
		create(null);
		set(new String[] { FLD_NAME, FLD_TYP, FLD_SUB_ID }, Name, Typ, subid);
	}

	@Override
	public String getLabel() {
		if (!exists()) {
			return "(" + getName() + ")";
		}
		return getInternalName();
	}

	public String[] getDisplayedFields() {
		return new String[] { FLD_TYP, FLD_NAME };
	}

	/**
	 * Den internen Namen setzen. Dieser ist vom Anwender frei wählbar und erscheint
	 * in der Artikelauswahl und auf der Rechnung.
	 *
	 * @param nick Der "Spitzname"
	 */
	public void setInternalName(final String nick) {
		set(EIGENNAME, nick);
	}

	/**
	 * Den internen Namen holen
	 *
	 * @return
	 */
	public String getInternalName() {
		String ret = get(EIGENNAME);
		if (StringTool.isNothing(ret)) {
			ret = getName();
		}
		return ret;
	}

	/**
	 * Den offiziellen namen holen
	 *
	 * @return
	 */
	public String getName() {
		return checkNull(get(FLD_NAME));
	}

	/**
	 * Den "echten" Namen setzen. Dies ist der offizielle Name des Artikels, wie er
	 * beispielsweise in Katalogen aufgeführt ist. Dieser sollte normalerweise nicht
	 * geändert werden.
	 *
	 * @param name der neue "echte" Name
	 */
	public void setName(final String name) {
		set(FLD_NAME, name);
	}

	/**
	 * Basis-Einkaufspreis in Rappen pro Einheit
	 *
	 * @return
	 */
	public Money getEKPreis() {
		try {
			return new Money(checkZero(get(FLD_EK_PREIS)));
		} catch (Throwable ex) {
			log.error("Fehler beim Einlesen von EK für " + getLabel());
		}
		return new Money();

	}

	/**
	 * Basis-Verkaufspreis in Rappen pro Einheit
	 *
	 * @return
	 */
	public Money getVKPreis() {
		try {
			return new Money(checkZero(get(FLD_VK_PREIS)));
		} catch (Throwable ex) {
			log.error("Fehler beim Einlesen von VK für " + getLabel());
		}
		return new Money();

	}

	/**
	 * Einkaufspreis setzen. Das sollte normalerweise nur der Importer tun
	 *
	 * @param preis
	 */
	public void setEKPreis(final Money preis) {
		set(FLD_EK_PREIS, preis.getCentsAsString());
	}

	/**
	 * Den Verkaufspreis setzen. Das sollte bei gesetztlich festgelegten Artikeln
	 * nur der Importer tun.
	 *
	 * @param preis
	 */
	public void setVKPreis(final Money preis) {
		set(FLD_VK_PREIS, preis.getCentsAsString());
	}

	/**
	 * Versuche, die Verpakcungseinheit herauszufinden. Entweder haben wir sie im
	 * Artikeldetail angegeben, dann ist es trivial, oder vielleicht steht im Namen
	 * etwas wie xx Stk.
	 *
	 * @return einen educated guess oder 0 (unknown)
	 */
	public int guessVE() {
		int ret = getVerpackungsEinheit();
		if (ret == 0) {
			String name = getName();
			Matcher matcher = NAME_VE_PATTERN.matcher(name);
			if (matcher.matches()) {
				String num = matcher.group(1);
				try {
					return Integer.parseInt(num);
				} catch (Exception ex) {

				}
			}
		}
		return ret;
	}

	public String getEAN() {
		return get(FLD_EAN);
	}

	@Override
	public String getGTIN() {
		return getEAN();
	}

	public void setEAN(String ean) {
		set(FLD_EAN, ean);
	}

	public boolean isVaccination() {
		String atcCode = getATC_code();
		if (atcCode != null) {
			if (atcCode.toUpperCase().startsWith("J07") && !atcCode.toUpperCase().startsWith("J07AX")) {
				return true;
			}
		}
		return false;
	}

	public String getATC_code() {
		String ATC_code = get(FLD_ATC_CODE);
		return ATC_code;
	}

	public void setATC_code(String ATC_code) {
		set(FLD_ATC_CODE, ATC_code);
	}

	public void setPharmaCode(String pharmacode) {
		Map<Object, Object> ext = getMap(FLD_EXTINFO);
		ext.put(FLD_PHARMACODE, pharmacode);
		setMap(FLD_EXTINFO, ext);
	}

	public String getPharmaCode() {
		return checkNull(getExt(FLD_PHARMACODE));
	}

	public int getVerpackungsEinheit() {
		return checkZero((String) getExtInfoStoredObjectByKey(VERPACKUNGSEINHEIT));
	}

	@Override
	public int getPackageUnit() {
		return getVerpackungsEinheit();
	}

	public void setVerpackungsEinheit(int ve) {
		setExt(VERPACKUNGSEINHEIT, Integer.toString(ve));
	}

	public int getVerkaufseinheit() {
		return checkZero(getExt(VERKAUFSEINHEIT));
	}

	@Override
	public int getSellingUnit() {
		return getVerkaufseinheit();
	}

	public void setVerkaufseinheit(int number) {
		setExt(VERKAUFSEINHEIT, Integer.toString(number));
	}

	/**
	 * @return the package size, zero if not defined
	 */
	public int getPackungsGroesse() {
		return checkZero(getExt(VERPACKUNGSEINHEIT));
	}

	public void setPackungsGroesse(int packageSize) {
		setExtInfoStoredObjectByKey(VERPACKUNGSEINHEIT, Integer.toString(packageSize));
	}

	public String getPackungsGroesseDesc() {
		return Integer.toString(getPackungsGroesse());
	}

	public int getAbgabeEinheit() {
		return checkZero(getExt(VERKAUFSEINHEIT));
	}

	@SuppressWarnings("unchecked")
	public void setExt(final String name, final String value) {
		Map h = getMap(FLD_EXTINFO);
		if (value == null) {
			h.remove(name);
		} else {
			h.put(name, value);
		}
		setMap(FLD_EXTINFO, h);
	}

	@SuppressWarnings("unchecked")
	public String getExt(final String name) {
		Map h = getMap(FLD_EXTINFO);
		return checkNull((String) h.get(name));
	}

	protected Artikel(final String id) {
		super(id);
	}

	protected Artikel() {
	}

	/************************ Verrechenbar ************************/
	@Override
	public String getCode() {
		return getId();
	}

	@Override
	public String getText() {
		return getInternalName();
	}

	@Override
	public String getCodeSystemName() {
		return ARTIKEL;
	}

	@Override
	public boolean isProduct() {
		return false;
	}

	@SuppressWarnings("unchecked")
	public int getPreis(final TimeTool dat, final IFall fall) {
		double vkt = checkZeroDouble(get(FLD_VK_PREIS));
		Map ext = getMap(FLD_EXTINFO);
		double vpe = checkZeroDouble((String) ext.get(VERPACKUNGSEINHEIT));
		double vke = checkZeroDouble((String) ext.get(VERKAUFSEINHEIT));
		if ((vpe > 0.0 && vke > 0.0) && (vpe != vke)) {
			return (int) Math.round(vke * (vkt / vpe));
		} else {
			return (int) Math.round(vkt);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Money getKosten(final TimeTool dat) {
		double vkt = checkZeroDouble(get(FLD_EK_PREIS));
		Map ext = getMap(FLD_EXTINFO);
		double vpe = checkZeroDouble((String) ext.get(VERPACKUNGSEINHEIT));
		double vke = checkZeroDouble((String) ext.get(VERKAUFSEINHEIT));
		if ((vpe > 0.0 && vke > 0.0) && (vpe != vke)) {
			return new Money((int) Math.round(vke * (vkt / vpe)));
		} else {
			return new Money((int) Math.round(vkt));
		}
	}

	public int getTP(final TimeTool date, final IFall fall) {
		return getPreis(date, fall);
	}

	public double getFactor(final TimeTool date, final IFall fall) {
		return 1.0;
	}

	@Override
	protected String[] getExportFields() {
		return new String[] { FLD_EAN, FLD_SUB_ID, FLD_KLASSE, FLD_NAME, FLD_EK_PREIS, FLD_VK_PREIS, FLD_TYP,
				FLD_CODECLASS, FLD_ATC_CODE, FLD_EXTINFO };
	}

	@Override
	protected String getExportUIDValue() {
		String pharmacode = getExt(FLD_PHARMACODE);
		String ean = get(FLD_EAN);
		return ean + "_" + pharmacode;
	}

	/**
	 * Convenience conversion method, loads object via model service
	 *
	 * @return
	 * @since 3.8
	 * @throws IllegalStateException if entity could not be loaded
	 */
	public ch.elexis.core.model.IArticle toIArticle() {
		return CoreModelServiceHolder.get().load(getId(), ch.elexis.core.model.IArticle.class)
				.orElseThrow(() -> new IllegalStateException("Could not convert article [" + getId() + "]"));
	}
}
