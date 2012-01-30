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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.io.compress.CompressionOutputStream;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestLzoIndex {
  private static final Path testBuildData = 
    new Path(System.getProperty("test.build.data"));
  private static final Path outputDir = 
    new Path(System.getProperty("test.scratch"), "outputDir");

  @Test
  public void testCreateIndex() throws IOException {
    Configuration conf = new Configuration();
    FileSystem fs = FileSystem.getLocal(conf);
    // wipe the directory and copy the input file
    fs.delete(outputDir, true);
    fs.mkdirs(outputDir);
    FileUtil.copy(fs, new Path(testBuildData, "100000.txt.lzo"), fs, outputDir,
		  false, false, conf);

    // create the index file
    Path lzoFile = new Path(outputDir, "100000.txt.lzo");
    LzoIndex.createIndex(fs, lzoFile);

    // make sure we read it back correctly
    LzoIndex index = LzoIndex.readIndex(fs, lzoFile);
    assertEquals(3, index.getNumberOfBlocks());

    // rename the index to the old name
    fs.rename(LzoIndex.makeIndexName(lzoFile),
	      lzoFile.suffix(".index"));

    // make sure we read it back correctly
    index = LzoIndex.readIndex(fs, lzoFile);
    assertEquals(3, index.getNumberOfBlocks());
  }
}