package forge.screens.quest;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;

import forge.FThreads;
import forge.Forge;
import forge.GuiBase;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.FDeckEditor.EditorType;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.itemmanager.filters.ItemFilter;
import forge.limited.BoosterDraft;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.quest.IQuestTournamentView;
import forge.quest.QuestDraftUtils;
import forge.quest.QuestEventDraft;
import forge.quest.QuestTournamentController;
import forge.quest.QuestDraftUtils.Mode;
import forge.quest.data.QuestEventDraftContainer;
import forge.screens.limited.DraftingProcessScreen;
import forge.toolbox.FButton;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.FTextField;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.util.Utils;
import forge.util.Localizer;
import java.util.Arrays;

public class QuestTournamentsScreen extends QuestLaunchScreen implements IQuestTournamentView {
    //Select Tournament panel
    private final SelectTournamentPanel pnlSelectTournament = add(new SelectTournamentPanel());
    private final Localizer localizer = Localizer.getInstance();

    private final FLabel lblCredits = pnlSelectTournament.add(new FLabel.Builder().icon(FSkinImage.QUEST_COINSTACK)
            .iconScaleFactor(0.75f).font(FSkinFont.get(16)).build());

    private final FLabel btnSpendToken = pnlSelectTournament.add(new FLabel.ButtonBuilder().text(Localizer.getInstance().getMessage("btnSpendToken") + " (0)").build());

