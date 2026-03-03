package utils;

public class AppState {
    private static String coachingLang = "en";
    public static String getCoachingLang() { return coachingLang; }
    public static void setCoachingLang(String lang) { coachingLang = lang; }
}