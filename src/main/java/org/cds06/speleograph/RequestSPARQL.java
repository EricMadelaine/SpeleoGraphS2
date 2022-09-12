/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cds06.speleograph;

import static com.hp.hpl.jena.assembler.JA.Model;
import java.io.InputStream;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import static org.apache.jena.assembler.JA.Model;
import static org.apache.jena.assembler.JA.OntModel;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Léa
 */
@SuppressWarnings("FieldCanBeLocal")
public class RequestSPARQL {
    
    /**
    * Logger for debug and errors.
    */
    @NonNls
    private static final Logger log = LoggerFactory.getLogger(FormPanel.class);
    
    public String request;
    public List list_mesure = new ArrayList();
    public InputStream reader=null;
    
    public RequestSPARQL(String request, String lieu_onto){
        this.request = request;
        
        String monontologie=lieu_onto;
        //creer un modele d'ontologie
        Model model = ModelFactory.createDefaultModel();
        reader = FileManager.get().open(monontologie);
        if (reader == null) {
            throw new IllegalArgumentException("File: " + monontologie + " not found");
        }
        model.read(reader , "","RDF/XML");
        /// nom de controlleur : 
        Query query = QueryFactory.create(this.request) ;
        Dataset dataset=DatasetFactory.create(model);
        QueryExecution qexec = QueryExecutionFactory.create(query,model);
        ResultSet resultset = qexec.execSelect() ;
        String res = ResultSetFormatter.asText(resultset);
        log.debug(res);
        String[] ligne = res.split("\n");
        int i = 0 ;
        for (String ligne1 : ligne) {
            Pattern pattern = Pattern.compile("^\\|");
            if(pattern.matcher(ligne1).find() && i ==1){
                log.debug("je crois ma mesure ");
                String[] row = ligne1.split("\\|");
                log.debug(Arrays.toString(row));
                String dd = row[2].substring(2, 12);
                String df = row[3].substring(2, 12);
                String mesure = row[4].substring(1, row[4].length()-1);
                log.debug("row5 :"+row[5]);
                String url = row[5].substring(2, row[5].length()-2);
                log.debug("prec url :"+url);
                String capteur = row[6].substring(2, row[6].length()-2);
                Series_de_Mesure sm = new Series_de_Mesure(url, dd, df, mesure,capteur);
                list_mesure.add(sm);
            }
            else if(pattern.matcher(ligne1).find() && i==0){
                i++;
            }
        }
        log.debug(list_mesure.toString());
    }
    
    public List get_list_mesure(){
        return list_mesure;
    }
}
