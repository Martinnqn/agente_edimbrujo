/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agenteedimbrujo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author joan
 */
public class conexion {

    String servidor;
    URL url;
    HttpURLConnection conn;
    String token;

    public conexion(String s) {
        servidor = s;
    }

    //Metodo que permite conectarse al servidor y obtiene el token como respuesta del mismo, el token solo sirve para conexiones
    public String iniciar(String user) throws MalformedURLException, IOException {
        token = "";

        url = new URL(servidor + "/enter?rol=" + user);
        System.out.println("Inicia Conexión");
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed: HTTP error code : " + conn.getResponseCode());
        }
        System.out.println("iniciando...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        token = reader.readLine();

        System.out.println(token);
        System.out.println("Inicio con Exito");

        return token;
    }

    public String getFullState() throws MalformedURLException, IOException {
        url = new URL(servidor + "/getFullState");
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed: HTTP error code : " + conn.getResponseCode());
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String res = reader.readLine();
        return res;
    }

    public String getFullStaticState() throws MalformedURLException, IOException {
        url = new URL(servidor + "/getFullStaticState");
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed: HTTP error code : " + conn.getResponseCode());
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String res = reader.readLine();
        System.out.println(token);
        return res;
    }

    public String makeAction(String action) throws MalformedURLException, IOException {
        url = new URL(servidor + "/action?action=" + action + "&session=" + token);
        System.out.println(url.toString());
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed: HTTP error code : " + conn.getResponseCode());
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String res = reader.readLine();
        System.out.println(token);
        return res;
    }

    public String makeRangeAtack(String x, String y) throws MalformedURLException, IOException {
        url = new URL(servidor + "/actionFire?x=" + x + "&y=" + y + "&session=" + token);
        System.out.println(url.toString());
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed: HTTP error code : " + conn.getResponseCode());
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String res = reader.readLine();
        System.out.println(token);
        return res;
    }

    public String makeRangeAtack(String acc) throws MalformedURLException, IOException {
        url = new URL(servidor + "/actionFire?" + acc + "&session=" + token);
        //System.out.println(url.toString());
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed: HTTP error code : " + conn.getResponseCode());
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String res = reader.readLine();
        //System.out.println(token);
        return res;
    }
}
