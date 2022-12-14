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

package org.cds06.speleograph.data;

import org.apache.commons.lang3.Validate;
import org.cds06.speleograph.GraphPanel;
import org.cds06.speleograph.I18nSupport;
import org.cds06.speleograph.graph.DrawStyle;
import org.cds06.speleograph.utils.Modification;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.xy.HighLowRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.time.DateRange;
import org.jfree.data.xy.OHLCDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Represent a Series of Data.
 * A series is coherent set of Data.
 */
public class Series implements Comparable, OHLCDataset, Cloneable {

    /**
     * Logger for debug and errors in Series instances.
     */
    @SuppressWarnings("UnusedDeclaration")
    @NonNls
    private static final Logger log = LoggerFactory.getLogger(Series.class);
    private static GraphPanel graphPanel;
    private boolean stepped = false;
    private boolean minMax = false;

    /**
     * Create a new Series opened from a file with a default Type.
     *
     * @param origin The file where this series has been read.
     * @param type   The type for this series
     */
    public Series(@NotNull File origin, @NotNull Type type) {
        Validate.notNull(type, "Type can not be null");// NON-NLS
        Validate.notNull(origin);
        this.origin = origin;
        Series.lastOpenedFile = origin;
        this.type = type;
        this.itemsName = "Initialisation";
        instances.add(this);
        setStyle(DrawStyle.AUTO);
        notifyListeners();
    }

    /**
     * Flag to define if we must show this series on chart.
     */
    private boolean show = false;
    /**
     * The file where this series has been read.
     */
    private File origin = null;

    /**
     * The last opened file, independent of the current series.
     */
    private static File lastOpenedFile = null;

    /**
     * Series items, children of series.
     */
    private ArrayList<Item> items = new ArrayList<>();

    /**
     * Are the current items linked to others ? (modification on more than one series cancelled, for example)
     */
    private boolean applyToAll = false;

    /**
     * Series items list name.
     */
    private String itemsName;

    /**
     * Series previous modifications, just undone.
     */
    private ArrayList<Modification> previousModifs = new ArrayList<>(MAX_UNDO_ITEMS);

    /**
     * Series next modifications, waiting to be redone.
     */
    private ArrayList<Modification> nextModifs = new ArrayList<>(MAX_UNDO_ITEMS);

    /**
     * The number of modification that can be canceled.
     */
    public static final int MAX_UNDO_ITEMS = 10;

    /**
     * The name of the series.
     */
    private String name;
    /**
     * Axis linked to this series.
     * This axis replaces the Type's Axis only if it's not null.
     */
    private NumberAxis axis = null;

    private static final ArrayList<Series> instances = new ArrayList<>(20);

    /**
     * Get all series currently in the SpeleoGraph Instance
     *
     * @return Unmodifiable list of instances.
     */
    public static List<Series> getInstances() {
        return Collections.unmodifiableList(instances);
    }

    /**
     * Detect if series is the first element of instances list.
     *
     * @return true if it's the first element.
     */
    public boolean isFirst() {
        return instances.indexOf(this) == 0;
    }

    /**
     * Detect if series is the last element of instances list.
     *
     * @return true if it's the last element.
     */
    public boolean isLast() {
        return instances.indexOf(this) == instances.size() - 1;
    }

    private double seriesMaxValue;
    private double seriesMinValue;

    /**
     * Move the current series to n-1 position.
     */
    public void upSeriesInList() {
        int index = instances.indexOf(this), newIndex = index - 1;
        if (newIndex < 0) return; // We are already on top.
        Series buffer = instances.get(newIndex);
        instances.set(newIndex, instances.get(index));
        instances.set(index, buffer);
        notifyListeners();
    }

    /**
     * Move the current series to n+1 position.
     */
    public void downSeriesInList() {
        int index = instances.indexOf(this), newIndex = index + 1;
        if (newIndex >= instances.size()) return; // We are already on top.
        Series buffer = instances.get(newIndex);
        instances.set(newIndex, instances.get(index));
        instances.set(index, buffer);
        notifyListeners();
    }

