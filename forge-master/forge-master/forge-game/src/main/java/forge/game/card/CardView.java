package forge.game.card;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import forge.ImageKeys;
import forge.card.*;
import forge.card.mana.ManaCost;
import forge.game.Direction;
import forge.game.EvenOdd;
import forge.game.GameEntityView;
import forge.game.GameType;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.Tracker;
import forge.util.Lang;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CardView extends GameEntityView {
    private static final long serialVersionUID = -3624090829028979255L;

    public static CardView get(Card c) {
        return c == null ? null : c.getView();
    }
    public static CardStateView getState(Card c, CardStateName state) {
        if (c == null) { return null; }
        CardState s = c.getState(state);
        return s == null ? null : s.getView();
    }

    public static CardView getCardForUi(IPaperCard pc) {
        return Card.getCardForUi(pc).getView();
    }

    public static TrackableCollection<CardView> getCollection(Iterable<Card> cards) {
        if (cards == null) {
            return null;
        }
        TrackableCollection<CardView> collection = new TrackableCollection<>();
        for (Card c : cards) {
            if (c.getCardForUi() == c) { //only add cards that match their card for UI
                collection.add(c.getView());
            }
        }
        return collection;
    }

    public static boolean mayViewAny(Iterable<CardView> cards, Iterable<PlayerView> viewers) {
        if (cards == null) { return false; }

        for (CardView cv : cards) {
            if (cv.canBeShownToAny(viewers)) {
                return true;
            }
        }
        return false;
    }

    public CardView(final int id0, final Tracker tracker) {
        super(id0, tracker);
        set(TrackableProperty.CurrentState, new CardStateView(id0, CardStateName.Original, tracker));
    }
    public CardView(final int id0, final Tracker tracker, final String name0) {
        this(id0, tracker);
        getCurrentState().setName(name0);
        set(TrackableProperty.Name, name0);
        set(TrackableProperty.ChangedColorWords, new HashMap<String, String>());
        set(TrackableProperty.ChangedTypes, new HashMap<String, String>());
        set(TrackableProperty.Sickness, true);
    }
    public CardView(final int id0, final Tracker tracker, final String name0, final PlayerView ownerAndController, final String imageKey) {
        this(id0, tracker, name0);
        set(TrackableProperty.Owner, ownerAndController);
        set(TrackableProperty.Controller, ownerAndController);
        set(TrackableProperty.ImageKey, imageKey);
    }

    public PlayerView getOwner() {
        return get(TrackableProperty.Owner);
    }
    void updateOwner(Card c) {
        set(TrackableProperty.Owner, PlayerView.get(c.getOwner()));
    }

    public PlayerView getController() {
        return get(TrackableProperty.Controller);
    }
    void updateController(Card c) {
        set(TrackableProperty.Controller, PlayerView.get(c.getController()));
    }

    public ZoneType getZone() {
        return get(TrackableProperty.Zone);
    }
    void updateZone(Card c) {
        set(TrackableProperty.Zone, c.getZone() == null ? null : c.getZone().getZoneType());
    }
    public boolean isInZone(final Iterable<ZoneType> zones) {
        return Iterables.contains(zones, getZone());
    }

    public boolean isCloned() {
        return get(TrackableProperty.Cloned);
    }

    public boolean isFaceDown() {
        return get(TrackableProperty.Facedown);//  getCurrentState().getState() == CardStateName.FaceDown;
    }

    public boolean isFlipCard() {
        return get(TrackableProperty.FlipCard);
    }

    public boolean isFlipped() {
        return get(TrackableProperty.Flipped); // getCurrentState().getState() == CardStateName.Flipped;
    }

    public boolean isSplitCard() {
        return get(TrackableProperty.SplitCard);
    }

    /*
    public boolean isTransformed() {
        return getCurrentState().getState() == CardStateName.Transformed;
    }
    //*/

    public boolean isAttacking() {
        return get(TrackableProperty.Attacking);
    }
    void updateAttacking(Card c) {
        Combat combat = c.getGame().getCombat();
        set(TrackableProperty.Attacking, combat != null && combat.isAttacking(c));
    }

    public boolean isExertedThisTurn() {
        return get(TrackableProperty.ExertedThisTurn);
    }
    void updateExertedThisTurn(Card c, boolean exerted) {
        set(TrackableProperty.ExertedThisTurn, exerted);
    }

    public boolean isBlocking() {
        return get(TrackableProperty.Blocking);
    }
    void updateBlocking(Card c) {
        Combat combat = c.getGame().getCombat();
        set(TrackableProperty.Blocking, combat != null && combat.isBlocking(c));
    }

    public boolean isPhasedOut() {
        return get(TrackableProperty.PhasedOut);
    }
    void updatePhasedOut(Card c) {
        set(TrackableProperty.PhasedOut, c.isPhasedOut());
    }

    public boolean isFirstTurnControlled() {
        return get(TrackableProperty.Sickness);
    }
    public boolean hasSickness() {
        return isFirstTurnControlled() && !getCurrentState().hasHaste();
    }
    public boolean isSick() {
        return getZone() == ZoneType.Battlefield && getCurrentState().isCreature() && hasSickness();
    }
    void updateSickness(Card c) {
        set(TrackableProperty.Sickness, c.isFirstTurnControlled());
    }

    public boolean isTapped() {
        return get(TrackableProperty.Tapped);
    }
    void updateTapped(Card c) {
        set(TrackableProperty.Tapped, c.isTapped());
    }

    public boolean isToken() {
        return get(TrackableProperty.Token);
    }
    void updateToken(Card c) {
        set(TrackableProperty.Token, c.isToken());
    }

    public boolean isCommander() {
        return get(TrackableProperty.IsCommander);
    }
    void updateCommander(Card c) {
        boolean isCommander = c.isCommander();
        set(TrackableProperty.IsCommander, isCommander);
        if (isCommander) {
            if (c.getGame().getRules().hasAppliedVariant(GameType.Oathbreaker)) {
                //store alternate type for oathbreaker or signature spell for display in card text
                if (c.getPaperCard().getRules().canBeSignatureSpell()) {
                    set(TrackableProperty.CommanderAltType, "Signature Spell");
                }
                else {
                    set(TrackableProperty.CommanderAltType, "Oathbreaker");
                }
            } else {
                set(TrackableProperty.CommanderAltType, "Commander");
            }
        }
    }
    public String getCommanderType() {
        return get(TrackableProperty.CommanderAltType);
    }

    public Map<CounterType, Integer> getCounters() {
        return get(TrackableProperty.Counters);
    }
    public int getCounters(CounterType counterType) {
        final Map<CounterType, Integer> counters = getCounters();
        if (counters != null) {
            Integer count = counters.get(counterType);
            if (count != null) {
                return count;
            }
        }
        return 0;
    }
    public boolean hasSameCounters(CardView otherCard) {
        Map<CounterType, Integer> counters = getCounters();
        if (counters == null) {
            return otherCard.getCounters() == null;
        }
        return counters.equals(otherCard.getCounters());
    }
    void updateCounters(Card c) {
        set(TrackableProperty.Counters, c.getCounters());
        updateLethalDamage(c);
        CardStateView state = getCurrentState();
        state.updatePower(c);
        state.updateToughness(c);
        state.updateLoyalty(c);
    }

    public int getDamage() {
        return get(TrackableProperty.Damage);
    }
    void updateDamage(Card c) {
        set(TrackableProperty.Damage, c.getDamage());
        updateLethalDamage(c);
    }

    public int getAssignedDamage() {
        return get(TrackableProperty.AssignedDamage);
    }
    void updateAssignedDamage(Card c) {
        set(TrackableProperty.AssignedDamage, c.getTotalAssignedDamage());
        updateLethalDamage(c);
    }

    public int getLethalDamage() {
        return get(TrackableProperty.LethalDamage);
    }
    void updateLethalDamage(Card c) {
        set(TrackableProperty.LethalDamage, c.getLethalDamage());
    }

    public int getShieldCount() {
        return get(TrackableProperty.ShieldCount);
    }
    void updateShieldCount(Card c) {
        set(TrackableProperty.ShieldCount, c.getShieldCount());
    }

    public String getChosenType() {
        return get(TrackableProperty.ChosenType);
    }
    void updateChosenType(Card c) {
        set(TrackableProperty.ChosenType, c.getChosenType());
    }

    public List<String> getChosenColors() {
        return get(TrackableProperty.ChosenColors);
    }
    void updateChosenColors(Card c) {
        set(TrackableProperty.ChosenColors, c.getChosenColors());
    }

    public FCollectionView<CardView> getChosenCards() {
        return get(TrackableProperty.ChosenCards);
    }

    public PlayerView getChosenPlayer() {
        return get(TrackableProperty.ChosenPlayer);
    }
    void updateChosenPlayer(Card c) {
        set(TrackableProperty.ChosenPlayer, PlayerView.get(c.getChosenPlayer()));
    }

    public Direction getChosenDirection() {
        return get(TrackableProperty.ChosenDirection);
    }
    void updateChosenDirection(Card c) {
        set(TrackableProperty.ChosenDirection, c.getChosenDirection());
    }
    public EvenOdd getChosenEvenOdd() {
        return get(TrackableProperty.ChosenEvenOdd);
    }
    void updateChosenEvenOdd(Card c) {
        set(TrackableProperty.ChosenEvenOdd, c.getChosenEvenOdd());
    }

    public String getChosenMode() {
        return get(TrackableProperty.ChosenMode);
    }
    void updateChosenMode(Card c) {
        set(TrackableProperty.ChosenMode, c.getChosenMode());
    }

    private String getRemembered() {
        return get(TrackableProperty.Remembered);
    }
    void updateRemembered(Card c) {
        if (c.getRemembered() == null || Iterables.size(c.getRemembered()) == 0) {
            set(TrackableProperty.Remembered, null);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\r\nRemembered: \r\n");
        for (final Object o : c.getRemembered()) {
            if (o != null) {
                sb.append(o.toString());
                sb.append("\r\n");
            }
        }
        set(TrackableProperty.Remembered, sb.toString());
    }

    public String getNamedCard() {
        return get(TrackableProperty.NamedCard);
    }
    void updateNamedCard(Card c) {
        set(TrackableProperty.NamedCard, c.getNamedCard());
    }

    public boolean mayPlayerLook(PlayerView pv) {
        TrackableCollection<PlayerView> col = get(TrackableProperty.PlayerMayLook);
        if (col != null && col.contains(pv)) {
            return true;
        }
        col = get(TrackableProperty.PlayerMayLookTemp);
        return col != null && col.contains(pv);
    }
    void setPlayerMayLook(Player p, boolean mayLook, boolean temp) {
        TrackableProperty prop = temp ? TrackableProperty.PlayerMayLookTemp : TrackableProperty.PlayerMayLook;
        TrackableCollection<PlayerView> col = get(prop);
        if (mayLook) {
            if (col == null) {
                col = new TrackableCollection<>(p.getView());
                set(prop, col);
            }
            else if (col.add(p.getView())) {
                flagAsChanged(prop);
            }
        }
        else if (col != null) {
            if (col.remove(p.getView())) {
                if (col.isEmpty()) {
                    set(prop, null);
                }
                else {
                    flagAsChanged(prop);
                }
            }
        }
    }

    public boolean canBeShownToAny(final Iterable<PlayerView> viewers) {
        if (viewers == null || Iterables.isEmpty(viewers)) { return true; }

        return Iterables.any(viewers, new Predicate<PlayerView>() {
            public final boolean apply(final PlayerView input) {
                return canBeShownTo(input);
            }
        });
    }

    private boolean canBeShownTo(final PlayerView viewer) {
        if (viewer == null) { return false; }

        ZoneType zone = getZone();
        if (zone == null) { return true; } //cards outside any zone are visible to all

        final PlayerView controller = getController();
        switch (zone) {
        case Ante:
        case Command:
        case Battlefield:
        case Graveyard:
        case Flashback:
        case Stack:
            //cards in these zones are visible to all
            return true;
        case Exile:
            //in exile, only face up cards and face down cards you can look at should be shown (since "exile face down" is a thing)
            if (!isFaceDown()) {
                return true;
            }
            break;
        case Hand:
            if (controller.hasKeyword("Play with your hand revealed.")) {
                return true;
            }
            //$FALL-THROUGH$
        case Sideboard:
            //face-up cards in these zones are hidden to opponents unless they specify otherwise
            if (controller.isOpponentOf(viewer) && !mayPlayerLook(viewer)) {
                break;
            }
            return true;
        case Library:
        case PlanarDeck:
            //cards in these zones are hidden to all unless they specify otherwise
            break;
        case SchemeDeck:
            // true for now, to actually see the Scheme cards (can't see deck anyway)
            return true;
        default:
            break;
        }

        // special viewing permissions for viewer
        if (mayPlayerLook(viewer)) {
            return true;
        }

        //if viewer is controlled by another player, also check if card can be shown to that player
        PlayerView mindSlaveMaster = controller.getMindSlaveMaster();
        if (mindSlaveMaster != null && mindSlaveMaster == viewer) {
            return canBeShownTo(controller);
        }
        return false;
    }

    public boolean canFaceDownBeShownToAny(final Iterable<PlayerView> viewers) {
        return Iterables.any(viewers, new Predicate<PlayerView>() {
            @Override public final boolean apply(final PlayerView input) {
                return canFaceDownBeShownTo(input);
            }
        });
    }

    private boolean canFaceDownBeShownTo(final PlayerView viewer) {
        if (!isFaceDown()) {
            return true;
        }

        // special viewing permissions for viewer
        if (mayPlayerLook(viewer)) {
            return true;
        }

        //if viewer is controlled by another player, also check if face can be shown to that player
        final PlayerView mindSlaveMaster = viewer.getMindSlaveMaster();
        if (mindSlaveMaster != null && canFaceDownBeShownTo(mindSlaveMaster)) {
            return true;
        }
        return isInZone(EnumSet.of(ZoneType.Battlefield, ZoneType.Stack, ZoneType.Sideboard)) && getController().equals(viewer);
    }

    public FCollectionView<CardView> getEncodedCards() {
        return get(TrackableProperty.EncodedCards);
    }

    public GameEntityView getEntityAttachedTo() {
        return get(TrackableProperty.EntityAttachedTo);
    }
    void updateAttachedTo(Card c) {
        set(TrackableProperty.EntityAttachedTo, GameEntityView.get(c.getEntityAttachedTo()));
    }

    public CardView getAttachedTo() {
        GameEntityView enchanting = getEntityAttachedTo();
        if (enchanting instanceof CardView) {
            return (CardView) enchanting;
        }
        return null;
    }
    public PlayerView getEnchantedPlayer() {
        GameEntityView enchanting = getEntityAttachedTo();
        if (enchanting instanceof PlayerView) {
            return (PlayerView) enchanting;
        }
        return null;
    }

    public FCollectionView<CardView> getGainControlTargets() {
        return get(TrackableProperty.GainControlTargets);
    }

    public CardView getCloneOrigin() {
        return get(TrackableProperty.CloneOrigin);
    }

    public FCollectionView<CardView> getImprintedCards() {
        return get(TrackableProperty.ImprintedCards);
    }

    public FCollectionView<CardView> getHauntedBy() {
        return get(TrackableProperty.HauntedBy);
    }

    public CardView getHaunting() {
        return get(TrackableProperty.Haunting);
    }

    public FCollectionView<CardView> getMustBlockCards() {
        return get(TrackableProperty.MustBlockCards);
    }

    public CardView getPairedWith() {
        return get(TrackableProperty.PairedWith);
    }

    public Map<String, String> getChangedColorWords() {
        return get(TrackableProperty.ChangedColorWords);
    }
    void updateChangedColorWords(Card c) {
        set(TrackableProperty.ChangedColorWords, c.getChangedTextColorWords());
    }
    public Map<String, String> getChangedTypes() {
        return get(TrackableProperty.ChangedTypes);
    }
    void updateChangedTypes(Card c) {
        set(TrackableProperty.ChangedTypes, c.getChangedTextTypeWords());
    }

    void updateNonAbilityText(Card c) {
        set(TrackableProperty.NonAbilityText, c.getNonAbilityText());
    }

    public String getText() {
        return getText(getCurrentState(), null);
    }
    public String getText(CardStateView state, HashMap<String, String> translationsText) {
        final StringBuilder sb = new StringBuilder();
        //final boolean isSplitCard = (state.getState() == CardStateName.LeftSplit);

        String tname = "", toracle = "", taltname = "", taltoracle = "";

        // If we have translations, use them
        if (translationsText != null) {
            tname = translationsText.get("name");
            taltname = translationsText.get("altname");

            // Don't translate oracles if the card is a cloned one
            if (((String) get(TrackableProperty.Cloner)).isEmpty()) {
                toracle = translationsText.get("oracle");
                taltoracle = translationsText.get("altoracle");
            }
        }

        tname = tname.isEmpty() ? state.getName() : tname;

        if (isSplitCard()) {
            taltname = getAlternateState().getName();
            taltoracle = getAlternateState().getOracleText();
        }

        if (getId() < 0) {
            toracle = toracle.isEmpty() ? state.getOracleText() : toracle;
            if (isSplitCard()) {
                sb.append("(").append(tname).append(") ");
                sb.append(toracle);
                sb.append("\r\n\r\n");
                sb.append("(").append(taltname).append(") ");
                sb.append(taltoracle);
                return sb.toString().trim();
            } else {
                return toracle;
            }
        }

        final String rulesText = state.getRulesText();
        if (!toracle.isEmpty()) {
            sb.append(toracle).append("\r\n\r\n");
        } else if (!rulesText.isEmpty()) {
            sb.append(rulesText).append("\r\n\r\n");
        }
        if (isCommander()) {
            sb.append(getOwner()).append("'s ").append(getCommanderType()).append("\r\n");
            sb.append(getOwner().getCommanderInfo(this)).append("\r\n");
        }

        if (isSplitCard() && !isFaceDown()) {
            // TODO: Translation?
            CardStateView view = state.getState() == CardStateName.LeftSplit ? state : getAlternateState();
            if (getZone() != ZoneType.Stack) {
                sb.append("(");
                sb.append(view.getName());
                sb.append(") ");
            }
            sb.append(view.getAbilityText());
        } else {
            if (toracle.isEmpty()) sb.append(state.getAbilityText());
        }

        if (isSplitCard() && !isFaceDown() && getZone() != ZoneType.Stack) {
            //ensure ability text for right half of split card is included unless spell is on stack
            sb.append("\r\n\r\n").append("(").append(taltname).append(") ").append(taltoracle);
        }

        String nonAbilityText = get(TrackableProperty.NonAbilityText);
        if (!nonAbilityText.isEmpty()) {
            sb.append("\r\n \r\nNon ability features: \r\n");
            sb.append(nonAbilityText.replaceAll("CARDNAME", getName()));
        }

        sb.append(getRemembered());

        PlayerView chosenPlayer = getChosenPlayer();
        if (chosenPlayer != null) {
            sb.append("\r\n[Chosen player: ");
            sb.append(chosenPlayer);
            sb.append("]\r\n");
        }

        Direction chosenDirection = getChosenDirection();
        if (chosenDirection != null) {
            sb.append("\r\n[Chosen direction: ");
            sb.append(chosenDirection);
            sb.append("]\r\n");
        }

        EvenOdd chosenEvenOdd = getChosenEvenOdd();
        if (chosenEvenOdd != null) {
            sb.append("\r\n[Chosen value: ");
            sb.append(chosenEvenOdd);
            sb.append("]\r\n");
        }

        CardView pairedWith = getPairedWith();
        if (pairedWith != null) {
            sb.append("\r\n \r\nPaired With: ").append(pairedWith);
            sb.append("\r\n");
        }

        if (getCanBlockAny()) {
            sb.append("\r\n\r\n");
            sb.append("CARDNAME can block any number of creatures.".replaceAll("CARDNAME", getName()));
            sb.append("\r\n");
        } else {
            int i = getBlockAdditional();
            if (i > 0) {
                sb.append("\r\n\r\n");
                sb.append("CARDNAME can block an additional ".replaceAll("CARDNAME", getName()));
                sb.append(i == 1 ? "creature" : Lang.nounWithNumeral(i, "creature"));
                sb.append(" each combat.");
                sb.append("\r\n");
            }

        }

        Set<String> cantHaveKeyword = this.getCantHaveKeyword();
        if (cantHaveKeyword != null && !cantHaveKeyword.isEmpty()) {
            sb.append("\r\n\r\n");
            for(String k : cantHaveKeyword) {
                sb.append("CARDNAME can't have or gain ".replaceAll("CARDNAME", getName()));
                sb.append(k);
                sb.append(".");
                sb.append("\r\n");
            }
        }

        String cloner = get(TrackableProperty.Cloner);
        if (!cloner.isEmpty()) {
            sb.append("\r\nCloned by: ").append(cloner);
        }

        return sb.toString().trim();
    }

    public CardStateView getCurrentState() {
        return get(TrackableProperty.CurrentState);
    }

    public boolean hasAlternateState() {
        return getAlternateState() != null;
    }
    public CardStateView getAlternateState() {
        return get(TrackableProperty.AlternateState);
    }
    CardStateView createAlternateState(final CardStateName state0) {
        return new CardStateView(getId(), state0, tracker);
    }

    public CardStateView getState(final boolean alternate0) {
        return alternate0 ? getAlternateState() : getCurrentState();
    }
    void updateState(Card c) {
        updateName(c);
        updateDamage(c);

        boolean isSplitCard = c.isSplitCard();
        set(TrackableProperty.Cloned, c.isCloned());
        set(TrackableProperty.SplitCard, isSplitCard);
        set(TrackableProperty.FlipCard, c.isFlipCard());
        set(TrackableProperty.Facedown, c.isFaceDown());

        final Card cloner = c.getCloner();

        //CardStateView cloner = CardView.getState(c, CardStateName.Cloner);
        set(TrackableProperty.Cloner, cloner == null ? null : cloner.getName() + " (" + cloner.getId() + ")");

        CardState currentState = c.getCurrentState();
        if (isSplitCard) {
            if (c.getCurrentStateName() != CardStateName.LeftSplit && c.getCurrentStateName() != CardStateName.RightSplit && c.getCurrentStateName() != CardStateName.FaceDown) {
                currentState = c.getState(CardStateName.LeftSplit);
            }
        }

        CardStateView currentStateView = currentState.getView();
        if (getCurrentState() != currentStateView) {
            set(TrackableProperty.CurrentState, currentStateView);
            currentStateView.updateName(currentState);
            currentStateView.updatePower(c); //ensure power, toughness, and loyalty updated when current state changes
            currentStateView.updateToughness(c);
            currentStateView.updateLoyalty(c);

            // update the color only while in Game
            if (c.getGame() != null) {
                currentStateView.updateColors(currentState);
                currentStateView.updateHasChangeColors(!c.getChangedCardColors().isEmpty());
            }
        } else {
            currentStateView.updateLoyalty(currentState);
        }
        currentState.getView().updateKeywords(c, currentState); //update keywords even if state doesn't change
        currentState.getView().setOriginalColors(c); //set original Colors

        CardState alternateState = isSplitCard && isFaceDown() ? c.getState(CardStateName.RightSplit) : c.getAlternateState();

        if (isSplitCard && isFaceDown()) {
            // face-down (e.g. manifested) split cards should show the original face on their flip side
            alternateState = c.getState(CardStateName.Original);
        }

        if (alternateState == null) {
            set(TrackableProperty.AlternateState, null);
        }
        else {
            CardStateView alternateStateView = alternateState.getView();
            if (getAlternateState() != alternateStateView) {
                set(TrackableProperty.AlternateState, alternateStateView);
                alternateStateView.updateName(alternateState);
                alternateStateView.updatePower(c); //ensure power, toughness, and loyalty updated when current state changes
                alternateStateView.updateToughness(c);
                alternateStateView.updateLoyalty(c);

                // update the color only while in Game
                if (c.getGame() != null) {
                    alternateStateView.updateColors(alternateState);
                }
            } else {
                alternateStateView.updateLoyalty(alternateState);
            }
            alternateState.getView().updateKeywords(c, alternateState);
        }
    }

    public int getHiddenId() {
        final Integer hiddenId = get(TrackableProperty.HiddenId);
        if (hiddenId == null) {
            return getId();
        }
        return hiddenId.intValue();
    }
    void updateHiddenId(final int hiddenId) {
        set(TrackableProperty.HiddenId, hiddenId);
    }

    int getBlockAdditional() {
        return get(TrackableProperty.BlockAdditional);
    }

    boolean getCanBlockAny() {
        return get(TrackableProperty.BlockAny);
    }

    void updateBlockAdditional(Card c) {
        set(TrackableProperty.BlockAdditional, c.canBlockAdditional());
        set(TrackableProperty.BlockAny, c.canBlockAny());
    }

    Set<String> getCantHaveKeyword() {
        return get(TrackableProperty.CantHaveKeyword);
    }

    void updateCantHaveKeyword(Card c) {
        Set<String> keywords = Sets.newTreeSet();
        for (Keyword k : c.getCantHaveKeyword()) {
            keywords.add(k.toString());
        }
        set(TrackableProperty.CantHaveKeyword, keywords);
    }

    @Override
    public String toString() {
        String name = getName();
        if (getId() <= 0) { //if fake card, just return name
            return name;
        }

        if (name.isEmpty()) {
            CardStateView alternate = getAlternateState();
            if (alternate != null) {
                if (isFaceDown()) {
                    return "Face-down card (H" + getHiddenId() + ")";
                } else {
                    return getAlternateState().getName() + " (" + getId() + ")";
                }
            }
        }
        return (name + " (" + getId() + ")").trim();
    }

    public class CardStateView extends TrackableObject {
        private static final long serialVersionUID = 6673944200513430607L;

        private final CardStateName state;

        public CardStateView(final int id0, final CardStateName state0, final Tracker tracker) {
            super(id0, tracker);
            state = state0;
        }

        public String getDisplayId() {
            if (getState().equals(CardStateName.FaceDown)) {
                return "H" + getHiddenId();
            }
            final int id = getId();
            if (id > 0) {
                return String.valueOf(getId());
            }
            return StringUtils.EMPTY;
        }

        @Override
        public String toString() {
            return (getName() + " (" + getDisplayId() + ")").trim();
        }

        public CardView getCard() {
            return CardView.this;
        }

        public CardStateName getState() {
            return state;
        }

        public String getName() {
            return get(TrackableProperty.Name);
        }
        void updateName(CardState c) {
            Card card = c.getCard();
            setName(card.getName(c));

            if (CardView.this.getCurrentState() == this) {
                if (card != null) {
                    CardView.this.updateName(card);
                }
            }
        }
        private void setName(String name0) {
            set(TrackableProperty.Name, name0);
        }

        public ColorSet getColors() {
            return get(TrackableProperty.Colors);
        }
        public ColorSet getOriginalColors() {
            return get(TrackableProperty.OriginalColors);
        }
        void updateColors(Card c) {
            set(TrackableProperty.Colors, c.determineColor());
        }
        void updateColors(CardState c) {
            set(TrackableProperty.Colors, ColorSet.fromMask(c.getColor()));
        }
        void setOriginalColors(Card c) {
            set(TrackableProperty.OriginalColors, c.determineColor());
        }
        void updateHasChangeColors(boolean hasChangeColor) {
            set(TrackableProperty.HasChangedColors, hasChangeColor);
        }
        public boolean hasChangeColors() { return get(TrackableProperty.HasChangedColors); }
        public String getImageKey() {
            return getImageKey(null);
        }
        public String getImageKey(Iterable<PlayerView> viewers) {
            if (canBeShownToAny(viewers)) {
                return get(TrackableProperty.ImageKey);
            }
            return ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD);
        }
        void updateImageKey(Card c) {
            set(TrackableProperty.ImageKey, c.getImageKey());
        }
        void updateImageKey(CardState c) {
            set(TrackableProperty.ImageKey, c.getImageKey());
        }

        public CardTypeView getType() {
            if (isFaceDown() && !isInZone(EnumSet.of(ZoneType.Battlefield, ZoneType.Stack))) {
                return CardType.EMPTY;
            }
            return get(TrackableProperty.Type);
        }
        void updateType(CardState c) {
            CardTypeView type = c.getType();
            if (CardView.this.getCurrentState() == this) {
                Card card = c.getCard();
                if (card != null) {
                    type = type.getTypeWithChanges(card.getChangedCardTypes()); //TODO: find a better way to do this
                    updateRulesText(card.getRules(), type);
                }
            }
            set(TrackableProperty.Type, type);
        }

        public ManaCost getManaCost() {
            return get(TrackableProperty.ManaCost);
        }
        void updateManaCost(CardState c) {
            set(TrackableProperty.ManaCost, c.getManaCost());
        }

        public String getOracleText() {
            return get(TrackableProperty.OracleText);
        }
        void updateOracleText(Card c) {
            set(TrackableProperty.OracleText, c.getOracleText().replace("\\n", "\r\n").trim());
        }

        public String getRulesText() {
            return get(TrackableProperty.RulesText);
        }
        void updateRulesText(CardRules rules, CardTypeView type) {
            String rulesText = null;

            if (type.isVanguard() && rules != null) {
                rulesText = "Hand Modifier: " + rules.getHand() +
                        "\r\nLife Modifier: " + rules.getLife();
            }
            set(TrackableProperty.RulesText, rulesText);
        }

        public int getPower() {
            return get(TrackableProperty.Power);
        }
        void updatePower(Card c) {
            if (c.getCurrentState().getView() == this || c.getAlternateState() == null) {
                set(TrackableProperty.Power, c.getNetPower());
            }
            else {
                set(TrackableProperty.Power, c.getNetPower() - c.getBasePower() + c.getAlternateState().getBasePower());
            }
        }
        void updatePower(CardState c) {
            Card card = c.getCard();
            if (card != null) {
                updatePower(card); //TODO: find a better way to do this
                return;
            }
            set(TrackableProperty.Power, c.getBasePower());
        }

        public int getToughness() {
            return get(TrackableProperty.Toughness);
        }
        void updateToughness(Card c) {
            if (c.getCurrentState().getView() == this || c.getAlternateState() == null) {
                set(TrackableProperty.Toughness, c.getNetToughness());
            }
            else {
                set(TrackableProperty.Toughness, c.getNetToughness() - c.getBaseToughness() + c.getAlternateState().getBaseToughness());
            }
        }
        void updateToughness(CardState c) {
            Card card = c.getCard();
            if (card != null) {
                updateToughness(card); //TODO: find a better way to do this
                return;
            }
            set(TrackableProperty.Toughness, c.getBaseToughness());
        }

        public String getLoyalty() {
            return get(TrackableProperty.Loyalty);
        }
        void updateLoyalty(Card c) {
            if (c.isInZone(ZoneType.Battlefield)) {
                updateLoyalty(String.valueOf(c.getCurrentLoyalty()));
            } else {
                updateLoyalty(c.getCurrentState().getBaseLoyalty());
            }
        }
        void updateLoyalty(String loyalty) {
            set(TrackableProperty.Loyalty, loyalty);
        }
        void updateLoyalty(CardState c) {
            if (CardView.this.getCurrentState() == this) {
                Card card = c.getCard();
                if (card != null) {
                    if (card.isInZone(ZoneType.Battlefield)) {
                        updateLoyalty(card);
                    } else {
                        updateLoyalty(c.getBaseLoyalty());
                    }

                    return;
                }
            }
            set(TrackableProperty.Loyalty, "0"); //alternates don't need loyalty
        }

        public String getSetCode() {
            return get(TrackableProperty.SetCode);
        }
        void updateSetCode(CardState c) {
            set(TrackableProperty.SetCode, c.getSetCode());
        }

        public CardRarity getRarity() {
            return get(TrackableProperty.Rarity);
        }
        void updateRarity(CardState c) {
            set(TrackableProperty.Rarity, c.getRarity());
        }

        private int foilIndexOverride = -1;
        public int getFoilIndex() {
            if (foilIndexOverride >= 0) {
                return foilIndexOverride;
            }
            return get(TrackableProperty.FoilIndex);
        }
        void updateFoilIndex(Card c) {
            updateFoilIndex(c.getCurrentState());
        }
        void updateFoilIndex(CardState c) {
            set(TrackableProperty.FoilIndex, c.getFoil());
        }
        public void setFoilIndexOverride(int index0) {
            if (index0 < 0) {
                index0 = CardEdition.getRandomFoil(getSetCode());
            }
            foilIndexOverride = index0;
        }

        public String getKeywordKey() { return get(TrackableProperty.KeywordKey); }
        public String getProtectionKey() { return get(TrackableProperty.ProtectionKey); }
        public String getHexproofKey() { return get(TrackableProperty.HexproofKey); }
        public boolean hasDeathtouch() { return get(TrackableProperty.HasDeathtouch); }
        public boolean hasDefender() { return get(TrackableProperty.HasDefender); }
        public boolean hasDoubleStrike() { return get(TrackableProperty.HasDoubleStrike); }
        public boolean hasFirstStrike() { return get(TrackableProperty.HasFirstStrike); }
        public boolean hasFlying() { return get(TrackableProperty.HasFlying); }
        public boolean hasFear() { return get(TrackableProperty.HasFear); }
        public boolean hasHexproof() { return get(TrackableProperty.HasHexproof); }
        public boolean hasHorsemanship() { return get(TrackableProperty.HasHorsemanship); }
        public boolean hasIndestructible() { return get(TrackableProperty.HasIndestructible); }
        public boolean hasIntimidate() { return get(TrackableProperty.HasIntimidate); }
        public boolean hasLifelink() { return get(TrackableProperty.HasLifelink); }
        public boolean hasMenace() { return get(TrackableProperty.HasMenace); }
        public boolean hasReach() { return get(TrackableProperty.HasReach); }
        public boolean hasShadow() { return get(TrackableProperty.HasShadow); }
        public boolean hasShroud() { return get(TrackableProperty.HasShroud); }
        public boolean hasTrample() { return get(TrackableProperty.HasTrample); }
        public boolean hasVigilance() { return get(TrackableProperty.HasVigilance); }

        public boolean hasHaste() {
            return get(TrackableProperty.HasHaste);
        }
        public boolean hasInfect() {
            return get(TrackableProperty.HasInfect);
        }
        public boolean hasStorm() {
            return get(TrackableProperty.HasStorm);
        }

        public String getAbilityText() {
            return get(TrackableProperty.AbilityText);
        }
        void updateAbilityText(Card c, CardState state) {
            set(TrackableProperty.AbilityText, c.getAbilityText(state));
        }
        void updateKeywords(Card c, CardState state) {
            c.updateKeywordsCache(state);
            set(TrackableProperty.HasDeathtouch, c.hasKeyword(Keyword.DEATHTOUCH, state));
            set(TrackableProperty.HasDefender, c.hasKeyword(Keyword.DEFENDER, state));
            set(TrackableProperty.HasDoubleStrike, c.hasKeyword(Keyword.DOUBLE_STRIKE, state));
            set(TrackableProperty.HasFirstStrike, c.hasKeyword(Keyword.FIRST_STRIKE, state));
            set(TrackableProperty.HasFlying, c.hasKeyword(Keyword.FLYING, state));
            set(TrackableProperty.HasFear, c.hasKeyword(Keyword.FEAR, state));
            set(TrackableProperty.HasHexproof, c.hasKeyword(Keyword.HEXPROOF, state));
            set(TrackableProperty.HasHorsemanship, c.hasKeyword(Keyword.HORSEMANSHIP, state));
            set(TrackableProperty.HasIndestructible, c.hasKeyword(Keyword.INDESTRUCTIBLE, state));
            set(TrackableProperty.HasIntimidate, c.hasKeyword(Keyword.INTIMIDATE, state));
            set(TrackableProperty.HasLifelink, c.hasKeyword(Keyword.LIFELINK, state));
            set(TrackableProperty.HasMenace, c.hasKeyword(Keyword.MENACE, state));
            set(TrackableProperty.HasReach, c.hasKeyword(Keyword.REACH, state));
            set(TrackableProperty.HasShadow, c.hasKeyword(Keyword.SHADOW, state));
            set(TrackableProperty.HasShroud, c.hasKeyword(Keyword.SHROUD, state));
            set(TrackableProperty.HasTrample, c.hasKeyword(Keyword.TRAMPLE, state));
            set(TrackableProperty.HasVigilance, c.hasKeyword(Keyword.VIGILANCE, state));
            set(TrackableProperty.HasHaste, c.hasKeyword(Keyword.HASTE, state));
            set(TrackableProperty.HasInfect, c.hasKeyword(Keyword.INFECT, state));
            set(TrackableProperty.HasStorm, c.hasKeyword(Keyword.STORM, state));
            updateAbilityText(c, state);
            //set protectionKey for Icons
            set(TrackableProperty.ProtectionKey, c.getProtectionKey());
            //set hexproofKeys for Icons
            set(TrackableProperty.HexproofKey, c.getHexproofKey());
            //keywordkey
            set(TrackableProperty.KeywordKey, c.getKeywordKey());
        }

        public boolean isBasicLand() {
            return getType().isBasicLand();
        }
        public boolean isCreature() {
            return getType().isCreature();
        }
        public boolean isLand() {
            return getType().isLand();
        }
        public boolean isPlane() {
            return getType().isPlane();
        }
        public boolean isPhenomenon() {
            return getType().isPhenomenon();
        }
        public boolean isPlaneswalker() {
            return getType().isPlaneswalker();
        }
    }

    //special methods for updating card and player properties as needed and returning the new collection
    Card setCard(Card oldCard, Card newCard, TrackableProperty key) {
        if (newCard != oldCard) {
            set(key, CardView.get(newCard));
        }
        return newCard;
    }
    CardCollection setCards(CardCollection oldCards, CardCollection newCards, TrackableProperty key) {
        if (newCards == null || newCards.isEmpty()) { //avoid storing empty collections
            set(key, null);
            return null;
        }
        set(key, CardView.getCollection(newCards)); //TODO prevent overwriting list if not necessary
        return newCards;
    }
    CardCollection setCards(CardCollection oldCards, Iterable<Card> newCards, TrackableProperty key) {
        if (newCards == null) {
            set(key, null);
            return null;
        }
        return setCards(oldCards, new CardCollection(newCards), key);
    }
    CardCollection addCard(CardCollection oldCards, Card cardToAdd, TrackableProperty key) {
        if (cardToAdd == null) { return oldCards; }

        if (oldCards == null) {
            oldCards = new CardCollection();
        }
        if (oldCards.add(cardToAdd)) {
            TrackableCollection<CardView> views = get(key);
            if (views == null) {
                views = new TrackableCollection<>();
                views.add(cardToAdd.getView());
                set(key, views);
            }
            else if (views.add(cardToAdd.getView())) {
                flagAsChanged(key);
            }
        }
        return oldCards;
    }
    CardCollection addCards(CardCollection oldCards, Iterable<Card> cardsToAdd, TrackableProperty key) {
        if (cardsToAdd == null) { return oldCards; }

        TrackableCollection<CardView> views = get(key);
        if (oldCards == null) {
            oldCards = new CardCollection();
        }
        boolean needFlagAsChanged = false;
        for (Card c : cardsToAdd) {
            if (c != null && oldCards.add(c)) {
                if (views == null) {
                    views = new TrackableCollection<>();
                    views.add(c.getView());
                    set(key, views);
                }
                else if (views.add(c.getView())) {
                    needFlagAsChanged = true;
                }
            }
        }
        if (needFlagAsChanged) {
            flagAsChanged(key);
        }
        return oldCards;
    }
    CardCollection removeCard(CardCollection oldCards, Card cardToRemove, TrackableProperty key) {
        if (cardToRemove == null || oldCards == null) { return oldCards; }

        if (oldCards.remove(cardToRemove)) {
            TrackableCollection<CardView> views = get(key);
            if (views == null) {
                set(key, null);
            } else if (views.remove(cardToRemove.getView())) {
                if (views.isEmpty()) {
                    set(key, null); //avoid keeping around an empty collection
                }
                else {
                    flagAsChanged(key);
                }
            }
            if (oldCards.isEmpty()) {
                oldCards = null; //avoid keeping around an empty collection
            }
        }
        return oldCards;
    }
    CardCollection removeCards(CardCollection oldCards, Iterable<Card> cardsToRemove, TrackableProperty key) {
        if (cardsToRemove == null || oldCards == null) { return oldCards; }

        TrackableCollection<CardView> views = get(key);
        boolean needFlagAsChanged = false;
        for (Card c : cardsToRemove) {
            if (oldCards.remove(c)) {
                if (views == null) {
                    set(key, null);
                } else if (views.remove(c.getView())) {
                    if (views.isEmpty()) {
                        views = null;
                        set(key, null); //avoid keeping around an empty collection
                        needFlagAsChanged = false; //doesn't need to be flagged a second time
                    }
                    else {
                        needFlagAsChanged = true;
                    }
                }
                if (oldCards.isEmpty()) {
                    oldCards = null; //avoid keeping around an empty collection
                    break;
                }
            }
        }
        if (needFlagAsChanged) {
            flagAsChanged(key);
        }
        return oldCards;
    }
    CardCollection clearCards(CardCollection oldCards, TrackableProperty key) {
        if (oldCards != null) {
            set(key, null);
        }
        return null;
    }
}
