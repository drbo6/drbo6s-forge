package forge.drbo6scustoms;

import forge.localinstance.properties.ForgeConstants;

import java.io.*;
import java.util.*;

public class DraftClassTracker {

    public static void InitializeDraftStatsFile(String humanDeckName) {
        String fileName = "draftstats.properties";
        String filepath = ForgeConstants.DECK_DRAFT_DIR + humanDeckName + ForgeConstants.PATH_SEPARATOR + fileName;
        File draftStatsFile = new File(filepath);

        Properties props = new Properties();
        if (!draftStatsFile.exists()) {
            props.setProperty("humanDeckName", humanDeckName);
            props.setProperty("latestAIOpponentDeckNumber", "0");
            for (int i = 1; i <= 7; i++) {
                props.setProperty("GWvs" + i, "0"); // Games won vs AI
                props.setProperty("GLvs" + i, "0");
                props.setProperty("MWvs" + i, "0"); // Matches won vs AI
                props.setProperty("MLvs" + i, "0");
            }
            props.setProperty("GauntletActive", "false");
            WriteToPropsFile(filepath, props);
        } else {
            System.out.println(filepath + " exists. No need to create the file at this time.");
        }
    }

    public static void UpdateDraftStatsOpponentAndDuelType(String humanDeckName, String latestAIOpponentDeckNumber, Boolean gauntletActive) {
        String fileName = "draftstats.properties";
        String filepath = ForgeConstants.DECK_DRAFT_DIR + humanDeckName + ForgeConstants.PATH_SEPARATOR + fileName;
        File draftStatsFile = new File(filepath);

        Properties props = new Properties();
        if (draftStatsFile.exists()) {
            props = ReadFromPropsFile(filepath); // This is needed to ensure you do not delete entries that you are not modifying
            props.setProperty("humanDeckName", humanDeckName);
            props.setProperty("latestAIOpponentDeckNumber", latestAIOpponentDeckNumber);
            props.setProperty("GauntletActive", gauntletActive.toString());
            WriteToPropsFile(filepath, props);
        } else {
            System.out.println(filepath + " does not exists! Check your code.");
        }
    }

    public static void UpdateDraftStatsResults(String humanDeckName, String latestAIOpponentDeckNumber) {
        String fileName = "draftstats.properties";
        String filepath = ForgeConstants.DECK_DRAFT_DIR + humanDeckName + ForgeConstants.PATH_SEPARATOR + fileName;
        File draftStatsFile = new File(filepath);
        Properties props = new Properties();
        if (draftStatsFile.exists()) {
            // Read properties from the existing file
            props = ReadFromPropsFile(filepath);

            // Cancel the operation if any of the keys does not exist
            String[] requiredKeys = {"humanDeckName", "latestAIOpponentDeckNumber", "GauntletActive"};
            for (String key : requiredKeys) {
                if (!props.containsKey(key)) {
                    System.out.println("One or more required keys are missing from the properties file. Cancelling operation.");
                    return; // Cancel operation if any key is missing
                }
            }
            String[] keyPrefixes = {"GW", "GL", "MW", "ML"};
            for (String prefix : keyPrefixes) {
                for (int i = 1; i <= 7; i++) {
                    String key = prefix + "vs" + i;  // Generate the key like "GWvs1", "GWvs2", ..., "MLvs7"
                    if (!props.containsKey(key)) {
                        System.out.println("One or more required keys are missing from the properties file. Cancelling operation.");
                        return; // Cancel operation if any key is missing
                    }
                }
            }

            // Create variables from the properties file content
            String prevHumanDeckName = props.getProperty("humanDeckName");
            String AIOpponentDeckNumber = props.getProperty("latestAIOpponentDeckNumber"); // This will be needed for gauntlet
            String prevGauntletActive = props.getProperty("GauntletActive"); // This will be needed for gauntlet

            // Make sure that we are writing to the correct file. We should be, but just in case.
            if (Objects.equals(humanDeckName, prevHumanDeckName)) {
                int intAIDeckNumber = Integer.parseInt(AIOpponentDeckNumber);
                String MWKey = "MWvs" + intAIDeckNumber;
                String MWValue = props.getProperty(MWKey);
                int GWValueInt = Integer.parseInt(MWValue);
                GWValueInt++; // Just increase it as a test; CONTINUE HERE
                props.setProperty(MWKey, String.valueOf(GWValueInt));  // Save the updated value back to the properties
            }

            // Write the updated properties back to the file
            WriteToPropsFile(filepath, props);
        } else {
            System.out.println("Draft Stats file does not exist, unable to update.");
        }
    }

    public static Map<String, Integer> loadDraftStatsResults(String humanDeckName) {
        String fileName = "draftstats.properties";
        String filepath = ForgeConstants.DECK_DRAFT_DIR + humanDeckName + ForgeConstants.PATH_SEPARATOR + fileName;
        File draftStatsFile = new File(filepath);
        Properties props = new Properties();
        if (draftStatsFile.exists()) {

            // Read properties from the existing file
            props = ReadFromPropsFile(filepath);

            // Create a map to hold the results for win/loss
            Map<String, Integer> winLossData = new HashMap<>();

            // Load data for both wins and losses
            for (int i = 1; i <= 7; i++) { // Iterate over the 7 possible AI deck numbers
                String gameWinKey = "GWvs" + i; // Key for "Games Won" against AI deck i
                String gameLossKey = "GLvs" + i; // Key for "Games Lost" against AI deck i
                String matchWinKey = "MWvs" + i; // Key for "Matches Won" against AI deck i
                String matchLossKey = "MLvs" + i; // Key for "Matches Lost" against AI deck i

                // Get the values for wins and losses, defaulting to 0 if not found
                int wins = Integer.parseInt(props.getProperty(gameWinKey, "0"));
                int losses = Integer.parseInt(props.getProperty(gameLossKey, "0"));
                int matchWins = Integer.parseInt(props.getProperty(matchWinKey, "0"));
                int matchLosses = Integer.parseInt(props.getProperty(matchLossKey, "0"));

                // Store the data in the map
                winLossData.put("GWvs" + i, wins);
                winLossData.put("GLvs" + i, losses);
                winLossData.put("MWvs" + i, matchWins);
                winLossData.put("MLvs" + i, matchLosses);
            }
            return winLossData;
        } else {
            System.out.println("Draft Stats file does not exist, unable to display the win/loss data.");
            return null;
        }
    }

    private static void WriteToPropsFile(String filepath, Properties props) {
        // Update the file
        try (FileWriter writer = new FileWriter(filepath)) {
            props.store(writer, "DrBo6's Draft Stats Tracker");
            System.out.println("Properties written to " + filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Properties ReadFromPropsFile(String filepath) {
        Properties props = new Properties();
        try (FileReader reader = new FileReader(filepath)) {
            props.load(reader);
            System.out.println("Properties read from " + filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

    public static void printReceivedValues(Object... values) {
        for (Object value : values) {
            System.out.println(value);
        }
    }

}