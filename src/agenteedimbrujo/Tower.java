/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agenteedimbrujo;

import java.awt.Point;
import java.util.LinkedList;

/**
 *
 * @author Martin
 */
public class Tower {

    public boolean dead;
    public int width;
    public int height;
    public int x;
    public int y;
    public String name;

    public Tower(boolean dead, int width, int height, int x, int y, String name) {
        this.dead = dead;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.name = name;
    }

    public LinkedList<Point> getArea() {
        LinkedList<Point> area = new LinkedList<>();
        int cellX = x - width / 2;
        int cellY = y - height / 2;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                area.add(new Point(cellX, cellY));
                cellY++;
            }
            cellY = y - height / 2;
            cellX++;
        }
        return area;
    }

    public boolean colision(Point xy) {
        boolean res = false;
        int i = 0;
        LinkedList<Point> area = getArea();
        while (i < area.size() && !res) {
            if (area.get(i).x == xy.x && area.get(i).y == xy.y) {
                res = true;
            }
            i++;
        }
        return res;
    }

}
