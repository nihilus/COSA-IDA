package com.tosnos.cosa.util;

/**
 * Created by kevin on 8/6/14.
 */
public class Ansi {
    public static final int BLACK = 0;

    ;
    public static final int RED = 1;
    public static final int GREEN = 2;
    public static final int YELLOW = 3;
    public static final int BLUE = 4;
    public static final int PURPLE = 5;
    public static final int CYAN = 6;
    public static final int WHITE = 7;
    private static final String ANSI = "\u001B[";
    private static final String ANSI_RESET = "\u001B[0m";
    private static boolean enabled = true;
    private StringBuilder sb;

    public Ansi() {
        sb = new StringBuilder();
    }

    public Ansi(StringBuilder sb) {
        this.sb = sb;
    }

    public Ansi fg(int COLOR) {
        return new Ansi(enabled ? sb.append(ANSI).append(30 + COLOR).append("m") : sb);
    }

    public Ansi bg(int COLOR) {
        return new Ansi(enabled ? sb.append(ANSI).append(40 + COLOR).append("m") : sb);
    }

    public Ansi a(String text) {
        return new Ansi(sb.append(text));
    }

    public Ansi reset() {
        return new Ansi(enabled ? sb.append(ANSI_RESET) : sb);
    }

    public void disable() {
        enabled = false;
    }

    public void enable() {
        enabled = true;
    }

    public String toString() {
        return sb.toString();
    }

    public enum color {BLACK, RED, GREEN, YELLOW, BLUE, PURPLE, CYAN, WHITE}
}
