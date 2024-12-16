package forge.screens.home.sanctioned;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckProxy;
import forge.drbo6scustoms.DraftClassTracker;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gamemodes.match.HostedMatch;
import forge.gui.GuiBase;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.gui.UiCommand;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.itemmanager.ItemManagerConfig;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorDraftingProcess;
import forge.screens.deckeditor.views.VProbabilities;
import forge.screens.deckeditor.views.VStatistics;
import forge.toolbox.FOptionPane;
import forge.util.Localizer;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controls the draft submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
@SuppressWarnings("serial")
public enum CSubmenuDraft implements ICDoc {
    SINGLETON_INSTANCE;

    private final UiCommand cmdDeckSelect = () -> {
        VSubmenuDraft.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
        fillOpponentComboBox();
    };

    private final ActionListener radioAction = e -> fillOpponentComboBox();

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;

        view.getLstDecks().setSelectCommand(cmdDeckSelect);

        view.getBtnBuildDeck().setCommand((UiCommand) this::setupDraft);

        view.getBtnStart().addActionListener(e -> startGame(GameType.Draft));

        view.getRadSingle().addActionListener(radioAction);

        view.getRadAll().addActionListener(radioAction);
        view.getRadMultiple().addActionListener(radioAction);
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;
        final JButton btnStart = view.getBtnStart();

        view.getLstDecks().setPool(DeckProxy.getAllDraftDecks());
        view.getLstDecks().setup(ItemManagerConfig.DRAFT_DECKS); // Here we are setting up the decks in the DeckManager which is an extension of ItemManager

        if (!view.getLstDecks().getPool().isEmpty()) {
            btnStart.setEnabled(true);
            fillOpponentComboBox();
        }