    public static void setGraphPanel(GraphPanel graphPanel) {
        Series.graphPanel = graphPanel;
    }

    /**
     * Count the number of items into this Series.
     *
     * @return The number of items (assuming is 0 or more)
     */
    public int getItemCount() {
        if (items == null) return 0;
        return items.size();
    }

    /**
     * Get the file used to read the data.
     *
     * @return The data origin's file.
     */
    public File getOrigin() {
        return origin;
    }

    public double getSeriesMaxValue() {
        return seriesMaxValue;
    }

    public double getSeriesMinValue() {
        return seriesMinValue;
    }

    /**
     * Compute the date range of the items in this set.
     *
     * @return A date range which contains the lower and upper bounds of data.
     */
    public DateRange getRange() {
        int max = getItemCount();
        DateRange range;
        if (max == 0) {
            Date now = Calendar.getInstance().getTime();
            return new DateRange(now, now);
        }
        Date minDate = new Date(Long.MAX_VALUE), maxDate = new Date(Long.MIN_VALUE);
        for (int i = 0; i < max; i++) {
            Item item = items.get(i);
            if (item.getDate().before(minDate)) minDate = item.getDate();
            if (item.getDate().after(maxDate)) maxDate = item.getDate();
        }
        range = new DateRange(minDate, maxDate);
        return range;
    }

    /**
     * Returns the last opened file, independent of the current series.
     * @return the last opened file, independent of the current series.
     */
    public static File getLastOpenedFile() {
        return lastOpenedFile;
    }

    /**
     * Getter for the series Type.
     * If this series is not attached to a DataSet, then we suppose that Type is {@link Type#UNKNOWN}
     *
     * @return The type for this series
     */
    @NotNull
    public Type getType() {
        return type;
    }

    /**
     * Say if we should show this series on a graph.
     *
     * @return true if we should show this series.
     */
    public boolean isShow() {
        return show;
    }

    /**
     * Set if we should show this series on a graph.
     *
     * @param v true if we should show
     */
    public void setShow(boolean v) {
        show = v;
        notifyListeners();
    }

    /**
     * Getter for the axis to display for this series.
     * If the series does not define his own axis, this function will search the Type's Axis.
     *
     * @return A NumberAxis to display it in a chart (never null)
     * @throws IllegalStateException if we can not find an axis for this series.
     */
    public NumberAxis getAxis() {
        if (axis != null) return axis;
        else if (type != null) return getType().getAxis();
        else throw new IllegalStateException("Can not find an axis for series !"); //NON-NLS
    }

    /**
     * Setter for the axis.
     * If an axis is set to the series, then the Type axis would not be shown and the Chart will display this axis for
     * the series even if other shown series are using the Type's axis.
     *
     * @param axis The axis to set for this series.
     */
    public void setAxis(NumberAxis axis) {
        if (axis == null) {
            log.info("Setting a null axis to series " + getName());
        }
        this.axis = axis;
        notifyInstanceListeners();
    }

    /**
     * Listeners for this series.
     */
    private ArrayList<DatasetChangeListener> listeners = new ArrayList<>();

    /**
     * Notify listeners about something changed into the series.
     */
    public void notifyListeners() {
        final DatasetChangeEvent event = new DatasetChangeEvent(this, this);
        if (graphPanel != null)
            graphPanel.datasetChanged(event);
        for (DatasetChangeListener listener : staticListeners) {
            listener.datasetChanged(event);
        }
        for (DatasetChangeListener listener : listeners) {
            listener.datasetChanged(event);
        }
    }

