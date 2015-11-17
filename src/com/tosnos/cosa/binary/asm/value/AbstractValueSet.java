package com.tosnos.cosa.binary.asm.value;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kevin on 10/5/15.
 */
public class AbstractValueSet extends AbstractValue { // for possible value set
    List<AbstractValue> valueList = new ArrayList<AbstractValue>();

    public void addValue(AbstractValue value) {
        this.valueList.add(value);
    }

    public int size() {
        return valueList.size();
    }

    public List<AbstractValue> getValues() {
        return valueList;
    }

    @Override
    public boolean isValueSet() {
        return true;
    }

    @Override
    public Integer intValue() {
        throw new RuntimeException("N/A");
    }
}
