package onethreeseven.roi.model;


import onethreeseven.datastructures.algorithm.TrajectoryDragonCurve;
import onethreeseven.datastructures.model.Trajectory;
import onethreeseven.datastructures.util.DataGeneratorUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Testing mining spaces and their cell density.
 * @see RoIGrid
 * @see MiningSpaceFactory
 * @author Luke Bermingham
 */
public class MiningSpaceFactoryTest {

    @Test
    public void testFindDenseCells() throws Exception {

        TrajectoryDragonCurve algo = new TrajectoryDragonCurve();
        algo.setBounds(new double[][]{new double[]{0, 100}, new double[]{0, 100}, new double[]{0, 100}});

        RoIGrid roIGrid = MiningSpaceFactory.createGrid(
                DataGeneratorUtil.generateCurvyTrajectories(algo, 10),
                new int[]{10, 10, 10}, 1);

        Collection<? extends MiningCell> denseCells = roIGrid.getDenseCells();

        Logger logger = Logger.getLogger(this.getClass().getName());
        int previousDensity = Integer.MAX_VALUE;
        for (MiningCell gridCell : denseCells) {
            int density = gridCell.getDensity();
            logger.info("Density:" + density);
            Assert.assertTrue(density <= previousDensity);
            previousDensity = density;
        }

        //we have dense cells
        Assert.assertTrue(!denseCells.isEmpty());

    }

    @Test
    public void testFindObviousDenseCells() throws Exception {

        int nTrajectories = 10;
        int distance = 7;
        int nDimensions = 3;
        Map<String,Trajectory> trajectories = DataGeneratorUtil.generateStraightTrajectories(
                nDimensions, distance, nTrajectories);
        int[] cellsPerDimension = new int[nDimensions];
        Arrays.fill(cellsPerDimension, 1);
        cellsPerDimension[0] = distance;

        BitSet expectedIndices = new BitSet();
        for (int i = 0; i < distance + 1; i++) {
            expectedIndices.set(i);
        }

        //set those trajectories into the grid
        RoIGrid grid = MiningSpaceFactory.createGrid(trajectories, cellsPerDimension, 0);

        //expected ids
        String[] expectedIds = trajectories.keySet().toArray(new String[nTrajectories]);

        Logger logger = Logger.getLogger(this.getClass().getName());
        for (MiningCell denseCell : grid.getDenseCells()) {
            Assert.assertTrue(expectedIndices.get(denseCell.getIndex()));
            Assert.assertTrue(denseCell.getDensity() == nTrajectories);
            Assert.assertTrue(denseCell.containsAll(expectedIds));
            logger.info(denseCell.toString());
        }


    }

}