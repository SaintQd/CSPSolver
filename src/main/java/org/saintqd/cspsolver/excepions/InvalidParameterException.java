package org.saintqd.cspsolver.excepions;

public class InvalidParameterException extends RuntimeException {

    public InvalidParameterException() {
        super("Array length mismatch error. Recheck the parameters.");
    }

}
