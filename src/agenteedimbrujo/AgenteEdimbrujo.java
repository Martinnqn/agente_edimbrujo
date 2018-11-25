/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agenteedimbrujo;

import java.awt.Point;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author joan
 */
public class AgenteEdimbrujo {

    /**
     * @param args the command line arguments
     */
    private static String fullStatic;
    private static String fullState;
    private static Mapa mapa;
    private static LinkedList<Spawn> spawns = new LinkedList<>();
    private static LinkedList<Tower> towers = new LinkedList<>();
    private static String nameT;
    private static Player player;
    private static LinkedList<Player> players = new LinkedList<>();
    private static boolean startGame = false;

    public static void main(String[] args) throws IOException {
        try {
            conexion con = new conexion("http://localhost:8080/Edimbrujo/webservice/server");
            String[] move = {"up", "down", "left", "right", "upleft", "upright", "downleft", "downright"};
            nameT = con.iniciar("__-__");
            System.out.println(nameT);
            fullStatic = con.getFullStaticState();
            cargarStaticS();
            con.makeAction("ready");
            Accion acc;
            //fullState = con.getFullState();
            while (true) {
                fullState = con.getFullState();
                //System.out.println("full " + fullState);
                cargarDinamicS();
                if (startGame && player != null && !player.dead) {
                    acc = selectAction();
                    if (acc != null) {
                        switch (acc.nombre) {
                            case "move":
                                con.makeAction(acc.accion);
                                break;
                            case "fire":
                                con.makeRangeAtack(acc.accion);
                                break;
                        }
                    }
                }
            }
        } catch (ParseException ex) {
            Logger.getLogger(AgenteEdimbrujo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static Accion selectAction() {
        Accion res = null;
        //si ataca
        if (player.role == 1) {
            Point xy = canFireTower();
            if (xy != null) {
                res = new Accion("fire", "x=" + xy.x + "&y=" + xy.y);
            } else {
                xy = closestTower();
                //calcular movimiento
                if (xy != null) {
                    String dir = calcMove(xy);
                    res = new Accion("move", dir);
                }
            }
        } else {
            //si defiende
            Point xy = canFireEnemy();
            if (xy != null) {
                res = new Accion("fire", "x=" + xy.x + "&y=" + xy.y);
            } else {
                xy = closestEnemy();
                //calcular movimiento
                if (xy != null) {
                    String dir = calcMove(xy);
                    res = new Accion("move", dir);
                }
            }
        }
        return res;
    }

    private static void cargarStaticS() throws ParseException {
        HashMap<Point, Integer> cells = new HashMap<>();
        int width;
        int height;
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(fullStatic);
        JSONObject staticS;
        int i = 0;
        while ((staticS = (JSONObject) json.get(i + "")) != null) {
            JSONObject attrs;
            if ((attrs = (JSONObject) staticS.get("Map")) != null) {
                JSONArray cell = ((JSONArray) attrs.get("cells"));
                for (int j = 0; j < cell.size(); j++) {
                    //System.out.println("cell " + cell.get(j).toString());
                    JSONObject at = (JSONObject) cell.get(j);
                    cells.put(new Point((int) (long) at.get("x"), (int) (long) at.get("y")), (int) (long) at.get("val"));
                }
                width = ((int) (long) attrs.get("width"));
                height = ((int) (long) attrs.get("height"));
                mapa = new Mapa(cells, width, height);
            } else if ((attrs = (JSONObject) staticS.get("Spawn")) != null) {
                //System.out.println("Spawn " + attrs.toString());
                int x = (int) (long) attrs.get("x");
                int y = (int) (long) attrs.get("y");
                String name = ((JSONObject) ((JSONObject) attrs.get("super")).get("State")).get("name").toString();
                String destr = ((JSONObject) ((JSONObject) attrs.get("super")).get("State")).get("destroy").toString();
                spawns.add(new Spawn(x, y, name, destr));
            }
            i++;
        }
    }

    private static void cargarDinamicS() throws ParseException {
        cleanLists();
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(fullState);
        JSONObject dinamicS;
        int i = 0;
        while ((dinamicS = (JSONObject) json.get(i + "")) != null) {
            JSONObject match;
            System.out.println("Recibe " + dinamicS.toString());
            if ((match = (JSONObject) dinamicS.get("Match")) != null) {
                startGame = (boolean) match.get("startGame");
            }

            if (startGame) {
                JSONObject attrs;
                //players
                if ((attrs = (JSONObject) dinamicS.get("Player")) != null) {
                    String id = attrs.get("id").toString();
                    if (id.equals(nameT)) {
                        int role = (int) (long) attrs.get("role");
                        boolean dead = (boolean) attrs.get("dead");
                        int team = (int) (long) attrs.get("team");
                        int x = (int) (long) ((JSONObject) ((JSONObject) attrs.get("super")).get("Entity")).get("x");
                        int y = (int) (long) ((JSONObject) ((JSONObject) attrs.get("super")).get("Entity")).get("y");
                        player = new Player(dead, team, role, x, y);
                    } else {
                        int role = (int) (long) attrs.get("role");
                        boolean dead = (boolean) attrs.get("dead");
                        int team = (int) (long) attrs.get("team");
                        int x = (int) (long) ((JSONObject) ((JSONObject) attrs.get("super")).get("Entity")).get("x");
                        int y = (int) (long) ((JSONObject) ((JSONObject) attrs.get("super")).get("Entity")).get("y");
                        players.add(new Player(dead, team, role, x, y));
                    }
                }
                //torres
                if ((attrs = (JSONObject) dinamicS.get("Tower")) != null) {
                    String id = attrs.get("id").toString();
                    boolean dead = (boolean) attrs.get("dead");
                    int x = (int) (long) ((JSONObject) ((JSONObject) attrs.get("super")).get("Entity")).get("x");
                    int y = (int) (long) ((JSONObject) ((JSONObject) attrs.get("super")).get("Entity")).get("y");
                    towers.add(new Tower(dead, x, y, id));
                }
            }
            i++;
        }
    }

    private static Point closestTower() {
        Point xy = null;
        int dist = 80000;
        int auxDist;
        for (Tower tower : towers) {
            if (!tower.dead) {
                auxDist = Math.abs(tower.x - player.x) + Math.abs(tower.y - player.y);
                if (auxDist < dist) {
                    dist = auxDist;
                    xy = new Point(tower.x, tower.y);
                }
            }
        }
        return xy;
    }

    private static Point closestEnemy() {
        Point xy = null;
        int dist = 80000;
        int auxDist;
        for (Player pl : players) {
            if (pl.team != player.team && !pl.dead) {
                auxDist = (pl.x - player.x) + (pl.y - player.y);
                if (auxDist < dist) {
                    dist = auxDist;
                    xy = new Point(pl.x, pl.y);
                }
            }
        }
        return xy;
    }

    private static Point canFireTower() {
        Point xy = null;
        int i = 0;
        int j = 0;
        int x;
        int y;
        boolean found = false;
        while (i < towers.size() && !found) {
            x = towers.get(i).x;
            y = towers.get(i).y;
            found = canFire(new Point(x, y), new Point(player.x, player.y));
            i++;
        }
        if (found) {
            i--;
            xy = new Point(towers.get(i).x, towers.get(i).y);
        }
        return xy;
    }

    private static Point canFireEnemy() {
        Point xy = null;
        int i = 0;
        int x;
        int y;
        boolean found = false;
        while (i < players.size() && !found) {
            if (players.get(i).team != player.team && !players.get(i).dead) {
                x = players.get(i).x;
                y = players.get(i).y;
                found = canFire(new Point(x, y), new Point(player.x, player.y));
            }
            i++;
        }
        if (found) {
            i--;
            xy = new Point(players.get(i).x, players.get(i).y);
        }
        return xy;
    }

    private static boolean canFire(Point xy, Point wz) {
        boolean flag = true;
        boolean found = false;
        int menorX = (xy.x < wz.x) ? xy.x : wz.x;
        int menorY = (xy.y < wz.y) ? xy.y : wz.y;
        if (xy.x == wz.x) {
            int distY = Math.abs(xy.y - player.y);
            int k = 0;
            while (flag && k < distY) {
                flag = mapa.canWalk(new Point(xy.x, menorY + k));
                k++;
            }
            found = (k == distY && flag);
        } else if (xy.y == wz.y) {
            int distX = Math.abs(xy.x - wz.x);
            int k = 0;
            while (flag && k < distX) {
                flag = mapa.canWalk(new Point(menorX + k, xy.y));
                k++;
            }
            found = (k == distX && flag);
        } else if (xy.x != wz.x && xy.y != wz.y) {
            int distX = Math.abs(xy.x - wz.x);
            int distY = Math.abs(xy.y - wz.y);
            if (distX == distY) {
                int k = 0;
                while (flag && k < distX) {
                    flag = mapa.canWalk(new Point(menorX + k, menorY + k));
                    k++;
                }
                found = (k == distX && flag);
            }
        }
        return found;
    }

    private static String calcMove(Point xy) {
        String move = "";
        int x = player.x;
        int y = player.y;
        Point[] pos = new Point[4];
        String[] movs = new String[4];
        movs[0] = "upleft";
        movs[1] = "up";
        movs[2] = "left";
        movs[2] = "right";
        if (xy.x < x && xy.y < y) {
            System.out.println("Aca 1");
            x--;
            y--;
            pos[0] = new Point(x, y);
            pos[1] = new Point(++x, y);
            pos[2] = new Point(--x, ++y);
            pos[3] = new Point(x, ++y);
            movs[0] = "upleft";
            movs[1] = "up";
            movs[2] = "left";
            movs[3] = "downleft";
        } else if (xy.x < x && xy.y > y) {
            System.out.println("Aca 2");
            x--;
            y++;
            pos[0] = new Point(x, y);
            pos[1] = new Point(++x, y);
            pos[2] = new Point(--x, --y);
            pos[3] = new Point(x, --y);
            movs[0] = "downleft";
            movs[1] = "down";
            movs[2] = "left";
            movs[3] = "upleft";
        } else if (xy.x > x && xy.y < y) {
            System.out.println("Aca 3");
            x++;
            y--;
            pos[0] = new Point(x, y);
            pos[1] = new Point(--x, y);
            pos[2] = new Point(++x, ++y);
            pos[3] = new Point(x, --y);
            movs[0] = "upright";
            movs[1] = "up";
            movs[2] = "right";
            movs[3] = "downright";

        } else if (xy.x > x && xy.y > y) {
            System.out.println("Aca 4");
            x++;
            y++;
            pos[0] = new Point(x, y);
            pos[1] = new Point(--x, y);
            pos[2] = new Point(++x, --y);
            pos[3] = new Point(x, --y);
            movs[0] = "downright";
            movs[1] = "down";
            movs[2] = "right";
            movs[3] = "upright";
        } else if (xy.x == x && xy.y > y) {
            System.out.println("Aca 5");
            x--;
            y++;
            pos[0] = new Point(x, y);
            pos[1] = new Point(++x, y);
            pos[2] = new Point(--x, --y);
            pos[3] = new Point(x + 2, y);
            movs[0] = "downleft";
            movs[1] = "down";
            movs[2] = "left";
            movs[3] = "right";
        } else if (xy.x == x && xy.y < y) {
            System.out.println("Aca 6");
            x--;
            y--;
            pos[0] = new Point(x, y);
            pos[1] = new Point(++x, y);
            pos[2] = new Point(--x, ++y);
            pos[3] = new Point(x + 2, y);
            movs[0] = "upleft";
            movs[1] = "up";
            movs[2] = "left";
            movs[3] = "right";
        } else if (xy.x > x && xy.y == y) {
            System.out.println("Aca 7");
            x++;
            y--;
            pos[0] = new Point(x, y);
            pos[1] = new Point(--x, y);
            pos[2] = new Point(++x, ++y);
            pos[3] = new Point(x - 2, y);
            movs[0] = "upright";
            movs[1] = "up";
            movs[2] = "right";
            movs[3] = "left";
        } else if (xy.x < x && xy.y == y) {
            System.out.println("Aca 8");
            x--;
            y--;
            pos[0] = new Point(x, y);
            pos[1] = new Point(++x, y);
            pos[2] = new Point(--x, ++y);
            pos[3] = new Point(x + 2, y);
            movs[0] = "upleft";
            movs[1] = "up";
            movs[2] = "left";
            movs[3] = "right";
        }

        int i = 1;
        move = movs[0];
        while (i < movs.length && !mapa.canWalk(pos[i - 1])) {
            System.out.println(move + " en " + pos[i - 1] + " no se puede.");
            move = movs[i];
            i++;
        }
        System.out.println("elige " + move + " " + pos[i - 1] + ".");
        return move;
    }

    private static void cleanLists() {
        players.clear();
    }

}

//
// 
