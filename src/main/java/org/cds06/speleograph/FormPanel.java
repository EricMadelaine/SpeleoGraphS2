/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cds06.speleograph;

import javax.swing.JPanel;
import org.jfree.chart.ChartMouseListener;
import org.jfree.data.general.DatasetChangeListener;
import org.apache.commons.lang3.Validate;
import org.cds06.speleograph.data.Series;
import org.cds06.speleograph.graph.DateAxisEditor;
import org.cds06.speleograph.graph.SpeleoXYPlot;
import org.cds06.speleograph.graph.ValueAxisEditor;
import org.jetbrains.annotations.NonNls;
import org.jfree.chart.*;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.AxisEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import static java.lang.Double.parseDouble;
import static java.lang.Math.toRadians;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Léa
 */
@SuppressWarnings("FieldCanBeLocal")
public class FormPanel extends JPanel implements DatasetChangeListener, ChartMouseListener,ActionListener, ItemListener {
    
    /**
     * Logger for debug and errors.
     */
    @NonNls
    private static final Logger log = LoggerFactory.getLogger(FormPanel.class);
    
    /**
     * Instance of SpeleoGraph linked to this Graph.
     */
    private final SpeleoGraphApp application;
    
    private JCheckBox checkid = new JCheckBox("Id");
    private JCheckBox checkname = new JCheckBox("Name");
    private JCheckBox checklocation = new JCheckBox("Localisation");
    
    private JPanel panelid;
    private JPanel panelname;
    private JPanel panellocation;
    private JPanel panelontologie;
    private JTextField saisieontologie = new JTextField(100);
    private JTextField saisieid = new JTextField(100);
    private JTextField saisiename = new JTextField(100);
    private JTextField saisielatitude = new JTextField(20);
    private JTextField saisielongitude = new JTextField(20);
    private JTextField saisierayon = new JTextField(10);
    private ArrayList list_result;
    
    private JPanel research;
    private JPanel fenetre;
   
    private JButton button;
    
    List<Caves> list_caves = new ArrayList();
    
    private String lieu_ontologie;
    
    public SpeleoGraphApp getApplication() {
        return application;
    }
    
    
    /**
     * Construct a new FormPanel for an application instance.
     *
     * @param app The instance which should be linked with this Chart.
     * @param lieu_ontologie lieu de l'ontologie url ou chemin de dossiers
     */
    
    public FormPanel(SpeleoGraphApp app, String lieu_ontologie) {
        this.lieu_ontologie=lieu_ontologie;
        Validate.notNull(app);
        application = app;
        //add an action listener for the checkbox
        checkid.addActionListener(this);
        checkname.addActionListener(this);
        checklocation.addActionListener(this);
        
        list_result = new ArrayList();
        //construction des jpanel des champs de selection
        panelontologie = new JPanel();
        panelid = new JPanel();
        panelname = new JPanel();
        panellocation = new JPanel();
        JLabel labelontologie = new JLabel("Rentrer ou se situe l'ontologie et ses instances (ex: C:/Desktop/karstlinkS2.owl):");
        saisieontologie.setText(lieu_ontologie);
        JLabel labelid = new JLabel("Saisir l'id de la cavité:");
        JLabel labelname = new JLabel("Saisir le nom de la cavité:");
        JLabel labellocation = new JLabel("Saisir les coordonnées du centre de la recherche (latitude,longitude) (en dégrès decimaux) :");
        JLabel labelrayon = new JLabel("Saisir le rayon du carré (km) :");        
        
        panelontologie.add(labelontologie);
        panelontologie.add(saisieontologie);
        panelid.add(labelid);
        panelid.add(saisieid);
        panelid.setVisible(false);
        panelname.add(labelname);
        panelname.add(saisiename);
        panelname.setVisible(false);
        panellocation.add(labellocation);
        panellocation.add(saisielatitude);
        panellocation.add(saisielongitude);
        panellocation.add(labelrayon);
        panellocation.add(saisierayon);
        panellocation.setVisible(false);
        
        JPanel panel_button = new JPanel();
        button = new JButton("Rechercher");
        button.addActionListener(this);
        panel_button.add(button);
        
        JPanel choise_selection = new JPanel();
        research =new JPanel();
        research.setLayout(new GridLayout(4, 1));
        research.add(panelontologie);
        research.add(panelid);
        research.add(panelname);
        research.add(panellocation);
        setLayout(new BorderLayout());
        choise_selection.add(checkid);
        choise_selection.add(checkname);
        choise_selection.add(checklocation);
        fenetre=new JPanel();
        fenetre.setLayout(new BorderLayout());
        fenetre.add(choise_selection, BorderLayout.NORTH);
        fenetre.add(research,BorderLayout.CENTER);
        fenetre.add(panel_button, BorderLayout.SOUTH);
        add(fenetre);
        
        log.info("FormPanel is initialized");
    }

