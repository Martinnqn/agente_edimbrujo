/*
Para ejecutar:  Ingresar URL y Nombre por parámetro. Ejemplo: 
"java -jar AgenteEdimbrujo.jar http://localhost:8080/Edimbrujo/webservice/server NombreBot"

Movimiento: para moverse, tiene planteado 8 casos considerando la posición del
objetivo (arriba a la izquierda, arriba a la derecha, abajo a la izquierda, etc,
considerando como casos diferentes los casos en que se encuentra en el mismo "x" o "y").

Por cada caso tiene un pool de cuatro acciones de movimiento, ordenado por preferencia 
de mayor a menor, en función de cuál cumple mejor el objetivo. Si no puede utilizar la
de mayor preferencia (por algún obstáculo), prueba con la siguiente. Si no puede
utilizar ninguna, elige una aleatoria del conjunto total de acciones de movimiento.

Para detectar si el movimiento del turno anterior surgió efecto, guarda su posición 
anterior y la compara con la actual. Por ejemplo, cuando elige moverse a una posición,
no tiene información a priori de si otro jugador también quizo moverse a esa posición,
y como el juego no permite esto, los agentes no se moverían de su lugar, pero seguirían
creyendo que están haciendo un movimiento válido (por lo tanto, en el siguiente turno, 
si están en la misma circunstancia, elegirían la misma acción, entrando en deadlock). 
Cuando detecta livelock o deadlock, elige un movimiento aleatorio. 

Guardar su posición anterior también evita que elija un movimiento que lo lleve 
a la situación anterior, y lo obliga a buscar otra alternativa.

Los casos en los que se encuentra en la misma "x" o misma "y" que su objetivo, 
son casos que pueden llevar a hacer ciclos, por esto, para elegir un movimiento 
en estos casos, tiene un 50% de probabilidad de elegir un movimiento que cumple 
parcialmente el objetivo, y otro 50% de probabilidad de elegir un movimiento
completamente aleatorio.

Las primeras tres acciones de cada pool de acciones ayudan a acercarse al objetivo.
La cuarta opción va en contra de las primeras tres, con la intención de obligar al 
agente a buscar un camino alternativo en sentido contrario, si ninguna de las primeras
tres se puede utilizar. Esto hace que el pool sea cohesivo, pero que no se atasque con
obstáculos.
 */
package agenteedimbrujo;

