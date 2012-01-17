/*
 * This file is part of lzo-java, an implementation of LZO in Java.
 * https://github.com/Karmasphere/lzo-java
 *
 * The Java portion of this library is:
 * Copyright (C) 2011 Shevek <shevek@anarres.org>
 * All Rights Reserved.
 *
 * The preprocessed C portion of this library is:
 * Copyright (C) 2006-2011 Markus Franz Xaver Johannes Oberhumer
 * All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation; either version 
 * 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with the LZO library; see the file COPYING.
 * If not, see <http://www.gnu.org/licenses/> or write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.

 * As a special exception, the copyright holders of this file
 * give you permission to link this file with independent
 * modules to produce an executable, regardless of the license 
 * terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided
 * that you also meet, for each linked independent module, 
 * the terms and conditions of the license of that module. An
 * independent module is a module which is not derived from or 
 * based on this library or file. If you modify this file, you may 
 * extend this exception to your version of the file, but
 * you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version.
 */
package org.anarres.lzo;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author shevek
 */
public class LzoOutputStream extends OutputStream {

    private static final Log LOG = LogFactory.getLog(LzoOutputStream.class.getName());
    protected final OutputStream out;
    private final LzoCompressor compressor; // Replace with BlockCompressor.
    private final byte[] inputBuffer;
    private int inputBufferLen;
    private byte[] inputHoldoverBuffer;
    private int inputHoldoverBufferPos;
    private int inputHoldoverBufferLen;
    private final byte[] outputBuffer;
    private final lzo_uintp outputBufferLen = new lzo_uintp();	// Also, end, since we base outputBuffer at 0.

    /**
     * Creates a new compressor using the specified {@link LzoCompressor}.
     *
     * @param out the OutputStream to write to
     * @param compressor the compressor to use
     * @param inputBufferSize size of the input buffer to be used.
     */
    public LzoOutputStream(OutputStream out, LzoCompressor compressor, int inputBufferSize) {
        this.out = out;
        this.compressor = compressor;
        this.inputBuffer = new byte[inputBufferSize];
        this.outputBuffer = new byte[inputBufferSize + compressor.getCompressionOverhead(inputBufferSize)];
        reset();
    }

    /**
     * Creates a new compressor with the default lzo1x_1 compression.
     */
    public LzoOutputStream(OutputStream out) {
        this(out, LzoLibrary.getInstance().newCompressor(null, null), 64 * 1024);
    }

    public LzoCompressor getCompressor() {
        return compressor;
    }

    public LzoAlgorithm getAlgorithm() {
        return getCompressor().getAlgorithm();
    }

    public LzoConstraint[] getConstraints() {
        return getCompressor().getConstraints();
    }

    private void reset() {
        inputBufferLen = 0;
        inputHoldoverBuffer = null;
        inputHoldoverBufferPos = -1;
        inputHoldoverBufferLen = -1;
        outputBufferLen.value = 0;
    }

    private void logState(String when) {
        LOG.info("\n");
        LOG.info(when + " Input buffer length=" + inputBufferLen + "/" + inputBuffer.length);
        if (inputHoldoverBuffer == null) {
            LOG.info(when + " Input holdover = null");
        } else {
            LOG.info(when + " Input holdover pos=" + inputHoldoverBufferPos + "; length=" + inputHoldoverBufferLen);
        }
        LOG.info(when + " Output buffer length=" + outputBufferLen + "/" + outputBuffer.length);
        testInvariants();
    }

