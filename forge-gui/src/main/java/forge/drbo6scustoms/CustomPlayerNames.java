package forge.drbo6scustoms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomPlayerNames {

    // Add this field to hold the names
    public static final Map<Integer, String> customPlayerNames = new HashMap<>();

    // Method to load names from the file
    public static Map<Integer, String> loadCustomPlayerNames(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    int index = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    customPlayerNames.put(index, name);
                }
            }
            return customPlayerNames;
        } catch (IOException e) {
            System.err.println("Error reading player names file: " + filename + " - e: " + e.getMessage());
            return null;
        }
    }

}