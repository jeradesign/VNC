package com.jera.vnc;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.security.Key;
import java.security.GeneralSecurityException;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.DataBufferUShort;
import java.awt.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

import EDU.oswego.cs.dl.util.concurrent.Semaphore;

/**
 * Created by IntelliJ IDEA.
 * User: jbrewer
 * Date: Sep 10, 2003
 * Time: 8:34:39 PM
 * To change this template use Options | File Templates.
 */
public class VNC {
	public static final int VNC_PORT = 5900;
	public static final int PROTOCOL_VERSION_LENGTH = 12;
	public static final int SECURITY_CHALLENGE_LENGTH = 16;
	public static final int DES_KEY_SIZE = 8;
	public static final int sharedFlag = 0;

	private String serverAddress;
	private String password;

	private DataInputStream input;
	private DataOutputStream output;
	private String protocolVersion;
	private int securityType;
	private int authenticationResponse;
	private int frameBufferWidth;
	private int frameBufferHeight;
	private PixelFormat pixelFormat;
	private String serverName;
	private BufferedImage image;

	private Semaphore semaphore = new Semaphore(1);
	private VNCObserver observer;
	private boolean connected;

	public static void main(String[] args) throws IOException, GeneralSecurityException {
		String serverName = args[0];
		String password = args[1];

		final VNC vnc = new VNC(serverName, password);

		final JFrame frame = new JFrame("Test");
		VNCPanel panel = new VNCPanel(vnc);

		vnc.setVNCObserver(panel);
		vnc.connect();
		System.out.println(vnc);
		vnc.startThreads();

		frame.getContentPane().add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.show();

//		VNCTester.showScriptFrame(vnc);
	}

	public VNC(String theServerName, String thePassword) {
		serverAddress = theServerName;
		password = thePassword;
	}

	public synchronized void connect() throws IOException, GeneralSecurityException {
		Socket socket = new Socket(serverAddress, VNC_PORT);
		input = new DataInputStream(socket.getInputStream());
		output = new DataOutputStream(socket.getOutputStream());

		doProtocolVersion();
		doSecurity();
		doClientInitialization();
		doServerInitialization();
		observer.setScreenSize(frameBufferWidth, frameBufferHeight);
		sendSetPixelFormat();
		sendSetEncodings();
//		output.writeByte(0xde);
//		output.writeByte(0xad);
//		output.writeByte(0xbe);
//		output.writeByte(0xef);
		connected = true;   // TODO: deal with ordering problem
		sendFramebufferUpdateRequest(false);
//		output.writeByte(0xde);
//		output.writeByte(0xad);
//		output.writeByte(0xbe);
//		output.writeByte(0xef);
	}

	private void sendSetPixelFormat() throws IOException {
		output.writeByte(0);
		output.writeByte(0);
		output.writeByte(0);
		output.writeByte(0);
		pixelFormat.writeTo(output);
	}

	private void sendSetEncodings() throws IOException {
		output.writeByte(2);
		output.writeByte(0);
		output.writeShort(1);
		output.writeInt(0);
	}

	private void doProtocolVersion() throws IOException {
		byte[] protocolVersionBytes = new byte[PROTOCOL_VERSION_LENGTH];
		input.readFully(protocolVersionBytes);
		output.write(protocolVersionBytes);

		protocolVersion = new String(protocolVersionBytes, 0, protocolVersionBytes.length -1, "UTF8");
	}

	private void doSecurity() throws IOException, GeneralSecurityException {
		int count = input.readByte() & 0xff;
		int[] securityTypes = new int[count];

		securityType = 0;
		for (int i = 0; i < count; i++) {
			securityTypes[i] = input.readByte() & 0xff;
			if (securityTypes[i] > securityType && securityTypes[i] <= 2) {
				securityType = securityTypes[i];
			}
		}
		output.writeByte(securityType);
		if (securityType == 0) {
			throw new IOException(readString());
		}
		else if (securityType == 1) {
			return;
		}

		byte[] challenge = new byte[SECURITY_CHALLENGE_LENGTH];
		input.readFully(challenge);

		byte[] response = authenticate(password, challenge);
		output.write(response);

		authenticationResponse = input.readInt();
		if (authenticationResponse != 0) {
			throw new IOException("Couldn't authenticate with VNC server (response = " + readString() + ").");
		}
	}

