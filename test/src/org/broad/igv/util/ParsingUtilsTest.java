/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

package org.broad.igv.util;

import org.broad.igv.Globals;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackProperties;
import org.broad.igv.track.WindowFunction;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * User: jrobinso
 * Date: Feb 8, 2010
 */
public class ParsingUtilsTest {

    public final static String characters = "0123456789abcdefghijklmnopqrstuvwxyz";
    public final static int numChars = characters.length();

    @Before
    public void setUp() {
        TestUtils.setUpHeadless();
        Globals.CONNECT_TIMEOUT = 5 * 60 * 1000;
    }

    private String genRandString() {
        int numWords = 10;
        int max_length = 20;
        String ret = "";
        for (int _ = 0; _ < numWords; _++) {
            ret += getRandWord(max_length) + "\t";
        }
        return ret;
    }

    private String getRandWord(int max_length) {
        int length = (int) Math.random() * max_length + 1;
        String ret = "";
        for (int _ = 0; _ < length; _++) {
            ret += characters.charAt((int) Math.random() * numChars);
        }
        return ret;
    }

    @Test
    public void testSplit1() {
        String blankColumnLine = "a\tb\t\td";
        String[] tokens = Globals.tabPattern.split(blankColumnLine);
        int nTokens = tokens.length;
        assertEquals(4, nTokens);
        assertEquals("a", tokens[0]);
        assertEquals("b", tokens[1]);
        assertEquals("", tokens[2]);
        assertEquals("d", tokens[3]);
    }

    @Test
    public void testSplit2() {
        String blankColumnLine = "a\tb\t\td\t";
        String[] tokens = Globals.tabPattern.split(blankColumnLine);
        int nTokens = tokens.length;
        assertEquals(5, nTokens);
        assertEquals("a", tokens[0]);
        assertEquals("b", tokens[1]);
        assertEquals("", tokens[2]);
        assertEquals("d", tokens[3]);
        assertEquals("", tokens[2]);
    }


    @Test
    public void testComputeReadingShifts
            () {
        // Add your code here
    }

    @Test
    public void testGetContentLengthFTP() {
        long contLength = ParsingUtils.getContentLength(TestUtils.AVAILABLE_FTP_URL);
        assertTrue("Error retrieving content length: " + contLength, contLength > 0);

        long start_time = System.currentTimeMillis();
        assertTrue(ParsingUtils.getContentLength(TestUtils.UNAVAILABLE_FTP_URL) == -1);
        long end_time = System.currentTimeMillis();
        assertTrue(end_time - start_time < Globals.CONNECT_TIMEOUT + 1000);
        assertTrue(end_time - start_time < Globals.CONNECT_TIMEOUT + 1000);
    }

    @Test
    public void testParseInt() {
        String with_commas = "123456";
        int expected = 123456;
        int actual = ParsingUtils.parseInt(with_commas);
        assertEquals(expected, actual);

        String exp_not = "3.5e4";
        expected = 35000;
        assertEquals(expected, ParsingUtils.parseInt(exp_not));
    }

    @Test
    public void testParseTrackLine() {
        String trackLine = "track type=bigWig name=\"Track 196\" visibility=2 " +
                "description=\" CD34 - H3K27me3 - hg19 - 18.7 M/20.9 M - 61P7DAAXX.6\" " +
                "maxHeightPixels=70 viewLimits=0:18 windowingFunction=mean autoScale=off " +
                "bigDataUrl=http://www.broadinstitute.org/epigenomics/dataportal/track_00196.portal.bw " +
                "color=255,0,0";

        TrackProperties props = new TrackProperties();
        ParsingUtils.parseTrackLine(trackLine, props);
        assertEquals("Track 196", props.getName());
        assertEquals(Track.DisplayMode.EXPANDED, props.getDisplayMode());
        assertEquals(" CD34 - H3K27me3 - hg19 - 18.7 M/20.9 M - 61P7DAAXX.6", props.getDescription());
        assertEquals(70, props.getHeight());
        assertEquals(0, props.getMinValue(), 1.0e-9);
        assertEquals(18, props.getMaxValue(), 1.0e-9);
        assertEquals(WindowFunction.mean, props.getWindowingFunction());
        assertEquals(false, props.isAutoScale());
        assertEquals(new Color(255, 0, 0), props.getColor());
        assertEquals("http://www.broadinstitute.org/epigenomics/dataportal/track_00196.portal.bw", props.getDataURL());
    }
}

