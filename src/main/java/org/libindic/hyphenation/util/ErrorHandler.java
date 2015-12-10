package org.libindic.hyphenation.util;


/**
 * Generic Error Handler interface
 */
public interface ErrorHandler {
    /**
     * debug
     *
     * @param domain  a string used to display debugging information selectively
     * @param message debugging information
     */
    public void debug(String domain, String message);

    /**
     * say something
     *
     * @param s the thing to say
     */
    public void info(String s);

    /**
     * report a warning
     *
     * @param s explanation
     */
    public void warning(String s);

    /**
     * report an error
     *
     * @param s explanation
     */
    public void error(String s);

    /**
     * report an error caused by a caught exception;
     *
     * @param s explanation
     * @param e exception
     */
    public void exception(String s, Exception e);
}
