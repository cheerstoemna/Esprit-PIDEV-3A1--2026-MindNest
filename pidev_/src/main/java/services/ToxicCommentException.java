package services;

public class ToxicCommentException extends RuntimeException {
    public ToxicCommentException(String message) {
        super(message);
    }
}