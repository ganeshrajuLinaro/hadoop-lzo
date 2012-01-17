/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.lzo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import com.hadoop.compression.lzo.LzopInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.LineRecordReader;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author shevek
 */
public class HadoopLzopFileTest {

    private static final Log LOG = LogFactory.getLog(BlockCompressorStreamTest.class);

    public static void compareFile(File file, 
                                   byte[] actual) throws IOException {
      byte[] expected = FileUtils.readFileToByteArray(file);
      assertArrayEquals(expected, actual);
    }

    @Test
    public void testInputFile() throws Exception {
        try {
            File dir = LzopFileTest.getDataDirectory();
            File file = new File(dir, "input.txt.lzo");
            FileInputStream fi = new FileInputStream(file);
            LzopInputStream ci = new LzopInputStream(fi);
            ByteArrayOutputStream bo = 
              new ByteArrayOutputStream((int) (file.length() * 2));
            bo.reset();
            long start = System.currentTimeMillis();
            IOUtils.copy(ci, bo);
            long end = System.currentTimeMillis();
            LOG.info("Uncompression took " + ((end - start) / 1000d) + " ms");
            LOG.info("Uncompressed data is " + bo.size() + " bytes.");
            compareFile(new File(dir, "input.txt"), bo.toByteArray());
        } finally {
            System.out.flush();
            System.err.flush();
            Thread.sleep(100);
        }
    }

    @Test
    public void testLineReader() throws Exception {
        try {
            File dir = LzopFileTest.getDataDirectory();
            File file = new File(dir, "input.txt.lzo");
            FileInputStream fi = new FileInputStream(file);
            LzopInputStream ci = new LzopInputStream(fi);
            LineRecordReader reader = 
              new LineRecordReader(ci, 0L, 9999L, 9999);
            LineRecordReader expected =
              new LineRecordReader(new FileInputStream(new File(dir, 
                                                                "input.txt")),
                                   0L, 9999L, 9999);
            LongWritable key = new LongWritable();
            Text value = new Text();
            LongWritable expectedKey = new LongWritable();
            Text expectedValue = new Text();
            while (reader.next(key, value)) {
              assertTrue(expected.next(expectedKey, expectedValue));
              assertEquals("key match", expectedKey, key);
              assertEquals("value match", expectedValue, value);
            }
	    assertFalse(expected.next(expectedKey, expectedValue));
        } catch (Exception e) {
            LOG.info("Test failed", e);
            throw e;
        } finally {
            System.out.flush();
            System.err.flush();
            Thread.sleep(100);
        }
    }
}
