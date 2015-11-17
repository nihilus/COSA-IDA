package com.tosnos.cosa.binary.asm.value;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kevin on 1/22/15.
 */
public class Pthread extends AbstractValue {
    private static Map<AbstractValue, Pthread> threadMap = new HashMap<AbstractValue, Pthread>();


    private static int total_id = 1;
    private final int id;
    private boolean detached = false;

    public Pthread() {
        id = total_id++;
    }

    public int getID() {
        return id;
    }

    @Override
    public String toString() {
        return "Pthread(" + id + ")";
    }

    public void setDetached() {
        detached = false;
    }

    @Override
    public boolean isPthread_t() {
        return true;
    }

    public Integer intValue() {
        return id;
    }

    @Override
    public Numeral getNumeral() {
        return new Numeral(id);
    }

    public static Pthread getPthead(AbstractValue key) {
        Pthread pthread = threadMap.get(key);
        if (pthread == null) {
            pthread = new Pthread();
            threadMap.put(key, pthread);
        }
        return pthread;
    }

    @Override
    public AbstractValue getReplacement(Numeral numeral) {
        return this;
    }

    public static Key getKey() {
        return new Key();
    }


    public static class Key extends AbstractValue {
        private static int total_id = 1;
        private final int id;
        AbstractValue value;

        public Key() {
            id = total_id++;
        }

        @Override
        public AbstractValue getReplacement(Numeral numeral) {
            return this;
        }

        public void setSpecificValue(AbstractValue value) {
            this.value = value;
        }

        public AbstractValue getSpecificValue() {
            return value;
        }
        @Override
        public Numeral getNumeral() {
            return new Numeral(id);
        }

        public String toString() {
            return "PthreadKey";
        }
    }

}