    /**
     * Add a listener on series' properties.
     *
     * @param listener The listener will be called on events
     */
    public void addChangeListener(DatasetChangeListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    /**
     * Remove a listener on series' properties.
     *
     * @param listener The listener which will be removed.
     */
    public void removeChangeListener(DatasetChangeListener listener) {
        if (listeners.contains(listener)) listeners.remove(listener);
    }

    /**
     * Getter for the human name of this series.
     * If the name is not set, it computes a name as "[Origin File Name] - [Name of the Type]"
     *
     * @return The display name for this Series.
     */
    public String getName() {
        return name == null ? getOrigin().getName() + " - " + getType().getName() : name;
    }

    /**
     * Say if the Series name has been chosen by human or generated.
     *
     * @return true if name is set by human
     */
    public boolean isNameHumanSet() {
        return name != null;
    }

    /**
     * Set an human name for this Series.
     *
     * @param name The name to set (should not be null)
     */
    public void setName(String name) {
        this.name = name;
        notifyListeners();
    }

    @Override
    public int compareTo(@NotNull Object o) {
        return this.equals(o) ? 0 : -1;
    }

    /**
     * Add an item to this series.
     *
     * @param item The item to add.
     */
    public void add(Item item) {
        Validate.notNull(item);
        items.add(item);
        if (item.getValue() > seriesMaxValue)
            seriesMaxValue = item.getValue();
        else if (item.getValue() < seriesMinValue)
            seriesMinValue = item.getValue();
    }

    /**
     * Returns the high-value for the specified series and item.
     *
     * @param series the series (zero-based index).
     * @param item   the item (zero-based index).
     * @return The value.
     */
    @Override
    public Number getHigh(int series, int item) {
        return getHighValue(series, item);
    }

    /**
     * Returns the high-value (as a double primitive) for an item within a
     * series.
     *
     * @param series the series (zero-based index).
     * @param item   the item (zero-based index).
     * @return The high-value.
     */
    @Override
    public double getHighValue(int series, int item) {
        if (isMinMax())
            if (isShow() && (item > -1 && item < items.size()))
                return items.get(item).getHigh();
            else
                return Double.NaN;
        else
            return Double.NaN;
    }

    /**
     * Returns the low-value for the specified series and item.
     *
     * @param series the series (zero-based index).
     * @param item   the item (zero-based index).
     * @return The value.
     */
    @Override
    public Number getLow(int series, int item) {
        return getLowValue(series, item);
    }

    /**
     * Returns the low-value (as a double primitive) for an item within a
     * series.
     *
     * @param series the series (zero-based index).
     * @param item   the item (zero-based index).
     * @return The low-value.
     */
    @Override
    public double getLowValue(int series, int item) {
        if (isMinMax())
            if (isShow() && (item > -1 && item < items.size()))
                return items.get(item).getLow();
            else
                return Double.NaN;
        else
            return Double.NaN;
    }

    /**
     * Returns the open-value for the specified series and item.
     *
     * @param series the series (zero-based index).
     * @param item   the item (zero-based index).
     * @return The value.
     */
    @Override
    public Number getOpen(int series, int item) {
        return getOpenValue(series, item);
    }

    /**
     * Returns the open-value (as a double primitive) for an item within a
     * series.
     *
     * @param series the series (zero-based index).
     * @param item   the item (zero-based index).
     * @return The open-value.
     */
    @Override
    public double getOpenValue(int series, int item) {
        return Double.NaN;
    }

    /**
     * Returns the y-value for the specified series and item.
     *
     * @param series the series (zero-based index).
     * @param item   the item (zero-based index).
     * @return The value.
     */
    @Override
    public Number getClose(int series, int item) {
        return getCloseValue(series, item);
    }

    /**
     * Returns the close-value (as a double primitive) for an item within a
     * series.
     *
     * @param series the series (zero-based index).
     * @param item   the item (zero-based index).
     * @return The close-value.
     */
    @Override
    public double getCloseValue(int series, int item) {
        return Double.NaN;
    }

    /**
     * Returns the volume for the specified series and item.
     *
     * @param series the series (zero-based index).
     * @param item   the item (zero-based index).
     * @return The value.
     */
    @Override
    public Number getVolume(int series, int item) {
        return getVolumeValue(series, item);
    }

    /**
     * Returns the volume-value (as a double primitive) for an item within a
     * series.
     *
     * @param series the series (zero-based index).
     * @param item   the item (zero-based index).
     * @return The volume-value.
     */
    @Override
    public double getVolumeValue(int series, int item) {
        return Double.NaN;
    }

    /**
     * Returns the order of the domain (or X) values returned by the dataset.
     *
     * @return The order (never <code>null</code>).
     */
    @Override
    public DomainOrder getDomainOrder() {
        return DomainOrder.ASCENDING;
    }

    /**
     * Returns the number of items in a series.
     * <br><br>
     * It is recommended that classes that implement this method should throw
     * an <code>IllegalArgumentException</code> if the <code>series</code>
     * argument is outside the specified range.
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @return The item count.
     */
    @Override
    public int getItemCount(int series) {
        return isShow() ? items.size() : 0;
    }

    /**
     * Returns the x-value for an item within a series.  The x-values may or
     * may not be returned in ascending order, that is up to the class
     * implementing the interface.
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @param item   the item index (in the range <code>0</code> to
     *               <code>getItemCount(series)</code>).
     * @return The x-value (never <code>null</code>).
     */
    @Override
    public Number getX(int series, int item) {
        return getXValue(series, item);
    }

    /**
     * Returns the x-value for an item within a series.
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @param item   the item index (in the range <code>0</code> to
     *               <code>getItemCount(series)</code>).
     * @return The x-value.
     */
    @Override
    public double getXValue(int series, int item) {
        try {
            if (isShow() && (item > -1 && item < items.size()))
                return items.get(item).getDate().getTime();
            else
                return Double.NaN;
        } catch (NullPointerException e) {
            return Double.NaN;
        }
    }

    /**
     * Returns the y-value for an item within a series.
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @param item   the item index (in the range <code>0</code> to
     *               <code>getItemCount(series)</code>).
     * @return The y-value (possibly <code>null</code>).
     */
    @Override
    public Number getY(int series, int item) {
        return getYValue(series, item);
    }

    /**
     * Returns the y-value (as a double primitive) for an item within a series.
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @param item   the item index (in the range <code>0</code> to
     *               <code>getItemCount(series)</code>).
     * @return The y-value.
     */
    @Override
    public double getYValue(int series, int item) {
        if (isShow() && (item > -1 && item < items.size()))
            return items.get(item).getValue();
        else
            return Double.NaN;
    }

    /**
     * Returns the number of series in the dataset.
     *
     * @return The series count.
     */
    @Override
    public int getSeriesCount() {
        return isShow() ? 1 : 0;
    }

    /**
     * Returns the key for a series.
     *
     * @param series the series index (in the range <code>0</code> to
     *               <code>getSeriesCount() - 1</code>).
     * @return The key for the series.
     */
    @Override
    public Comparable getSeriesKey(int series) {
        return getName();
    }

    /**
     * Returns the index of the series with the specified key, or -1 if there
     * is no such series in the dataset.
     *
     * @param seriesKey the series key (<code>null</code> permitted).
     * @return The index, or -1.
     */
    @Override
    public int indexOf(Comparable seriesKey) {
        return getName().compareTo(String.valueOf(seriesKey)) == 0 ? 0 : -1;
    }

    public void setType(Type type) {
        this.type = type;
    }

    private Type type = Type.UNKNOWN;

    /**
     * Returns the dataset group.
     *
     * @return The dataset group.
     */
    @Override
    public Type getGroup() {
        return getType();
    }

    /**
     * Sets the dataset group.
     *
     * @param group the dataset group.
     */
    @Override
    public void setGroup(DatasetGroup group) {
        if (group instanceof Type) {
            setType((Type) group);
        } else {
            throw new IllegalArgumentException("Group must by a SpeleoGraph Type");
        }
    }

    /**
     * Define the {@link Object#toString()} to be alias of {@link #getName()}.
     *
     * @return The name of this series.
     * @see #getName()
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Has the ability to return an html version of the {@link #toString()} containing more info.
     * @param b Want to get html version ?
     * @return The html version of the {@link #toString()} method containing min/max values and unit.
     */
    public String toString(boolean b) {
        if (b)
            return "<em>" + this.getName() + "</em> - [" +
                    this.getSeriesMinValue() + " --> " + this.getSeriesMaxValue() +
                    "] " + this.getType().getUnit();
        else
            return toString();
    }

    public void delete() {
        instances.remove(this);
        items.clear();
        notifyListeners();
    }

    private XYItemRenderer renderer;

    public XYItemRenderer getRenderer() {
        if (renderer == null) setupRendererAuto();
        if (color != null) {
            renderer.setSeriesPaint(0, color);
        }
        return renderer;
    }

    //TODO Write doc
    public Series generateSampledSeries(long length) {
        final Series newSeries = new Series(origin, Type.WATER);
        newSeries.setStepped(true);
        final int itemsCount = getItemCount();
        final ArrayList<Item> newItems = newSeries.items;
        double bufferValue = 0D;
        DateRange range = getRange();
        long lastStartBuffer = range.getLowerMillis();
        newItems.add(new Item(newSeries, new Date(lastStartBuffer), 0.0));
        for (int i = 1; i < itemsCount; i++) {
            final Item originalItem = items.get(i), previousOriginalItem = items.get(i - 1);
            if (lastStartBuffer + length <= originalItem.getDate().getTime()) {
                newItems.add(new Item(newSeries, new Date(lastStartBuffer), bufferValue));
                newItems.add(new Item(newSeries, new Date(lastStartBuffer + length), bufferValue));
                final long time = originalItem.getDate().getTime();
                lastStartBuffer = lastStartBuffer + length;
                if (lastStartBuffer + 2 * length < time) {
                    newItems.add(new Item(newSeries, new Date(lastStartBuffer), 0));
                    lastStartBuffer = time -
                            ((originalItem.getDate().getTime() - lastStartBuffer) % length);
                    newItems.add(new Item(newSeries, new Date(lastStartBuffer), 0));
                }
                bufferValue = 0D;
            }
            bufferValue = bufferValue + (originalItem.getValue() - previousOriginalItem.getValue());
        }
        newItems.add(new Item(newSeries, new Date(lastStartBuffer), bufferValue));
        newItems.add(new Item(newSeries, new Date(range.getUpperMillis()), bufferValue));
        return newSeries;
    }

    private static final HashSet<DatasetChangeListener> staticListeners = new HashSet<>(2);

    public static void addListener(DatasetChangeListener listener) {
        staticListeners.add(listener);
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public String getItemsName() {
        return itemsName;
    }

    private DrawStyle style = DrawStyle.AUTO;

    public DrawStyle getStyle() {
        return style;
    }

    public void setStyle(DrawStyle style) {
        Validate.notNull(style);
        if (isMinMax() && !(style == DrawStyle.AUTO || style == DrawStyle.HIGH_LOW)) return;
        if (style.equals(this.style)) return;
        this.style = style;
        switch (style) {
            case AUTO:
                setupRendererAuto();
                break;
            case AREA:
                renderer = new NewAreaRenderer(XYAreaRenderer.AREA);
                break;
            case HIGH_LOW:
                renderer = new NewHighLowRenderer();
                break;
            default:
            case LINE:
                renderer = new NewLineAndShapeRenderer(true, false);

        }
        notifyListeners();
    }

    private void setupRendererAuto() {
        if (isMinMax()) {
            renderer = new NewHighLowRenderer();
        } else if (isStepped()) {
            renderer = new NewAreaRenderer(XYAreaRenderer.AREA);
        } else {
            renderer = new NewLineAndShapeRenderer(true, false);
        }
    }

    /**
     * Used to override the display of the legend
     */
    private static class NewLineAndShapeRenderer extends XYLineAndShapeRenderer {
        public NewLineAndShapeRenderer(boolean a, boolean b) {
            super(a,b);
        }

        @Override
        public LegendItem getLegendItem(int datasetIndex, int series) {
            LegendItem legend = super.getLegendItem(datasetIndex, series);
            return new LegendItem(legend.getLabel(), legend.getDescription(), legend.getToolTipText(), legend.getURLText(), Plot.DEFAULT_LEGEND_ITEM_BOX, legend.getFillPaint());
        }
    }

    /**
     * Used to override the display of the legend
     */
    private static class NewAreaRenderer extends XYAreaRenderer {
        public NewAreaRenderer(int a) {
            super(a);
        }

        @Override
        public LegendItem getLegendItem(int datasetIndex, int series) {
            LegendItem legend = super.getLegendItem(datasetIndex, series);
            return new LegendItem(legend.getLabel(), legend.getDescription(), legend.getToolTipText(), legend.getURLText(), Plot.DEFAULT_LEGEND_ITEM_BOX, legend.getFillPaint());
        }
    }

    /**
     * Used to override the display of the legend
     */
    private static class NewHighLowRenderer extends HighLowRenderer {
        public NewHighLowRenderer() {
            super();
        }

        @Override
        public LegendItem getLegendItem(int datasetIndex, int series) {
            LegendItem legend = super.getLegendItem(datasetIndex, series);
            return new LegendItem(legend.getLabel(), legend.getDescription(), legend.getToolTipText(), legend.getURLText(), Plot.DEFAULT_LEGEND_ITEM_BOX, legend.getFillPaint());
        }
    }

    /**
     * Color of the series on screen.
     */
    private Color color;

    public Color getColor() {
        if (color == null && renderer != null) {
            return (Color) renderer.getSeriesPaint(0);
        }
        return color;
    }

    public void setColor(Color color) {
        if (renderer == null) setupRendererAuto();
        this.color = color;
        notifyListeners();
    }

    /**
     * Notify all static listeners that an edit occurs.
     * <p>Note: This function will refresh graphics, so it could occur thread blocking</p>
     */
    public static void notifyInstanceListeners() {
        final DatasetChangeEvent event = new DatasetChangeEvent(Series.class, null);
        if (graphPanel != null)
            graphPanel.datasetChanged(event);
        for (DatasetChangeListener listener : staticListeners) {
            listener.datasetChanged(event);
        }
    }

    public boolean hasOwnAxis() {
        return axis != null;
    }

    public void setStepped(boolean stepped) {
        this.stepped = stepped;
    }

    public boolean isStepped() {
        return stepped;
    }

    public void setMinMax(boolean minMax) {
        this.minMax = minMax;
    }

    public boolean isMinMax() {
        return minMax;
    }

    public boolean isWater() {
        return this.getType().getName().equals(Type.WATER.getName());
    }

    public boolean isWaterCumul() {
        return this.getType().getName().equals(Type.WATER_CUMUL.getName());
    }

    public boolean isPressure() {
        return this.getType().getName().equals(Type.PRESSURE.getName());
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean isTemperature() {
        return this.getType().getName().equals(Type.TEMPERATURE.getName());
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean isWaterHeight() {
        return this.getType().getName().equals(Type.WATER_HEIGHT.getName());
    }

    /**
     * Remplace les donn??es de la s??rie par celles d'une sous-s??rie.
     * Par exemple : On a des donn??es du 25/07 au 30/07, on veut uniquement les donn??es du 28 ?? 7h au 28 ?? 9h.
     * @param start Date de d??but.
     * @param end Date de fin.
     * @param applyToAll Is the modification applied to more than one series ?
     */
    public void subSeries(Date start, Date end, boolean applyToAll) {
        this.setItems(extractSubSerie(start, end), I18nSupport.translate("actions.limit"), applyToAll);
        notifyListeners();
    }

    /**
     * Extraire une sous-s??rie de donn??es.
     * Par exemple : On a des donn??es du 25/07 au 30/07, on veut extraire les donn??es du 28 ?? 7h au 28 ?? 9h.
     * @param start Date de d??but.
     * @param end Date de fin.
     * @return The {@link java.util.ArrayList} containing all the items that match the date range.
     */
    public ArrayList<Item> extractSubSerie(Date start, Date end) {
        ArrayList<Item> newItems = new ArrayList<>(items.size());
        for (Item i : items) {
            if (i.getDate().after(start) && i.getDate().before(end))
                newItems.add(i);
        }
        return newItems;
    }

    /**
     * Setter for items, stores the old items in a field so they can be retrieved by {@link #undo()}.
     * The stored list of changes is limited to ten items.
     * This method also clears the redo list.
     * @param items The {@link ArrayList} to set in place of the existing one.
     * @param name The name of the modification that occurred.
     */
    public void setItems(ArrayList<Item> items, String name) {
        setItems(items, name, false);
    }

    /**
     * Setter for items, stores the old items in a field so they can be retrieved by {@link #undo()}.
     * The stored list of changes is limited to ten items.
     * This method also clears the redo list.
     * @param items The {@link ArrayList} to set in place of the existing one.
     * @param name The name of the modification that occurred.
     * @param applyToAll Is the modification applied to more than one series ?
     */
    public void setItems(ArrayList<Item> items, String name, boolean applyToAll) {
        Modification m = new Modification(this.itemsName, new Date(), this.items, this , applyToAll);
        this.applyToAll = applyToAll;
        this.previousModifs.add(m);
        this.nextModifs.clear();
        Modification.clearRedoList();
        Modification.addToUndoList(m);

        this.items = items;
        setMinMaxValue();

        this.itemsName = name;
        if (this.previousModifs.size() > MAX_UNDO_ITEMS)
            this.previousModifs.remove(0);
        notifyListeners();
    }

    private void setMinMaxValue() {
        seriesMaxValue = Collections.max(items).getValue();
        seriesMinValue = Collections.min(items).getValue();
    }

    /**
     * Undo the last destructive action (done through {@link #setItems(java.util.ArrayList, java.lang.String)} done on the series.
     * Can only undo ten items.
     * All undone actions can be retrieved using {@link #redo()}.
     * @return true if the undo could have been done, else false.
     */
    public boolean undo() {
        if (!this.canUndo()) return false;
        final int previousModifsSize = this.previousModifs.size();
        Modification m = this.createModif();
        this.nextModifs.add(m);
        Modification.addToRedoList(m);
        Modification old = this.previousModifs.get(previousModifsSize - 1);
        this.items = old.getItems();
        setMinMaxValue();
        this.applyToAll = old.isApplyToAll();
        this.previousModifs.remove(previousModifsSize - 1);
        Modification.removeLastUndo();
        notifyListeners();
        return true;
    }

    /**
     * Reset the series (undo everything).
     * @return true if the reset could have been done, else false.
     */
    public boolean reset() {
        if (!this.canUndo()) return false;
        while (this.canUndo())
            undo();
        return true;
    }

    /**
     * Redo the last undone action (see {@link #undo()}) on the series.
     * Can only redo things that have been undone.
     * @return true if the redo could have been done, else false.
     */
    public boolean redo() {
        if (!this.canRedo()) return false;
        final int nextModifsSize = this.nextModifs.size();
        Modification m = this.createModif();
        this.previousModifs.add(m);
        Modification.addToUndoList(m);
        Modification next = this.nextModifs.get(nextModifsSize-1);
        this.items = next.getItems();
        setMinMaxValue();
        this.applyToAll = next.isApplyToAll();
        this.nextModifs.remove(nextModifsSize-1);
        Modification.removeLastRedo();
        notifyListeners();
        return true;
    }

    /**
     * Tells if a series can undo something.
     * @return true if an undo can be done, else false.
     */
    public boolean canUndo() {
        return this.previousModifs.size() > 0;
    }

    /**
     * Tells if a series can redo something.
     * @return true if an redo can be done, else false.
     */
    public boolean canRedo() {
        return this.nextModifs.size() > 0;
    }

    /**
     * Create a {@link org.cds06.speleograph.utils.Modification} with current data (items, items name and current date).
     * @return the new {@link org.cds06.speleograph.utils.Modification}
     */
    public Modification createModif() {
        return new Modification(this.itemsName, new Date(), this.items, this, this.applyToAll);
    }

    public String getLastUndoName() {
        if (!canUndo()) return "Pas de modification ?? annuler";
        return getLastModif().getName();
    }
    public Modification getLastModif() {
        if (canUndo()) return this.previousModifs.get(previousModifs.size()-1);
        return null;
    }
    public String getNextRedoName() {
        if (!canRedo()) return "Pas de modification ?? refaire";
        return getNextRedo().getName();
    }
    public Modification getNextRedo() {
        if (canRedo()) return this.nextModifs.get(nextModifs.size()-1);
        return null;
    }
}
