package org.orph2020.pst.apiimpl.entities.opticalTelescopeService;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * contains the Instrument data.
 */
public class Instrument {

    private static final Logger LOGGER = Logger.getLogger("Instrument");

    // the name of the instrument
    private final String name;

    // the named elements and their options.
    private final HashMap<String, Field> elements;

    /**
     * builds an instrument
     *
     * @param name: the name of the instrument.
     */
    public Instrument(String name) {
        this.name = name;
        this.elements = new HashMap<String, Field>();
    }

    /**
     * adds an element to the instrument.
     *
     * @param element the element to add.
     * @param type: enum to help data type mapping
     */
    public void addElement(String element, Field.TYPES type) {
        if( !this.elements.containsKey(element)) {
            this.elements.put(element, new Field(type));
        }
    }

    /**
     * adds an option to the element.
     *
     * @param element the element to add the option to.
     *                builds a default one if none exist.
     * @param option the option to add to the element.
     */
    public void addOption(String element, String option) throws Exception {
        // verify that the element exists.
        if( !this.elements.containsKey(element)) {
            LOGGER.log(Level.WARNING, String.format(
                "The instrument %s, does not contain %s and so option %s " +
                    "cant be used in this combination. If this was wanted. " +
                    "Please place element %s into its " +
                    "instrumentConfigurations subfields config",
                this.name, element, option, element
            ));
        } else {
            this.elements.get(element).add(option);
        }
    }

    /**
     * the getter for name.
     *
     * @return the instrument name.
     */
    public String getName() {
        return name;
    }

    /**
     * getter for elements.
     *
     * @return the map of elements.
     */
    public HashMap<String, Field> getElements() {
        return elements;
    }

    /**
     * generates the string representation of the instrument.
     *
     * @return the string representation of the instrument and its options.
     */
    @Override
    public String toString() {
        return "Instrument{" +
            "name='" + name + '\'' +
            ", elements=" + elements +
            '}';
    }
}