        SwingUtilities.invokeLater(() -> {
            if (btnStart.isEnabled()) {
                view.getBtnStart().requestFocusInWindow();
            } else {
                view.getBtnBuildDeck().requestFocusInWindow();
            }
        });
    }

    private void startGame(final GameType gameType) {
        final Localizer localizer = Localizer.getInstance();
        final boolean gauntlet = VSubmenuDraft.SINGLETON_INSTANCE.isGauntlet();
        final DeckProxy humanDeck = VSubmenuDraft.SINGLETON_INSTANCE.getLstDecks().getSelectedItem();

        if (humanDeck == null) {
            FOptionPane.showErrorDialog(localizer.getMessage("lblNoDeckSelected"), localizer.getMessage("lblNoDeck"));
            return;
        }

        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            final String errorMessage = gameType.getDeckFormat().getDeckConformanceProblem(humanDeck.getDeck());
            if (null != errorMessage) {
                FOptionPane.showErrorDialog("Your deck " + errorMessage + " Please edit or choose a different deck.", "Invalid Deck");
                return;
            }
        }

        FModel.getGauntletMini().resetGauntletDraft();

        // ------------------
        // Bob Code Injection
        // ------------------
        // The combobox has more information than just the number of the AI deck, so we are fixing resulting errors.
        // DuelType passes on the secondary information
        // For a single opponent, it is the deck number. For multiple opponents, it is the number of them. For Gauntlet, it just says Gauntlet.
        // We updated the code to use the index instead of the content, so that we can add more information to the text
        String duelType = (String)VSubmenuDraft.SINGLETON_INSTANCE.getCbOpponent().getSelectedItem(); // Original code that will now cause an error for non-gauntlet choices as it is no longer the number of the chosen deck or number of concurrent opponents; hence we added 4 lines below
        if (duelType != "Gauntlet") {
            int selectedIndex = VSubmenuDraft.SINGLETON_INSTANCE.getCbOpponent().getSelectedIndex() + 1; // +1 as it is zero indexed otherwise
            duelType = String.valueOf(selectedIndex);
        }

        // ------------------
        // Bob Code Injection
        // ------------------
        // As part of starting the game, we we are saving the dueltype and the ai opponent deck to a Java properties file.
        if (duelType == null) { // This block is the original code
            FOptionPane.showErrorDialog("Please select duel types for the draft match.", "Missing opponent items");
            return;
        } else { // This block starts our edits
            if (duelType == "Gauntlet") { // Initialize a Gauntlet Run
                DraftClassTracker.UpdateDraftStatsOpponentAndDuelType(humanDeck.getDeck().getName(), "1", true);
            }
            if (VSubmenuDraft.SINGLETON_INSTANCE.isSingleSelected()) { // Initialize a single opponent run (multiple opponents are untracked; you can get those by entering else to this)
                DraftClassTracker.UpdateDraftStatsOpponentAndDuelType(humanDeck.getDeck().getName(), duelType, false);
            }
        }

        final DeckGroup opponentDecks = FModel.getDecks().getDraft().get(humanDeck.getName());

        // BOB - THIS IS WHERE THE GAUNTLET LIKELY GETS LAUNCHED
        if (gauntlet) { // Shorthand for if the player selected Gauntlet
            if ("Gauntlet".equals(duelType)) {
                final int rounds = opponentDecks.getAiDecks().size();
                FModel.getGauntletMini().launch(rounds, humanDeck.getDeck(), gameType);
            } else if ("Tournament".equals(duelType)) {
                // TODO Allow for tournament style draft, instead of always a gauntlet
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {
            SOverlayUtils.startGameOverlay();
            SOverlayUtils.showOverlay();
        });

        Map<Integer, Deck> aiMap = Maps.newHashMap();
        if (VSubmenuDraft.SINGLETON_INSTANCE.isSingleSelected()) { // isSingleSelected refers to the radio button to select "Single opponent"
            // Restore Zero Indexing
            final int aiIndex = Integer.parseInt(duelType)-1; // This gets the deck number -1 so deck 1 is 0.

            // Below outputs the name of the selected deck as listed in the text file which looks like this: forge.gamemodes.limited.DeckColors@ and then a hexadecimal value (Hash Code). It is likely Generated from an object using ToString().
            // opponentDecks outputs the name of the Human Deck (as that is the main folder name needed to get all decks)
            // getAIDecks gets all the names for the AI decks
            // the index then picks the selected one
            final Deck aiDeck = opponentDecks.getAiDecks().get(aiIndex);

            if (aiDeck == null) {
                throw new IllegalStateException("Draft: Computer deck is null!");
            }

            aiMap.put(aiIndex + 1, aiDeck); // This is a hashmap that just contains deck number = deck name for the opponent
        } else { // Since we already ruled out Single and Gauntlet, it is multiple (which means multiple players at the same time)
            final int numOpponents = Integer.parseInt(duelType);

            int maxDecks = opponentDecks.getAiDecks().size();
            if (numOpponents > maxDecks) {
                throw new IllegalStateException("Draft: Not enough decks for the number of opponents!");
            }

            List<Integer> aiIndices = Lists.newArrayList();
            for(int i = 0; i < maxDecks; i++) {
                aiIndices.add(i);
            }
            Collections.shuffle(aiIndices);
            aiIndices = aiIndices.subList(0, numOpponents);

            for(int i : aiIndices) {
                final Deck aiDeck = opponentDecks.getAiDecks().get(i);
                if (aiDeck == null) {
                    throw new IllegalStateException("Draft: Computer deck is null!");
                }

                aiMap.put(i + 1, aiDeck);
            }
        }

        // Bob's Notes: Once the AI decks are chosen, the human player (humanDeck) and the AI players (aiDeck) are registered in a starter list, which is used to initialize the match. See in-line notes below.
        // THIS DOES NOT LAUNCH THE GAUNTLETS; that happens above.
        final List<RegisteredPlayer> starter = new ArrayList<>();
        // Human is 0
        final RegisteredPlayer human = new RegisteredPlayer(humanDeck.getDeck()).setPlayer(GamePlayerUtil.getGuiPlayer());
        starter.add(human); // Adds the human player, which includes their deck, their ID is 0
        human.setId(0);
        human.assignConspiracies();
        for(Map.Entry<Integer, Deck> aiDeck : aiMap.entrySet()) {
            RegisteredPlayer aiPlayer = new RegisteredPlayer(aiDeck.getValue()).setPlayer(GamePlayerUtil.createAiPlayer());
            aiPlayer.setId(aiDeck.getKey());
            starter.add(aiPlayer); // Adds multiple aiPlayers, each of which has the key (just the number of the deck; player is zero, deck 1 is 1 and so on) of the aiDeck as their ID (aiDeck.getKey() which is then set to the aiPlayer with setID()).
            aiPlayer.assignConspiracies();
        }

        final HostedMatch hostedMatch = GuiBase.getInterface().hostMatch();
        hostedMatch.startMatch(GameType.Draft, null, starter, human, GuiBase.getInterface().getNewGuiGame()); // Starter is a list of players that has all the information needed to start the game

        SwingUtilities.invokeLater(SOverlayUtils::hideOverlay);
    }

    /** */
    private void setupDraft() {
        final Localizer localizer = Localizer.getInstance();
        // Determine what kind of booster draft to run
        final LimitedPoolType poolType = GuiChoose.oneOrNone(localizer.getMessage("lblChooseDraftFormat"), LimitedPoolType.values(true));
        if (poolType == null) { return; }

        final BoosterDraft draft = BoosterDraft.createDraft(poolType);
        if (draft == null) { return; }

        final CEditorDraftingProcess draftController = new CEditorDraftingProcess(CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());
        draftController.showGui(draft);

        Singletons.getControl().setCurrentScreen(FScreen.DRAFTING_PROCESS);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(draftController);
        VProbabilities.SINGLETON_INSTANCE.getLayoutControl().update();
        VStatistics.SINGLETON_INSTANCE.getLayoutControl().update();

    }

    private void fillOpponentComboBox() {

        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;
        JComboBox<String> combo = view.getCbOpponent();
        combo.removeAllItems();

        final DeckProxy humanDeck = view.getLstDecks().getSelectedItem();
        if (humanDeck == null) {
            return;
        }

        // ------------------
        // Bob Code Injection
        // ------------------
        // We need a Java properties file to be in place to start the draft and to name the AI decks
        String humanDeckName = humanDeck.getDeck().getName();
        DraftClassTracker.InitializeDraftStatsFile(humanDeckName); // The method will check if the file already exists and not do anything if it does.
        Map<String, Integer> draftStatsResults = DraftClassTracker.loadDraftStatsResults(humanDeckName);
        String[] AIDeckNames = DraftClassTracker.getAllAIDeckNamesFromDraftStats(humanDeckName);

        if (VSubmenuDraft.SINGLETON_INSTANCE.isSingleSelected()) {
            // Single opponent
            final DeckGroup opponentDecks = FModel.getDecks().getDraft().get(humanDeck.getName());
            int indx = 0;

            for (@SuppressWarnings("unused") Deck d : opponentDecks.getAiDecks()) { // The latter is a list of the decks with their hashed names in order to match the number of the file
                indx++; // 1-7 instead of 0-6
                if (draftStatsResults == null) {
                    System.out.println("No W/L data found.");
                    combo.addItem(String.valueOf(indx)); // Add the indexes to the combo box. You can access the deck here using d.
                } else if (AIDeckNames.length == 0) {
                    System.out.println("No AI deck names found.");
                    combo.addItem(String.valueOf(indx)); // Add the indexes to the combo box. You can access the deck here using d.
                } else {
                    Integer gameWins = draftStatsResults.get("GWvs" + indx);
                    Integer gameLosses = draftStatsResults.get("GLvs" + indx);
                    Integer matchWins = draftStatsResults.get("MWvs" + indx);
                    Integer matchLosses = draftStatsResults.get("MLvs" + indx);
                    combo.addItem(String.valueOf(indx) + ". " + AIDeckNames[indx - 1]
                            + " [" + String.valueOf(matchWins) + "-" + String.valueOf(matchLosses)  // Add the indexes to the combo box. You can access the deck here using d.
                            + " | " + String.valueOf(gameWins) + "-" + String.valueOf(gameLosses) + "]"); // Add the indexes to the combo box. You can access the deck here using d.
                }

            }
        } else if (VSubmenuDraft.SINGLETON_INSTANCE.isGauntlet()) {
            // Gauntlet/Tournament
            combo.addItem("Gauntlet");
            //combo.addItem("Tournament");
        } else {
            // Playing against multiple opponents
            combo.addItem("2");
            combo.addItem("3");
            combo.addItem("4");
            combo.addItem("5");
        }
    }

}
