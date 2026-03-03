package models;

public class Book {
    private String title;
    private String authors;
    private String previewLink;

    public Book(String title, String authors, String previewLink) {
        this.title = title;
        this.authors = authors;
        this.previewLink = previewLink;
    }

    // Getters & Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getPreviewLink() {
        return previewLink;
    }

    public void setPreviewLink(String previewLink) {
        this.previewLink = previewLink;
    }

    @Override
    public String toString() {
        return title + " by " + authors;
    }
}