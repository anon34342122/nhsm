module com.anon.nhsm {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires org.apache.commons.io;
    requires org.apache.logging.log4j;
    requires org.semver4j;

    exports com.anon.nhsm.app;
    opens com.anon.nhsm.app to javafx.fxml;
    exports com.anon.nhsm.data;
    opens com.anon.nhsm.data to javafx.fxml;
    exports com.anon.nhsm;
    opens com.anon.nhsm to javafx.fxml;
    exports com.anon.nhsm.controllers;
    opens com.anon.nhsm.controllers to javafx.fxml;
}