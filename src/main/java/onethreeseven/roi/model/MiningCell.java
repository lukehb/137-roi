package onethreeseven.roi.model;

/**
 * A cell that has both density and state (processed or not) tracked.
 * @author Luke Bermingham
 */
public class MiningCell extends DensityCell implements Processable {

    private boolean processed = false;

    public MiningCell(int index) {
        super(index);
    }

    @Override
    public void markProcessed() {
        processed = true;
    }

    @Override
    public void markUnprocessed() {
        processed = false;
    }

    @Override
    public boolean isProcessed() {
        return processed;
    }
}
