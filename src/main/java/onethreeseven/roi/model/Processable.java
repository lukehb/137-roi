package onethreeseven.roi.model;

/**
 * Enforces some processing methods.
 * Useful for RoI mining, to check whether this cell has already been added to an RoI/discarded.
 *
 */
public interface Processable {

    void markProcessed();

    void markUnprocessed();

    boolean isProcessed();

}