import java.awt.Point;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;
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
    private static LinkedList<Entity> towers = new LinkedList<>();
    private static String nameT;
    private static Player player;
    private static LinkedList<Entity> players = new LinkedList<>();
    private static boolean startGame = false;
    private static boolean jugo = false;
    private static Point posAnterior = new Point(-1, -1);
    private static String[] moves = {"up", "down", "left", "right", "upleft", "upright", "downleft", "downright"};
    private static SecureRandom random = new SecureRandom();

    public static void main(String[] args) throws IOException {
        try {
            if (args.length != 2) {
                System.out.println("Ingresar URL y Nombre por parámetro. ");
                System.out.println("Ejemplo: ");
                System.out.println("java -jar file.jar http://localhost:8080/Edimbrujo/webservice/server NombreBot");
            } else {
                //http://localhost:8080/Edimbrujo/webservice/server
                conexion con = new conexion(args[0]);
                nameT = con.iniciar(args[1]);
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

    /**
     * Cargar los estados estáticos.
     *
     * @throws ParseException
     */
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

    /**
     * Cargar los estados dinámicos.
     *
     * @throws ParseException
     */
    private static void cargarDinamicS() throws ParseException {
        cleanLists();
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(fullState);
        JSONObject dinamicS;
        int i = 0;
        while ((dinamicS = (JSONObject) json.get(i + "")) != null) {
            JSONObject match;
            //System.out.println("Recibe " + dinamicS.toString());
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
                    int width = (int) (long) attrs.get("width");
                    int height = (int) (long) attrs.get("height");
                    int x = (int) (long) ((JSONObject) ((JSONObject) attrs.get("super")).get("Entity")).get("x");
                    int y = (int) (long) ((JSONObject) ((JSONObject) attrs.get("super")).get("Entity")).get("y");
                    towers.add(new Tower(dead, width, height, x, y, id));
                }
            }
            i++;
        }
    }

    private static Point closestTower() {
        Point xy = null;
        int dist = Integer.MAX_VALUE;
        int auxDist;
        for (Entity tower : towers) {
            if (!((Tower) tower).dead) {
                auxDist = Math.abs(((Tower) tower).x - player.x) + Math.abs(((Tower) tower).y - player.y);
                if (auxDist < dist) {
                    dist = auxDist;
                    xy = new Point(((Tower) tower).x, ((Tower) tower).y);
                }
            }
        }
        return xy;
    }

    private static Point closestEnemy() {
        Point xy = null;
        int dist = Integer.MAX_VALUE;
        int auxDist;
        for (Entity pl : players) {
            if (((Player) pl).team != player.team && !((Player) pl).dead) {
                auxDist = (((Player) pl).x - player.x) + (((Player) pl).y - player.y);
                if (auxDist < dist) {
                    dist = auxDist;
                    xy = new Point(((Player) pl).x, ((Player) pl).y);
                }
            }
        }
        return xy;
    }

    /**
     * Devuelve un punto xy si puede dispararle a una torre. Devuelve null si no
     * puede.
     *
     * @return
     */
    private static Point canFireTower() {
        Point xy = null;
        int i = 0;
        int j = 0;
        boolean found = false;
        LinkedList<Point> tw = new LinkedList<>();
        while (i < towers.size() && !found) {
            if (!((Tower) towers.get(i)).dead) {
                tw = ((Tower) towers.get(i)).getArea();
                j = 0;
                while (j < tw.size() && !found) {
                    found = canFire(tw.get(j), new Point(player.x, player.y));
                    j++;
                }
            }
            i++;
        }
        if (found) {
            i--;
            j--;
            xy = tw.get(j);
        }
        return xy;
    }

    /**
     * Devuelve un punto xy si puede dispararle a un enemigo. Devuelve null si
     * no puede.
     *
     * @return
     */
    private static Point canFireEnemy() {
        Point xy = null;
        int i = 0;
        int x;
        int y;
        boolean found = false;
        while (i < players.size() && !found) {
            if (((Player) players.get(i)).team != player.team && !((Player) players.get(i)).dead) {
                x = ((Player) players.get(i)).x;
                y = ((Player) players.get(i)).y;
                found = canFire(new Point(x, y), new Point(player.x, player.y));
            }
            i++;
        }
        if (found) {
            i--;
            xy = new Point(((Player) players.get(i)).x, ((Player) players.get(i)).y);
        }
        return xy;
    }

    /**
     * Devuelve true si el camino entre xy y wz es recto y además no tiene
     * obstáculos.
     *
     * @param xy
     * @param wz
     * @return
     */
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

    /**
     * Calculo el siguiente movimiento para ir hacia xy.
     *
     * @param xy
     * @return
     */
    private static String calcMove(Point xy) {
        String move = "";
        int x = player.x;
        int y = player.y;
        int r = 0;
        Point[] pos = new Point[4];

        String[] movs = new String[4];

        if (xy.x < x && xy.y < y) {
//            System.out.println("Aca 1");
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
        } else if (xy.x < x && xy.y > y) {
//            System.out.println("Aca 2");
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
        } else if (xy.x > x && xy.y < y) {
//            System.out.println("Aca 3");
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
        } else if (xy.x > x && xy.y > y) {
//            System.out.println("Aca 4");
            x++;
            y++;
            pos[0] = new Point(x, y);
            pos[1] = new Point(--x, y);
            pos[2] = new Point(++x, --y);
            pos[3] = new Point(x - 2, y);
            movs[0] = "downright";
            movs[1] = "down";
            movs[2] = "right";
            movs[3] = "left";
        } else if (xy.x == x && xy.y > y) {
//            System.out.println("Aca 5");
            r = random.nextInt(1000000000) % 2;
            if (r == 0) {
                x++;
                y++;
                pos[0] = new Point(x, y);
                pos[1] = new Point(--x, y);
                pos[2] = new Point(++x, --y);
                pos[3] = new Point(x - 2, y);
                movs[0] = "downright";
                movs[1] = "down";
                movs[2] = "right";
                movs[3] = "left";
            } else {
                move = moves[random.nextInt(1000000000) % 8];
            }
        } else if (xy.x == x && xy.y < y) {
//            System.out.println("Aca 6");
            r = random.nextInt(1000000000) % 2;
            if (r == 0) {
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
            } else {
                move = moves[random.nextInt(1000000000) % 8];
            }
        } else if (xy.x > x && xy.y == y) {
//            System.out.println("Aca 7");
            r = random.nextInt(1000000000) % 2;
            if (r == 0) {
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
            } else {
                move = moves[random.nextInt(1000000000) % 8];
            }
        } else if (xy.x < x && xy.y == y) {
//            System.out.println("Aca 8");
            r = random.nextInt(1000000000) % 2;
            if (r == 0) {
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
            } else {
                move = moves[random.nextInt(1000000000) % 8];
            }
        }

        int i = 1;
        if (r == 0) {
            move = movs[0];
            while (i < movs.length && (!canWalk(pos[i - 1])
                    || (pos[i - 1].x == posAnterior.x && pos[i - 1].y == posAnterior.y))) {
                move = movs[i];
                i++;
            }
            //si la ultima accion es en la misma posicion donde estaba en el turno anterior, o la posicion actual
            //es la misma donde estaba el turno anterior, elige una random
            if ((pos[i - 1].x == posAnterior.x && pos[i - 1].y == posAnterior.y)
                    || (player.x == posAnterior.x && player.y == posAnterior.y)) {
                move = moves[random.nextInt(1000000000) % 8];
            }
        }

        posAnterior = new Point(player.x, player.y);

        System.out.println("elige " + move + " " + pos[i - 1] + ".");
        return move;
    }

    /**
     * Limpia las listas cargadas con elementos dinamicos.
     */
    private static void cleanLists() {
        players.clear();
        towers.clear();
    }

    /**
     * Devuelve true si xy es caminable (segun el mapa), y si no tiene
     * obstáculos. Torres y players son considerados obstáculos.
     *
     * @param xy
     * @return
     */
    private static boolean canWalk(Point xy) {
        boolean res = mapa.canWalk(xy);
        if (res) {
            for (Entity tower : towers) {
                res = res && !tower.colision(xy);
            }
        }

        if (res) {
            for (Entity pl : players) {
                res = res && !pl.colision(xy);
            }
        }
        return res;
    }
}
