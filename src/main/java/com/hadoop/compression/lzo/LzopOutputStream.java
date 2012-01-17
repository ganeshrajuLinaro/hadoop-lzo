package com.hadoop.compression.lzo;

import java.io.IOException;
import java.io.OutputStream;
import org.anarres.lzo.LzopConstants;
import org.apache.hadoop.io.compress.CompressionOutputStream;

public class LzopOutputStream extends CompressionOutputStream {

  public LzopOutputStream(OutputStream out, 
			  LzoCompressor.CompressionStrategy strategy, 
			  int bufferSize) throws IOException {
    super(new org.anarres.lzo.LzopOutputStream(out, strategy.newCompressor(),
					       bufferSize, 
					       LzopConstants.F_ADLER32_C | 
					       LzopConstants.F_ADLER32_D));
  }

  @Override
  public void write(byte[] buf, int off, int len) throws IOException {
    out.write(buf, off, len);
  }

  @Override
  public void write(int b) throws IOException {
    out.write(b);
  }

  @Override
  public void finish() throws IOException {
  }

  @Override
  public void resetState() throws IOException {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
