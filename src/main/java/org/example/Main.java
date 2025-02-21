package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main {
    private static JsonObject data; // JSON-Daten für den Chatbot
    private static final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE; // NLP-Tokenisierung

    public static void main(String[] args) {
        try (InputStream inputStream = Main.class.getResourceAsStream("/chatbot_data.json")) {
            if (inputStream == null) {
                throw new FileNotFoundException("JSON-Datei nicht gefunden!");
            }

            // JSON-Daten lesen und parsen
            String json = new String(inputStream.readAllBytes());
            data = JsonParser.parseString(json).getAsJsonObject();

            // Chat starten
            System.out.println("Willkommen beim Chatbot! Wie kann ich Ihnen helfen? (Tippen Sie 'exit' zum Beenden)");

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Sie: ");
                String userInput = scanner.nextLine().trim().toLowerCase();

                // Chat beenden
                if (userInput.equals("exit")) {
                    System.out.println("Chatbot: Vielen Dank! Auf Wiedersehen!");
                    break;
                }

                // Generiere Antwort basierend auf der Eingabe
                String response = generateResponse(userInput);
                System.out.println("Chatbot: " + response);
            }

        } catch (Exception e) {
            System.err.println("Ein Fehler ist aufgetreten: " + e.getMessage());
        }
    }

    private static String generateResponse(String input) {
        // NLP: Tokenisiere den Satz
        String[] tokens = tokenizer.tokenize(input);

        // Liste der Schlüsselwörter für Typo-Toleranz
        String[] keywords = {"fortbildung", "urlaubstag", "urlaub", "ansprechpartner", "kontakt", "datenschutz"};
        String matchedKeyword = findClosestKeyword(tokens, keywords);

        if (matchedKeyword == null) {
            return "Das habe ich nicht verstanden. Können Sie Ihre Frage anders formulieren?";
        }

        switch (matchedKeyword) {
            case "fortbildung":
                return getFortbildungen();
            case "urlaubstag":
            case "urlaub":
                return getUrlaubstage();
            case "ansprechpartner":
            case "kontakt":
                String department = extractDepartment(tokens);
                return getAnsprechpartner(department);
            case "datenschutz":
                return data.get("datenschutz").getAsString();
            default:
                return "Das habe ich nicht verstanden. Können Sie Ihre Frage anders formulieren?";
        }
    }

    private static String extractDepartment(String[] tokens) {
        // Liste der möglichen Abteilungen
        String[] departments = {"IT", "HR", "Finanzen"};

        // Suche nach der nächsten Übereinstimmung (Typo-Toleranz beachten)
        return findClosestKeyword(tokens, departments);
    }

    private static String findClosestKeyword(String[] tokens, String[] keywords) {
        String closestKeyword = null;
        int minDistance = Integer.MAX_VALUE;

        for (String token : tokens) {
            for (String keyword : keywords) {
                int distance = levenshteinDistance(token.toLowerCase(), keyword.toLowerCase());
                if (distance < minDistance) {
                    minDistance = distance;
                    closestKeyword = keyword;
                }
            }
        }

        // Maximale Toleranz: Schlüsselwort muss mindestens ähnlich sein
        return (minDistance <= 2) ? closestKeyword : null;
    }

    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    private static String getFortbildungen() {
        JsonArray fortbildungen = data.getAsJsonArray("fortbildungen");
        StringBuilder response = new StringBuilder("Hier sind die verfügbaren Fortbildungen:\n");
        for (JsonElement element : fortbildungen) {
            JsonObject fortbildung = element.getAsJsonObject();
            response.append(String.format("- %s (%s) am %s in %s, Level: %s%n",
                    fortbildung.get("titel").getAsString(),
                    fortbildung.get("kategorie").getAsString(),
                    fortbildung.get("datum").getAsString(),
                    fortbildung.get("ort").getAsString(),
                    fortbildung.get("level").getAsString()));
        }
        return response.toString();
    }

    private static String getUrlaubstage() {
        JsonObject urlaubstage = data.getAsJsonObject("urlaubstage");
        StringBuilder response = new StringBuilder("Ihre verbleibenden Urlaubstage:\n");
        for (String mitarbeiter : urlaubstage.keySet()) {
            int verbleibend = urlaubstage.get(mitarbeiter).getAsInt();
            response.append(String.format("- %s: %d Tage%n", mitarbeiter, verbleibend));
        }
        response.append("\nEinen Urlaubsantrag können Sie in der HR einreichen.");
        return response.toString();
    }

    private static String getAnsprechpartner(String abteilungFilter) {
        JsonArray ansprechpartner = data.getAsJsonArray("ansprechpartner");
        StringBuilder response = new StringBuilder();

        for (JsonElement element : ansprechpartner) {
            JsonObject person = element.getAsJsonObject();
            String abteilung = person.get("abteilung").getAsString();

            if (abteilungFilter == null || abteilung.equalsIgnoreCase(abteilungFilter)) {
                response.append(String.format("- Abteilung: %s, Name: %s, E-Mail: %s%n",
                        abteilung,
                        person.get("name").getAsString(),
                        person.get("email").getAsString()));
            }
        }

        if (response.length() == 0) {
            return "Keine Ansprechpartner für die angegebene Abteilung gefunden.";
        }

        return response.toString();
    }
}