package org.cds06.speleograph;

/**
 * This file is created by PhilippeGeek.
 * Distributed on licence GNU GPL V3.
 */

import au.com.bytecode.opencsv.CSVReader;
import org.cds06.speleograph.Data.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Design a DataSet model.
 * A Data set contains information about something. The data is stored as date belongs to one data.
 * An DataSet come from a CSV file
 */
public class DataSetReader {

    private static final Logger log = LoggerFactory.getLogger(DataSetReader.class);

    private String title = null;
    private File dataOriginFile = null;
    private HashMap<Type, DataSet> dataSets = new HashMap<>(3);

    {
        for (Type type : Type.values()) {
            dataSets.put(type, new DataSet());
            dataSets.get(type).setReader(this);
        }
    }

    public DataSetReader(File file) throws IOException {
        setDataOriginFile(file);
        read();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public File getDataOriginFile() {
        return dataOriginFile;
    }

    public void setDataOriginFile(File dataOriginFile) {
        this.dataOriginFile = dataOriginFile;

    }

    protected void read() throws IOException {
        CSVReader csvReader = new CSVReader(new FileReader(getDataOriginFile()), ';');
        String[] line;
        HeadersList headers = null;
        ArrayList<Type> availableTypes = new ArrayList<>();
        int id = 0;
        while ((line = csvReader.readNext()) != null) {
            if (line.length <= 1) {                            // Title Line
                if (line.length != 0) setTitle(line[0]);
            } else if (headers == null) {                         // The first line is headers
                headers = new HeadersList(line.length);
                Collections.addAll(headers, line);
                availableTypes = headers.getAvailableTypes();
            } else {                                        // An data line
                String date = line[headers.getDateColumnId()] + " " + line[headers.getTimeColumnId()];
                String format = "dd/MM/yyyy HH:mm:ss";
                Data data = null;
                for (Type t : availableTypes) {
                    data = new Data();
                    data.setDataType(t);
                    data.setDate(date, format);
                    if(line[headers.getValueColumnIdForType(t)].length()>0){
                        data.setValue(Double.valueOf(line[headers.getValueColumnIdForType(t)].replace(',', '.')));
                        dataSets.get(t).add(data);
                    }
                }
                if (data == null) {
                    log.error("Unable to read line " + id, line);
                }
                id++;
            }
        }
    }

    @Deprecated
    public DataSet getWater() {
        return dataSets.get(Type.WATER);
    }

    @Deprecated
    public DataSet getPressure() {
        return dataSets.get(Type.PRESSURE);
    }

    @Deprecated
    public DataSet getTemperature() {
        return dataSets.get(Type.TEMPERATURE);
    }

    private class HeadersList extends ArrayList<String> {

        private final HashMap<Type, String[]> headerConditions = new HashMap<>();

        {
            headerConditions.put(Type.PRESSURE, new String[]{});
            headerConditions.put(Type.TEMPERATURE, new String[]{"Moy. : Température, °C","Max. : Température, °C","Min. : Température, °C"});
            headerConditions.put(Type.WATER, new String[]{"Pluvio"});
        }

        private final String dateColumn = "Date";
        private final String timeColumn = "Heure";

        public ArrayList<Type> getAvailableTypes() {
            ArrayList<Type> list = new ArrayList<>(headerConditions.size());
            for (Type type : headerConditions.keySet())
                if (hasType(type)) list.add(type);
            return list;
        }

        public boolean hasType(Type t) {
            if (!headerConditions.keySet().contains(t)) return false;
            if (headerConditions.get(t).length > 0)
                for (String c : this)
                    if (c.contains(headerConditions.get(t)[0])) return true;
            return false;
        }

        public int getValueColumnIdForType(Type t) {
            if (!hasType(t)) return -1;
            for (int i = 0; i < this.size(); i++)
                if (this.get(i).contains(headerConditions.get(t)[0])) return i;
            return -1;
        }

        public int getDateColumnId() {
            for (int i = 0; i < this.size(); i++)
                if (this.get(i).contains(dateColumn)) return i;
            return -1;
        }

        public int getTimeColumnId() {
            for (int i = 0; i < this.size(); i++)
                if (this.get(i).contains(timeColumn)) return i;
            return -1;
        }

        public HeadersList(int i) {
            super(i);
        }

    }

    static public void main(String[] args) {
        try {
            DataSetReader reader = new DataSetReader(new File("C:\\Users\\PhilippeGeek\\Dropbox\\CDS06 Comm Scientifique\\Releves-Instruments\\Pluvio Villebruc\\2315774_9-tous.txt"));
            long before= System.currentTimeMillis();
            reader.read();
            long after= System.currentTimeMillis();
            for(Data d:reader.getTemperature())
                System.out.println(d);
            System.out.println("\n\n In "+Long.toString(after-before));
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }
}
