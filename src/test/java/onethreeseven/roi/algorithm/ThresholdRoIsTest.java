package onethreeseven.roi.algorithm;


import onethreeseven.common.util.NDUtil;
import onethreeseven.datastructures.model.Trajectory;
import onethreeseven.datastructures.util.DataGeneratorUtil;
import onethreeseven.roi.model.MiningCell;
import onethreeseven.roi.model.MiningSpaceFactory;
import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIGrid;
import org.junit.Assert;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Testing the Threshold RoIs algorithm.
 * @see ThresholdRoIs
 * @author Luke Bermingham
 */
public class ThresholdRoIsTest {

    @Test
    public void testRun() throws Exception {

        int nTrajectories = 10;
        int distance = 7;
        int nDimensions = 3;
        Map<String, Trajectory> trajectories = DataGeneratorUtil.generateStraightTrajectories(
                nDimensions, distance, nTrajectories);
        int[] cellsPerDimension = new int[nDimensions];
        Arrays.fill(cellsPerDimension, 1);
        cellsPerDimension[0] = distance + 1;

        int[] expectedIndices = new int[distance + 1];
        for (int i = 0; i < expectedIndices.length; i++) {
            expectedIndices[i] = i;
        }

        //set those trajectories into the grid
        RoIGrid grid = MiningSpaceFactory.createGrid(trajectories, cellsPerDimension, 0);

        //do mining
        Collection<RoI> rois = new ThresholdRoIs().run(grid, nTrajectories);
        Assert.assertTrue(!rois.isEmpty());
        RoI bigRoI = rois.iterator().next();

        for (int expectedIndice : expectedIndices) {
            System.out.println("Checking if threshold roi contained cell idx: " + expectedIndice);
            boolean contains = bigRoI.contains(expectedIndice);
            Assert.assertTrue("RoI did not contain!", contains);
        }
    }

    @Test
    public void testLShape() throws Exception {

        int nTrajectories = 10;

        Map<String, Trajectory> trajectories = DataGeneratorUtil.generateNTrajectoriesFrom(
                new double[][]{
                        new double[]{0, 0, 0},
                        new double[]{3, 0, 0},
                        new double[]{6, 0, 0},
                        new double[]{6, 3, 0},
                }, nTrajectories
        );

        int[] cellsPerDimension = new int[]{7, 4, 1};

        List<Integer> expectedIndices = Arrays.asList(
                NDUtil.flattenIndices(new int[]{0, 0, 0}, cellsPerDimension),
                NDUtil.flattenIndices(new int[]{1, 0, 0}, cellsPerDimension),
                NDUtil.flattenIndices(new int[]{2, 0, 0}, cellsPerDimension),
                NDUtil.flattenIndices(new int[]{3, 0, 0}, cellsPerDimension),
                NDUtil.flattenIndices(new int[]{4, 0, 0}, cellsPerDimension),
                NDUtil.flattenIndices(new int[]{5, 0, 0}, cellsPerDimension),
                NDUtil.flattenIndices(new int[]{6, 0, 0}, cellsPerDimension),
                NDUtil.flattenIndices(new int[]{6, 1, 0}, cellsPerDimension),
                NDUtil.flattenIndices(new int[]{6, 2, 0}, cellsPerDimension),
                NDUtil.flattenIndices(new int[]{6, 3, 0}, cellsPerDimension)
        );


        RoIGrid roiGrid = MiningSpaceFactory.createGrid(trajectories, cellsPerDimension, 0);
        Collection<RoI> rois = new ThresholdRoIs().run(roiGrid, nTrajectories);
        for (RoI roi : rois) {
            for (Integer cellIdx : roi) {
                MiningCell cell = roiGrid.getCell(cellIdx);
                Assert.assertTrue(cell.getDensity() >= nTrajectories);
                //convert to ndIdx
                Assert.assertTrue(expectedIndices.contains(cellIdx));
            }
        }


    }


}