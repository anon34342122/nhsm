package com.anon.nhsm.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class EditIslandController {
    @FXML private TextField islandName;
    @FXML private TextField description;

    public String getIslandName() {
        return islandName.getText();
    }

    public String getIslandDescription() {
        return description.getText();
    }

    public void setIslandName(final String islandName) {
        this.islandName.setText(islandName);
    }

    public void setIslandDescription(final String islandDescription) {
        this.description.setText(islandDescription);
    }
}
