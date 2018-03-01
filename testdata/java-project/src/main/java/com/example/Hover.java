package com.example;

/**
 * Hover javadoc.
 *
 * @see Definition
 */
public class Hover {

    /**
     * Field javadoc.
     */
    public final int value;

    /**
     * Constructor javadoc.
     * @see #Hover(int)
     */
    public Hover() {
        this(1);
    }

    /**
     * Secondary constructor javadoc.
     * @param value value param
     */
    public Hover(int value) {
        this.value = value;
    }

    /**
     * Method javadoc.
     */
    public void method() {
        System.out.println(value);
    }
}
