package com.nsn.tinymeeting;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class Perspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(false);
		layout.setFixed(true);
//		layout.addStandaloneView(AllListPlayView.ID, false, IPageLayout.LEFT,
//				0.7f, layout.getEditorArea());
//		layout.addStandaloneView(SelfPlayView.ID, false, IPageLayout.RIGHT,
//				0.3f, layout.getEditorArea());
		
	}

}
