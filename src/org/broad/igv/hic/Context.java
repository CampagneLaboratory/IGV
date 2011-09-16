package org.broad.igv.hic;

import org.broad.igv.hic.data.Chromosome;

/**
 * @author jrobinso
 * @date Aug 11, 2010
 */
public class Context {

    private Chromosome chromosome;
    private int zoom = 4;
    private int origin = 0;
    private int visibleWidth;
    private double scale;

    public Context(Chromosome chromosome) {
        this.chromosome = chromosome;
    }


    public void setOrigin(int x) {
        int maxOrigin = Math.max(0, chromosome.getSize() - visibleWidth);
        origin = Math.min(maxOrigin, Math.max(0, x));

    }

    public void moveBy(int delta) {
       setOrigin(origin + delta);
    }

    public int getZoom() {
        return zoom;
    }

    public void setZoom(int zoom, double v) {
        this.scale = v;
        this.zoom = zoom;
    }

    public int getOrigin() {
        return origin;
    }

    public int getVisibleWidth() {
        return visibleWidth;
    }

    public void setVisibleWidth(int visibleWidth) {
        this.visibleWidth = visibleWidth;
    }

    public int getChrLength() {
        return chromosome.getSize();
    }


    public double getScale() {
        return scale;  //To change body of created methods use File | Settings | File Templates.
    }

    /**
     * Return the screen position corresponding to the chromosomal position.
     *
     * @param chromosomePosition
     * @return
     */
    public int getScreenPosition(double chromosomePosition) {
        return (int) ((chromosomePosition - origin) / getScale());
    }

    public Chromosome getChromosome() {
        return chromosome;
    }
}