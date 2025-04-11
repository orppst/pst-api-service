package org.orph2020.pst.apiimpl.entities.opticalTelescopeService;

import java.util.ArrayList;
import java.util.List;

/**
 * container for a fields type and its options if it is a list.
 */
public class Field {

    // the enum values.
    public enum TYPES {LIST, TEXT, BOOLEAN}

    // the type to mark this field.
    private final TYPES type;

    // the values of this option.
    private final ArrayList<String> values;

    /**
     * builds a new field object.
     *
     * @param type: the type of this field.
     */
    public Field(TYPES type){
        this.type = type;
        this.values = new ArrayList<>();
    }

    /**
     * adds a new option to the field object (when it's a list).
     *
     * @param value: the value to add.
     */
    public void add(String value) throws Exception {
        if (this.type != TYPES.LIST) {
            throw new Exception(
                "Tried to add a option to a type which doesnt accept options.");
        }
        this.values.add(value);
    }

    /**
     * getter for the type of this field.
     *
     * @return the type of this field.
     */
    public TYPES getType() {
        return type;
    }

    /**
     * getter for the values of the field.
     *
     * @return the list of values.
     */
    public List<String> getValues() {
        return values;
    }

    /**
     *  public to string.
     *
     * @return human representation of the field.
     */
    @Override
    public String toString() {
        return "Field{" +
            "type=" + type +
            ", values=" + values +
            '}';
    }
}
