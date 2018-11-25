package agenteedimbrujo;

import java.awt.Point;
import java.util.HashMap;

public class Mapa {

    private HashMap<Point, Integer> cells;
    private int width;
    private int height;

    public Mapa(HashMap<Point, Integer> cells, int width, int height) {
        this.cells = cells;
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean canWalk(Point xy) {
        boolean res;
        res = (cells.containsKey(xy) && cells.get(xy) == 1);
        return res;
    }
}