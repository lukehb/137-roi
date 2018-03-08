package onethreeseven.roi.model;

import onethreeseven.common.util.NDUtil;
import onethreeseven.geo.model.LatLonBounds;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;

import java.util.*;

/**
 * A grid that can be partitioned and queried in n-dimensions
 * Grid is implemented lazy-style, so grid-cells are not calculated until requested.
 * Note: This lazy-style means they are not cached either.
 * @author Luke Bermingham
 */
public class Grid {

    /**
     * The min and max of each dimension of this grid-cell
     */
    final double[][] dimensionalRanges;
    /**
     * The number of cells in each dimension, so like: {3,3,3}
     */
    final int[] nCellsPerDimension;

    /**
     * Create a grid cell given the following dimensional ranges (min, max in each dimension)
     *
     * @param nCellsPerDimension how many partitions to make in each dimension
     * @param bounds             min/max in each dimension like: {[0, 10], [50, 100], [-137, 137]}
     */
    public Grid(int[] nCellsPerDimension, double[][] bounds) {
        this.dimensionalRanges = bounds;

        //ensure each dimension has at least one cell in it
        for (double[] dimensionalRange : dimensionalRanges) {
            double range = dimensionalRange[1] - dimensionalRange[0];
            if (range == 0) {
                //give it a range of one
                dimensionalRange[1] = 1;
            }
        }

        this.nCellsPerDimension = new int[nCellsPerDimension.length];
        System.arraycopy(nCellsPerDimension, 0, this.nCellsPerDimension, 0, nCellsPerDimension.length);
    }

    /**
     * @param indices The indexes in each dimension used to get the cell, so like {1,3,7}
     * @return the grid cell calculated resolve the grid
     */
    public NdCell getCell(int[] indices) {
        return () -> indices;
    }

    /**
     * Converts the 1d idx to n-d indices
     *
     * @param idx1D 1d index
     * @return n-d indices
     */
    public int[] toNdIdx(int idx1D) {
        return NDUtil.inflateIndex(idx1D, nCellsPerDimension);
    }

    /**
     * Converts an n-dimensional index to 1d
     * @param ndIdx nd index
     * @return a flattened 1d index
     */
    public int to1dIdx(int[] ndIdx) {
        return NDUtil.flattenIndices(ndIdx, nCellsPerDimension);
    }

    /**
     * Get indices between a given starting and ending point, whilst considering a cell radius
     *
     * @param startPt the starting point
     * @param endPt   the ending point
     * @param radius  the cell radius
     * @return a set of cell 1d grid cell indices indicating which cells are relevant
     */
    public Set<Integer> getIndicesBetween(double[] startPt, double[] endPt, int radius) {
        Set<Integer> allIndices = new HashSet<>();
        //get the indices between the two points
        int[][] indicesBetween = NDUtil.interpolate(getIndices(startPt), getIndices(endPt));
        //using the centroids gets any surrounding cells
        if (radius > 0) {
            for (int[] centroid : indicesBetween) {
                for (int cellIdx1d : getIndicesAround(centroid, radius)) {
                    allIndices.add(cellIdx1d);
                }
            }
        } else {
            //just centroids
            for (int[] centroid : indicesBetween) {
                allIndices.add(NDUtil.flattenIndices(centroid, this.nCellsPerDimension));
            }
        }
        return allIndices;
    }

    /**
     * Given a central position, find all the cells around that point within a given raidus
     *
     * @param centroidIndices The indices of the central cell
     * @param radius          The radius of cells around the central cell to get
     * @return a collection of the indices around the central indices (within the overall grid bounds),
     * note: includes centroid itself
     */
    public int[] getIndicesAround(int[] centroidIndices, int radius) {
        int nDimensions = centroidIndices.length;
        //compute min and max indices given the centroid and the radius
        int[] minIndices = new int[nDimensions];
        int[] maxIndices = new int[nDimensions];
        int[] shape = new int[nDimensions];
        //compute min/max by +/- onto centroid using radius, and then clamping within grid bounds
        for (int i = 0; i < nDimensions; i++) {
            minIndices[i] = Math.max(0, centroidIndices[i] - radius);
            maxIndices[i] = Math.min(nCellsPerDimension[i], centroidIndices[i] + radius);
            shape[i] = maxIndices[i] - minIndices[i] + 1;
        }
        return fitIndicesIntoDimensions(shape, minIndices, maxIndices);
    }