    private boolean testInvariants() {
        if (inputHoldoverBuffer != null) {
            if (inputBufferLen != 0 && inputBufferLen != inputBuffer.length)
                throw new IllegalStateException("Funny input buffer length " + inputBufferLen + " with array size " + inputBuffer.length + " and holdover.");
            if (inputHoldoverBufferPos < 0)
                throw new IllegalStateException("Using holdover buffer, but invalid holdover position " + inputHoldoverBufferPos);
            if (inputHoldoverBufferLen < 0)
                throw new IllegalStateException("Using holdover buffer, but invalid holdover length " + inputHoldoverBufferLen);
        } else {
            if (inputHoldoverBufferPos != -1)
                throw new IllegalStateException("No holdover buffer, but valid holdover position " + inputHoldoverBufferPos);
            if (inputHoldoverBufferLen != -1)
                throw new IllegalStateException("No holdover buffer, but valid holdover length " + inputHoldoverBufferLen);
        }

        if (outputBufferLen.value < 0)
            throw new IllegalStateException("Output buffer overrun length=" + outputBufferLen);

        return true;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        // logState("Before setInput");
        if (b == null)
            throw new NullPointerException();
        if (off < 0 || len < 0 || off > b.length - len)
            throw new ArrayIndexOutOfBoundsException("Illegal range in buffer: Buffer length=" + b.length + ", offset=" + off + ", length=" + len);
        if (inputHoldoverBuffer != null)
            throw new IllegalStateException("Cannot accept input while holdover is present.");

        inputHoldoverBuffer = b;
        inputHoldoverBufferPos = off;
        inputHoldoverBufferLen = len;

	// keep iterating until we have compressed or copied all of the input
	// that was passed to us.
        while (inputHoldoverBuffer != null) {
          compact();
	}
        assert testInvariants();
        // logState("After setInput");
    }

    @Override
    public void flush() throws IOException {
      if (inputBufferLen > 0) {
	compress(inputBuffer, 0, inputBufferLen);
	inputBufferLen = 0;
        assert testInvariants();
      }
    }

    @Override
    public void close() throws IOException {
      flush();
      out.close();
    }

    /*
     * Take the data from inputHoldoverBuffer that was just given to us
     * and either compress it or add it to the inputBuffer. If the inputBuffer
     * is full, we compress it and return. 
     */
    private void compact() throws IOException {
        int remaining = inputBuffer.length - inputBufferLen;
	// if there isn't any saved input
	if (inputBufferLen == 0) {

	  // if we have enough to fill a buffer, do so directly without a copy
	  if (inputHoldoverBufferLen >= remaining) {
	    compress(inputHoldoverBuffer, inputHoldoverBufferPos,
		     remaining);
	    inputHoldoverBufferPos += remaining;
	    inputHoldoverBufferLen -= remaining;
	    // are we done?
	    if (inputHoldoverBufferLen == 0) {
	      inputHoldoverBuffer = null;
	      inputHoldoverBufferPos = -1;
	      inputHoldoverBufferLen = -1;
	    }
	  } else {
	    System.arraycopy(inputHoldoverBuffer, inputHoldoverBufferPos, 
			     inputBuffer, 0, inputHoldoverBufferLen);
	    inputBufferLen += inputHoldoverBufferLen;
	    inputHoldoverBuffer = null;
	    inputHoldoverBufferPos = -1;
	    inputHoldoverBufferLen = -1;
	  }
	} else {
	  int copiedBytes = Math.min(remaining, inputHoldoverBufferLen);
	  System.arraycopy(inputHoldoverBuffer, inputHoldoverBufferPos, 
			   inputBuffer, inputBufferLen, copiedBytes);
	  inputBufferLen += copiedBytes;
	  inputHoldoverBufferPos += copiedBytes;
	  inputHoldoverBufferLen -= copiedBytes;
	  if (inputBufferLen == inputBuffer.length) {
	    compress(inputBuffer, 0, inputBufferLen);
	    inputBufferLen = 0;
	  } else if (inputHoldoverBufferLen == 0) {
	    inputHoldoverBuffer = null;
	    inputHoldoverBufferPos = -1;
	    inputHoldoverBufferLen = -1;
	  }
	}
    }

    private void compress(byte[] buffer, int off, int len) throws IOException {
        // logState("Before compress");

        outputBufferLen.value = outputBuffer.length;
        try {
            int code = compressor.compress(buffer, off, len, outputBuffer, 0,
                                           outputBufferLen);
            if (code != LzoTransformer.LZO_E_OK) {
                logState("LZO error: " + code);
                throw new IllegalArgumentException(compressor.toErrorString
                                                     (code));
            }
        } catch (IndexOutOfBoundsException e) {
            logState("IndexOutOfBoundsException: " + e);
            throw new IOException(e);
        }

        writeBlock(buffer, off, len, outputBuffer, 0, outputBufferLen.value);
    }

    protected void writeBlock(byte[] inputData, int inputPos, int inputLen, byte[] outputData, int outputPos, int outputLen)
            throws IOException {
        writeInt(inputLen);
        writeInt(outputLen);
        out.write(outputData, outputPos, outputLen);
    }

    protected void writeInt(int v) throws IOException {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write(v & 0xFF);
    }
}
