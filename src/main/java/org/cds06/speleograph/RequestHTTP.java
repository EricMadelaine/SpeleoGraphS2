/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cds06.speleograph;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import jdk.nashorn.internal.parser.JSONParser;
import org.jetbrains.annotations.NonNls;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Léa
 */
public class RequestHTTP {
    /**
    * Logger for debug and errors.
    */
    
    public String url;
    
    @NonNls
    private static final Logger log = LoggerFactory.getLogger(FormPanel.class);
    public RequestHTTP(String url) throws IOException{
        this.url = url;
        log.debug(this.url);
    }
    
    private  String readAll(Reader rd) {
        StringBuilder sb = new StringBuilder();
        int cp;
        try {
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(RequestHTTP.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sb.toString();
    }

    public List readJsonFromUrl() {
        List<Caves> my_caves = new ArrayList();
        /*JSONParser jsonP = new JSONParser();
        try {
           JSONObject jsonO = (JSONObject)jsonP.parse(new FileReader(url));

           String name = (String) jsonO.get("name");
           String age = (String) jsonO.get("age");
           String address = (String) jsonO.get("address");
        } catch (FileNotFoundException e) {
           e.printStackTrace();
        } catch (IOException e) {
           e.printStackTrace();
        } catch (ParseException e) {
           e.printStackTrace();
        }*/
        return my_caves;
    }

    public List get_id() throws IOException{
        StringBuilder builder = new StringBuilder();
        List list_result = new ArrayList();
        try {
            URL url1 = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) url1.openConnection();
            conn.setRequestMethod("GET");
            conn.addRequestProperty("User-Agent", "Mozilla");
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
            
            for(String line = reader.readLine(); line != null; line = reader.readLine()) {
                builder.append(line + "\n");
            }
            log.debug(builder.toString());
        }catch (Exception e) {
            e.printStackTrace();
            return list_result;
        }
        
        list_result = convert_result_id(builder.toString());
        return list_result;
    }
    
    public List get() throws IOException{
        StringBuilder builder = new StringBuilder();
        List list_result = new ArrayList();
        try {
            URL url1 = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) url1.openConnection();
            conn.setRequestMethod("GET");
            conn.addRequestProperty("User-Agent", "Mozilla");
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
            
            for(String line = reader.readLine(); line != null; line = reader.readLine()) {
                builder.append(line + "\n");
            }
            log.debug(builder.toString());
        }catch (Exception e) {
            e.printStackTrace();
            return list_result;
        }
        
        list_result = convert_result_get(builder.toString());
        return list_result;
    }
    
    public List post() throws IOException{
        String result = "";
        OutputStreamWriter writer = null;
        BufferedReader reader = null;
        try {
            //création de la connection
            URL url = new URL(this.url);
            URLConnection conn = url.openConnection();
            conn.addRequestProperty("User-Agent", "Mozilla");
            conn.setDoOutput(true);

            //envoi de la requête
            writer = new OutputStreamWriter(conn.getOutputStream());
            writer.flush();

            //lecture de la réponse
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                result+=ligne;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally{
            try{writer.close();}catch(Exception e){}
            try{reader.close();}catch(Exception e){}
        }
        List list_result = convert_result_post(result);
        return list_result;
    }
    
    public List convert_result_get(String result) throws IOException{
        result= result.substring(1,result.length()-1);
        String[] div_result = result.split(",\\{"); 
        int j=0;
        List<String> mes_result = new ArrayList();
        for(String s : div_result){
            if(j==0){
                j=1;
                mes_result.add(s);
            }
            else{
                char c = '{';
                s = c+s;
                mes_result.add(s);
            }
        }
        List<Caves> my_caves = new ArrayList();
        ObjectMapper mapper = new ObjectMapper();
        try {
            for(String s : mes_result){
                new StringWriter();
                final JsonReader reader = new JsonReader(new StringReader(s));
                JsonNode node = mapper.readTree(s);
                String id = node.get("id").asText();
                String name = node.get("name").asText();
                String latitude = node.get("latitude").asText();
                String longitude = node.get("longitude").asText();
                my_caves.add(new Caves(id,name, latitude, longitude));
                reader.close();
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return my_caves;
    }  
    
    public List convert_result_id(String result) throws IOException{
        List<Caves> my_caves = new ArrayList();
        ObjectMapper mapper = new ObjectMapper();
        try {
            new StringWriter();
            final JsonReader reader = new JsonReader(new StringReader(result));
            JsonNode node = mapper.readTree(result);
            String id = node.at("/cave/id").asText();
            String name = node.at("/cave/name").asText();
            String latitude = node.at("/cave/latitude").asText();
            String longitude = node.at("/cave/longitude").asText();
            my_caves.add(new Caves(id,name, latitude, longitude));
            reader.close();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return my_caves;
    }  
    
    public List convert_result_post(String result) throws IOException{
        String[] div_result = result.split(":",2);
        result= div_result[1].substring(1);
        div_result = result.split("],\"totalNbResults\":",2);
        result= div_result[0];
        div_result=result.split(",\\{");
        int j=0;
        List<String> mes_result = new ArrayList();
        for(String s : div_result){
            if(s.isEmpty()){
                log.debug("videeeeeee");
            }
            else if(j==0){
                j=1;
                mes_result.add(s);
                log.debug(s);
            } 
            else{
                char c = '{';
                s = c+s;
                mes_result.add(s);
                log.debug(s);
            }
        }
        List<Caves> my_caves = new ArrayList();
        ObjectMapper mapper = new ObjectMapper();
        log.debug(mes_result.toString());
        if(!mes_result.isEmpty()){
            try {
                for(String s : mes_result){
                    new StringWriter();
                    final JsonReader reader = new JsonReader(new StringReader(s));
                    log.debug(s);
                    JsonNode node = mapper.readTree(s);
                    String id = node.get("id").asText();
                    String name = node.get("name").asText();
                    String latitude = node.get("latitude").asText();
                    String longitude = node.get("longitude").asText();
                    my_caves.add(new Caves(id,name, latitude, longitude));
                    reader.close();
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return my_caves;
    }  
}
