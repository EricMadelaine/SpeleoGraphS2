/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cds06.speleograph;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NonNls;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Léa
 */
@SuppressWarnings("FieldCanBeLocal")
public class Table2Panel extends JPanel implements DatasetChangeListener, ChartMouseListener,ActionListener, ItemListener {

    /**
     * Logger for debug and errors.
     */
    @NonNls
    private static final Logger log = LoggerFactory.getLogger(FormPanel.class);
    /**
     * Instance of SpeleoGraph linked to this Graph.
     */
    private final SpeleoGraphApp application;
    
    public SpeleoGraphApp getApplication() {
        return application;
    }
    
    List<Series_de_Mesure> list_mesure = new ArrayList();
    
    List<String> list_url_selected = new ArrayList();
    
    private MesuresTable my_table;
    
    private JPanel fenetre;
    private JPanel panel_button;
    private JPanel panel_table;
    
    private JButton button;
    private JButton button_return;
    
    private String lieu_ontologie;
    
    /**
     * Construct a new TablePanel for an application instance.
     *
     * @param app The instance which should be linked with this Chart.
     * @param list_caves
     */
    
    public Table2Panel(SpeleoGraphApp app, List list_mesure, String lieu_ontologie) {
        this.lieu_ontologie = lieu_ontologie;
        Validate.notNull(app);
        application = app;
        
        this.list_mesure= list_mesure;
        
        
        JPanel titre = new JPanel();
        button_return = new JButton("Retour");
        button_return.addActionListener(this);
        JLabel labelHead = new JLabel("Liste des mesures");
        labelHead.setFont(new Font("Arial",Font.TRUETYPE_FONT,20));
        titre.add(labelHead);
        titre.add(button_return);
        
        panel_table = new JPanel();
        my_table = new MesuresTable(this.list_mesure);
        my_table.setVisible(true);
        
        panel_table.add(my_table);
        panel_table.setVisible(true);
        
        panel_button = new JPanel();
        button = new JButton("Télécharger les fichiers!");
        button.addActionListener(this);
        panel_button.add(button);
        
        
        fenetre=new JPanel();
        fenetre.setLayout(new BorderLayout());
        fenetre.add(titre,BorderLayout.NORTH);
        fenetre.add(panel_table,BorderLayout.CENTER);
        fenetre.add(panel_button, BorderLayout.SOUTH);
        add(fenetre);
        
        log.info("TablePanel2 is initialized");
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
        if(source==button_return){
            remove(fenetre);
            repaint();
            FormPanel form = new FormPanel(application, lieu_ontologie);
            add(form);
        }
        if(source==button){
            list_url_selected = my_table.get_url_selected();
            
            for(String s : list_url_selected){
                log.debug(s);
                downloadCSV(s);
            }
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void downloadCSV(String url) {
        url = url.replaceAll("%20"," ");
        String[] s=url.split("/");
        String nom_de_fichier = s[s.length-1];
        try(InputStream is = new URL(url).openConnection().getInputStream()) {			
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));

            StringBuilder builder = new StringBuilder();
            for(String line = reader.readLine(); line != null; line = reader.readLine()) {
              builder.append(line + "\n");
            }
            String fileName = "./fichiersCSV";
            Path path = Paths.get(fileName);

            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
            File f = new File("./fichiersCSV/"+nom_de_fichier);
            if (f.createNewFile()){
                  System.out.println("File created");
            }
            PrintWriter writer = new PrintWriter(f);
            writer.write(builder.toString());
            writer.close();
            log.debug("fichier crée");
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Table2Panel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
