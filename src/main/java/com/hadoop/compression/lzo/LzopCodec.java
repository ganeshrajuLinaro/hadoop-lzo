/*
 * This file is part of Hadoop-Gpl-Compression.
 *
 * Hadoop-Gpl-Compression is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Hadoop-Gpl-Compression is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hadoop-Gpl-Compression.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.hadoop.compression.lzo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.apache.hadoop.io.compress.Compressor;
import org.apache.hadoop.io.compress.Decompressor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.CompressionCodec;

/**
 * A {@link CompressionCodec} for a streaming
 * <b>lzo</b> compression/decompression pair compatible with lzop.
 * http://www.lzop.org/
 */
public class LzopCodec extends LzoCodec {

  public static final String DEFAULT_LZO_EXTENSION = ".lzo";
  private static int count = 0;

  @Override
  public CompressionOutputStream createOutputStream(OutputStream out, 
                                                    Compressor compressor
                                                    ) throws IOException {
    Configuration conf = getConf();
    LzoCompressor.CompressionStrategy strategy = 
      LzoCodec.getCompressionStrategy(conf);
    int bufferSize = LzoCodec.getBufferSize(conf);
    return new LzopOutputStream(out, strategy, bufferSize);
  }

  @Override
  public CompressionInputStream createInputStream(InputStream in, 
                                                  Decompressor decompressor
                                                  ) throws IOException {
    return new LzopInputStream(in);
  }

  @Override
  public Decompressor createDecompressor() {
    return null;
  }

  @Override
  public String getDefaultExtension() {
    return DEFAULT_LZO_EXTENSION;
  }
}
