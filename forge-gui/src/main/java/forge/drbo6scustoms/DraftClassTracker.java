package forge.drbo6scustoms;

import forge.localinstance.properties.ForgeConstants;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class DraftClassTracker {

    public void StartDraftStats(String humanDeckName, String AIDeckNumber, Boolean gauntletActive) {
        String fileName = "draftstats.properties";
        String filepath = ForgeConstants.DECK_DRAFT_DIR + humanDeckName + ForgeConstants.PATH_SEPARATOR + fileName;
        File draftStatsFile = new File(filepath);

        Properties props = new Properties();
        if (!draftStatsFile.exists()) {
            props.setProperty("humanDeckName", humanDeckName);
            props.setProperty("AIDeckNumber", AIDeckNumber);
            props.setProperty("GWvs1", "0"); // Games won vs AI deck 1
            props.setProperty("GWvs2", "0");
            props.setProperty("GWvs3", "0");
            props.setProperty("GWvs4", "0");
            props.setProperty("GWvs5", "0");
            props.setProperty("GWvs6", "0");
            props.setProperty("GWvs7", "0");
            props.setProperty("GLvs1", "0");
            props.setProperty("GLvs2", "0");
            props.setProperty("GLvs3", "0");
            props.setProperty("GLvs4", "0");
            props.setProperty("GLvs5", "0");
            props.setProperty("GLvs6", "0");
            props.setProperty("GLvs7", "0");
            props.setProperty("MWvs1", "0"); // Matches won vs AI deck 1
            props.setProperty("MWvs2", "0");
            props.setProperty("MWvs3", "0");
            props.setProperty("MWvs4", "0");
            props.setProperty("MWvs5", "0");
            props.setProperty("MWvs6", "0");
            props.setProperty("MWvs7", "0");
            props.setProperty("MLvs1", "0");
            props.setProperty("MLvs2", "0");
            props.setProperty("MLvs3", "0");
            props.setProperty("MLvs4", "0");
            props.setProperty("MLvs5", "0");
            props.setProperty("MLvs6", "0");
            props.setProperty("MLvs7", "0");
            props.setProperty("GauntletActive", gauntletActive.toString());
            WriteToPropsFile(filepath, props);
        } else {
            System.out.println(filepath + " exists. No need to create the file at this time.");
        }
    }

    public void UpdateDraftStats(String humanDeckName, String AIDeckNumber) {
        String fileName = "draftstats.properties";
        String filepath = ForgeConstants.DECK_DRAFT_DIR + humanDeckName + ForgeConstants.PATH_SEPARATOR + fileName;
        File draftStatsFile = new File(filepath);
        Properties props = new Properties();
        if (draftStatsFile.exists()) {
            // Read properties from the existing file
            props = ReadFromPropsFile(filepath);

            // Cancel the operation if any of the keys does not exist
            String[] requiredKeys = {"humanDeckName", "AIDeckNumber", "GauntletActive"};
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
            String prevAIDeckNumber = props.getProperty("AIDeckNumber"); // This will be needed for gauntlet
            String prevGauntletActive = props.getProperty("GauntletActive"); // This will be needed for gauntlet

            // Make sure that we are writing to the correct file. We should be, but just in case.
            if (Objects.equals(humanDeckName, prevHumanDeckName)) {
                int intAIDeckNumber = Integer.parseInt(AIDeckNumber);
                String GWKey = "GWvs" + intAIDeckNumber;
                String GWValue = props.getProperty(GWKey);
                int GWValueInt = Integer.parseInt(GWValue);
                GWValueInt++; // Just increase it as a test; CONTINUE HERE
                props.setProperty(GWKey, String.valueOf(GWValueInt));  // Save the updated value back to the properties
            }

            // Write the updated properties back to the file
            WriteToPropsFile(filepath, props);
        } else {
            System.out.println("Draft Stats file does not exist, unable to update.");
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

    public void printReceivedValues(Object... values) {
        for (Object value : values) {
            System.out.println(value);
        }
    }

}