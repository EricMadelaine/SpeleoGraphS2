package org.cds06.speleograph.actions.data;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.cds06.speleograph.I18nSupport;
import org.cds06.speleograph.SpeleoGraphApp;
import org.cds06.speleograph.data.Item;
import org.cds06.speleograph.data.Series;
import org.cds06.speleograph.utils.FormDialog;
import org.jetbrains.annotations.NonNls;
import org.jfree.data.time.DateRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Gabriel Augendre.
 * Allow the user to recalibrate a Pressure Serie.
 * Recalibration will align a serie of data on another.
 */
public class WaterHeightAction extends AbstractAction {

    private final Series series;

    /**
     * A value in minute.
     * We consider two points of data taken at the same moment if they are spaced-out of at maximum this value.
     */
    private static final int TEMPORAL_RANGE_ACCEPTED = 3;

    /**
     * Logger for errors and info.
     */
    @SuppressWarnings("UnusedDeclaration")
    @NonNls
    private static Logger log = LoggerFactory.getLogger(SpeleoGraphApp.class);

    public WaterHeightAction(Series series) {
        super();
        putValue(NAME, I18nSupport.translate("actions.waterHeight"));
        this.series = series;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PromptDialog dialog = new PromptDialog();
        dialog.setVisible(true);
    }

    private class PromptDialog extends FormDialog {
        private JComboBox<Series> seriesList = new JComboBox<>();
        private JComboBox<String> unitList = new JComboBox<>();
        {
            ListModel<Series> listModel = SpeleoGraphApp.getInstance().getSeriesList().getModel();
            for (int i = 0; i < listModel.getSize(); i++) {
                Series item = listModel.getElementAt(i);
                if (!item.equals(series) && item.isPressure())
                    seriesList.addItem(listModel.getElementAt(i));
            }
            unitList.addItem("cm");
            unitList.addItem("m");
            unitList.addItem("ft");
            unitList.addItem("in");
        }
        private FormLayout layout = new FormLayout("p:grow, p", "p,4dlu,p,4dlu,p,6dlu,p"); //NON-NLS

        public PromptDialog() {
            super();
            construct();
            this.setTitle(I18nSupport.translate("actions.waterHeight.title"));
        }

        @Override
        protected void setup() {
            PanelBuilder builder = new PanelBuilder(layout, getPanel());
            builder.addLabel(I18nSupport.translate("actions.waterHeight.selectSerie"));
            builder.nextLine(2);
            builder.add(seriesList);
            builder.nextLine(2);
            builder.add(unitList);
            builder.nextLine(2);
            builder.add(new JButton(new AbstractAction() {

                {
                    putValue(NAME, I18nSupport.translate("ok"));
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    validateForm();
                }
            }));

            Dimension dim = getPanel().getPreferredSize();
            getPanel().setPreferredSize(new Dimension(dim.width + 50, dim.height));
        }

        @Override
        protected void validateForm() {
            DateRange range = series.getRange();
            List<Item> itemsStandards = ((Series) seriesList.getSelectedItem()).getItems();
            List<Item> itemsToCompute = series.extractSubSerie(range.getLowerDate(), range.getUpperDate());
            ArrayList<Item> newItems = new ArrayList<>(itemsToCompute.size());
            final Series newSerie;
            final String seriesUnit = (String) (unitList.getSelectedItem());

            switch (seriesUnit.toLowerCase()) {
                case "cm":
                    newSerie = new Series(series.getOrigin(), org.cds06.speleograph.data.Type.WATER_HEIGHT);
                    break;
                default:
                    newSerie = new Series(series.getOrigin(), org.cds06.speleograph.data.Type.getType(I18nSupport.translate("actions.setType.waterHeight"), seriesUnit));
                    break;
            }
            newSerie.setName(series.getName() + " - " + I18nSupport.translate("actions.waterHeight.setName"));


            double multiplier = 1.02;
            String unit = series.getType().getUnit();

            // Pour avoir un r??sultat en cm
            if (unit.equalsIgnoreCase("bar"))
                multiplier *= 1000;
            else if (unit.equalsIgnoreCase("Pa"))
                multiplier /= 100;

            unit = newSerie.getType().getUnit();
            // Pour convertir ce r??sultat vers une autre unit??
            if (unit.equalsIgnoreCase("m"))
                multiplier /= 100;
            else if (unit.equalsIgnoreCase("ft"))
                multiplier /= 30.48;
            else if (unit.equalsIgnoreCase("in"))
                multiplier /= 2.54;

            for (Item item : itemsStandards) {
                ListIterator<Item> iter = itemsToCompute.listIterator();
                while (iter.hasNext()) {
                    Item itemCom = iter.next();
                    // Divided by 60000 to transform milliseconds into minutes.
                    if ((item.getDate().getTime() - itemCom.getDate().getTime())/60000 > TEMPORAL_RANGE_ACCEPTED) {
                        iter.remove();
                    } else if ((itemCom.getDate().getTime() - item.getDate().getTime())/60000 > TEMPORAL_RANGE_ACCEPTED) {
                        break;
                    } else {
                        newItems.add(new Item(newSerie, itemCom.getDate(), (itemCom.getValue() - item.getValue()) * multiplier));
                        iter.remove();
                    }
                }
            }
            newSerie.setItems(newItems, (String) getValue(NAME));

            setVisible(false);
        }

        @Override
        protected FormLayout getFormLayout() {
            return null;
        }
    }
}
