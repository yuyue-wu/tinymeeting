package com.nsn.tinymeeting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class JMFInit {
	final static String dllPath = "D:\\wyy\\workspace\\com.nsn.tinymeeting\\bin\\com\\nsn\\tinymeeting\\";
//	final static String[] windowsDllList = new String[] { "jmacm.dll",
//			"jmam.dll", "jmcvid.dll", "jmdaud.dll", "jmdaudc.dll",
//			"jmddraw.dll", "jmfjawt.dll", "jmg723.dll", "jmgdi.dll",
//			"jmgsm.dll", "jmh261.dll", "jmh263enc.dll", "jmjpeg.dll",
//			"jmmci.dll", "jmmpa.dll", "jmmpegv.dll", "jmutil.dll", "jmvcm.dll",
//			"jmvfw.dll", "jmvh263.dll", "jsound.dll" };

	public static void init() {
//		for (int i = 0; i < windowsDllList.length; i++)
//			System.load(dllPath + windowsDllList[i]);

		File confFile = new File(dllPath + "jmf.properties");
		try {
			InputStream is = new FileInputStream(dllPath
					+ "jmf.properties.orig");
			byte[] buff = new byte[1024];
			FileOutputStream os = new FileOutputStream(confFile);
			while (is.read(buff) != -1) {
				os.write(buff);
			}
			os.flush();
			os.close();
			is.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