    /**
     * Fit the given indices (defined by a min/max and a shape) into
     * the nCellsPerDimension limit imposed by this grid.
     * @param shape the bounding shape of the indices
     * @param minIndices the min indices
     * @param maxIndices the max indices
     * @return the indices after they have been fitted into the nCellsPerDimension
     */
    protected int[] fitIndicesIntoDimensions(int[] shape, int[] minIndices, int[] maxIndices){
        //flatten the n-dimensional indices to 1d, using the given shape.
        //however, if this shape is different to the grid extents then we
        //will have to translate where the indices end up in the final grid.
        int minIdx1D = NDUtil.flattenIndices(minIndices, shape);
        int maxIdx1D = NDUtil.flattenIndices(maxIndices, shape);
        //get all the indices between the extremes
        int[] indices = new int[maxIdx1D - minIdx1D + 1];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = minIdx1D + i;
        }
        //case: we will have to translate
        if (!Arrays.equals(shape, nCellsPerDimension)) {
            //translation means inflating the indices, adding the starting position
            //then flattening back to the original extents
            for (int i = 0; i < indices.length; i++) {
                //convert the 1d index back into n-d, now that we have the whole set
                int[] ndIndices = NDUtil.inflateIndex(indices[i], shape);
                //add the starting index onto all of them
                for (int n = 0; n < minIndices.length; n++) {
                    ndIndices[n] += minIndices[n];
                }
                indices[i] = NDUtil.flattenIndices(ndIndices, this.nCellsPerDimension);
            }
            return indices;
        }
        return indices;
    }

    /**
     * @param point a n-d point
     * @return The 1d index of a given point
     */
    public int getIndex(double[] point) {
        return NDUtil.flattenIndices(getIndices(point), nCellsPerDimension);
    }

    /**
     * @param point The query point
     * @return The indices of the grid cell that this point resides in
     */
    public int[] getIndices(double[] point) {
        int[] indices = new int[nDimensions()];
        for (int n = 0; n < nDimensions(); n++) {
            double nD = point[n];
            double minN = getMin(n);
            double deltaN = deltaN(n);
            double partitionSize = deltaN / nCellsPerDimension[n];
            double normD = (nD - minN) / partitionSize;
            int nIdx = (int) Math.floor(normD);
            indices[n] = nIdx;
        }
        return indices;
    }

    /**
     * @param ordinate the dimension index
     * @return The minimum value of this dimension
     */
    public double getMin(int ordinate) {
        return getBounds()[ordinate][0];
    }

    /**
     * @param ordinate the dimension index
     * @return The maximum value of this dimension
     */
    public double getMax(int ordinate) {
        return getBounds()[ordinate][1];
    }

    /**
     * @param n the dimension to query
     * @return the delta of this dimension
     */
    private double deltaN(int n) {
        double minN = getMin(n);
        double maxN = getMax(n);
        return (maxN - minN) + 1; //because zero-based;
    }

    /**
     * @param n The dimensions being queried
     * @param i The index the grid partitions within a given dimension,
     *          so partitions like: [_][_][_][_] in one dimension, which one do you want?
     * @return min/max in some given dimension, using a given index
     */
    private double[] getMinMaxN(int n, int i) {
        double partitionSize = deltaN(n) / nCellsPerDimension[n];
        double iMin = partitionSize * i;
        double iMax = iMin + partitionSize;
        return new double[]{iMin + dimensionalRanges[n][0], iMax + dimensionalRanges[n][0]};
    }

    public BoundingCoordinates getCellBounds(Integer idx) {
        return getCellBounds(toNdIdx(idx));
    }

    public BoundingCoordinates getCellBounds(int[] indices) {
        final double[][] bounds = new double[indices.length][];

        for (int n = 0; n < indices.length; n++) {
            double[] minMaxN = getMinMaxN(n, indices[n]);
            bounds[n] = minMaxN;
        }

        final double[][] corners = BoundsUtil.getCorners(bounds);
        final List<double[]> cornerList = Arrays.asList(corners);

        return new BoundingCoordinates() {
            @Override
            public Iterator<double[]> geoCoordinateIter() {
                throw new UnsupportedOperationException("Geographic coordinates do not make sense in this context of index bounds.");
            }

            @Override
            public Iterator<double[]> coordinateIter() {
                return cornerList.iterator();
            }

            @Override
            public double[][] getBounds() {
                return bounds;
            }

            @Override
            public LatLonBounds getLatLonBounds() {
                throw new UnsupportedOperationException("Geographic coordinates do not make sense in this context of index bounds.");
            }
        };
    }

    public double[][] getBounds() {
        return dimensionalRanges;
    }

    /**
     * @return the number of dimensions as specified by the bounds
     */
    public int nDimensions() {
        return getBounds().length;
    }

}
