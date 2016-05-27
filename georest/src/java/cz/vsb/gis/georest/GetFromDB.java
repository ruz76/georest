/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.vsb.gis.georest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author svo0051
 * Problems with Warning:   
 * The web application [unknown] registered the JDBC driver [org.postgresql.Driver] but failed 
 * to unregister it when the web application was stopped. 
 * To prevent a memory leak, the JDBC Driver has been forcibly unregistered.
 * Solution: Undeploy resDB manually from Glassfish
 * 
 */
@Path("getfromdb")
public class GetFromDB {
    
    private final Properties configProp = new Properties();

    @Context
    private UriInfo context;

    /**
     * Creates a new instance of GenericResource
     */
    public GetFromDB() {
      //Private constructor to restrict new instances
      //ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      //InputStream in = classLoader.getResourceAsStream("database.properties");
      //InputStream in = this.getClass().getClassLoader().getResourceAsStream("database.properties");
      /* 
        No idea where to put database.properties - 
        tried classes, tried WEB-INF/lib, and more other 
        so it is placed in /etc/
        */
      /*
        Structure for properties file:
        password=***
        database=***
        user=***
        */  
      System.out.println("Read all properties from file");
      try {
          BufferedReader in = new BufferedReader(new FileReader("/etc/georest/database.properties"));  
          configProp.load(in);
      } catch (IOException e) {
          e.printStackTrace();
      }
    }

    /**
     * Retrieves representation of an instance of Api.GenericResource
     *
     * @param tabulka name of table to read from
     * @param pr_sloupec name of geometry column
     * @param atributy list of attributes in ouput separated by coma
     * @param podminka where condition - I feel there SQL injection problem
     * @return an instance of java.lang.String
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getUserDetailsFromAddress(@QueryParam("tabulka") String tabulka, @QueryParam("pr_sloupec") 
            String pr_sloupec, @QueryParam("atributy") String atributy, @QueryParam("podminka") String podminka) {
        //TODO return proper representation object
        String vysledek = "";
        String dotaz = "";

        if (podminka.equals("nope")) {
            dotaz = "SELECT row_to_json(fc) FROM ( SELECT 'FeatureCollection' As type, array_to_json(array_agg(f)) "
                    + "As features FROM (SELECT 'Feature' As type, ST_AsGeoJSON((lg." + pr_sloupec + "),15,0)::json As geometry, "
                    + "row_to_json((" + atributy + ")) As properties "
                    + "FROM " + tabulka + " As lg) As f ) As fc;";
        } else {
            dotaz = "SELECT row_to_json(fc) FROM ( SELECT 'FeatureCollection' As type, array_to_json(array_agg(f)) "
                    + "As features FROM (SELECT 'Feature' As type, ST_AsGeoJSON((lg." + pr_sloupec + "),15,0)::json As geometry, "
                    + "row_to_json((" + atributy + ")) As properties "
                    + "FROM " + tabulka + " As lg WHERE " + podminka + ") As f ) As fc;";
        }
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            String database = configProp.getProperty("database");
            String user = configProp.getProperty("user");
            String password = configProp.getProperty("password");
            c = DriverManager.getConnection("jdbc:postgresql://" + database, user, password);
            c.setAutoCommit(false);
            stmt = c.createStatement();

            ResultSet rs = stmt.executeQuery(dotaz);
            while (rs.next()) {
                vysledek = (rs.getString(1));
            }

            rs.close();
            stmt.close();
            c.close();
            return vysledek;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * PUT method for updating or creating an instance of GenericResource
     *
     * @param content representation for the resource
     */
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    public void putText(String content) {
    }
}
