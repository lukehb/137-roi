import onethreeseven.roi.view.RoIMenuSupplier;
import onethreeseven.trajsuitePlugin.model.EntitySupplier;

module onethreeseven.roi{
    requires onethreeseven.datastructures;
    requires onethreeseven.common;
    requires onethreeseven.collections;
    requires java.desktop;

    exports onethreeseven.roi.model;
    exports onethreeseven.roi.graphics;
    exports onethreeseven.roi.algorithm;
    exports onethreeseven.roi.view;
    exports onethreeseven.roi.view.controller;

    //for resources
    opens onethreeseven.roi.view;

    //for javafx
    exports onethreeseven.roi to javafx.graphics;

    //for menus services
    provides onethreeseven.trajsuitePlugin.view.MenuSupplier with RoIMenuSupplier;

    //for getting entities
    uses EntitySupplier;

}