    private final FLabel lblInfo = pnlSelectTournament.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblSelectaTournament") + ":")
            .align(Align.center).font(FSkinFont.get(16)).build());

    private final FLabel lblNoTournaments = pnlSelectTournament.add(new FLabel.Builder()
            .align(Align.center).text(Localizer.getInstance().getMessage("lblNoTournaments")).insets(Vector2.Zero)
            .font(FSkinFont.get(12)).build());

    private final QuestEventPanel.Container pnlTournaments = pnlSelectTournament.add(new QuestEventPanel.Container());

    //Prepare Deck panel
    private final PrepareDeckPanel pnlPrepareDeck = add(new PrepareDeckPanel());

    private final FButton btnEditDeck = add(new FButton(Localizer.getInstance().getMessage("btnEditDeck")));
    private final FButton btnLeaveTournament = add(new FButton(Localizer.getInstance().getMessage("btnLeaveTournament")));
    private final CardManager deckViewer = pnlPrepareDeck.add(new CardManager(false));

    //Tournament Active panel
    private final TournamentActivePanel pnlTournamentActive = add(new TournamentActivePanel());
    private final FButton btnEditDeckInTourn = add(new FButton(Localizer.getInstance().getMessage("btnEditDeck")));
    private final FButton btnLeaveTournamentInTourn = add(new FButton(Localizer.getInstance().getMessage("btnLeaveTournament")));

    //Results labels
    private static final FSkinFont RESULTS_FONT = FSkinFont.get(15);
    private static final Vector2 RESULTS_INSETS = new Vector2(2 * PADDING, 0);
    private final FLabel lblFirst = new FLabel.Builder().font(RESULTS_FONT).insets(RESULTS_INSETS).build();
    private final FLabel lblSecond = new FLabel.Builder().font(RESULTS_FONT).insets(RESULTS_INSETS).build();
    private final FLabel lblThird = new FLabel.Builder().font(RESULTS_FONT).insets(RESULTS_INSETS).build();
    private final FLabel lblFourth = new FLabel.Builder().font(RESULTS_FONT).insets(RESULTS_INSETS).build();

    private Mode mode;
    private final QuestTournamentController controller;

    public QuestTournamentsScreen() {
        super();
        controller = new QuestTournamentController(this);
        btnSpendToken.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FThreads.invokeInBackgroundThread(new Runnable() { //must run in background thread to handle alerts
                    @Override
                    public void run() {
                        controller.spendToken();
                    }
                });
            }
        });
        btnEditDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editDeck(true);
            }
        });
        btnLeaveTournament.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FThreads.invokeInBackgroundThread(new Runnable() { //must run in background thread to handle alerts
                    @Override
                    public void run() {
                        controller.endTournamentAndAwardPrizes();
                    }
                });
            }
        });

        // TODO: is it possible to somehow reuse the original btnEditDeck/btnLeaveTournament
        btnEditDeckInTourn.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                editDeck(true);
            }
        });
        btnLeaveTournamentInTourn.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FThreads.invokeInBackgroundThread(new Runnable() { //must run in background thread to handle alerts
                    @Override
                    public void run() {
                        controller.endTournamentAndAwardPrizes();
                    }
                });
            }
        });

        pnlPrepareDeck.add(btnEditDeck);
        pnlPrepareDeck.add(btnLeaveTournament);

        pnlTournamentActive.add(btnEditDeckInTourn);
        pnlTournamentActive.add(btnLeaveTournamentInTourn);

        deckViewer.setCaption(localizer.getMessage("ttMain"));
        deckViewer.setup(ItemManagerConfig.QUEST_DRAFT_DECK_VIEWER);
        setMode(Mode.SELECT_TOURNAMENT);
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        height -= startY;
        pnlSelectTournament.setBounds(0, startY, width, height);
        pnlPrepareDeck.setBounds(0, startY, width, height);
        pnlTournamentActive.setBounds(0, startY, width, height);
    }

    @Override
    protected String getGameType() {
        return "Tournaments";
    }

    @Override
    public void onUpdate() {
        controller.update();
        if (mode == Mode.PREPARE_DECK) {
            Deck deck = getDeck();
            if (deck != null) {
                deckViewer.setPool(deck.getMain());
            }
            else {
                deckViewer.setPool(new CardPool());
            }
        }
    }

    @Override
    protected void updateHeaderCaption() {
        if (mode == Mode.PREPARE_DECK) {
            setHeaderCaption(FModel.getQuest().getName() + " - " + getGameType() + "\n" + localizer.getMessage("lblDraft") + " - " + FModel.getQuest().getAchievements().getCurrentDraft().getTitle());
        }
        else {
            super.updateHeaderCaption();
        }
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public void setMode(Mode mode0) {
        if (mode == mode0) { return; }
        mode = mode0;
        pnlSelectTournament.setVisible(mode == Mode.SELECT_TOURNAMENT || mode == Mode.EMPTY);
        pnlPrepareDeck.setVisible(mode == Mode.PREPARE_DECK);
        pnlTournamentActive.setVisible(mode == Mode.TOURNAMENT_ACTIVE);
        btnEditDeckInTourn.setVisible(mode == Mode.TOURNAMENT_ACTIVE);
        btnLeaveTournamentInTourn.setVisible(mode == Mode.TOURNAMENT_ACTIVE);
        btnEditDeck.setVisible(mode == Mode.PREPARE_DECK);
        btnLeaveTournament.setVisible(mode == Mode.PREPARE_DECK);

        updateHeaderCaption();
    }

    @Override
    public void populate() {
        //not needed
    }

    @Override
    public void updateEventList(QuestEventDraftContainer events) {
        pnlTournaments.clear();

        if (events != null) {
            for (QuestEventDraft event : events) {
                pnlTournaments.add(new QuestEventPanel(event, pnlTournaments));
            }
        }

        pnlTournaments.revalidate();

        boolean hasTournaments = pnlTournaments.getChildCount() > 0;
        pnlTournaments.setVisible(hasTournaments);
        lblNoTournaments.setVisible(!hasTournaments);
    }

    @Override
    public void updateTournamentBoxLabel(String playerID, int iconID, int box, boolean first) {
        pnlTournamentActive.clear();
        pnlTournamentActive.revalidate();
    }

    @Override
    public void startDraft(BoosterDraft draft) {
        Forge.openScreen(new DraftingProcessScreen(draft, EditorType.QuestDraft, controller));
    }
    
    private Deck getDeck() {
        DeckGroup deckGroup = FModel.getQuest().getDraftDecks().get(QuestEventDraft.DECK_NAME);
        if (deckGroup != null) {
            return deckGroup.getHumanDeck();
        }
        return null;
    }

    public void editDeck(boolean isExistingDeck) {
        Deck deck = getDeck();
        if (deck != null) {
            if (isExistingDeck) {
                Forge.openScreen(new QuestDraftDeckEditor(deck.getName()));
            }
            else {
                Forge.openScreen(new QuestDraftDeckEditor(deck));
            }
        }
    }

    @Override
    protected void startMatch() {
        if (mode == Mode.TOURNAMENT_ACTIVE /*&& FModel.getQuestPreferences().getPrefInt(QuestPreferences.QPref.SIMULATE_AI_VS_AI_RESULTS) == 1*/ && QuestDraftUtils.isNextMatchAIvsAI()) {
            // Special handling for simulating AI vs. AI match outcome - do not invoke in background thread (since the match is not played out)
            // and instead revalidate right after the outcome is decided in order to refresh the tournament screen.
            controller.startNextMatch();
            revalidate();
            return;
        }

        FThreads.invokeInBackgroundThread(new Runnable() { //must run in background thread to handle alerts
            @Override
            public void run() {
                switch (mode) {
                case SELECT_TOURNAMENT:
                    controller.startDraft();
                    break;
                case PREPARE_DECK:
                    controller.startTournament();
                    break;
                case TOURNAMENT_ACTIVE:
                    controller.startNextMatch();
                    break;
                default:
                    break;
                }
            }
        });
    }

    @Override
    public FLabel getLblCredits() {
        return lblCredits;
    }

    @Override
    public FLabel getLblFirst() {
        return lblFirst;
    }

    @Override
    public FLabel getLblSecond() {
        return lblSecond;
    }

    @Override
    public FLabel getLblThird() {
        return lblThird;
    }

    @Override
    public FLabel getLblFourth() {
        return lblFourth;
    }

    @Override
    public FLabel getBtnSpendToken() {
        return btnSpendToken;
    }

    @Override
    public FButton getBtnLeaveTournament() {
        return btnLeaveTournament;
    }

    public FButton getBtnEditDeckInTourn() {
        return btnEditDeckInTourn;
    }
    public FButton getBtnLeaveTournamentInTourn() {
        return btnLeaveTournamentInTourn;
    }

    private class SelectTournamentPanel extends FContainer {
        @Override
        protected void doLayout(float width, float height) {
            float gap = Utils.scale(2);
            float y = gap; //move credits label down a couple pixels so it looks better

            float halfWidth = width / 2;
            lblCredits.setBounds(0, y, halfWidth, lblCredits.getAutoSizeBounds().height);
            btnSpendToken.setBounds(halfWidth, y, halfWidth - gap, lblCredits.getHeight());
            y += lblCredits.getHeight() + gap;

            float x = PADDING;
            float w = width - 2 * PADDING;
            lblInfo.setBounds(x, y, w, lblInfo.getAutoSizeBounds().height);
            y += lblInfo.getHeight() + gap;
            lblNoTournaments.setBounds(x, y, w, lblNoTournaments.getAutoSizeBounds().height);
            pnlTournaments.setBounds(x, y, w, height - y);
        }
    }

    private class PrepareDeckPanel extends FContainer {
        @Override
        protected void doLayout(float width, float height) {
            float y = PADDING;
            float buttonWidth = (width - 3 * PADDING) / 2;
            btnEditDeck.setBounds(PADDING, y, buttonWidth, FTextField.getDefaultHeight());
            btnLeaveTournament.setBounds(btnEditDeck.getRight() + PADDING, y, buttonWidth, btnEditDeck.getHeight());
            y += btnEditDeck.getHeight() + PADDING - ItemFilter.PADDING;
            deckViewer.setBounds(0, y, width, height - y);
        }
    }

    private class TournamentActivePanel extends FContainer {
        @Override
        protected void doLayout(float width, float height) {
            // TODO: updating bracket results should really be handled via updateTournamentBoxLabel

            QuestEventDraft qd = FModel.getQuest().getAchievements().getCurrentDraft();
            if (qd == null) {
                return;
            }

            float x = PADDING;
            float w = width - 2 * PADDING;
            float buttonWidth = (width - 3 * PADDING) / 2;
            float y = PADDING;

            FLabel[] labels = new FLabel[16];
            String[] playerIDs = new String[16];
            int[] iconIDs = new int[16];

            String draftTitle = qd.getFullTitle();
            FLabel lblStandings = add(new FLabel.Builder().text(localizer.getMessage("lblDraft") + ": " + draftTitle).align(Align.center).font(FSkinFont.get(20)).build());
            lblStandings.setBounds(x, y, w, lblStandings.getAutoSizeBounds().height);
            y += lblStandings.getHeight() + PADDING;

            boolean tournamentComplete = !qd.playerHasMatchesLeft();

            btnEditDeckInTourn.setVisible(mode == Mode.TOURNAMENT_ACTIVE);
            btnLeaveTournamentInTourn.setVisible(mode == Mode.TOURNAMENT_ACTIVE);

            if (tournamentComplete) {
                String sid = qd.getStandings()[qd.getStandings().length - 1];
                String winnersName = sid.equals(QuestEventDraft.HUMAN) ? FModel.getPreferences().getPref(FPref.PLAYER_NAME) : 
                        sid.equals(QuestEventDraft.UNDETERMINED) ? "---" : qd.getAINames()[Integer.parseInt(sid) - 1];
                FLabel lblWinner = add(new FLabel.Builder().text(localizer.getMessage("lblWinner") + ": " + winnersName).align(Align.center).font(FSkinFont.get(20)).build());
                lblWinner.setBounds(x, y, w, lblStandings.getAutoSizeBounds().height);
                y += lblWinner.getHeight() + PADDING;
                getBtnLeaveTournamentInTourn().setText(localizer.getMessage("lblCollectPrizes"));
            } else {
                getBtnLeaveTournamentInTourn().setText(localizer.getMessage("btnLeaveTournament"));

                String sid1, sid2, pairedPlayer1 = "NONE", pairedPlayer2 = "NONE";
                int pos = Arrays.asList(qd.getStandings()).indexOf(QuestEventDraft.UNDETERMINED);
                if (pos != -1) {
                    int offset = (pos - 8) * 2;
                    sid1 = qd.getStandings()[offset];
                    sid2 = qd.getStandings()[offset + 1];
                    pairedPlayer1 = sid1.equals(QuestEventDraft.HUMAN) ? FModel.getPreferences().getPref(FPref.PLAYER_NAME) : qd.getAINames()[Integer.parseInt(sid1) - 1];
                    pairedPlayer2 = sid2.equals(QuestEventDraft.HUMAN) ? FModel.getPreferences().getPref(FPref.PLAYER_NAME) : qd.getAINames()[Integer.parseInt(sid2) - 1];
                }

                for (int i = 0; i < 15; i++) {
                    String playerID = qd.getStandings()[i];

                    switch (playerID) {
                        case QuestEventDraft.HUMAN:
                            playerIDs[i] = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
                            if (FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",").length > 0) {
                                iconIDs[i] = Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",")[0]);
                            }
                            break;
                        case QuestEventDraft.UNDETERMINED:
                            playerIDs[i] = "Undetermined";
                            iconIDs[i] = GuiBase.getInterface().getAvatarCount() - 1;
                            break;
                        default:
                            iconIDs[i] = qd.getAIIcons()[Integer.parseInt(playerID) - 1];
                            playerIDs[i] = qd.getAINames()[Integer.parseInt(playerID) - 1];
                            break;
                    }

                }

                for (int j = 0; j < 13; j += 2) {
                    switch (j) {
                        case 0:
                            FLabel qfinals = add(new FLabel.Builder().text(localizer.getMessage("lblQuarterfinals")).align(Align.center).font(FSkinFont.get(16)).build());
                            qfinals.setBounds(x, y, w, qfinals.getAutoSizeBounds().height);
                            y += qfinals.getHeight() + PADDING;
                            break;
                        case 8:
                            FLabel sfinals = add(new FLabel.Builder().text(localizer.getMessage("lblSemifinals")).align(Align.center).font(FSkinFont.get(16)).build());
                            sfinals.setBounds(x, y, w, sfinals.getAutoSizeBounds().height);
                            y += sfinals.getHeight() + PADDING;
                            break;
                        case 12:
                            FLabel finals = add(new FLabel.Builder().text(localizer.getMessage("lblFinalMatch")).align(Align.center).font(FSkinFont.get(16)).build());
                            finals.setBounds(x, y, w, finals.getAutoSizeBounds().height);
                            y += finals.getHeight() + PADDING;
                            break;
                        default:
                            break;
                    }

                    boolean currentMatch = (playerIDs[j].equals(pairedPlayer1) || playerIDs[j + 1].equals(pairedPlayer1))
                            && (playerIDs[j].equals(pairedPlayer2) || playerIDs[j + 1].equals(pairedPlayer2));
                    String labelText = playerIDs[j] + " vs. " + playerIDs[j + 1];

                    /* TODO: Implement drawing avatar pictures next to player names
                    FTextureRegionImage avatar1 = new FTextureRegionImage(FSkin.getAvatars().get(iconIDs[j]));
                    FTextureRegionImage avatar2 = new FTextureRegionImage(FSkin.getAvatars().get(iconIDs[j+1]));
                     */
                    if (Forge.hdbuttons)
                        labels[j] = add(new FLabel.Builder().icon(currentMatch ? FSkinImage.HDSTAR_FILLED : FSkinImage.HDSTAR_OUTLINE).text(labelText).align(Align.center).font(FSkinFont.get(16)).build());
                    else
                        labels[j] = add(new FLabel.Builder().icon(currentMatch ? FSkinImage.STAR_FILLED : FSkinImage.STAR_OUTLINE).text(labelText).align(Align.center).font(FSkinFont.get(16)).build());

                    labels[j].setBounds(x, y, w, labels[j].getAutoSizeBounds().height);
                    if (currentMatch) {
                        labels[j].setTextColor(FSkinColor.get(FSkinColor.Colors.CLR_ACTIVE));
                    }

                    y += labels[j].getHeight();

                    if (j == 6 || j == 10) {
                        y += PADDING;
                    }
                }
            }

            y += lblStandings.getHeight() + PADDING;

            btnEditDeckInTourn.setBounds(PADDING, y, buttonWidth, FTextField.getDefaultHeight());
            btnLeaveTournamentInTourn.setBounds(btnEditDeckInTourn.getRight() + PADDING, y, buttonWidth, btnEditDeckInTourn.getHeight());
        }
    }
}
