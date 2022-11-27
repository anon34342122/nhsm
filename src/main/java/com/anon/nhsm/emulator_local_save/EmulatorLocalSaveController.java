package com.anon.nhsm.emulator_local_save;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ResourceBundle;

public class EmulatorLocalSaveController {
    @FXML private AnchorPane ap;
    @FXML private URL location;
    @FXML private ResourceBundle resources;
    @FXML private TextField islandName;
    @FXML private TextField description;

    public String getIslandName() {
        return islandName.getText();
    }

    public String getIslandDescription() {
        return description.getText();
    }

    public void initialize() {

    }
}
