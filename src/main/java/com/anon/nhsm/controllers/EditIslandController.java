package com.anon.nhsm.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ResourceBundle;

public class EditIslandController {
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

    public void setIslandName(final String islandName) {
        this.islandName.setText(islandName);
    }

    public void setIslandDescription(final String islandDescription) {
        this.description.setText(islandDescription);
    }
}