    @Override
    public void datasetChanged(DatasetChangeEvent dce) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent cme) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent cme) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if(source==button){
            list_result.clear();
            log.debug("button selectionné");
            if(checkid.isSelected()){
                log.debug("text id: "+saisieid.getText());
                try {
                    lieu_ontologie=saisieontologie.getText();
                    String id = saisieid.getText();
                    RequestHTTP req = new RequestHTTP("https://api.grottocenter.org/api/v1/entrances/"+id);
                    list_caves = req.get_id();
                    if(list_caves.isEmpty()){
                        JOptionPane.showMessageDialog(this, "Nous avons trouvé aucune cavité avec la recherche effectuée","avertissement",JOptionPane.WARNING_MESSAGE);
                    }
                    else{
                        //creation panel tableau avec check avec un bouton
                        remove(fenetre);
                        repaint();
                        TablePanel table = new TablePanel(application, list_caves, lieu_ontologie);
                        //affichage
                        add(table);
                    }
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(FormPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if(checkname.isSelected()){
                log.debug("text name: "+saisiename.getText());
                try {
                    //doownload csv sur internet
                    /*Series_de_Mesure sm = new Series_de_Mesure("http://www-sop.inria.fr/agos-sophia/sis/KarstLinkS2/TimeSeries/Beget/Beget Ref 9juillet19-27fev20.csv");
                    sm.downloadCSV();*/
                    lieu_ontologie=saisieontologie.getText();
                    RequestHTTP req = new RequestHTTP("https://api.grottocenter.org/api/v1/advanced-search?complete=false&resourceType=entrances&name="+saisiename.getText()+"&from=0&size=30");
                    list_caves = req.post();
                    if(list_caves.isEmpty()){
                        JOptionPane.showMessageDialog(this, "Nous avons trouvé aucune cavité avec la recherche effectuée","avertissement",JOptionPane.WARNING_MESSAGE);
                    }
                    else{
                        //creation panel tableau avec check avec un bouton
                        remove(fenetre);
                        repaint();
                        TablePanel table = new TablePanel(application, list_caves, lieu_ontologie);
                        //affichage
                        add(table);
                    }
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(FormPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if(checklocation.isSelected()){
                lieu_ontologie=saisieontologie.getText();
                log.debug("latitude: "+saisielatitude.getText()+" longitude: "+saisielongitude.getText() +"rayon: "+saisierayon.getText());
                String[] result = find_interval(saisielatitude.getText(), saisielongitude.getText(),saisierayon.getText());
                try {
                    RequestHTTP req = new RequestHTTP("https://api.grottocenter.org/api/v1/geoloc/entrances?sw_lat="+result[0]+"&sw_lng="+result[1]+"&ne_lat="+result[2]+"&ne_lng="+result[3]+"&from=0&size=30");
                    list_caves = req.get();
                    if(list_caves.isEmpty()){
                        JOptionPane.showMessageDialog(this, "Nous avons trouvé aucune cavité avec la recherche effectuée","avertissement",JOptionPane.WARNING_MESSAGE);
                    }
                    else{
                        //creation panel tableau avec check avec un bouton
                        remove(fenetre);
                        repaint();
                        TablePanel table = new TablePanel(application, list_caves, lieu_ontologie);
                        //affichage
                        add(table);
                    }
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(FormPanel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if (source == checkid && checkid.isSelected()) {
            panelid.setVisible(true);
            log.debug("Checkbox id is clicked");
            panelid.revalidate();
            panelid.repaint();
        } 
        else if (source == checkid && !checkid.isSelected()){
            panelid.setVisible(false);
            panelid.revalidate();
            panelid.repaint();
        }
        if (source == checkname && checkname.isSelected()) {
            panelname.setVisible(true);
            log.debug("Checkbox name is clicked");
            panelname.revalidate();
            panelname.repaint();
        } 
        else if (source == checkname && !checkname.isSelected()){
            panelname.setVisible(false);
            panelname.revalidate();
            panelname.repaint();
        }
        if (source == checklocation && checklocation.isSelected()) {
            panellocation.setVisible(true);
            log.debug("Checkbox location is clicked");
            panellocation.revalidate();
            panellocation.repaint();
        }
        else if (source == checklocation && !checklocation.isSelected()){
            panellocation.setVisible(false);
            panellocation.revalidate();
            panellocation.repaint();
        }
        
        
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    private String[] find_interval(String latitude, String longitude, String rayon) {
        double lat, lon, ray ;
        lat=parseDouble(latitude);
        lon=parseDouble(longitude);
        ray=parseDouble(rayon);
        String[] result = new String[4];
        
        double degres_latitude = 111.11;
        double degres_longitude = degres_latitude * Math.cos(toRadians(lat));
        
        log.debug("°lat:"+degres_latitude+" °long:"+degres_longitude);
        
        double interval_latitude = ray/degres_latitude;
        double interval_longitude = ray/degres_longitude;
        
        log.debug("int_lat:"+interval_latitude+" int_long:"+interval_longitude);
        double sw_lat = lat-interval_latitude;
        double sw_long = lon-interval_longitude;
        double ne_lat = lat+interval_latitude;
        double ne_long = lon+interval_longitude;
        
        
        result[0] = Double.toString(sw_lat);
        result[1] = Double.toString(sw_long);
        result[2] = Double.toString(ne_lat);
        result[3] = Double.toString(ne_long);
        
        log.debug("result : "+ Arrays.toString(result));
        
        return result;
    }
    
}



