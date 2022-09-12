/*
 * Copyright (c) 2013 Philippe VIENNE
 *
 * This file is a part of SpeleoGraph
 *
 * SpeleoGraph is free software: you can redistribute
 * it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * SpeleoGraph is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with SpeleoGraph.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.cds06.speleograph.data.fileio;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.apache.commons.lang3.StringUtils;
import org.cds06.speleograph.I18nSupport;
import org.cds06.speleograph.data.Item;
import org.cds06.speleograph.data.Series;
import org.cds06.speleograph.data.Type;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * Open a CSV Reefnet V2 file and transform it to a CSV for SpeleoGraph.
 * <div>
 * An Reefnet V2 is a comma-separated CSV file with this column :
 * <ol>
 * <li>Index (not used here)</li>
 * <li>Day/Month/Year Hour:Minute</li>
 * <li>Pressure</li>
 * <li>Temperature (Integer part)</li>
 * <li>Temperature (Decimal part)</li>
 * </ol>
 * This is the format when you export from Sensus Manager and excepted for this converter.
 * </div>
 *
 * @author Lea Corp dit Genti
 * @since 1.0
 */
public class ReefnetFileReaderV2 implements DataFileReader {

    /**
     * Logger for errors and information.
     */
    @NonNls
    private static final Logger log = LoggerFactory.getLogger(ReefnetFileReaderV2.class);

    /**
     * When test if a CSV file is a ReefNet File, Maximal not ReefNet lines allowed before.
     */
    private static final int MAX_ALLOWED_HEADERS = 30;

    /**
     * Detect if a file is a ReefNet CSV format.
     * <p>Open the file as a csv, read the first line, we check that :</p>
     * <ul>
     * <li>has got 4 or 5 elements</li>
     * </ul>
     * @param file File to test
     * @return true if it's a ReefNet file
     */
    public static boolean isReefnetFileV2(File file) {
        try {
            CSVReader csvReader = new CSVReader(new FileReader(file), ',');
            String[] line;
            for (int i = 0; i < MAX_ALLOWED_HEADERS && (line = csvReader.readNext()) != null; i++) {
                int size = line.length;
                if (3 < size && size < 6 ) // NON-NLS
                    return true;
                if (size > 1)
                    return false;
            }
        } catch (IOException e) {
            log.error("Can not test if it's a ReefFile V2, continuing as if it's not one", e); // NON-NLS
        }
        return false;
    }

    /**
     * Date format used to parse date in ReefNet entries.
     * This variable must not be altered without editing {@link #readDate(String[], java.util.Calendar)}
     */
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("y-M-d H:m:s");

    /**
     * Read a ReefNet File.
     *
     * @param file The file to read.
     * @throws FileReadingError When an error occurs when read the file.
     * @see #readReefnetEntry(String[], org.cds06.speleograph.data.Series, org.cds06.speleograph.data.Series, String, java.util.Calendar)
     * @see #readDate(String[], java.util.Calendar)
     */
    @Override
    public void readFile(File file) throws FileReadingError {
        log.info("Start reading file: " + file);
        FileReader fileReader;
        try {
            fileReader = new FileReader(file);
        } catch (FileNotFoundException e) {
            throw new FileReadingError(
                    I18nSupport.translate("error.canNotOpenFile", file.getName()),
                    FileReadingError.Part.HEAD,
                    e
            );
        }
        CSVReader reader = new CSVReader(fileReader, ',');
        String[] line;
        try {
            line = reader.readNext();
        } catch (IOException e) {
            throw new FileReadingError(
                    I18nSupport.translate("error.canNotReadFileOrEmpty"),
                    FileReadingError.Part.HEAD,
                    e
            );
        }
        Series pressureSeries = new Series(file, Type.PRESSURE),
                temperatureSeries = new Series(file, Type.TEMPERATURE);
        String seriesId = "";
        final Calendar calendar = Calendar.getInstance();
        while (line != null) {
            if (3 < line.length && line.length < 6) {
                seriesId = readReefnetEntry(line, pressureSeries, temperatureSeries, seriesId, calendar);
            } else {
                log.info("Not a Reefnet line: " + StringUtils.join(line, ',')); //NON-NLS
            }
            try {
                line = reader.readNext();
            } catch (IOException e) {
                line = null;
            }
        }
        log.info("Reefnet File (" + file.getName() + ") has been read."); //NON-NLS
    }

