package onethreeseven.roi.algorithm;

import onethreeseven.datastructures.model.Trajectory;
import onethreeseven.datastructures.util.DataGeneratorUtil;
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
 * Testing the slope RoI algorithm.
 * @see SlopeRoIs
 * @author Luke Bermingham
 */
public class SlopeRoIsTest {

    @Test
    public void testFindRoIs() throws Exception {

        int nTrajectories = 10;

        double[][] ptPool = new double[][]{
                new double[]{0, 0, 0},
                new double[]{3, 0, 0},
                new double[]{6, 0, 0},
                new double[]{6, 3, 0},
                new double[]{12, 3, 0}
        };

        Map<String, Trajectory> trajectories = DataGeneratorUtil.generateDensitySlopingTrajectoriesFrom(ptPool, nTrajectories);

        RoIGrid roiGrid = MiningSpaceFactory.createGrid(trajectories, new int[]{13, 4, 1}, 0);
        Collection<RoI> rois = new SlopeRoIs().run(roiGrid, 3);

        List<Integer> expectedIndices = Arrays.asList(
                roiGrid.to1dIdx(new int[]{0, 0, 0}),
                roiGrid.to1dIdx(new int[]{1, 0, 0}),
                roiGrid.to1dIdx(new int[]{2, 0, 0}),
                roiGrid.to1dIdx(new int[]{3, 0, 0}),
                roiGrid.to1dIdx(new int[]{4, 0, 0}),
                roiGrid.to1dIdx(new int[]{5, 0, 0}),
                roiGrid.to1dIdx(new int[]{6, 0, 0}),
                roiGrid.to1dIdx(new int[]{6, 1, 0}),
                roiGrid.to1dIdx(new int[]{6, 2, 0}),
                roiGrid.to1dIdx(new int[]{6, 3, 0}),
                roiGrid.to1dIdx(new int[]{7, 3, 0}),
                roiGrid.to1dIdx(new int[]{8, 3, 0}),
                roiGrid.to1dIdx(new int[]{9, 3, 0}),
                roiGrid.to1dIdx(new int[]{10, 3, 0}),
                roiGrid.to1dIdx(new int[]{11, 3, 0}),
                roiGrid.to1dIdx(new int[]{12, 3, 0})
        );

        for (RoI roi : rois) {
            for (Integer idx : roi) {
                Assert.assertTrue(expectedIndices.contains(idx));
            }
        }


    }
}