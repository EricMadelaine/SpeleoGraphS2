/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cds06.speleograph;

import static com.hp.hpl.jena.sparql.lib.DS.list;
import static com.hp.hpl.jena.sparql.lib.DS.list;
import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDateChooser;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import static java.util.Collections.list;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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
public class TablePanel extends JPanel implements DatasetChangeListener, ChartMouseListener,ActionListener, ItemListener {

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
    
    List<Caves> list_caves = new ArrayList();
    
    List<String> list_id_selected = new ArrayList();
    
    private CavesTable my_table;
    
    private JPanel fenetre;
    private JPanel panel_table;
    
    private JButton button;
    private JButton button_return;
    
    private JDateChooser  dd = new JDateChooser ();
    private JDateChooser  df = new JDateChooser ();
    
    private String lieu_ontologie;
    private ComboCheckBox cb;
    private List boxselected = new ArrayList();
    
    /**
     * Construct a new TablePanel for an application instance.
     *
     * @param app The instance which should be linked with this Chart.
     * @param list_caves
     * @param lieu_ontologie
     */
    
    public TablePanel(SpeleoGraphApp app, List list_caves, String lieu_ontologie) {
        this.lieu_ontologie = lieu_ontologie;
        Validate.notNull(app);
        application = app;
        JLabel datedebut = new JLabel("Selectionner la date de début:");
        JLabel datefin = new JLabel("Selectionner la date de fin:");

        JPanel selection = new JPanel();
        JPanel sud = new JPanel();
        selection.setLayout(new GridLayout(1, 5));
        
        sud.setLayout(new GridLayout(2, 1));

        this.list_caves= list_caves;
        cb = new ComboCheckBox();
        cb.setVisible(true);
        boxselected=cb.boxselected;
        
        JPanel titre = new JPanel();
        button_return = new JButton("Retour");
        button_return.addActionListener(this);
        JLabel labelHead = new JLabel("Liste des cavités");
        labelHead.setFont(new Font("Arial",Font.TRUETYPE_FONT,20));
        titre.add(labelHead);
        titre.add(button_return);
        JScrollPane pane = new JScrollPane(cb,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        selection.add(cb);
        selection.add(datedebut);
        selection.add(dd);
        selection.add(datefin);
        selection.add(df);
        
        panel_table = new JPanel();
        my_table = new CavesTable(this.list_caves);
        my_table.setVisible(true);
        
        panel_table.add(my_table);
        panel_table.setVisible(true);
        
        button = new JButton("Rechercher les séries de mesure");
        button.addActionListener(this);
        
        sud.add(selection);
        sud.add(button);
        
        fenetre=new JPanel();
        fenetre.setLayout(new BorderLayout());
        fenetre.add(titre,BorderLayout.NORTH);
        fenetre.add(panel_table,BorderLayout.CENTER);
        fenetre.add(sud, BorderLayout.SOUTH);
        setLayout(new BorderLayout());
        
        add(fenetre);
        
        log.info("TablePanel is initialized");
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
            //on recuperer la liste de cave selectionne (les id)
            //on recupere la selection de type de serie de mesure
            //on recupere date de debut et date de fin
            SimpleDateFormat dateStandart=new SimpleDateFormat("yyyy-MM-dd");
            String date_deb = dateStandart.format(dd.getDate());
            String date_fin = dateStandart.format(df.getDate());
            list_id_selected = my_table.get_id_selected();
            log.debug(date_deb+" "+date_fin);
            log.debug(boxselected.toString());
            log.debug(my_table.get_id_selected().toString());
            List<Series_de_Mesure> my_list_mesure = new ArrayList();
            for(Object box: boxselected){
                String sbox = box.toString();
                log.debug("mesure: "+sbox);
                for(String sid : list_id_selected){
                    log.debug("id: "+sid);
                    //cree ma requete pour chaque id, 
                    /*
                    en fonction de l'id, de la date de debut et de la date de fin, et la selection des mesures
                    je dois recuperer nom du fichier, date de debut, date de fin, mesure(unité), url
                    */
                    String requestsparql =                    
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                        "PREFIX geo: <http://www.geonames.org/ontology#> "+
                        "PREFIX karstlink: <https://ontology.uis-speleo.org/ontology/#> "+
                        "PREFIX sosa: <http://www.w3.org/ns/sosa/> "
                        +
                        "SELECT ?serie ?date_d ?date_f ?me ?url ?sensor  " +
                        "WHERE { " +
                        "  ?subject karstlink:id_GrottoCenter ?cav ." +
                        "  ?subject rdf:type karstlink:UndergroundCavity ." +
                        "  FILTER regex(?cav, \""+sid+"\", \"i\") ." +
                        "  ?serie rdf:type karstlink:Série_temporelle ." +
                        "  ?serie karstlink:appartient_à ?subject ." +
                        "  ?serie karstlink:URL ?url ." +
                        "  ?serie karstlink:Date_début ?date_d ." +
                        "  ?serie karstlink:date_fin ?date_f ." +
                        "  ?serie sosa:madeBySensor ?sen ." +
                        "  ?sen geo:name ?sensor ." +
                        //"  FILTER ( ?date_d < \""+date_deb+"\"^^xsd:dateTime ) ."+
                        //"  FILTER ( ?date_f > \""+date_fin+"\"^^xsd:dateTime ) ." +
                        "  ?serie sosa:observedProperty ?mesure ." +
                        "  ?mesure geo:name ?me ." +
                        "  FILTER regex(?me, \""+sbox+"\", \"i\")" +
                        "} ";
                    log.debug("ma requete sparql\n" + requestsparql);
                    RequestSPARQL my_sparql=null;
                    try{
                        my_sparql = new RequestSPARQL(requestsparql, lieu_ontologie);
                    }catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Nous avons pas trouvé le fichier de l'ontologie\n"+ex,"avertissement",JOptionPane.WARNING_MESSAGE);
                    }
                    if(my_sparql!=null){
                        my_list_mesure.addAll(my_sparql.get_list_mesure());
                    }
                }
            }
            //filtrage en fonction de la date.
            List my_list_final= new ArrayList();
            
            for(Series_de_Mesure m : my_list_mesure){
                log.debug("dated_file:"+m.date_debut+" datef_file:"+m.date_fin+" \ndated_demande:"+dd.getDate()+" datef_demande:"+df.getDate());
                //soit le fichier comprend la date de début de recherche soit le fichier comprend la date de fin de recherche soit les dates sont à l'interieur des date de recherches soit les dates sont toutes les deux a l'exterrieur des dtes de recherches
                if(((m.date_debut.before(dd.getDate()))&&(m.date_fin.after(dd.getDate())))||((m.date_debut.before(df.getDate()))&&(m.date_fin.after(df.getDate())))||((m.date_debut.after(dd.getDate()))&&(m.date_fin.before(df.getDate())))||((m.date_debut.before(dd.getDate()))&&(m.date_fin.after(df.getDate())))){
                    log.debug("ouii je prends");
                    my_list_final.add(m);
                }
            }
            if(my_list_final.isEmpty()){
                JOptionPane.showMessageDialog(this, "Nous avons trouvé aucune série de mesure avec la recherche effectuée","avertissement",JOptionPane.WARNING_MESSAGE);
            }
            else{
                remove(fenetre);
                repaint();
                Table2Panel table2 = new Table2Panel(application, my_list_final, lieu_ontologie);
                add(table2);
            }
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
