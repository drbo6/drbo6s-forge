package forge.screens.match;

import javax.swing.JButton;

import forge.drbo6scustoms.DraftClassTracker;
import forge.Singletons;
import forge.game.GameView;
import forge.gamemodes.match.NextGameDecision;
import forge.gui.SOverlayUtils;
import forge.gui.framework.FScreen;
import forge.interfaces.IGameController;

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

        DraftClassTracker DCT = new DraftClassTracker();
        DCT.UpdateDraftStats("ControlWinLose triggered");
        DCT = null; // Prepare for garbage collection

        // OLD
        // We therefore decided to just manually do our own thing because we would have to figure out how to round progression works and this all just takes too long for our purposes.
//        System.out.println("Hello, this is ControlWinLose speaking");
//        LobbyPlayer currentPlayer = GamePlayerUtil.getGuiPlayer();
//        System.out.println("Player: " + currentPlayer.getName());
//        System.out.println("Winning Player: " + lastGame.getWinningPlayerName());
//        System.out.println("AI Deck (in single matches): " + lastGame.getGame().getRegisteredPlayers().get(1).getId());
//        System.out.println("AI Deck (Gauntlet): " + lastGame.getGame().getRegisteredPlayers().);
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
