package forge.screens.match;

import javax.swing.JButton;

import forge.LobbyPlayer;
import forge.drbo6scustoms.DraftClassTracker;
import forge.Singletons;
import forge.game.GameView;
import forge.gamemodes.match.NextGameDecision;
import forge.gui.SOverlayUtils;
import forge.gui.framework.FScreen;
import forge.interfaces.IGameController;
import forge.model.FModel;
import forge.player.GamePlayerUtil;

/** 
 * Default controller for a ViewWinLose object. This class can
 * be extended for various game modes to populate the custom
 * panel in the win/lose screen.
 *
 */
public class ControlWinLose {
    private final ViewWinLose view;
    protected final GameView lastGame;
    protected final CMatchUI matchUI;

    /** @param v &emsp; ViewWinLose
     * @param match */
    public ControlWinLose(final ViewWinLose v, final GameView game0, final CMatchUI matchUI) {
        this.view = v;
        this.lastGame = game0;
        this.matchUI = matchUI;
        addListeners();

        // CODE INJECTION
        // The game just uses the index for the AI deck throughout, from 1-7 with the player deck being 0.
        // Gauntlet breaks this (in GauntletMini.java) by simply equating the AI deck to the round, so round 1 is deck 1, round 2 is deck 2 and so on.
        final LobbyPlayer currentPlayer = GamePlayerUtil.getGuiPlayer(); // This grabs the current player as a LobbyPlayer class; use .getName() to print the name
        final String humanDeckName = lastGame.getDeck(lastGame.getPlayers().get(0)).getName(); // This will get the name of the player's deck in the game that we are currently reporting on
        final String AIDeckNumber;
        if (FModel.getGauntletMini().isGauntletDraft()) { // This will tell us if the last draft that was launched was a gauntlet. This will cause issues if you are playing Gauntlet and non-Gauntlet games at the same time, but I didn't find a way to check that the match is associated with the gauntlet
            AIDeckNumber = String.valueOf(FModel.getGauntletMini().getCurrentRound());
        } else {
            AIDeckNumber = String.valueOf(lastGame.getGame().getRegisteredPlayers().get(1).getId()); // This will get the number of the AI deck that was played against if this is not a gauntlet
        }
        DraftClassTracker.UpdateDraftStatsResults(humanDeckName, AIDeckNumber, lastGame.isWinner(currentPlayer), lastGame.isMatchOver());
    }

    /** */
    public void addListeners() {
        view.getBtnContinue().addActionListener(e -> actionOnContinue());

        view.getBtnRestart().addActionListener(e -> actionOnRestart());

        view.getBtnQuit().addActionListener(e -> {
            actionOnQuit();
            ((JButton) e.getSource()).setEnabled(false);
        });
    }

    /** Action performed when "continue" button is pressed in default win/lose UI. */
    public void actionOnContinue() {
        nextGameAction(NextGameDecision.CONTINUE);
    }

    /** Action performed when "restart" button is pressed in default win/lose UI. */
    public void actionOnRestart() {
        nextGameAction(NextGameDecision.NEW);
    }

    /** Action performed when "quit" button is pressed in default win/lose UI. */
    public void actionOnQuit() {
        nextGameAction(NextGameDecision.QUIT);
        DraftClassTracker.setGameInProgress(false); // Bob - Let's put this here as well
        Singletons.getControl().setCurrentScreen(FScreen.HOME_SCREEN);
    }

    private void nextGameAction(final NextGameDecision decision) {
        SOverlayUtils.hideOverlay();
        saveOptions();
        for (final IGameController controller : matchUI.getOriginalGameControllers()) {
            controller.nextGameDecision(decision);
        }
    }

    /**
     * Either continues or restarts a current game. May be overridden for use
     * with other game modes.
     */
    public void saveOptions() {
        matchUI.writeMatchPreferences();
    }

    /**
     * <p>
     * populateCustomPanel.
     * </p>
     * May be overridden as required by controllers for various game modes
     * to show custom information in center panel. Default configuration is empty.
     * 
     * @return boolean, panel has contents or not.
     */
    public boolean populateCustomPanel() {
        return false;
    }

    /** @return ViewWinLose object this controller is in charge of */
    public ViewWinLose getView() {
        return this.view;
    }
}
