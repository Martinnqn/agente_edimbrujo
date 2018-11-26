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
public abstract class Entity {
    
    /**
     * retorna true si hay colision
     * @param xy
     * @return 
     */
    public abstract boolean colision(Point xy);
}