    /**
     * Read an entry from a Reefnet File.
     *
     * @param line              The line extracted from the file (length must be 12 or 13)
     * @param pressureSeries    The series where add pressure data
     * @param temperatureSeries The series where add temperature data
     * @param seriesId          The ReefNet's Series ID
     * @param calendar          The calendar which contains the start date of the current series.
     * @return The modified ReefNet's Series ID.
     * @throws FileReadingError When can not parse the date.
     * @see #readDate(String[], java.util.Calendar)
     * @see #readFile(java.io.File)
     */
    private String readReefnetEntry(
            String[] line, Series pressureSeries, Series temperatureSeries, String seriesId, Calendar calendar)
            throws FileReadingError {
        double temperature = 0;
        if (line.length == 4) {
            temperature = Double.parseDouble(line[3]) - 273.15;
        } else if (line.length == 5) {
            temperature = Double.parseDouble(line[3] + '.' + line[4]) - 273.15;
        }
        int pressure = Integer.parseInt(line[2]);
        seriesId = readDate(line, calendar);
        Calendar clone = (Calendar) calendar.clone();
        temperatureSeries.add(new Item(temperatureSeries, clone.getTime(), temperature));
        pressureSeries.add(new Item(pressureSeries, clone.getTime(), pressure));
        return seriesId;
    }

    /**
     * Read a date from a Reefnet entry.
     *
     * @param line     The line where the date is stored
     * @param calendar The calendar to update with the read date
     * @return The new ReefNet series ID which comes with this date.
     * @throws FileReadingError
     * @see #readFile(java.io.File)
     * @see #readReefnetEntry(String[], Series, Series, String, java.util.Calendar)
     */
    private String readDate(String[] line, Calendar calendar) throws FileReadingError {
        String seriesId;
        seriesId = line[0];
        Date d;
        //log.debug(seriesId);
        try {
            d = dateFormat.parse(line[1]);
            //log.debug("date :" +d);
        } catch (ParseException e) {
            log.error("Can not parse a date", e);
            throw new FileReadingError(
                    I18nSupport.translate("error.canNotReadDate"),
                    FileReadingError.Part.DATA,
                    e
            );
        }
        calendar.setTime(d);
        return seriesId;
    }

    /**
     * Get the name of file read by this class.
     *
     * @return The localized name of file.
     */
    @Override
    public String getName() {
        return "ReefNet File V2"; // NON-NLS
    }

    /**
     * Get the text for buttons or menus.
     *
     * @return The localized text.
     */
    @Override
    public String getButtonText() {
        return I18nSupport.translate("actions.openReefNetFileV2");
    }

    /**
     * Get the FileFilter to use.
     *
     * @return A file filter
     */
    @NotNull
    @Override
    public IOFileFilter getFileFilter() {
        return filter;
    }


    private static final AndFileFilter filter = new AndFileFilter(
            FileFileFilter.FILE,
            new AndFileFilter(
                    new SuffixFileFilter(new String[]{".csv", ".txt"}), // NON-NLS
                    new AbstractFileFilter() {
                        /**
                         * Checks to see if the File should be accepted by this filter.
                         *
                         * @param dir  the directory File to check
                         * @param name the filename within the directory to check
                         * @return true if this file matches the test
                         */
                        @Override
                        public boolean accept(File dir, String name) {
                            return isReefnetFileV2(FileUtils.getFile(dir, name));
                        }
                    }));
}
