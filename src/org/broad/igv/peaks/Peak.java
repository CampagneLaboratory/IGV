/*
 * Copyright (c) 2007-2011 by The Broad Institute, Inc. and the Massachusetts Institute of
 * Technology.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.peaks;

import org.broad.igv.feature.LocusScore;
import org.broad.igv.track.WindowFunction;
import org.broad.tribble.Feature;

/**
 *
 * Note:  implementing tribble.Feature will allow us to index these files in the future.
 *
 * @author jrobinso
 * @date Apr 22, 2011
 */
public class Peak implements LocusScore {

    String chr;
    int start;
    int end;
    private String name;
    private float combinedScore;
    private float [] timeScores;
    boolean dynamic = false;

    public Peak(String chr,int start, int end, String name, float combinedScore, float[] timeScores) {
        this.chr = chr;
        this.combinedScore = combinedScore;
        this.end = end;
        this.name = name;
        this.start = start;
        this.timeScores = timeScores;

        float dynThreshold = 3;
        float foldChange = timeScores[0] / timeScores[timeScores.length - 1];
        dynamic = (foldChange > dynThreshold || (1 / foldChange) > dynThreshold);
    }

    public String getChr() {
        return chr;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end; 
    }

    public String getName() {
        return name;
    }

    public float getCombinedScore() {
        return combinedScore;
    }

    public float[] getTimeScores() {
        return timeScores;
    }

    public boolean isDynamic() {
        return dynamic;
    }


    // Locus score interface
    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public float getScore() {
        return combinedScore;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public LocusScore copy() {
        return this;
    }

    public String getValueString(double position, WindowFunction windowFunction) {
        return String.valueOf(getScore());
    }
}