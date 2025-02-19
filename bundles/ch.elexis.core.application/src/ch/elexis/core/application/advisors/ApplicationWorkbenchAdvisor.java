/*******************************************************************************
 * Copyright (c) 2005-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    N. Giger - Bypass Login Dialog for development environments
 *
 *******************************************************************************/

package ch.elexis.core.application.advisors;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.data.extension.CoreOperationAdvisorHolder;
import ch.elexis.core.ui.Hub;
import ch.elexis.core.ui.UiDesk;
import ch.elexis.core.ui.actions.GlobalActions;
import ch.elexis.core.ui.constants.UiResourceConstants;
import ch.rgw.tools.ExHandler;

/**
 * Dies ist eine Eclipse-spezifische Klasse Wichtigste Funktion ist das
 * Festlegen der initialen Perspektive In eventloopException können spezifische
 * Verarbeitungen für nicht abgefangene Exceptions definiert werden (Hier
 * einfach Ausgabe). In eventLoopIdle können Arbeiten eingetragen werden, die
 * immer dann zu eredigen sind, wenn das Programm nichts weiter zu tun hat.
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private Logger log = LoggerFactory.getLogger(ApplicationWorkbenchAdvisor.class.getName());

	@Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(final IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @seeorg.eclipse.ui.application.WorkbenchAdvisor#initialize(org.eclipse.ui.
	 * application. IWorkbenchConfigurer)
	 */
	@Override
	public void initialize(final IWorkbenchConfigurer configurer) {
		Hub.pin.initializeDisplayPreferences(UiDesk.getDisplay());
		configurer.setSaveAndRestore(true);
		super.initialize(configurer);
	}

	@Override
	public IAdaptable getDefaultPageInput() {
		return super.getDefaultPageInput();
	}

	@Override
	public String getInitialWindowPerspectiveId() {
		String initPerspective = CoreOperationAdvisorHolder.get().getInitialPerspective();

		// avoid that nothing opens up after login in case the stored perspective can't
		// be found
		IPerspectiveRegistry perspectiveRegistry = PlatformUI.getWorkbench().getPerspectiveRegistry();
		if (perspectiveRegistry.findPerspectiveWithId(initPerspective) == null) {
			initPerspective = UiResourceConstants.PatientPerspektive_ID;
		}

		return initPerspective;
	}

	@Override
	public void preStartup() {
		super.preStartup();
		ElexisEventDispatcher.getInstance().start();
	}

	@Override
	public void eventLoopException(final Throwable exception) {
		log.error(Messages.ApplicationWorkbenchAdvisor_10 + exception.getMessage(), exception);
		ExHandler.handle(exception);
		super.eventLoopException(exception);
	}

	@Override
	public boolean preShutdown() {
		if (GlobalActions.fixLayoutAction != null) {
			GlobalActions.fixLayoutAction.setChecked(false);
		}
		return super.preShutdown();
	}

	@Override
	public void postShutdown() {
		Hub.postShutdown();
		super.postShutdown();
	}
}
