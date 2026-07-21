package org.saintqd.cspsolver.excepions;

public class InvalidLengthException extends RuntimeException {

    public InvalidLengthException() {
        super("Piece size exceeds the stock size.");
    }

}
