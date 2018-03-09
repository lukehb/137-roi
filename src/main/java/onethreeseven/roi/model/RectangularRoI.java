package onethreeseven.roi.model;

import onethreeseven.geo.model.LatLonBounds;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Given an RoI and the grid used to mine it, this class extracts the actual coordinates for the grid cell-based RoI.
 * @author Luke Bermingham
 */
public class RectangularRoI extends HashMap<Integer, BoundingCoordinates> implements BoundingCoordinates {

    private final RoIGrid roIGrid;
    private final double density;
    private double[][] bounds = null;
    private final AbstractGeographicProjection projection;
    private final int id;

    public RectangularRoI(RoIGrid roIGrid, RoI roi, AbstractGeographicProjection projection) {
        this.roIGrid = roIGrid;
        this.density = roi.getDensity() / roi.size();
        this.projection = projection;
        this.id = roi.getId();
        for (Integer cellIdx : roi) {
            this.put(cellIdx, roIGrid.getCellBounds(cellIdx));
        }
    }

    public int getId() {
        return id;
    }

    public RoIGrid getRoIGrid() {
        return roIGrid;
    }

    @Override
    public Iterator<double[]> geoCoordinateIter() {
        final double[][] corners = BoundsUtil.getCorners(getBounds());
        for (int i = 0; i < corners.length; i++) {
            corners[i] = projection.cartesianToGeographic(corners[i]);
        }
        final List<double[]> cornerList = Arrays.asList(corners);
        return cornerList.iterator();
    }

    @Override
    public Iterator<double[]> coordinateIter() {
        final double[][] corners = BoundsUtil.getCorners(getBounds());
        final List<double[]> cornerList = Arrays.asList(corners);
        return cornerList.iterator();
    }

    @Override
    public double[][] getBounds() {
        if (bounds == null) {
            bounds = BoundsUtil.calculateFromBoundingCoordinates(this.values());
        }
        return bounds;
    }

    @Override
    public LatLonBounds getLatLonBounds() {
        double[][] cartesianBounds = getBounds();
        double[] minXY = new double[]{cartesianBounds[0][0], cartesianBounds[1][0]};
        double[] maxXY = new double[]{cartesianBounds[0][1], cartesianBounds[1][1]};
        double[] minLatLon = projection.cartesianToGeographic(minXY);
        double[] maxLatLon = projection.cartesianToGeographic(maxXY);
        return new LatLonBounds(minLatLon[0], minLatLon[1], maxLatLon[0], maxLatLon[1]);
    }

    public double getDensity() {
        return density;
    }

    @Override
    public String toString() {
        return id + " (density = " + (int)density + ")";
    }
}
