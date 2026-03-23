package com.simon.diplom;

import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

public class LineGraphics {
    private Pane startNode;
    private Pane endNode;

    private Circle startMarker;
    private Circle endMarker;

    public LineGraphics(Pane startNode, Circle startMarker) {
        this.startNode = startNode;
        this.startMarker = startMarker;
        endNode = null;
        endMarker = null;
    }

    public LineGraphics(Pane startNode, Pane endNode, Circle startMarker, Circle endMarker) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.startMarker = startMarker;
        this.endMarker = endMarker;
    }

    public Pane getStartNode() {
        return startNode;
    }

    public void setStartNode(Pane startNode) {
        this.startNode = startNode;
    }

    public Pane getEndNode() {
        return endNode;
    }

    public void setEndNode(Pane endNode) {
        this.endNode = endNode;
    }

    public Circle getStartMarker() {
        return startMarker;
    }

    public void setStartMarker(Circle startMarker) {
        this.startMarker = startMarker;
    }

    public Circle getEndMarker() {
        return endMarker;
    }

    public void setEndMarker(Circle endMarker) {
        this.endMarker = endMarker;
    }
}
