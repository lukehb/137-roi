package onethreeseven.roi.model;


/**
 * A cell - it keeps track of its grid position as an n-dimensional index.
 * @author Luke Bermingham
 */
public interface NdCell {

    /**
     * @return Get the n-dimensional indices
     */
    int[] getIndices();

}


