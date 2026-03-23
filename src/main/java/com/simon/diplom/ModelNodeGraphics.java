package com.simon.diplom;

import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;

public class ModelNodeGraphics {
    private ModelNode node;
    private double offsetX;
    private double offsetY;
    private List<Line> inputLinks;
    private List<Line> outputLinks;


    public ModelNodeGraphics(ModelNode node, double offsetX, double offSetY) {
        this.node = node;
        this.offsetX = offsetX;
        this.offsetY = offSetY;
        this.inputLinks = new ArrayList<>();
        this.outputLinks = new ArrayList<>();
    }

    public ModelNode getNode() {
        return node;
    }

    public void setNode(ModelNode node) {
        this.node = node;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public List<Line> getInputLinks() {
        return inputLinks;
    }

    public void setInputLinks(List<Line> inputLinks) {
        this.inputLinks = inputLinks;
    }

    public List<Line> getOutputLinks() {
        return outputLinks;
    }

    public void setOutputLinks(List<Line> outputLinks) {
        this.outputLinks = outputLinks;
    }

    public void addInputLink(Line link){
        inputLinks.add(link);
    }
    public void addOutputLink(Line link){
        outputLinks.add(link);
    }
    public void removeLink(Line link){
        if(inputLinks.contains(link)) inputLinks.remove(link);
        else outputLinks.remove(link);
    }
}
