package com.nsn.tinymeeting;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.sun.media.util.Registry;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	

	// The plug-in ID
	public static final String PLUGIN_ID = "com.nsn.tinymeeting"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		// ugly hack. Allows JMF Registry find jmf.properties file
        final URL url = getClass().getClassLoader().getResource("jmf.properties");
        final URL nativeUrl = FileLocator.resolve(url);
        final String classpath = System.getProperty("java.class.path");
        String jmfCP = nativeUrl.getPath().substring(1);
        final int index = jmfCP.lastIndexOf("/");
        jmfCP = jmfCP.substring(0, index);
        System.setProperty("java.class.path", classpath + ";" + jmfCP);
        
        try {
            Registry.commit();
        } catch (final Exception e) {
            e.printStackTrace();
            // TODO log
        }
        
        System.setProperty("java.class.path", classpath);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
