package com.jera.vnc;

import java.io.DataInput;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: jbrewer
 * Date: Sep 11, 2003
 * Time: 2:14:39 AM
 * To change this template use Options | File Templates.
 */
public class PixelFormat {
	private int bitsPerPixel;
	private int depth;
	private boolean bigEndianFlag;
	private boolean trueColorFlag;
	private int redMax;
	private int greenMax;
	private int blueMax;
	private int redShift;
	private int greenShift;
	private int blueShift;

	public PixelFormat(DataInput input) throws IOException {
		bitsPerPixel = input.readByte() & 0xff;
		depth = input.readByte() & 0xff;
		bigEndianFlag = input.readBoolean();
		trueColorFlag = input.readBoolean();
		redMax = input.readShort() & 0xffff;
		greenMax = input.readShort() & 0xffff;
		blueMax = input.readShort() & 0xffff;
		redShift = input.readByte() & 0xff;
		greenShift = input.readByte() & 0xff;
		blueShift = input.readByte() & 0xff;
		skipPaddingBytes(input);
	}

	private void skipPaddingBytes(DataInput input) throws IOException {
		input.readByte();
		input.readByte();
		input.readByte();
	}

	public int getBitsPerPixel() {
		return bitsPerPixel;
	}

	public void setBitsPerPixel(int theBitsPerPixel) {
		bitsPerPixel = theBitsPerPixel;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int theDepth) {
		depth = theDepth;
	}

	public boolean isBigEndianFlag() {
		return bigEndianFlag;
	}

	public void setBigEndianFlag(boolean theBigEndianFlag) {
		bigEndianFlag = theBigEndianFlag;
	}

	public boolean isTrueColorFlag() {
		return trueColorFlag;
	}

	public void setTrueColorFlag(boolean theTrueColorFlag) {
		trueColorFlag = theTrueColorFlag;
	}

	public int getRedMax() {
		return redMax;
	}

	public void setRedMax(int theRedMax) {
		redMax = theRedMax;
	}

	public int getGreenMax() {
		return greenMax;
	}

	public void setGreenMax(int theGreenMax) {
		greenMax = theGreenMax;
	}

	public int getBlueMax() {
		return blueMax;
	}

	public void setBlueMax(int theBlueMax) {
		blueMax = theBlueMax;
	}

	public int getRedShift() {
		return redShift;
	}

	public void setRedShift(int theRedShift) {
		redShift = theRedShift;
	}

	public int getGreenShift() {
		return greenShift;
	}

	public void setGreenShift(int theGreenShift) {
		greenShift = theGreenShift;
	}

	public int getBlueShift() {
		return blueShift;
	}

	public void setBlueShift(int theBlueShift) {
		blueShift = theBlueShift;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("PixelFormat { ");
		buffer.append("bitsPerPixel = " + bitsPerPixel + ", ");
		buffer.append("depth = " + depth + ", ");
		buffer.append("bigEndianFlag = " + bigEndianFlag + ", ");
		buffer.append("trueColorFlag = " + trueColorFlag + ", ");
		buffer.append("redMax = " + redMax + ", ");
		buffer.append("greenMax = " + greenMax + ", ");
		buffer.append("blueMax = " + blueMax + ", ");
		buffer.append("redShift = " + redShift + ", ");
		buffer.append("greenShift = " + greenShift + ", ");
		buffer.append("blueShift = " + blueShift + " }");
		return buffer.toString();
	}
}
