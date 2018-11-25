/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agenteedimbrujo;

/**
 *
 * @author Martin
 */
public class Tower {

    public boolean dead;
    public int x;
    public int y;
    public String name;

    public Tower(boolean dead, int x, int y, String name) {
        this.dead = dead;
        this.x = x;
        this.y = y;
        this.name = name;
    }

}
