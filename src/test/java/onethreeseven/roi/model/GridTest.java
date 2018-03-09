package onethreeseven.roi.model;

import onethreeseven.common.util.NDUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Testing the grid and working with its cell indices.
 * @see Grid
 * @author Luke Bermingham
 */
public class GridTest {

    @Test
    public void testGetIndicesBetween() {

        int min = 0;
        int max = 10;

        int[] extents = new int[]{max, max};
        Grid grid = new Grid(extents, new double[][]{
                new double[]{min, max},
                new double[]{min, max}
        });

        Set<Integer> indices = grid.getIndicesBetween(new double[]{0, 0}, new double[]{max, 0}, 0);

        //test for the expected indices
        for (int x = 0; x < max; x++) {
            int idx1d = NDUtil.flattenIndices(new int[]{x, min}, extents);
            Assert.assertTrue(indices.contains(idx1d));
        }
    }

    @Test
    public void testGetIndicesAround() {
        Grid grid = new Grid(new int[]{3, 3, 3},
                new double[][]{
                        new double[]{-10, 10},
                        new double[]{-10, 10},
                        new double[]{-10, 10},
                });

        int[] indices = grid.getIndicesAround(new int[]{1, 1, 1}, 1);
        System.out.println(Arrays.toString(indices));

        int[] expected = new int[27];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = i;
        }

        Assert.assertTrue(Arrays.equals(indices, expected));
    }

    @Test
    public void testGetIndicesAroundCorner() {
        int[] extents = new int[]{3, 3, 3};
        Grid grid = new Grid(extents,
                new double[][]{
                        new double[]{0, 2},
                        new double[]{0, 2},
                        new double[]{0, 2},
                });

        //get indices around the corner index
        int[] indiciesAround1d = grid.getIndicesAround(new int[]{0, 0, 0}, 1);

        Set<Integer> expectedIndices1d = new HashSet<>();
        expectedIndices1d.add(NDUtil.flattenIndices(new int[]{0, 0, 0}, extents));
        expectedIndices1d.add(NDUtil.flattenIndices(new int[]{1, 0, 0}, extents));
        expectedIndices1d.add(NDUtil.flattenIndices(new int[]{0, 1, 0}, extents));
        expectedIndices1d.add(NDUtil.flattenIndices(new int[]{0, 0, 1}, extents));
        expectedIndices1d.add(NDUtil.flattenIndices(new int[]{1, 1, 0}, extents));
        expectedIndices1d.add(NDUtil.flattenIndices(new int[]{1, 1, 1}, extents));
        expectedIndices1d.add(NDUtil.flattenIndices(new int[]{1, 0, 1}, extents));
        expectedIndices1d.add(NDUtil.flattenIndices(new int[]{0, 1, 1}, extents));

        for (int idx : indiciesAround1d) {
            System.out.println("Checking for idx: " + Arrays.toString(NDUtil.inflateIndex(idx, extents)));
            Assert.assertTrue(expectedIndices1d.contains(idx));
        }

    }

}