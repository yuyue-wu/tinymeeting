package com.nsn.tinymeeting;


import java.awt.Component;
import java.awt.Frame;

import java.io.IOException;

import javax.media.CannotRealizeException;
import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoPlayerException;
import javax.media.Player;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class SelfPlayView extends ViewPart {
	public static final String ID = "com.nsn.tinymeeting.selfplayview";

	private Frame frame;
	private Composite composite;


	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		composite= new Composite(parent ,SWT.EMBEDDED);  
		 frame=  SWT_AWT.new_Frame(composite);
		 start(frame);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		
		composite.setFocus();
		
	}
	public static void start(Frame frame) {

		String defaultDevice = "vfw:Microsoft WDM Image Capture (Win32):0";
		CaptureDeviceInfo di = null;
		MediaLocator ml = null;
		Player player = null;

		di = CaptureDeviceManager.getDevice(defaultDevice);
		ml = di.getLocator();

		try {
			player = Manager.createRealizedPlayer(ml);
		} catch (NoPlayerException e) {
			e.printStackTrace();
		} catch (CannotRealizeException e) {
			e.printStackTrace();
		} catch (IOException e) {

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (player != null)
			player.start();
		Component comp = null;
		if ((comp = player.getVisualComponent()) != null){
			
			frame.add(comp);
		}
		

		
	}
	
}