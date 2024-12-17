package forge.drbo6scustoms;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;

import java.io.*;
import java.util.*;
import java.util.ArrayList;

public class DraftClassTracker {
    private static boolean isInitializingFile = false;

    public static void InitializeDraftStatsFile(String humanDeckName) {
        String fileName = "draftstats.properties";
        String filepath = ForgeConstants.DECK_DRAFT_DIR + humanDeckName + ForgeConstants.PATH_SEPARATOR + fileName;
        File draftStatsFile = new File(filepath);

        Properties props = new Properties();
        if (!draftStatsFile.exists()) {
            props.setProperty("humanDeckName", humanDeckName);
            for (int i = 1; i <= 7; i++) {
                props.setProperty("AIDeckName" + i, getAIDeckName(humanDeckName, i, 0)); // Games won vs AI
                props.setProperty("GWvs" + i, "0"); // Games won vs AI
                props.setProperty("GLvs" + i, "0");
                props.setProperty("MWvs" + i, "0"); // Matches won vs AI
                props.setProperty("MLvs" + i, "0");
            }
            props.setProperty("WinningStreak", "0");
            props.setProperty("LosingStreak", "0");
            props.setProperty("LongestWinningStreak", "0");
            props.setProperty("LongestLosingStreak", "0");
            props.setProperty("GauntletActive", "false");
            props.setProperty("latestAIOpponentDeckNumber", "0");
            WriteToPropsFile(filepath, props);
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

    public static void UpdateDraftStatsResults(String humanDeckName, String latestAIOpponentDeckNumber, Boolean humanWinner, Boolean matchCompleted) {
        String fileName = "draftstats.properties";
        String filepath = ForgeConstants.DECK_DRAFT_DIR + humanDeckName + ForgeConstants.PATH_SEPARATOR + fileName;
        File draftStatsFile = new File(filepath);
        Properties props = new Properties();
        if (draftStatsFile.exists()) {
            // Read properties from the existing file
            props = ReadFromPropsFile(filepath);

            // Cancel the operation if any of the keys does not exist
            String[] requiredKeys = {"humanDeckName", "latestAIOpponentDeckNumber", "GauntletActive", "WinningStreak", "LosingStreak", "LongestWinningStreak", "LongestLosingStreak"};
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

            // Load the data
            Map<String, Object> draftStatsMap = loadDraftStatsMap(props);

            // Process the results
            if (Objects.equals(humanDeckName, draftStatsMap.get("humanDeckName"))) { // Make sure that we are writing to the correct file. We should be, but just in case.

                if (matchCompleted) {
                    String matchWinKey = "MWvs" + latestAIOpponentDeckNumber;
                    String matchLossKey = "MLvs" + latestAIOpponentDeckNumber;
                    if (humanWinner) {
                        int currentWins = Integer.parseInt(props.getProperty(matchWinKey, "0"));
                        props.setProperty(matchWinKey, String.valueOf(currentWins + 1));
                    } else {
                        int currentLosses = Integer.parseInt(props.getProperty(matchLossKey, "0"));
                        props.setProperty(matchLossKey, String.valueOf(currentLosses + 1));
                    }
                }
                String gameWinKey = "GWvs" + latestAIOpponentDeckNumber;
                String gameLossKey = "GLvs" + latestAIOpponentDeckNumber;
                if (humanWinner) {
                    int currentWins = Integer.parseInt(props.getProperty(gameWinKey, "0"));
                    props.setProperty(gameWinKey, String.valueOf(currentWins + 1));
                } else {
                    int currentLosses = Integer.parseInt(props.getProperty(gameLossKey, "0"));
                    props.setProperty(gameLossKey, String.valueOf(currentLosses + 1));
                }

            } else {
                System.out.println("The decks are not matching up.");
            }

            // Write the updated properties back to the file
            WriteToPropsFile(filepath, props);
        } else {
            System.out.println("Draft Stats file does not exist, unable to update.");
        }
    }

    private static Map<String, Object> loadDraftStatsMap(Properties props) {
        Map<String, Object> draftStatsMap = new HashMap<>();

        // Get the human deck name
        draftStatsMap.put("humanDeckName", props.getProperty("humanDeckName", "Decky McDeckFace"));

        // Load AI Deck Names
        String[] aiDeckNames = new String[7];
        for (int i = 0; i < 7; i++) {
            aiDeckNames[i] = props.getProperty("AIDeckName" + (i + 1), "AI Deck " + String.valueOf(i + 1));
        }
        draftStatsMap.put("aiDeckNames", aiDeckNames);

        // Get the variables needed for Gauntlet
        // I don't think I ever actually use them as I found another way to check for Gauntlets and the AI deck number
        // The gauntletActive is set when launching one, but it is never turned off
        draftStatsMap.put("gauntletActive", Boolean.parseBoolean(props.getProperty("GauntletActive", "false")));
        draftStatsMap.put("latestAIOpponentDeckNumber", Integer.parseInt(props.getProperty("latestAIOpponentDeckNumber", "0")));

        // Get the winning streak variables
        draftStatsMap.put("WinningStreak", props.getProperty("WinningStreak", "0"));
        draftStatsMap.put("LosingStreak", props.getProperty("LosingStreak", "0"));
        draftStatsMap.put("LongestWinningStreak", props.getProperty("LongestWinningStreak", "0"));
        draftStatsMap.put("LongestLosingStreak", props.getProperty("LongestLosingStreak", "0"));

        // Get the win/loss records
        String[] keyPrefixes = {"GL", "GW", "ML", "MW"};
        Map<String, int[]> stats = new HashMap<>();
        for (String prefix : keyPrefixes) {
            int[] values = new int[7];
            for (int i = 0; i < 7; i++) {
                values[i] = Integer.parseInt(props.getProperty(prefix + "vs" + (i + 1), "0"));
            }
            stats.put(prefix, values);
        }
        draftStatsMap.put("stats", stats);

        return draftStatsMap;
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

    public static String getDraftStatAggregate(String humanDeckName, String stat) {
        String fileName = "draftstats.properties";
        String filepath = ForgeConstants.DECK_DRAFT_DIR + humanDeckName + ForgeConstants.PATH_SEPARATOR + fileName;
        File draftStatsFile = new File(filepath);
        Properties props = new Properties();
        if (draftStatsFile.exists()) {

            isInitializingFile = false;

            // Read properties from the existing file
            props = ReadFromPropsFile(filepath);

            // Get total of the Stat Value
            int StatTotal = 0;
            for (int i = 1; i <= 7; i++) { // Iterate over the 7 possible AI deck numbers
                String gameWinKey = stat + "vs" + i; // Key for "Games Won" against AI deck i

                // Get the values for wins and losses, defaulting to 0 if not found
                StatTotal = StatTotal + Integer.parseInt(props.getProperty(gameWinKey, "0"));
            }

            return String.valueOf(StatTotal);

        } else {
            if (isInitializingFile) {
                return "N/A";
            } else {
                isInitializingFile = true;
                InitializeDraftStatsFile(humanDeckName);
                return getDraftStatAggregate(humanDeckName, stat);
            }
        }
    }

    public static String getDraftStatPercentage(String humanDeckName, String stat) {
        String fileName = "draftstats.properties";
        String filepath = ForgeConstants.DECK_DRAFT_DIR + humanDeckName + ForgeConstants.PATH_SEPARATOR + fileName;
        File draftStatsFile = new File(filepath);
        Properties props = new Properties();
        if (draftStatsFile.exists()) {

            // Read properties from the existing file
            props = ReadFromPropsFile(filepath);

            // Get total of the Stat Value
            int WinTotal = 0;
            int GamesPlayed = 0;
            for (int i = 1; i <= 7; i++) { // Iterate over the 7 possible AI deck numbers
                String gameWinKey = stat + "Wvs" + i; // Key for "Games Won" against AI deck i
                String gameLossKey = stat + "Lvs" + i; // Key for "Games Won" against AI deck i

                // Get the values for wins and losses, defaulting to 0 if not found
                WinTotal = WinTotal + Integer.parseInt(props.getProperty(gameWinKey, "0"));
                GamesPlayed = GamesPlayed + Integer.parseInt(props.getProperty(gameWinKey, "0")) + Integer.parseInt(props.getProperty(gameLossKey, "0"));
            }

            return (GamesPlayed == 0 ? "0%" : String.valueOf("." + Math.round(1000.0 * WinTotal / GamesPlayed)));

        } else {
            if (isInitializingFile) {
                return "N/A";
            } else {
                isInitializingFile = true;
                InitializeDraftStatsFile(humanDeckName);
                return getDraftStatPercentage(humanDeckName, stat);
            }
        }
    }

    public static String[] getAllAIDeckNamesFromDraftStats(String humanDeckName) {
        // Path to the draft properties file for the specified human deck
        String fileName = "draftstats.properties";
        String filepath = ForgeConstants.DECK_DRAFT_DIR + humanDeckName + ForgeConstants.PATH_SEPARATOR + fileName;
        File draftPropsFile = new File(filepath);

        Properties props = new Properties();
        ArrayList<String> aiDeckNames = new ArrayList<>();

        if (draftPropsFile.exists()) {
            // Read properties from the existing file
            props = ReadFromPropsFile(filepath);

            // Loop through keys and find all AIDeckName keys
            for (int i = 1; i <= 7; i++) {
                String key = "AIDeckName" + i;  // Generate the key for each AI deck (AIDeckName1, AIDeckName2, ...)
                if (props.containsKey(key)) {
                    // Add the AI deck name to the list
                    aiDeckNames.add(props.getProperty(key));
                }
            }

            // Convert the ArrayList to a String array
            return aiDeckNames.toArray(new String[0]);
        } else {
            System.out.println("Draft properties file does not exist at: " + filepath);
            return new String[0];  // Return an empty array if the file doesn't exist
        }
    }


    private static String getAIDeckName(String humanDeckName, Integer AIDeckNumber, int attemptCount) {
        final DeckGroup opponentDecks = FModel.getDecks().getDraft().get(humanDeckName);
        Deck d = opponentDecks.getAiDecks().get(AIDeckNumber - 1);

        if (d == null) {
            System.out.println("Error: Deck with ID " + AIDeckNumber + " not found.");
            return "Decky McDeckFace";
        }

        // Get the CardPool
        CardPool cards = d.getAllDeckCardsInASinglePool();

        // Convert the CardPool to a List of card names
        ArrayList<String> cardNames = getAllCardNamesAsArrayList(cards);

        // Define lands to filter out
        List<String> lands = Arrays.asList("Plains", "Swamp", "Island", "Mountain", "Forest");

        // Remove forbidden card names from the list
        cardNames.removeIf(name -> lands.contains(name));

        // If there are fewer than 3 cards left after filtering, return a default message
        if (cardNames.size() < 3) {
            System.out.println("Error: Not enough valid cards in the deck to select three.");
            return "Decky McDeckFace";
        }

        // Shuffle the card names to select 3 random cards
        Collections.shuffle(cardNames);

        // Select the three cards
        String firstCardName = cardNames.get(0);
        String secondCardName = cardNames.get(1);
        String thirdCardName = cardNames.get(2);

        // Extract words from the selected cards
        String firstWord = firstCardName.split("\\s+")[0];  // First word of the first card
        String middleWord = secondCardName.split("\\s+")[secondCardName.split("\\s+").length / 2];  // Middle word of the second card
        String lastWord = thirdCardName.split("\\s+")[thirdCardName.split("\\s+").length - 1];  // Last word of the third card

        // Check if the second word starts with a lowercase character and if the first word ends with a comma
        if (Character.isLowerCase(middleWord.charAt(0)) && firstWord.endsWith(",")) {
            firstWord = firstWord.substring(0, firstWord.length() - 1);  // Remove the comma from the first word
        }

        // Check if the second word starts with a lowercase character and if the first word ends with a comma
        if (Character.isLowerCase(middleWord.charAt(0)) && firstWord.endsWith("'s")) {
            firstWord = firstWord.substring(0, firstWord.length() - 2);  // Remove the comma from the first word
        }

        // Concatenate the three words into a single string
        String result = String.join(" ", firstWord, middleWord, lastWord);

        // If the result is longer than 30 characters and the attempt count is less than 5, rerun the method
        if (result.length() > 20 && attemptCount < 3) {
            return getAIDeckName(humanDeckName, AIDeckNumber, attemptCount + 1);
        }

        // If the result is longer than 30 characters after 5 attempts, return the first two words
        if (result.length() > 20) {
            String fallbackResult = String.join(" ", firstWord, lastWord);
            return fallbackResult;
        }

        // Return the final result
        return result;
    }

    public static ArrayList<String> getAllCardNamesAsArrayList(CardPool cards) {
        ArrayList<String> cardList = new ArrayList<>();
        // Iterate through the entries of CardPool
        for (Map.Entry<PaperCard, Integer> c : cards) {
            String cardName = c.getKey().getName();  // Get the PaperCard object from the entry
            cardList.add(cardName);  // Add the PaperCard to the ArrayList
        }
        return cardList;  // Return the list of PaperCards
    }

    private static void WriteToPropsFile(String filepath, Properties props) {
        // Update the file
        try (FileWriter writer = new FileWriter(filepath)) {
            props.store(writer, "DrBo6's Draft Stats Tracker");
            //System.out.println("Properties written to " + filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Properties ReadFromPropsFile(String filepath) {
        Properties props = new Properties();
        try (FileReader reader = new FileReader(filepath)) {
            props.load(reader);
            //System.out.println("Properties read from " + filepath);
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