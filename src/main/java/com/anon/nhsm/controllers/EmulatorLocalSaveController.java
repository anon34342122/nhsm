package com.anon.nhsm.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class EmulatorLocalSaveController {
    @FXML private TextField islandName;
    @FXML private TextField description;

    public String getIslandName() {
        return islandName.getText();
    }

    public String getIslandDescription() {
        return description.getText();
    }
}