	private void doClientInitialization() throws IOException {
		output.writeByte(sharedFlag);
	}

	private void doServerInitialization() throws IOException {
		frameBufferWidth = input.readShort() & 0xffff;
		frameBufferHeight = input.readShort() & 0xffff;
		pixelFormat = new PixelFormat(input);
		serverName = readString();
	}

	/**
	 * Create a response for a VNC challenge.
	 * See: http://www.vidarholen.net/contents/junk/vnc.html
	 */
	public static byte[] authenticate(String thePassword, byte[] theChallenge)
	        throws GeneralSecurityException, UnsupportedEncodingException
	{
		Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
		byte[] keyBytes = getKeyBytes(thePassword);
		Key key = new SecretKeySpec(keyBytes, "DES");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] response = cipher.doFinal(theChallenge);
		return response;
	}

	public static byte[] getKeyBytes(String thePassword) throws UnsupportedEncodingException {
		byte[] keyBytes = new byte[8];
		byte[] passwordBytes = thePassword.getBytes("UTF8");
		System.arraycopy(passwordBytes, 0, keyBytes, 0, passwordBytes.length);

		for (int i = 0; i < 8; i++) {
			keyBytes[i] = (byte) reverseBits[keyBytes[i] & 0xff];
		}

		return keyBytes;
	}

	private void startThreads() {
		Thread receive = new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						receiveMessage();
						if (semaphore.permits() == 0) {
							semaphore.release();
						}
					}
					catch (EOFException e) {
						disconnected();
						return;
					}
					catch (IOException e) {
						e.printStackTrace();  //To change body of catch statement use Options | File Templates.
					}
				}
			}
		});
		receive.start();

		Thread send = new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						sendFramebufferUpdateRequest(true);
						Thread.sleep(30000);
					}
					catch (SocketException e) {
						disconnected();
						return;
					}
					catch (Exception e) {
						e.printStackTrace();  //To change body of catch statement use Options | File Templates.
					}
				}
			}
		});
		send.start();
	}

	private void disconnected() {
		System.out.println("Disconnected");
		Toolkit.getDefaultToolkit().beep();
		connected = false;
		return;
	}

	public synchronized void sendFramebufferUpdateRequest(boolean incremental) throws IOException {
		if (!connected) {
			return;
		}
		log("requesting upate");
		output.writeByte(3);
		output.writeBoolean(incremental);
		output.writeShort(0);
		output.writeShort(0);
		output.writeShort(frameBufferWidth);
		output.writeShort(frameBufferHeight);
	}

	public synchronized void sendPointerEvent(int theButton, int theX, int theY) throws IOException {
		if (!connected) {
			return;
		}
		System.out.println("pointer event, button = " + theButton + ", x = " + theX + ", y = " + theY);
		output.writeByte(5);
		output.writeByte(theButton);
		output.writeShort(theX);
		output.writeShort(theY);
	}

	public synchronized void sendKeyEvent(boolean downFlag, int theCode) throws IOException {
		if (!connected) {
			return;
		}
		System.out.println("KeyEvent: " + downFlag + ", " + theCode);
		output.writeByte(4);
		output.writeBoolean(downFlag);
		output.writeShort(0);
		output.writeInt(theCode);
	}

	private void receiveMessage() throws IOException {
		int messageType = input.readByte();
		switch(messageType) {
			case 0:
				receiveFrameBufferUpdate();
				break;
			case 1:
				receiveSetColorMapEntries();
				break;
			case 2:
				receiveBell();
				break;
			case 3:
				receiveServerCutText();
				break;
			default:
				throw new IOException("Unknown messageType: " + messageType);
		}
	}

	private void receiveFrameBufferUpdate() throws IOException {
		input.readByte();
		int numberOfRectangles = input.readShort();
		for (int i = 0; i < numberOfRectangles; i++) {
			int xPosition = input.readShort() & 0xffff;
			int yPosition = input.readShort() & 0xffff;
			int width = input.readShort() & 0xffff;
			int height = input.readShort() & 0xffff;
			int encodingType = input.readInt();
			System.out.println("receiveFrameBufferUpdate { xPosition = " + xPosition + ", yPosition = " + yPosition
			                   + ", width = " + width + ", height = " + height + ", encodingType = " + encodingType
			                   + " }");
			if (encodingType != 0) {
				throw new IOException("Unknown encoding: " + encodingType);
			}
			int pixelByteBufferSize = width * height * pixelFormat.getBitsPerPixel()/8;
			byte[] pixelByteBuffer = new byte[pixelByteBufferSize];
			input.readFully(pixelByteBuffer);

			if (image == null) {
				image = new BufferedImage(frameBufferWidth, frameBufferHeight, BufferedImage.TYPE_USHORT_565_RGB);
				observer.setImage(image);
			}
			Raster raster = image.getRaster();
			DataBufferUShort buffer = (DataBufferUShort) raster.getDataBuffer();

			short[] pixelBuffer = buffer.getData();

			copy16BitRaster(yPosition, height, xPosition, width, pixelByteBuffer, pixelBuffer);
		}
		observer.imageChanged();
	}

	private void copy16BitRaster(int theYPosition, int theHeight, int theXPosition, int theWidth, byte[] thePixelByteBuffer, short[] thePixelBuffer) {
		int j=0;
		for (int y = theYPosition; y < theYPosition + theHeight; y++) {
			for (int x = theXPosition; x < theXPosition + theWidth; x++) {
				thePixelBuffer[y * frameBufferWidth + x] = (short)(((thePixelByteBuffer[2 * j + 1] & 0xff) << 8) | (thePixelByteBuffer[2 * j] & 0xff));
				j++;
			}
		}
	}

	// { bitsPerPixel = 8, depth = 8, bigEndianFlag = true, trueColorFlag = true,
	// redMax = 7, greenMax = 7, blueMax = 3, redShift = 0, greenShift = 3, blueShift = 6 }
	private void copy8BitRaster(int theYPosition, int theHeight, int theXPosition, int theWidth, byte[] thePixelByteBuffer, short[] thePixelBuffer) {
		int j=0;
		for (int y = theYPosition; y < theYPosition + theHeight; y++) {
			for (int x = theXPosition; x < theXPosition + theWidth; x++) {
				int pixel = thePixelByteBuffer[j] & 0xff;
				int red = ((pixel >> pixelFormat.getRedShift()) & pixelFormat.getRedMax() << 2);
				int green = ((pixel >> pixelFormat.getGreenShift()) & pixelFormat.getGreenMax() << 2);
				int blue = ((pixel >> pixelFormat.getBlueShift()) & pixelFormat.getBlueMax() << 3);
				int pixel16 = (red << 10) | (green << 5) | blue;
				thePixelBuffer[y * frameBufferWidth + x] = (short) pixel16;
				j++;
			}
		}
	}

	private void receiveSetColorMapEntries() {
		throw new RuntimeException("Not implemented");
	}

	private void receiveBell() {
		throw new RuntimeException("Not implemented");
	}

	private void receiveServerCutText() throws IOException {
		input.readByte();
		input.readByte();
		input.readByte();
		String cutText = readString();
		System.out.println(cutText);
	}

	public static final int[] reverseBits = {
		0x00, 0x80, 0x40, 0xc0, 0x20, 0xa0, 0x60, 0xe0, 0x10, 0x90, 0x50, 0xd0, 0x30, 0xb0, 0x70, 0xf0,
		0x08, 0x88, 0x48, 0xc8, 0x28, 0xa8, 0x68, 0xe8, 0x18, 0x98, 0x58, 0xd8, 0x38, 0xb8, 0x78, 0xf8,
		0x04, 0x84, 0x44, 0xc4, 0x24, 0xa4, 0x64, 0xe4, 0x14, 0x94, 0x54, 0xd4, 0x34, 0xb4, 0x74, 0xf4,
		0x0c, 0x8c, 0x4c, 0xcc, 0x2c, 0xac, 0x6c, 0xec, 0x1c, 0x9c, 0x5c, 0xdc, 0x3c, 0xbc, 0x7c, 0xfc,
		0x02, 0x82, 0x42, 0xc2, 0x22, 0xa2, 0x62, 0xe2, 0x12, 0x92, 0x52, 0xd2, 0x32, 0xb2, 0x72, 0xf2,
		0x0a, 0x8a, 0x4a, 0xca, 0x2a, 0xaa, 0x6a, 0xea, 0x1a, 0x9a, 0x5a, 0xda, 0x3a, 0xba, 0x7a, 0xfa,
		0x06, 0x86, 0x46, 0xc6, 0x26, 0xa6, 0x66, 0xe6, 0x16, 0x96, 0x56, 0xd6, 0x36, 0xb6, 0x76, 0xf6,
		0x0e, 0x8e, 0x4e, 0xce, 0x2e, 0xae, 0x6e, 0xee, 0x1e, 0x9e, 0x5e, 0xde, 0x3e, 0xbe, 0x7e, 0xfe,
		0x01, 0x81, 0x41, 0xc1, 0x21, 0xa1, 0x61, 0xe1, 0x11, 0x91, 0x51, 0xd1, 0x31, 0xb1, 0x71, 0xf1,
		0x09, 0x89, 0x49, 0xc9, 0x29, 0xa9, 0x69, 0xe9, 0x19, 0x99, 0x59, 0xd9, 0x39, 0xb9, 0x79, 0xf9,
		0x05, 0x85, 0x45, 0xc5, 0x25, 0xa5, 0x65, 0xe5, 0x15, 0x95, 0x55, 0xd5, 0x35, 0xb5, 0x75, 0xf5,
		0x0d, 0x8d, 0x4d, 0xcd, 0x2d, 0xad, 0x6d, 0xed, 0x1d, 0x9d, 0x5d, 0xdd, 0x3d, 0xbd, 0x7d, 0xfd,
		0x03, 0x83, 0x43, 0xc3, 0x23, 0xa3, 0x63, 0xe3, 0x13, 0x93, 0x53, 0xd3, 0x33, 0xb3, 0x73, 0xf3,
		0x0b, 0x8b, 0x4b, 0xcb, 0x2b, 0xab, 0x6b, 0xeb, 0x1b, 0x9b, 0x5b, 0xdb, 0x3b, 0xbb, 0x7b, 0xfb,
		0x07, 0x87, 0x47, 0xc7, 0x27, 0xa7, 0x67, 0xe7, 0x17, 0x97, 0x57, 0xd7, 0x37, 0xb7, 0x77, 0xf7,
		0x0f, 0x8f, 0x4f, 0xcf, 0x2f, 0xaf, 0x6f, 0xef, 0x1f, 0x9f, 0x5f, 0xdf, 0x3f, 0xbf, 0x7f, 0xff
	};

	public static final void generateReverseBits() {
		/*						 '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' */
		final char[] nybbles = { '0', '8', '4', 'c', '2', 'a', '6', 'e', '1', '9', '5', 'd', '3', 'b', '7', 'f' };

		System.out.println("public static final int[] reverseBits = {");
		for (int i = 0; i < 16; i++) {
			System.out.print("\t0x" + nybbles[0] + nybbles[i]);
			for (int j = 1; j < 16; j ++) {
				System.out.print(", 0x" + nybbles[j] + nybbles[i]);
			}
			System.out.println();
		}
	}

	private String readString() throws IOException {
		int stringLength = input.readInt();
		byte[] stringBytes = new byte[stringLength];
		input.readFully(stringBytes);
		return new String(stringBytes, "UTF8");
	}

	public Image getImage() {
		return image;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("VNC { ");
		buffer.append("protocolVersion = " + protocolVersion + ", ");
		buffer.append("securityType = " + securityType + ", ");
		buffer.append("authenticationResponse = " + authenticationResponse + ", ");
		buffer.append("frameBufferWidth = " + frameBufferWidth + ", ");
		buffer.append("frameBufferHeight = " + frameBufferHeight + ", ");
		buffer.append("pixelFormat = " + pixelFormat + ", ");
		buffer.append("serverName = " + serverName);
		return buffer.toString();
	}

	private void setVNCObserver(VNCObserver thePanel) {
		observer = thePanel;
	}

	public static void log(String message) {
		System.out.println(message);
		return;
	}

	/* Convenience methods for Jython scripting */

	public void clickAt(int x, int y) throws IOException {
		sendPointerEvent(0, x, y);
		sendPointerEvent(1, x, y);
		sendPointerEvent(0, x, y);
	}

	public void type(String text) throws IOException {
		for (int i = 0; i < text.length(); i++) {
			sendKeyEvent(false, text.charAt(i));
			sendKeyEvent(true, text.charAt(i));
			sendKeyEvent(false, text.charAt(i));
		}
	}

	public void sleep(long milliseconds) throws InterruptedException {
		Thread.sleep(milliseconds);
	}
}
