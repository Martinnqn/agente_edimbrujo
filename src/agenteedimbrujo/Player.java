/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agenteedimbrujo;

import java.awt.Point;

/**
 *
 * @author Martin
 */
public class Player extends Entity {

    public boolean dead;
    public int team;
    public int role;
    public int x;
    public int y;

    public Player(boolean dead, int team, int role, int x, int y) {
        this.dead = dead;
        this.team = team;
        this.role = role;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean colision(Point xy) {
        return (xy.x == x && xy.y == y);
    }

}
