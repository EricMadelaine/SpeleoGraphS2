/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cds06.speleograph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import static java.net.URLEncoder.encode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.logging.Level;
import org.cds06.speleograph.data.fileio.FileReadingError;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Léa
 */
@SuppressWarnings("FieldCanBeLocal")
public class Series_de_Mesure {
    /**
    * Logger for debug and errors.
    */
    @NonNls
    private static final Logger log = LoggerFactory.getLogger(FormPanel.class);
    
    public URL url;
    public String nom_de_fichier;
    public Date date_debut;
    public Date date_fin;
    public String mesure;
    public String capteur;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    public Series_de_Mesure(String url, String dd, String df, String mesure, String capteur) {
        String[] s=url.split("/");
        this.nom_de_fichier = s[s.length-1];
        String[] s1 = url.split(".csv");
        s1[0] = s1[0] + ".csv";
        url=s1[0];
        log.debug("URL: " +url);
        try {
            this.url = new URL(url.replaceAll(" ", "%20"));
        } catch (MalformedURLException ex) {
            java.util.logging.Logger.getLogger(Series_de_Mesure.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            date_debut = dateFormat.parse(dd);
            date_fin = dateFormat.parse(df);
            //log.debug("date :" +d);
        } catch (ParseException e) {
            log.error("Can not parse a date", e);
            try {
                throw new FileReadingError(
                        I18nSupport.translate("error.canNotReadDate"),
                        FileReadingError.Part.DATA,
                        e
                );
            } catch (FileReadingError ex) {
                java.util.logging.Logger.getLogger(Series_de_Mesure.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.mesure=mesure;
        this.capteur=capteur;
    }
    
    
}
