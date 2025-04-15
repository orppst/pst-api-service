package org.orph2020.pst.apiimpl.entities.opticalTelescopeService;
import java.util.HashMap;

/**
 * container of a xml document.
 */
public class Telescope {

    // the telescope name
    private final String name;

    // container for instruments
    private final HashMap<String, Instrument> instruments;

    /**
     * constructor
     *
     * @param name: the telescope name
     */
    public Telescope(String name) {
        this.name = name;
        this.instruments = new HashMap<String, Instrument>();
    }

    /**
     * adds an instrument into the map of instruments.
     *
     * @param instrument: the instrument to add.
     */
    public void addInstrument(Instrument instrument) {
        this.instruments.put(instrument.getName(), instrument);
    }

    /**
     * gets an instrument. makes one if there isn't one already.
     *
     * @param name: the name of the instrument.
     * @return the instrument object.
     */
    public Instrument getInstrument(String name) throws Exception {
        return this.instruments.get(name);
    }

    /**
     * getter for name.
     * @return the name of the telescope.
     */
    public String getName() {
        return name;
    }

    /**
     * getter for the instruments.
     *
     * @return the hashmap of instruments.
     */
    public HashMap<String, Instrument> getInstruments() {
        return this.instruments;
    }

    /**
     * builds the to string to represent the telescope.
     *
     * @return the string representation of the telescope.
     */
    @Override
    public String toString() {
        // create instruments string.
        StringBuilder instruments = new StringBuilder();
        for(Instrument instrument: this.instruments.values()) {
            instruments.append(", ");
            instruments.append(instrument.toString());
        }

        return String.format(
            "Telescope name: %s, instruments: %s", this.name, instruments);
    }
}
