package com.jera.vnc;

import java.awt.image.BufferedImage;

/**
 * Created by IntelliJ IDEA.
 * User: jbrewer
 * Date: Sep 12, 2003
 * Time: 8:04:25 PM
 * To change this template use Options | File Templates.
 */
public interface VNCObserver {
	void setImage(BufferedImage theImage);

	void imageChanged();

	void setScreenSize(int width, int height);
}
