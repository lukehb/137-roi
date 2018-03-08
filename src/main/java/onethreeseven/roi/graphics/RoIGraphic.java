package onethreeseven.roi.graphics;

import onethreeseven.collections.DoubleArray;
import onethreeseven.common.util.ColorUtil;
import onethreeseven.roi.model.RectangularRoI;
import onethreeseven.roi.model.RoIGrid;
import onethreeseven.roi.model.RoIGridCell;
import onethreeseven.trajsuitePlugin.graphics.GraphicsPayload;
import onethreeseven.trajsuitePlugin.graphics.PackedVertexData;
import onethreeseven.trajsuitePlugin.graphics.RenderingModes;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;
import onethreeseven.trajsuitePlugin.settings.PluginSettings;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * A graphic representation of a grid-based RoI (2d or 3d).
 * @author Luke Bermingham.
 */
public class RoIGraphic extends GraphicsPayload {

    public RoIGraphic(RectangularRoI rectangularRoI) {
        super();
        //set heat map color
        double max = rectangularRoI.getRoIGrid().getMaxPossibleDensity();
        Color heat = ColorUtil.generateHeatMapColor(0, max, rectangularRoI.getDensity());
        this.fallbackColor.setValue(heat);
    }

    private double[][][] getFaces(double[][] bounds) {
        int nDimensions = bounds.length;
        //have to have at least two dimensions to draw a grid based cube
        if(nDimensions < 2){
            throw new IllegalStateException("Cannot visualise an RoI with less than 2 dimensions.");
        }

        double[][] corners;

        //extend 2d bounds into 3d, and then get corners
        if (nDimensions == 2) {
            double[][] bounds3d = new double[][]{
                    new double[]{bounds[0][0], bounds[0][1]},
                    new double[]{bounds[1][0], bounds[1][1]},
                    new double[]{0, PluginSettings.smallElevation.getSetting()}
            };
            corners = BoundsUtil.getCorners(bounds3d);
        }else{
            corners = BoundsUtil.getCorners(bounds);
        }

        //assembling the faces of a 3d rectangle should be possible now
        return new double[][][]{
                //face #0: -x
                new double[][]{corners[1], corners[0], corners[2], corners[3]},
                //face #1: +x
                new double[][]{corners[4], corners[5], corners[7], corners[6]},
                //face #2: -z
                new double[][]{corners[0], corners[4], corners[6], corners[2]},
                //face #3: z
                new double[][]{corners[5], corners[1], corners[3], corners[7]},
                //face #4: -y
                new double[][]{corners[0], corners[1], corners[5], corners[4]},
                //face #5: y
                new double[][]{corners[2], corners[6], corners[7], corners[3]}
        };

    }

    @Override
    protected RenderingModes defaultRenderingMode() {
        return RenderingModes.QUADS;
    }

    @Override
    public PackedVertexData createVertexData(BoundingCoordinates model) {

        if(!(model instanceof RectangularRoI)){
            throw new IllegalArgumentException("Can only make vertex data for rectangular rois.");
        }

        //go through each index of roi grid cell, check each face of that grid cell for neighbours

        RectangularRoI rectangularRoI = (RectangularRoI) model;
        RoIGrid grid = rectangularRoI.getRoIGrid();

        //these are the directions to check for our given cell
        //note: these are also the normals of each face of the cube when we draw
        final int[] negX = new int[]{-1, 0, 0};
        final int[] posX = new int[]{1, 0, 0};
        final int[] negZ = new int[]{0, 0, -1};
        final int[] posZ = new int[]{0, 0, 1};
        final int[] negY = new int[]{0, -1, 0};
        final int[] posY = new int[]{0, 1, 0};

        //note: this ordering is specific, it matches the above face ordering
        //in getFaces()
        final int[][] normals = new int[][]{
                negX, posX, negZ, posZ, negY, posY,
        };

        DoubleArray vertexData = new DoubleArray(24, false);

        for (Map.Entry<Integer, BoundingCoordinates> cellEntry : rectangularRoI.entrySet()) {

            //get all the faces of the given cell, we may remove some if they are shared
            final double[][] bounding = cellEntry.getValue().getBounds();
            final int nDims = bounding.length;
            final double[][][] faceVerts = getFaces(bounding);

            //have all the indices, one for each face
            Set<Integer> faceIndices = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5));
            int[] ndIdx = grid.toNdIdx(cellEntry.getKey());

            //remove the indices of shared faces because that indicates it is shared (hence not worth drawing)
            RoIGridCell cellNegX = grid.moveToCell(ndIdx, negX);
            int cellNegXIdx = (cellNegX == null) ? -1 : cellNegX.getIndex();

            RoIGridCell cellPosX = grid.moveToCell(ndIdx, posX);
            int cellPosXIdx = (cellPosX == null) ? -1 : cellPosX.getIndex();

            RoIGridCell cellNegZ = grid.moveToCell(ndIdx, negZ);
            int cellNegZIdx = (cellNegZ == null) ? -1 : cellNegZ.getIndex();

            RoIGridCell cellPosZ = grid.moveToCell(ndIdx, posZ);
            int cellPosZIdx = (cellPosZ == null) ? -1 : cellPosZ.getIndex();

            RoIGridCell cellNegY = grid.moveToCell(ndIdx, negY);
            int cellNegYIdx = (cellNegY == null) ? -1 : cellNegY.getIndex();

            RoIGridCell cellPosY = grid.moveToCell(ndIdx, posY);
            int cellPosYIdx = (cellPosY == null) ? -1 : cellPosY.getIndex();


            if (rectangularRoI.containsKey(cellNegXIdx)) {
                faceIndices.remove(0);
            }
            if (rectangularRoI.containsKey(cellPosXIdx)) {
                faceIndices.remove(1);
            }

            //only bother checking z if 3d
            if (nDims > 2) {
                if (rectangularRoI.containsKey(cellNegZIdx)) {
                    faceIndices.remove(2);
                }
                if (rectangularRoI.containsKey(cellPosZIdx)) {
                    faceIndices.remove(3);
                }
            }

            if (rectangularRoI.containsKey(cellNegYIdx)) {
                faceIndices.remove(4);
            }
            if (rectangularRoI.containsKey(cellPosYIdx)) {
                faceIndices.remove(5);
            }

            for (Integer faceIdx : faceIndices) {
                //verts of a single quad
                double[][] verts = faceVerts[faceIdx];
                int[] normal = normals[faceIdx];
                for (double[] vert : verts) {
                    //vert coords
                    for (double coord : vert) {
                        vertexData.add(coord);
                    }
                    //normal coords
                    for (int coord : normal) {
                        vertexData.add(coord);
                    }
                }
            }
        }

        return new PackedVertexData(
                vertexData.getArray(),
                new PackedVertexData.Types[]{
                        PackedVertexData.Types.VERTEX,
                        PackedVertexData.Types.NORMAL});
    }
}
