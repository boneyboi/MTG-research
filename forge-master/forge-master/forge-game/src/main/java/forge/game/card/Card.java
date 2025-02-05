/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.card;

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import forge.GameCommand;
import forge.ImageKeys;
import forge.StaticData;
import forge.card.*;
import forge.card.CardDb.SetPreference;
import forge.card.CardType.CoreType;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.*;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostSacrifice;
import forge.game.event.*;
import forge.game.event.GameEventCardDamaged.DamageType;
import forge.game.keyword.*;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplaceMoved;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.*;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.trackable.TrackableProperty;
import forge.util.*;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.maps.HashMapOfLists;
import forge.util.maps.MapOfLists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import java.util.*;
import java.util.Map.Entry;

import io.sentry.Sentry;
import io.sentry.event.BreadcrumbBuilder;

/**
 * <p>
 * Card class.
 * </p>
 *
 * Can now be used as keys in Tree data structures. The comparison is based
 * entirely on id.
 *
 * @author Forge
 * @version $Id$
 */
public class Card extends GameEntity implements Comparable<Card> {
    //research variables
    private int votes = 0;

    private final Game game;
    private final IPaperCard paperCard;

    private final Map<CardStateName, CardState> states = Maps.newEnumMap(CardStateName.class);
    private CardState currentState;
    private CardStateName currentStateName = CardStateName.Original;

    private ZoneType castFrom = null;
    private SpellAbility castSA = null;

    private final CardDamageHistory damageHistory = new CardDamageHistory();
    private Map<Card, Map<CounterType, Integer>> countersAddedBy = Maps.newTreeMap();
    // Hidden keywords won't be displayed on the card
    private final KeywordCollection hiddenExtrinsicKeyword = new KeywordCollection();

    // cards attached or otherwise linked to this card
    private CardCollection hauntedBy, devouredCards, exploitedCards, delvedCards, convokedCards, imprintedCards, encodedCards;
    private CardCollection mustBlockCards, gainControlTargets, chosenCards, blockedThisTurn, blockedByThisTurn;

    // if this card is attached or linked to something, what card is it currently attached to
    private Card encoding, cloneOrigin, haunting, effectSource, pairedWith, meldedWith;

    private SpellAbility effectSourceAbility = null;

    private GameEntity entityAttachedTo = null;

    private GameEntity mustAttackEntity = null;
    private GameEntity mustAttackEntityThisTurn = null;

    private final Map<StaticAbility, CardPlayOption> mayPlay = Maps.newHashMap();

    private final Multimap<Long, Player> withFlash = HashMultimap.create();

    // changes by AF animate and continuous static effects - timestamp is the key of maps
    private final Map<Long, CardChangedType> changedCardTypes = Maps.newTreeMap();
    private final NavigableMap<Long, String> changedCardNames = Maps.newTreeMap();
    private final Map<Long, KeywordsChange> changedCardKeywords = Maps.newTreeMap();
    private final Map<Long, CardTraitChanges> changedCardTraits = Maps.newTreeMap();
    private final Map<Long, CardColor> changedCardColors = Maps.newTreeMap();
    private final NavigableMap<Long, CardCloneStates> clonedStates = Maps.newTreeMap();
    private final NavigableMap<Long, CardCloneStates> textChangeStates = Maps.newTreeMap();

    private final Multimap<Long, Keyword> cantHaveKeywords = MultimapBuilder.hashKeys().enumSetValues(Keyword.class).build();

    private final Map<CounterType, Long> counterTypeTimestamps = Maps.newEnumMap(CounterType.class);

    private final Map<Long, Integer> canBlockAdditional = Maps.newTreeMap();
    private final Set<Long> canBlockAny = Sets.newHashSet();

    // changes that say "replace each instance of one [color,type] by another - timestamp is the key of maps
    private final CardChangedWords changedTextColors = new CardChangedWords();
    private final CardChangedWords changedTextTypes = new CardChangedWords();
    /** List of the keywords that have been added by text changes. */
    private final List<KeywordInterface> keywordsGrantedByTextChanges = Lists.newArrayList();

    /** Original values of SVars changed by text changes. */
    private Map<String, String> originalSVars = Maps.newHashMap();

    private final Set<Object> rememberedObjects = Sets.newLinkedHashSet();
    private final MapOfLists<GameEntity, Object> rememberMap = new HashMapOfLists<>(CollectionSuppliers.arrayLists());
    private Map<Player, String> flipResult;

    private Map<Card, Integer> receivedDamageFromThisTurn = Maps.newHashMap();
    private Map<Player, Integer> receivedDamageFromPlayerThisTurn = Maps.newHashMap();

    private Map<Card, Integer> dealtDamageToThisTurn = Maps.newTreeMap();
    private Map<String, Integer> dealtDamageToPlayerThisTurn = Maps.newTreeMap();
    private final Map<Card, Integer> assignedDamageMap = Maps.newTreeMap();

    private boolean isCommander = false;
    private boolean startsGameInPlay = false;
    private boolean drawnThisTurn = false;
    private boolean becameTargetThisTurn = false;
    private boolean startedTheTurnUntapped = false;
    private boolean cameUnderControlSinceLastUpkeep = true; // for Echo
    private boolean tapped = false;
    private boolean sickness = true; // summoning sickness
    private boolean token = false;
    private Card copiedPermanent = null;
    private boolean copiedSpell = false;

    private boolean canCounter = true;

    private boolean unearthed;

    private boolean monstrous = false;

    private boolean renowned = false;

    private boolean manifested = false;

    private long bestowTimestamp = -1;
    private long transformedTimestamp = 0;
    private boolean tributed = false;
    private boolean embalmed = false;
    private boolean eternalized = false;
    private boolean madness = false;
    private boolean madnessWithoutCast = false;

    private boolean flipped = false;
    private boolean facedown = false;
    // set for transform and meld, needed for clone effects
    private boolean backside = false;

    private boolean phasedOut = false;
    private boolean directlyPhasedOut = true;

    private boolean usedToPayCost = false;

    // for Vanguard / Manapool / Emblems etc.
    private boolean isImmutable = false;

    private int exertThisTurn = 0;
    private PlayerCollection exertedByPlayer = new PlayerCollection();

    private long timestamp = -1; // permanents on the battlefield

    // stack of set power/toughness
    private Map<Long, Pair<Integer,Integer>> newPT = Maps.newTreeMap();
    private Map<Long, Pair<Integer,Integer>> newPTCharacterDefining = Maps.newTreeMap();

    // x=Static Avility id or 0, y=timestamp
    private Table<Integer, Long, Pair<Integer,Integer>> boostPT = TreeBasedTable.create();

    private String basePowerString = null;
    private String baseToughnessString = null;
    private String oracleText = "";

    private int damage;
    private boolean hasBeenDealtDeathtouchDamage = false;

    // regeneration
    private FCollection<Card> shields = new FCollection<>();
    private int regeneratedThisTurn = 0;

    private int turnInZone;

    private Map<String, Integer> xManaCostPaidByColor;

    private int sunburstValue = 0;
    private byte colorsPaid = 0;

    private Player owner = null;
    private Player controller = null;
    private long controllerTimestamp = 0;
    private NavigableMap<Long, Player> tempControllers = Maps.newTreeMap();

    private String originalText = "", text = "";
    private String chosenType = "";
    private List<String> chosenColors;
    private String chosenName = "";
    private Integer chosenNumber;
    private Player chosenPlayer;
    private EvenOdd chosenEvenOdd = null;
    private Direction chosenDirection = null;
    private String chosenMode = "";

    private Card exiledWith = null;

    private Map<Long, Player> goad = Maps.newTreeMap();

    private final List<GameCommand> leavePlayCommandList = Lists.newArrayList();
    private final List<GameCommand> etbCommandList = Lists.newArrayList();
    private final List<GameCommand> untapCommandList = Lists.newArrayList();
    private final List<GameCommand> changeControllerCommandList = Lists.newArrayList();
    private final List<GameCommand> unattachCommandList = Lists.newArrayList();
    private final List<GameCommand> faceupCommandList = Lists.newArrayList();
    private final List<GameCommand> facedownCommandList = Lists.newArrayList();
    private final List<Object[]> staticCommandList = Lists.newArrayList();

    private final static ImmutableList<String> storableSVars = ImmutableList.of("ChosenX");

    // Zone-changing spells should store card's zone here
    private Zone currentZone = null;

    // LKI copies of cards are allowed to store the LKI about the zone the card was known to be in last.
    // For all cards except LKI copies this should always be null.
    private Zone savedLastKnownZone = null;
    // LKI copies of cards store CMC separately to avoid shenanigans with the game state visualization
    // breaking when the LKI object is changed to a different card state.
    private int lkiCMC = -1;

    private int countersAdded = 0;

    private CardRules cardRules;
    private final CardView view;

    private Table<Player, CounterType, Integer> etbCounters = HashBasedTable.create();

    private SpellAbility[] basicLandAbilities = new SpellAbility[MagicColor.WUBRG.length];

    private int planeswalkerAbilityActivated = 0;

    private final Map<SpellAbility, Integer> numberTurnActivations = Maps.newHashMap();
    private final Map<SpellAbility, Integer> numberGameActivations = Maps.newHashMap();

    private final Table<SpellAbility, StaticAbility, Integer> numberTurnActivationsStatic = HashBasedTable.create();
    private final Table<SpellAbility, StaticAbility, Integer> numberGameActivationsStatic = HashBasedTable.create();


    // Enumeration for CMC request types
    public enum SplitCMCMode {
        CurrentSideCMC,
        CombinedCMC,
        LeftSplitCMC,
        RightSplitCMC
    }

    /**
     * Instantiates a new card not associated to any paper card.
     * @param id0 the unique id of the new card.
     */
    public Card(final int id0, final Game game0) {
        this(id0, null, game0);
    }

    /**
     * Instantiates a new card with a given paper card.
     * @param id0 the unique id of the new card.
     * @param paperCard0 the {@link IPaperCard} of which the new card is a
     * representation, or {@code null} if this new {@link Card} doesn't represent any paper
     * card.
     * @see IPaperCard
     */
    public Card(final int id0, final IPaperCard paperCard0, final Game game0) {
        super(id0);

        game = game0;
        paperCard = paperCard0;
        view = new CardView(id0, game == null ? null : game.getTracker());
        currentState = new CardState(view.getCurrentState(), this);
        states.put(CardStateName.Original, currentState);
        view.updateChangedColorWords(this);
        view.updateChangedTypes(this);
        view.updateSickness(this);
    }

    //research methods - getVotes, setVotes, incrementVotes
    /**
     * returns the number of votes a card has
     * @return votes - umber of votes a card has
     */
    public int getVotes() {
        return votes;
    }

    /**
     * Sets the number of votes to the input value
     * @param value - value that sets
     */
    public void setVotes(int value) {
        votes = value;
    }

    /**
     * Increments number of votes by one
     */
    public void incrementVotes() {
        votes++;
    }

    public boolean changeToState(final CardStateName state) {
        if (hasState(state)) {
            return setState(state, true);
        }
        return false;
    }

    public long getTransformedTimestamp() {  return transformedTimestamp; }
    public void incrementTransformedTimestamp() {  this.transformedTimestamp++;  }

    public CardState getCurrentState() {
        return currentState;
    }

    public CardStateName getAlternateStateName() {
        if (hasAlternateState()) {
            if (isSplitCard()) {
                if (currentStateName == CardStateName.RightSplit) {
                    return CardStateName.LeftSplit;
                }
                else {
                    return CardStateName.RightSplit;
                }
            }
            else if (isFlipCard() && currentStateName != CardStateName.Flipped) {
                return CardStateName.Flipped;
            }
            else if (isDoubleFaced() && currentStateName != CardStateName.Transformed) {
                return CardStateName.Transformed;
            }
            else if (isMeldable() && currentStateName != CardStateName.Meld) {
                return CardStateName.Meld;
            }
            else if (this.isAdventureCard() && currentStateName != CardStateName.Adventure) {
                return CardStateName.Adventure;
            }
            else {
                return CardStateName.Original;
            }
        }
        else if (isFaceDown()) {
            return CardStateName.Original;
        }
        return null;
    }

    public CardState getAlternateState() {
        if (hasAlternateState() || isFaceDown()) {
            return states.get(getAlternateStateName());
        }
        return null;
    }

    public CardState getState(final CardStateName state) {
        return getState(state, false);
    }
    public CardState getState(final CardStateName state, boolean skipTextChange) {
        if (!skipTextChange) {
            CardCloneStates txtStates = getLastTextChangeState();
            if (txtStates != null) {
                return txtStates.get(state);
            }
        }
        CardCloneStates clStates = getLastClonedState();
        if (clStates == null) {
            return getOriginalState(state);
        } else {
            return clStates.get(state);
        }
    }

    public boolean hasState(final CardStateName state) {
        if (state == CardStateName.FaceDown) {
            return true;
        }
        CardCloneStates clStates = getLastClonedState();
        if (clStates == null) {
            return states.containsKey(state);
        } else {
            return clStates.containsKey(state);
        }
    }

    public CardState getOriginalState(final CardStateName state) {
        if (!states.containsKey(state) && state == CardStateName.FaceDown) {
            states.put(CardStateName.FaceDown, CardUtil.getFaceDownCharacteristic(this));
        }
        return states.get(state);
    }

    public boolean setState(final CardStateName state, boolean updateView) {
        return setState(state, updateView, false);
    }
    public boolean setState(final CardStateName state, boolean updateView, boolean forceUpdate) {
        CardCloneStates textChangeStates = getLastTextChangeState();

        if (textChangeStates != null) {
            if (!textChangeStates.containsKey(state)) {
                throw new RuntimeException(getName() + " tried to switch to non-existant text change state \"" + state + "\"!");
                //return false; // Nonexistant state.
            }
        } else {
            CardCloneStates cloneStates = getLastClonedState();
            if (cloneStates != null) {
                if (!cloneStates.containsKey(state)) {
                    throw new RuntimeException(getName() + " tried to switch to non-existant cloned state \"" + state + "\"!");
                    //return false; // Nonexistant state.
                }
            } else {
                if (!states.containsKey(state)) {
                    if (state == CardStateName.FaceDown) {
                        // The face-down state is created lazily only when needed.
                        states.put(CardStateName.FaceDown, CardUtil.getFaceDownCharacteristic(this));
                    } else {
                        System.out.println(getName() + " tried to switch to non-existant state \"" + state + "\"!");
                        return false; // Nonexistant state.
                    }
                }
            }
        }

        if (state.equals(currentStateName) && !forceUpdate) {
            return false;
        }

        // Cleared tests, about to change states
        if (currentStateName.equals(CardStateName.FaceDown) && state.equals(CardStateName.Original)) {
            this.setManifested(false);
        }

        currentStateName = state;
        currentState = getState(state);

        // update the host for static abilities
        for (StaticAbility sa : currentState.getStaticAbilities()) {
            sa.setHostCard(this);
        }

        if (updateView) {
            view.updateState(this);

            final Game game = getGame();
            if (game != null) {
                // update Type, color and keywords again if they have changed
                if (!changedCardTypes.isEmpty()) {
                    currentState.getView().updateType(currentState);
                }
                if (!changedCardColors.isEmpty()) {
                    currentState.getView().updateColors(this);
                }
                if (!changedCardKeywords.isEmpty()) {
                    updateKeywords();
                }

                if (state == CardStateName.FaceDown) {
                    view.updateHiddenId(game.nextHiddenCardId());
                }
                game.fireEvent(new GameEventCardStatsChanged(this)); //ensure stats updated for new characteristics
            }
        }
        return true;
    }

    public Set<CardStateName> getStates() {
        return states.keySet();
    }

    public CardStateName getCurrentStateName() {
        return currentStateName;
    }

    // use by CopyPermament
    public void setStates(Map<CardStateName, CardState> map) {
        states.clear();
        states.putAll(map);
    }

    // was only used for Clone Effects
    @Deprecated
    public void switchStates(final CardStateName from, final CardStateName to, boolean updateView) {
        final CardState tmp = states.get(from);
        states.put(from, states.get(to));
        states.put(to, tmp);
        if (currentStateName == from) {
            setState(to, false);
        }
        if (updateView) {
            view.updateState(this);
        }
    }

    public final void addAlternateState(final CardStateName state, final boolean updateView) {
        states.put(state, new CardState(this, state));
        if (updateView) {
            view.updateState(this);
        }
    }

    public void clearStates(final CardStateName state, boolean updateView) {
        if (states.remove(state) == null) {
            return;
        }
        if (state == currentStateName) {
            currentStateName = CardStateName.Original;
        }
        if (updateView) {
            view.updateState(this);
        }
    }

    public void updateStateForView() {
        view.updateState(this);
    }

    // The following methods are used to selectively update certain view components (text,
    // P/T, card types) in order to avoid card flickering due to aggressive full update
    public void updateAbilityTextForView() {
        updateKeywords();
        view.getCurrentState().updateAbilityText(this, getCurrentState());
    }

    public final void updatePowerToughnessForView() {
        view.updateCounters(this);
    }

    public final void updateTypesForView() {
        currentState.getView().updateType(currentState);
    }

    public boolean changeCardState(final String mode, final String customState) {
        if (mode == null)
            return changeToState(CardStateName.smartValueOf(customState));

        // flip and face-down don't overlap. That is there is no chance to turn face down a flipped permanent
        // and then any effect have it turn upface again and demand its former flip state to be restored
        // Proof: Morph cards never have ability that makes them flip, Ixidron does not suppose cards to be turned face up again,
        // Illusionary Mask affects cards in hand.
        CardStateName oldState = getCurrentStateName();
        if (mode.equals("Transform") && isDoubleFaced()) {
            if (!canTransform()) {
                return false;
            }

            backside = !backside;

            boolean result = changeToState(backside ? CardStateName.Transformed : CardStateName.Original);

            // do the Transform trigger there, it can also happen if the resulting state doesn't change

            // Clear old dfc trigger from the trigger handler
            getGame().getTriggerHandler().clearInstrinsicActiveTriggers(this, null);
            getGame().getTriggerHandler().registerActiveTrigger(this, false);
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Transformer, this);
            getGame().getTriggerHandler().runTrigger(TriggerType.Transformed, runParams, false);
            incrementTransformedTimestamp();

            return result;

        } else if (mode.equals("Flip") && isFlipCard()) {
            // 709.4. Flipping a permanent is a one-way process.
            if (isFlipped()) {
                return false;
            }

            flipped = true;

            // a facedown card does flip but the state doesn't change
            if (isFaceDown()) {
                return false;
            }

            return changeToState(CardStateName.Flipped);
        } else if (mode.equals("TurnFace")) {
            if (oldState == CardStateName.Original || oldState == CardStateName.Flipped) {
                return turnFaceDown();
            } else if (isFaceDown()) {
                return turnFaceUp();
            }
        } else if (mode.equals("Meld") && isMeldable()) {
            return changeToState(CardStateName.Meld);
        }
        return false;
    }

    public Card manifest(Player p, SpellAbility sa) {
        // Turn Face Down (even if it's DFC).
        ManaCost cost = getState(CardStateName.Original).getManaCost();

        boolean isCreature = isCreature();

        // Sometimes cards are manifested while already being face down
        if (!turnFaceDown(true) && !isFaceDown()) {
            return null;
        }
        // Move to p's battlefield
        Game game = p.getGame();

        // Just in case you aren't the controller, now you are!
        setController(p, game.getNextTimestamp());

        // Mark this card as "manifested"
        setManifested(true);

        Card c = game.getAction().moveToPlay(this, p, sa);

        // Add manifest demorph static ability for creatures
        if (c.isManifested() && isCreature && !cost.isNoCost()) {
            // Add Manifest to original State
            c.getState(CardStateName.Original).addSpellAbility(CardFactoryUtil.abilityManifestFaceUp(c, cost));
            c.updateStateForView();
        }

        return c;
    }

    public boolean turnFaceDown() {
        return turnFaceDown(false);
    }

    public boolean turnFaceDown(boolean override) {
        if (override || (!isDoubleFaced() && !isMeldable())) {
            facedown = true;
            if (setState(CardStateName.FaceDown, true)) {
                runFacedownCommands();
                return true;
            }
        }
        return false;
    }

    public boolean turnFaceDownNoUpdate() {
        facedown = true;
        return setState(CardStateName.FaceDown, false);
    }

    public boolean turnFaceUp() {
        return turnFaceUp(false, true);
    }

    public boolean turnFaceUp(boolean manifestPaid, boolean runTriggers) {
        if (isFaceDown()) {
            if (manifestPaid && isManifested() && !getRules().getType().isCreature()) {
                // If we've manifested a non-creature and we're demanifesting disallow it

                // Unless this creature also has a Morph ability
                return false;
            }

            boolean result;
            if (isFlipped() && isFlipCard()) {
                result = setState(CardStateName.Flipped, true);
            } else {
                result = setState(CardStateName.Original, true);
            }

            facedown = false;
            // need to run faceup commands, currently
            // it does cleanup the modified facedown state
            if (result) {
                runFaceupCommands();
            }

            if (result && runTriggers) {
                // Run replacement effects
                getGame().getReplacementHandler().run(ReplacementType.TurnFaceUp, AbilityKey.mapFromAffected(this));

                // Run triggers
                getGame().getTriggerHandler().registerActiveTrigger(this, false);
                getGame().getTriggerHandler().runTrigger(TriggerType.TurnFaceUp, AbilityKey.mapFromCard(this), false);
            }
            return result;
        }
        return false;
    }

    public boolean canTransform() {
        if (isFaceDown() || !isDoubleFaced()) {
            return false;
        }

        CardStateName destState = backside ? CardStateName.Original : CardStateName.Transformed;

        // below only when in play
        if (!isInPlay()) {
            return true;
        }

        // use Original State for the transform check
        if (!getOriginalState(destState).getType().isPermanent()) {
            return false;
        }

        return !hasKeyword("CARDNAME can't transform");
    }

    public int getHiddenId() {
        return view.getHiddenId();
    }

    public void updateAttackingForView() {
        view.updateAttacking(this);
        getGame().updateCombatForView();
    }
    public void updateBlockingForView() {
        view.updateBlocking(this);
        getGame().updateCombatForView(); //ensure blocking arrow shown/hidden as needed
    }

    @Override
    public final String getName() {
        return getName(currentState);
    }

    public final String getName(CardState state) {
        if (changedCardNames.isEmpty()) {
            return state.getName();
        }
        return changedCardNames.lastEntry().getValue();
    }

    @Override
    public final void setName(final String name0) {
        currentState.setName(name0);
    }

    public void addChangedName(final String name0, Long timestamp) {
        changedCardNames.put(timestamp, name0);
        updateNameforView();
    }

    public void removeChangedName(Long timestamp) {
        if (changedCardNames.remove(timestamp) != null) {
            updateNameforView();
        }
    }

    public void updateNameforView() {
        currentState.getView().updateName(currentState);
    }

    public Map<Long, String> getChangedCardNames() {
        return Collections.unmodifiableMap(changedCardNames);
    }

    public void setChangedCardNames(Map<Long, String> changedCardNames) {
        this.changedCardNames.clear();
        for (Entry<Long, String> entry : changedCardNames.entrySet()) {
            this.changedCardNames.put(entry.getKey(), entry.getValue());
        }
    }

    public final boolean isInAlternateState() {
        return currentStateName != CardStateName.Original;
    }

    public final boolean hasAlternateState() {
        // Note: Since FaceDown state is created lazily (whereas previously
        // it was always created), adjust threshold based on its existence.
        int threshold = (states.containsKey(CardStateName.FaceDown) ? 2 : 1);

        int numStates = states.keySet().size();

        return numStates > threshold;
    }

    public final boolean isDoubleFaced() {
        return getRules() != null && getRules().getSplitType() == CardSplitType.Transform;
    }

    public final boolean isMeldable() {
        return getRules() != null && getRules().getSplitType() == CardSplitType.Meld;
    }

    public final boolean isFlipCard() {
        return hasState(CardStateName.Flipped);
    }

    public final boolean isSplitCard() {
        return getRules() != null && getRules().getSplitType() == CardSplitType.Split;
    }

    public final boolean isAdventureCard() {
        return hasState(CardStateName.Adventure);
    }

    public final boolean isBackSide() {
        return backside;
    }

    public boolean isCloned() {
        return !clonedStates.isEmpty();
    }

    public static List<String> getStorableSVars() {
        return Card.storableSVars;
    }

    public final CardCollectionView getDevouredCards() {
        return CardCollection.getView(devouredCards);
    }
    public final void addDevoured(final Card c) {
        if (devouredCards == null) {
            devouredCards = new CardCollection();
        }
        devouredCards.add(c);
    }

    public final void clearDevoured() {
        devouredCards = null;
    }

    public final CardCollectionView getExploited() {
        return CardCollection.getView(exploitedCards);
    }
    public final void addExploited(final Card c) {
        if (exploitedCards == null) {
            exploitedCards = new CardCollection();
        }
        exploitedCards.add(c);
    }
    public final void clearExploited() {
        exploitedCards = null;
    }

    public final CardCollectionView getDelved() {
        return CardCollection.getView(delvedCards);
    }
    public final void addDelved(final Card c) {
        if (delvedCards == null) {
            delvedCards = new CardCollection();
        }
        delvedCards.add(c);
    }

    public final void clearDelved() {
        delvedCards = null;
    }


    public final CardCollectionView getConvoked() {
        return CardCollection.getView(convokedCards);
    }
    public final void addConvoked(final Card c) {
        if (convokedCards == null) {
            convokedCards = new CardCollection();
        }
        convokedCards.add(c);
    }
    public final void clearConvoked() {
        convokedCards = null;
    }

    public MapOfLists<GameEntity, Object> getRememberMap() {
        return rememberMap;
    }
    public final void addRememberMap(final GameEntity e, final List<Object> o) {
        rememberMap.addAll(e, o);
    }

    public final Iterable<Object> getRemembered() {
        return rememberedObjects;
    }
    public final boolean hasRemembered() {
        return !rememberedObjects.isEmpty();
    }
    public final int getRememberedCount() {
        return rememberedObjects.size();
    }
    public final Object getFirstRemembered() {
        return Iterables.getFirst(rememberedObjects, null);
    }
    public final <T> boolean isRemembered(T o) {
        return rememberedObjects.contains(o);
    }
    public final <T> void addRemembered(final T o) {
        if (rememberedObjects.add(o)) {
            view.updateRemembered(this);
        }
    }
    public final <T> void addRemembered(final Iterable<T> objects) {
        boolean changed = false;
        for (T o : objects) {
            if (rememberedObjects.add(o)) {
                changed = true;
            }
        }
        if (changed) {
            view.updateRemembered(this);
        }
    }
    public final <T> void removeRemembered(final T o) {
        if (rememberedObjects.remove(o)) {
            view.updateRemembered(this);
        }
    }

    public final <T> void removeRemembered(final Iterable<T> list) {
        boolean changed = false;
        for (T o : list) {
            if (rememberedObjects.remove(o)) {
                changed = true;
            }
        }
        if (changed) {
            view.updateRemembered(this);
        }
    }
    public final void clearRemembered() {
        if (rememberedObjects.isEmpty()) { return; }
        rememberedObjects.clear();
        view.updateRemembered(this);
    }

    public final CardCollectionView getImprintedCards() {
        return CardCollection.getView(imprintedCards);
    }
    public final boolean hasImprintedCard() {
        return FCollection.hasElements(imprintedCards);
    }
    public final boolean hasImprintedCard(Card c) {
        return FCollection.hasElement(imprintedCards, c);
    }
    public final void addImprintedCard(final Card c) {
        imprintedCards = view.addCard(imprintedCards, c, TrackableProperty.ImprintedCards);
    }
    public final void addImprintedCards(final Iterable<Card> cards) {
        imprintedCards = view.addCards(imprintedCards, cards, TrackableProperty.ImprintedCards);
    }
    public final void removeImprintedCard(final Card c) {
        imprintedCards = view.removeCard(imprintedCards, c, TrackableProperty.ImprintedCards);
    }
    public final void removeImprintedCards(final Iterable<Card> cards) {
        imprintedCards = view.removeCards(imprintedCards, cards, TrackableProperty.ImprintedCards);
    }
    public final void clearImprintedCards() {
        imprintedCards = view.clearCards(imprintedCards, TrackableProperty.ImprintedCards);
    }

    public final CardCollectionView getEncodedCards() {
        return CardCollection.getView(encodedCards);
    }
    public final boolean hasEncodedCard() {
        return FCollection.hasElements(encodedCards);
    }
    public final boolean hasEncodedCard(Card c) {
        return FCollection.hasElement(encodedCards, c);
    }
    public final void addEncodedCard(final Card c) {
        encodedCards = view.addCard(encodedCards, c, TrackableProperty.EncodedCards);
    }
    public final void addEncodedCards(final Iterable<Card> cards) {
        encodedCards = view.addCards(encodedCards, cards, TrackableProperty.EncodedCards);
    }
    public final void removeEncodedCard(final Card c) {
        encodedCards = view.removeCard(encodedCards, c, TrackableProperty.EncodedCards);
    }
    public final void clearEncodedCards() {
        encodedCards = view.clearCards(encodedCards, TrackableProperty.EncodedCards);
    }

    public final Card getEncodingCard() {
        return encoding;
    }

    public final void setEncodingCard(final Card e) {
        encoding = e;
    }

    public final String getFlipResult(final Player flipper) {
        if (flipResult == null) {
            return null;
        }
        return flipResult.get(flipper);
    }
    public final void addFlipResult(final Player flipper, final String result) {
        if (flipResult == null) {
            flipResult = Maps.newTreeMap();
        }
        flipResult.put(flipper, result);
    }
    public final void clearFlipResult() {
        flipResult = null;
    }

    public final FCollectionView<Trigger> getTriggers() {
        return currentState.getTriggers();
    }
    public final Trigger addTrigger(final Trigger t) {
        currentState.addTrigger(t);
        return t;
    }
    @Deprecated
    public final void removeTrigger(final Trigger t) {
        currentState.removeTrigger(t);
    }
    @Deprecated
    public final void removeTrigger(final Trigger t, final CardStateName state) {
        getState(state).removeTrigger(t);
    }
    public final void clearTriggersNew() {
        currentState.clearTriggers();
    }

    public final boolean hasTrigger(final Trigger t) {
       return currentState.hasTrigger(t);
    }

    public final boolean hasTrigger(final int id) {
        return currentState.hasTrigger(id);
    }

    public void updateTriggers(List<Trigger> list, CardState state) {
        if (hasRemoveIntrinsic()) {
            list.clear();
        }

        for (final CardTraitChanges ck : changedCardTraits.values()) {
            if (ck.isRemoveAll()) {
                list.clear();
            }
            list.addAll(ck.getTriggers());
        }

        for (KeywordInterface kw : getUnhiddenKeywords(state)) {
            list.addAll(kw.getTriggers());
        }
    }

    public final int getSunburstValue() {
        return sunburstValue;
    }
    public final void setSunburstValue(final int valueIn) {
        sunburstValue = valueIn;
    }

    public final byte getColorsPaid() {
        return colorsPaid;
    }
    public final void setColorsPaid(final byte s) {
        colorsPaid |= s;
    }

    public final int getXManaCostPaid() {
        if (getCastSA() != null) {
            Integer paid = getCastSA().getXManaCostPaid();
            return paid == null ? 0 : paid;
        }
        return 0;
    }

    public final Map<String, Integer> getXManaCostPaidByColor() {
        return xManaCostPaidByColor;
    }
    public final void setXManaCostPaidByColor(final Map<String, Integer> xByColor) {
        xManaCostPaidByColor = xByColor;
    }

    public final int getXManaCostPaidCount(final String colors) {
        int count = 0;
        if (xManaCostPaidByColor != null) {
            for (Entry<String, Integer> m : xManaCostPaidByColor.entrySet()) {
                if (colors.contains(m.getKey())) {
                    count += m.getValue();
                }
            }
        }
        return count;
    }

    public CardCollectionView getBlockedThisTurn() {
        return CardCollection.getView(blockedThisTurn);
    }
    public void addBlockedThisTurn(Card attacker) {
        if (blockedThisTurn == null) {
            blockedThisTurn = new CardCollection();
        }
        blockedThisTurn.add(attacker);
    }
    public void clearBlockedThisTurn() {
        blockedThisTurn = null;
    }

    public CardCollectionView getBlockedByThisTurn() {
        return CardCollection.getView(blockedByThisTurn);
    }
    public void addBlockedByThisTurn(Card blocker) {
        if (blockedByThisTurn == null) {
            blockedByThisTurn = new CardCollection();
        }
        blockedByThisTurn.add(blocker);
    }
    public void clearBlockedByThisTurn() {
        blockedByThisTurn = null;
    }

    //MustBlockCards are cards that this Card must block if able in an upcoming combat.
    //This is cleared at the end of each turn.
    public final CardCollectionView getMustBlockCards() {
        return CardCollection.getView(mustBlockCards);
    }
    public final void addMustBlockCard(final Card c) {
        mustBlockCards = view.addCard(mustBlockCards, c, TrackableProperty.MustBlockCards);
    }
    public final void addMustBlockCards(final Iterable<Card> attackersToBlock) {
        mustBlockCards = view.addCards(mustBlockCards, attackersToBlock, TrackableProperty.MustBlockCards);
    }
    public final void clearMustBlockCards() {
        mustBlockCards = view.clearCards(mustBlockCards, TrackableProperty.MustBlockCards);
    }

    public final void setMustAttackEntity(final GameEntity e) {
        mustAttackEntity = e;
    }
    public final GameEntity getMustAttackEntity() {
        return mustAttackEntity;
    }
    public final void clearMustAttackEntity(final Player playerturn) {
        if (getController().equals(playerturn)) {
            mustAttackEntity = null;
        }
        mustAttackEntityThisTurn = null;
    }
    public final GameEntity getMustAttackEntityThisTurn() { return mustAttackEntityThisTurn; }
    public final void setMustAttackEntityThisTurn(GameEntity entThisTurn) { mustAttackEntityThisTurn = entThisTurn; }

    public final Card getCloneOrigin() {
        return cloneOrigin;
    }
    public final void setCloneOrigin(final Card cloneOrigin0) {
        cloneOrigin = view.setCard(cloneOrigin, cloneOrigin0, TrackableProperty.CloneOrigin);
    }

    public final boolean hasFirstStrike() {
        return hasKeyword(Keyword.FIRST_STRIKE);
    }

    public final boolean hasDoubleStrike() {
        return hasKeyword(Keyword.DOUBLE_STRIKE);
    }

    public final boolean hasSecondStrike() {
        return hasDoubleStrike() || !hasFirstStrike();
    }

    public final boolean hasConverge() {
        return "Count$Converge".equals(getSVar("X")) || "Count$Converge".equals(getSVar("Y")) ||
            hasKeyword(Keyword.SUNBURST) || hasKeyword("Modular:Sunburst");
    }

    @Override
    public final boolean canReceiveCounters(final CounterType type) {

        // CantPutCounter static abilities
        for (final Card ca : getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.applyAbility("CantPutCounter", this, type)) {
                    return false;
                }
            }
        }

        if (type == CounterType.DREAM) {
            // need to be done extra because it is also a state based action
            return !hasKeyword("CARDNAME can't have more than seven dream counters on it.") || getCounters(CounterType.DREAM) <= 6;
        }
        return true;
    }

    public final int getTotalCountersToAdd() {
        return countersAdded;
    }

    public final void setTotalCountersToAdd(int value) {
        countersAdded = value;
    }

    public final int addCounter(final CounterType counterType, final int n, final Player source, final boolean applyMultiplier, GameEntityCounterTable table) {
        return addCounter(counterType, n, source, applyMultiplier, true, table);
    }
    public final int addCounterFireNoEvents(final CounterType counterType, final int n, final Player source, final boolean applyMultiplier, GameEntityCounterTable table) {
        return addCounter(counterType, n, source, applyMultiplier, false, table);
    }

    @Override
    public int addCounter(final CounterType counterType, final int n, final Player source, final boolean applyMultiplier, final boolean fireEvents, GameEntityCounterTable table) {
        int addAmount = n;
        if(addAmount <= 0) {
            addAmount = 0; // As per rule 107.1b
            return 0;
        }
        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(this);
        repParams.put(AbilityKey.Source, source);
        repParams.put(AbilityKey.CounterType, counterType);
        repParams.put(AbilityKey.CounterNum, addAmount);
        repParams.put(AbilityKey.EffectOnly, applyMultiplier);

        switch (getGame().getReplacementHandler().run(ReplacementType.AddCounter, repParams)) {
        case NotReplaced:
            break;
        case Updated: {
            addAmount = (int) repParams.get(AbilityKey.CounterNum);
            break;
        }
        default:
            return 0;
        }

        if (canReceiveCounters(counterType)) {
            if (counterType == CounterType.DREAM && hasKeyword("CARDNAME can't have more than seven dream counters on it.")) {
                addAmount = Math.min(7 - getCounters(CounterType.DREAM), addAmount);
            }
        }
        else {
            addAmount = 0;
        }

        if (addAmount <= 0) {
            return 0;
        }
        setTotalCountersToAdd(addAmount);

        final Integer oldValue = getCounters(counterType);
        final Integer newValue = addAmount + (oldValue == null ? 0 : oldValue);
        if (fireEvents) {
            // Not sure why firing events wraps EVERYTHING ins
            if (!newValue.equals(oldValue)) {
                final int powerBonusBefore = getPowerBonusFromCounters();
                final int toughnessBonusBefore = getToughnessBonusFromCounters();
                final int loyaltyBefore = getCurrentLoyalty();

                setCounters(counterType, newValue);
                getController().addCounterToPermThisTurn(counterType, addAmount);
                view.updateCounters(this);

                //fire card stats changed event if p/t bonuses or loyalty changed from added counters
                if (powerBonusBefore != getPowerBonusFromCounters() || toughnessBonusBefore != getToughnessBonusFromCounters() || loyaltyBefore != getCurrentLoyalty()) {
                    getGame().fireEvent(new GameEventCardStatsChanged(this));
                }

                // play the Add Counter sound
                getGame().fireEvent(new GameEventCardCounters(this, counterType, oldValue == null ? 0 : oldValue, newValue));
            }

            // Run triggers
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
            runParams.put(AbilityKey.Source, source);
            runParams.put(AbilityKey.CounterType, counterType);
            for (int i = 0; i < addAmount; i++) {
                runParams.put(AbilityKey.CounterAmount, oldValue + i + 1);
                getGame().getTriggerHandler().runTrigger(
                        TriggerType.CounterAdded, AbilityKey.newMap(runParams), false);
            }
            if (addAmount > 0) {
                runParams.put(AbilityKey.CounterAmount, addAmount);
                getGame().getTriggerHandler().runTrigger(
                        TriggerType.CounterAddedOnce, AbilityKey.newMap(runParams), false);
            }
        } else {
            setCounters(counterType, newValue);
            getController().addCounterToPermThisTurn(counterType, addAmount);
            view.updateCounters(this);
        }
        if (newValue <= 0) {
            removeCounterTimestamp(counterType);
        } else {
            addCounterTimestamp(counterType);
        }
        if (table != null) {
            table.put(this, counterType, addAmount);
        }
        return addAmount;
    }

    public boolean addCounterTimestamp(CounterType counterType) {
        return addCounterTimestamp(counterType, true);
    }
    public boolean addCounterTimestamp(CounterType counterType, boolean updateView) {
        if (!counterType.isKeywordCounter()) {
            return false;
        }
        removeCounterTimestamp(counterType);

        long timestamp = game.getNextTimestamp();
        counterTypeTimestamps.put(counterType, timestamp);
        addChangedCardKeywords(ImmutableList.of(counterType.getKeyword().toString()), null, false, false, timestamp, updateView);
        return true;
    }

    public boolean removeCounterTimestamp(CounterType counterType) {
        return removeCounterTimestamp(counterType, true);
    }

    public boolean removeCounterTimestamp(CounterType counterType, boolean updateView) {
        Long old = counterTypeTimestamps.remove(counterType);
        if (old != null) {
            removeChangedCardKeywords(old, updateView);
        }
        return old != null;
    }

    /**
     * <p>
     * addCountersAddedBy.
     * </p>
     * @param source - the card adding the counters to this card
     * @param counterType - the counter type added
     * @param counterAmount - the amount of counters added
     */
    public final void addCountersAddedBy(final Card source, final CounterType counterType, final int counterAmount) {
        final Map<CounterType, Integer> counterMap = Maps.newTreeMap();
        counterMap.put(counterType, counterAmount);
        countersAddedBy.put(source, counterMap);
    }

    /**
     * <p>
     * getCountersAddedBy.
     * </p>
     * @param source - the card the counters were added by
     * @param counterType - the counter type added
     * @return the amount of counters added.
     */
    public final int getCountersAddedBy(final Card source, final CounterType counterType) {
        int counterAmount = 0;
        if (countersAddedBy.containsKey(source)) {
            final Map<CounterType, Integer> counterMap = countersAddedBy.get(source);
            counterAmount = counterMap.containsKey(counterType) ? counterMap.get(counterType) : 0;
            countersAddedBy.remove(source);
        }
        return counterAmount;
    }

    @Override
    public final void subtractCounter(final CounterType counterName, final int n) {
        int oldValue = getCounters(counterName);
        int newValue = Math.max(oldValue - n, 0);

        final int delta = oldValue - newValue;
        if (delta == 0) { return; }

        int powerBonusBefore = getPowerBonusFromCounters();
        int toughnessBonusBefore = getToughnessBonusFromCounters();
        int loyaltyBefore = getCurrentLoyalty();

        setCounters(counterName, newValue);
        view.updateCounters(this);

        if (newValue <= 0) {
            this.removeCounterTimestamp(counterName);
        }

        //fire card stats changed event if p/t bonuses or loyalty changed from subtracted counters
        if (powerBonusBefore != getPowerBonusFromCounters() || toughnessBonusBefore != getToughnessBonusFromCounters() || loyaltyBefore != getCurrentLoyalty()) {
            getGame().fireEvent(new GameEventCardStatsChanged(this));
        }

        // Play the Subtract Counter sound
        getGame().fireEvent(new GameEventCardCounters(this, counterName, oldValue, newValue));

        // Run triggers
        int curCounters = oldValue;
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
        runParams.put(AbilityKey.CounterType, counterName);
        for (int i = 0; i < delta && curCounters != 0; i++) {
            runParams.put(AbilityKey.NewCounterAmount, --curCounters);
            getGame().getTriggerHandler().runTrigger(TriggerType.CounterRemoved, AbilityKey.newMap(runParams), false);
        }
        runParams.put(AbilityKey.CounterAmount, delta);
        getGame().getTriggerHandler().runTrigger(TriggerType.CounterRemovedOnce, AbilityKey.newMap(runParams), false);
    }

    @Override
    public final void setCounters(final Map<CounterType, Integer> allCounters) {
        boolean changed = false;
        for (CounterType ct : counters.keySet()) {
            if (removeCounterTimestamp(ct, false)) {
                changed = true;
            }
        }
        counters = allCounters;
        view.updateCounters(this);

        for (CounterType ct : counters.keySet()) {
            if (addCounterTimestamp(ct, false)) {
                changed = true;
            }
        }
        if (changed) {
            updateKeywords();
        }
    }

    @Override
    public final void clearCounters() {
        if (counters.isEmpty()) { return; }
        counters.clear();
        view.updateCounters(this);

        boolean changed = false;
        for (CounterType ct : counterTypeTimestamps.keySet()) {
            if (removeCounterTimestamp(ct, false)) {
                changed = true;
            }
        }
        if (changed) {
            updateKeywords();
        }
    }

    public final String getSVar(final String var) {
        return currentState.getSVar(var);
    }

    public final boolean hasSVar(final String var) {
        return currentState.hasSVar(var);
    }

    public final void setSVar(final String var, final String str) {
        currentState.setSVar(var, str);
    }

    public final Map<String, String> getSVars() {
        return currentState.getSVars();
    }

    public final void setSVars(final Map<String, String> newSVars) {
        currentState.setSVars(newSVars);
    }

    public final void removeSVar(final String var) {
        currentState.removeSVar(var);
    }

    public final int sumAllCounters() {
        int count = 0;
        for (final Integer value2 : counters.values()) {
            count += value2;
        }
        return count;
    }

    public final int getTurnInZone() {
        return turnInZone;
    }

    public final void setTurnInZone(final int turn) {
        turnInZone = turn;
    }

    public final void setManaCost(final ManaCost s) {
        currentState.setManaCost(s);
    }

    public final ManaCost getManaCost() {
        return currentState.getManaCost();
    }

    public final boolean hasChosenPlayer() {
        return chosenPlayer != null;
    }
    public final Player getChosenPlayer() {
        return chosenPlayer;
    }
    public final void setChosenPlayer(final Player p) {
        if (chosenPlayer == p) { return; }
        chosenPlayer = p;
        view.updateChosenPlayer(this);
    }

    public final boolean hasChosenNumber() {
        return chosenNumber != null;
    }

    public final Integer getChosenNumber() {
        return chosenNumber;
    }
    public final void setChosenNumber(final int i) {
        chosenNumber = i;
    }

    public final Card getExiledWith() {
        return exiledWith;
    }
    public final void setExiledWith(final Card e) {
        exiledWith = e;
    }

    // used for cards like Belbe's Portal, Conspiracy, Cover of Darkness, etc.
    public final String getChosenType() {
        return chosenType;
    }

    public final void setChosenType(final String s) {
        chosenType = s;
        view.updateChosenType(this);
    }

    public final boolean hasChosenType() {
        return chosenType != null && !chosenType.isEmpty();
    }

    public final String getChosenColor() {
        if (hasChosenColor()) {
            return chosenColors.get(0);
        }
        return "";
    }
    public final Iterable<String> getChosenColors() {
        if (chosenColors == null) {
            return Lists.newArrayList();
        }
        return chosenColors;
    }
    public final void setChosenColors(final List<String> s) {
        chosenColors = s;
        view.updateChosenColors(this);
    }
    public boolean hasChosenColor() {
        return chosenColors != null && !chosenColors.isEmpty();
    }
    public boolean hasChosenColor(String s) {
        return chosenColors != null && chosenColors.contains(s);
    }

    public final Card getChosenCard() {
        return getChosenCards().getFirst();
    }
    public final CardCollectionView getChosenCards() {
        return CardCollection.getView(chosenCards);
    }
    public final void setChosenCards(final CardCollection cards) {
        chosenCards = view.setCards(chosenCards, cards, TrackableProperty.ChosenCards);
    }
    public boolean hasChosenCard() {
        return FCollection.hasElements(chosenCards);
    }
    public boolean hasChosenCard(Card c) {
        return FCollection.hasElement(chosenCards, c);
    }

    public Direction getChosenDirection() {
        return chosenDirection;
    }
    public void setChosenDirection(Direction chosenDirection0) {
        if (chosenDirection == chosenDirection0) { return; }
        chosenDirection = chosenDirection0;
        view.updateChosenDirection(this);
    }

    public String getChosenMode() {
        return chosenMode;
    }
    public void setChosenMode(String mode) {
        chosenMode = mode;
        view.updateChosenMode(this);
    }

    public boolean hasChosenName() {
        return chosenName != null;
    }

    public String getChosenName() {
        return chosenName;
    }
    public final void setChosenName(final String s) {
        chosenName = s;
        view.updateNamedCard(this);
    }

    public boolean hasChosenEvenOdd() {
        return chosenEvenOdd != null;
    }

    public EvenOdd getChosenEvenOdd() {
        return chosenEvenOdd;
    }
    public void setChosenEvenOdd(EvenOdd chosenEvenOdd0) {
        if (chosenEvenOdd == chosenEvenOdd0) { return; }
        chosenEvenOdd = chosenEvenOdd0;
        view.updateChosenEvenOdd(this);
    }

    // used for cards like Meddling Mage...
    public final String getNamedCard() {
        return getChosenName();
    }
    public final void setNamedCard(final String s) {
        setChosenName(s);
    }

    public final boolean getDrawnThisTurn() {
        return drawnThisTurn;
    }
    public final void setDrawnThisTurn(final boolean b) {
        drawnThisTurn = b;
    }

    public final CardCollectionView getGainControlTargets() { //used primarily with AbilityFactory_GainControl
        return CardCollection.getView(gainControlTargets);
    }
    public final void addGainControlTarget(final Card c) {
        gainControlTargets = view.addCard(gainControlTargets, c, TrackableProperty.GainControlTargets);
    }
    public final void removeGainControlTargets(final Card c) {
        gainControlTargets = view.removeCard(gainControlTargets, c, TrackableProperty.GainControlTargets);
    }
    public final boolean hasGainControlTarget() {
        return FCollection.hasElements(gainControlTargets);
    }
    public final boolean hasGainControlTarget(Card c) {
        return FCollection.hasElement(gainControlTargets, c);
    }

    public final String getSpellText() {
        return text;
    }

    public final void setText(final String t) {
        originalText = t;
        text = originalText;
    }

    // get the text that does not belong to a cards abilities (and is not really
    // there rules-wise)
    public final String getNonAbilityText() {
        return keywordsToText(getHiddenExtrinsicKeywords());
    }

    // convert a keyword list to the String that should be displayed in game
    public final String keywordsToText(final Collection<KeywordInterface> keywords) {
        final StringBuilder sb = new StringBuilder();
        final StringBuilder sbLong = new StringBuilder();

        // Prepare text changes
        final Set<Entry<String, String>> textChanges = Sets.union(
                changedTextColors.toMap().entrySet(), changedTextTypes.toMap().entrySet());

        int i = 0;
        for (KeywordInterface inst : keywords) {
            String keyword = inst.getOriginal();
            try {
                if (keyword.startsWith("SpellCantTarget")) {
                    continue;
                }
                // format text changes
                if (CardUtil.isKeywordModifiable(keyword)
                        && keywordsGrantedByTextChanges.contains(inst)) {
                    for (final Entry<String, String> e : textChanges) {
                        final String value = e.getValue();
                        if (keyword.contains(value)) {
                            keyword = TextUtil.fastReplace(keyword, value,
                                    TextUtil.concatNoSpace("<strike>", e.getKey(), "</strike> ", value));
                            // assume (for now) max one change per keyword
                            break;
                        }
                    }
                }
                if (keyword.startsWith("CantBeCounteredBy") || keyword.startsWith("Panharmonicon")
                        || keyword.startsWith("Dieharmonicon")) {
                    final String[] p = keyword.split(":");
                    sbLong.append(p[2]).append("\r\n");
                } else if (keyword.startsWith("etbCounter")) {
                    final String[] p = keyword.split(":");
                    final StringBuilder s = new StringBuilder();
                    if (p.length > 4) {
                        if (!"no desc".equals(p[4])) {
                            s.append(p[4]);
                        }
                    } else {
                        s.append(getName());
                        s.append(" enters the battlefield with ");
                        s.append(Lang.nounWithNumeral(p[2], CounterType.valueOf(p[1]).getName() + " counter"));
                        s.append(" on it.");
                    }
                    sbLong.append(s).append("\r\n");
                } else if (keyword.startsWith("Protection:")) {
                    final String[] k = keyword.split(":");
                    sbLong.append(k[2]).append("\r\n");
                } else if (keyword.startsWith("Creatures can't attack unless their controller pays")) {
                    final String[] k = keyword.split(":");
                    if (!k[3].equals("no text")) {
                        sbLong.append(k[3]).append("\r\n");
                    }
                } else if (keyword.startsWith("Enchant")) {
                    String k = keyword;
                    k = TextUtil.fastReplace(k, "Curse", "");
                    sbLong.append(k).append("\r\n");
                } else if (keyword.startsWith("Ripple")) {
                    sbLong.append(TextUtil.fastReplace(keyword, ":", " ")).append("\r\n");
                } else if (keyword.startsWith("Madness")) {
                    String[] parts = keyword.split(":");
                    // If no colon exists in Madness keyword, it must have been granted and assumed the cost from host
                    if (parts.length < 2) {
                        sbLong.append(parts[0]).append(" ").append(this.getManaCost()).append("\r\n");
                    } else {
                        sbLong.append(parts[0]).append(" ").append(ManaCostParser.parse(parts[1])).append("\r\n");
                    }
                } else if (keyword.startsWith("Morph") || keyword.startsWith("Megamorph") || keyword.startsWith("Escape")) {
                    String[] k = keyword.split(":");
                    sbLong.append(k[0]);
                    if (k.length > 1) {
                        final Cost mCost = new Cost(k[1], true);
                        if (!mCost.isOnlyManaCost()) {
                            sbLong.append("—");
                        }
                        if (mCost.isOnlyManaCost()) {
                            sbLong.append(" ");
                        }
                        sbLong.append(mCost.toString()).delete(sbLong.length() - 2, sbLong.length());
                        if (!mCost.isOnlyManaCost()) {
                            sbLong.append(".");
                        }
                        sbLong.append(" (").append(inst.getReminderText()).append(")");
                        sbLong.append("\r\n");
                    }
                } else if (keyword.startsWith("Emerge")) {
                    final String[] k = keyword.split(":");
                    sbLong.append(k[0]).append(" ").append(ManaCostParser.parse(k[1]));
                    sbLong.append(" (").append(inst.getReminderText()).append(")");
                    sbLong.append("\r\n");
                } else if (keyword.startsWith("Echo")) {
                    sbLong.append("Echo ");
                    final String[] upkeepCostParams = keyword.split(":");
                    sbLong.append(upkeepCostParams.length > 2 ? "- " + upkeepCostParams[2] : ManaCostParser.parse(upkeepCostParams[1]));
                    sbLong.append(" (At the beginning of your upkeep, if CARDNAME came under your control since the beginning of your last upkeep, sacrifice it unless you pay its echo cost.)");
                    sbLong.append("\r\n");
                } else if (keyword.startsWith("Cumulative upkeep")) {
                    sbLong.append("Cumulative upkeep ");
                    final String[] upkeepCostParams = keyword.split(":");
                    sbLong.append(upkeepCostParams.length > 2 ? "- " + upkeepCostParams[2] : ManaCostParser.parse(upkeepCostParams[1]));
                    sbLong.append("\r\n");
                } else if (keyword.startsWith("Alternative Cost")) {
                    sbLong.append("Has alternative cost.");
                } else if (keyword.startsWith("AlternateAdditionalCost")) {
                    final String costString1 = keyword.split(":")[1];
                    final String costString2 = keyword.split(":")[2];
                    final Cost cost1 = new Cost(costString1, false);
                    final Cost cost2 = new Cost(costString2, false);
                    sbLong.append("As an additional cost to cast ")
                            .append(getName()).append(", ")
                            .append(cost1.toSimpleString())
                            .append(" or pay ")
                            .append(cost2.toSimpleString())
                            .append(".\r\n");
                } else if (keyword.startsWith("Multikicker")) {
                    if (!keyword.endsWith("Generic")) {
                        final String[] n = keyword.split(":");
                        final Cost cost = new Cost(n[1], false);
                        sbLong.append("Multikicker ").append(cost.toSimpleString());
                        sbLong.append(" (").append(inst.getReminderText()).append(")").append("\r\n");
                    }
                } else if (keyword.startsWith("Kicker")) {
                    if (!keyword.endsWith("Generic")) {
                        final StringBuilder sbx = new StringBuilder();
                        final String[] n = keyword.split(":");
                        sbx.append("Kicker ");
                        final Cost cost = new Cost(n[1], false);
                        sbx.append(cost.toSimpleString());
                        if (Lists.newArrayList(n).size() > 2) {
                            sbx.append(" and/or ");
                            final Cost cost2 = new Cost(n[2], false);
                            sbx.append(cost2.toSimpleString());
                        }
                        sbx.append(" (").append(inst.getReminderText()).append(")");
                        sbLong.append(sbx).append("\r\n");
                    }
                } else if (keyword.startsWith("Hexproof:")) {
                    final String[] k = keyword.split(":");
                    sbLong.append("Hexproof from ").append(k[2])
                        .append(" (").append(inst.getReminderText()).append(")").append("\r\n");
                } else if (keyword.endsWith(".") && !keyword.startsWith("Haunt")) {
                    sbLong.append(keyword).append("\r\n");
                } else if (keyword.startsWith("Presence") || keyword.startsWith("MayFlash")) {
                    // Pseudo keywords, only print Reminder
                    sbLong.append(inst.getReminderText());
                } else if (keyword.contains("At the beginning of your upkeep, ")
                        && keyword.contains(" unless you pay")) {
                    sbLong.append(keyword).append("\r\n");
                } else if (keyword.startsWith("Strive") || keyword.startsWith("Escalate")
                        || keyword.startsWith("ETBReplacement")
                        || keyword.startsWith("CantBeBlockedBy ")
                        || keyword.startsWith("Affinity")
                        || keyword.equals("CARDNAME enters the battlefield tapped.")
                        || keyword.startsWith("UpkeepCost")) {
                } else if (keyword.equals("Provoke") || keyword.equals("Ingest") || keyword.equals("Unleash")
                        || keyword.equals("Soulbond") || keyword.equals("Partner") || keyword.equals("Retrace")
                        || keyword.equals("Living Weapon") || keyword.equals("Myriad") || keyword.equals("Exploit")
                        || keyword.equals("Changeling") || keyword.equals("Delve")
                        || keyword.equals("Split second") || keyword.equals("Sunburst")
                        || keyword.equals("Suspend") // for the ones without amounnt
                        || keyword.equals("Hideaway") || keyword.equals("Ascend")
                        || keyword.equals("Totem armor") || keyword.equals("Battle cry")
                        || keyword.equals("Devoid") || keyword.equals("Riot")){
                    sbLong.append(keyword).append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.startsWith("Partner:")) {
                    final String[] k = keyword.split(":");
                    sbLong.append("Partner with ").append(k[1]).append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.startsWith("Modular") || keyword.startsWith("Bloodthirst") || keyword.startsWith("Dredge")
                        || keyword.startsWith("Fabricate") || keyword.startsWith("Soulshift") || keyword.startsWith("Bushido")
                        || keyword.startsWith("Crew") || keyword.startsWith("Tribute") || keyword.startsWith("Absorb")
                        || keyword.startsWith("Graft") || keyword.startsWith("Fading") || keyword.startsWith("Vanishing")
                        || keyword.startsWith("Afterlife")
                        || keyword.startsWith("Afflict") || keyword.startsWith ("Poisonous") || keyword.startsWith("Rampage")
                        || keyword.startsWith("Renown") || keyword.startsWith("Annihilator") || keyword.startsWith("Devour")) {
                    final String[] k = keyword.split(":");
                    sbLong.append(k[0]).append(" ").append(k[1]).append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.contains("Haunt")) {
                    sb.append("\r\nHaunt (");
                    if (isCreature()) {
                        sb.append("When this creature dies, exile it haunting target creature.");
                    } else {
                        sb.append("When this spell card is put into a graveyard after resolving, ");
                        sb.append("exile it haunting target creature.");
                    }
                    sb.append(")");
                } else if (keyword.equals("Convoke") || keyword.equals("Dethrone")|| keyword.equals("Fear")
                         || keyword.equals("Melee") || keyword.equals("Improvise")|| keyword.equals("Shroud")
                         || keyword.equals("Banding") || keyword.equals("Intimidate")|| keyword.equals("Evolve")
                         || keyword.equals("Exalted") || keyword.equals("Extort")|| keyword.equals("Flanking")
                         || keyword.equals("Horsemanship") || keyword.equals("Infect")|| keyword.equals("Persist")
                         || keyword.equals("Phasing") || keyword.equals("Shadow")|| keyword.equals("Skulk")
                         || keyword.equals("Undying") || keyword.equals("Wither") || keyword.equals("Cascade")
                         || keyword.equals("Mentor")) {
                    if (sb.length() != 0) {
                        sb.append("\r\n");
                    }
                    sb.append(keyword).append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.endsWith(" offering")) {
                    String offeringType = keyword.split(" ")[0];
                    if (sb.length() != 0) {
                        sb.append("\r\n");
                    }
                    sbLong.append(keyword);
                    sbLong.append(" (").append(Keyword.getInstance("Offering:" + offeringType).getReminderText()).append(")");
                } else if (keyword.startsWith("Equip") || keyword.startsWith("Fortify") || keyword.startsWith("Outlast")
                        || keyword.startsWith("Unearth") || keyword.startsWith("Scavenge") || keyword.startsWith("Spectacle")
                        || keyword.startsWith("Evoke") || keyword.startsWith("Bestow") || keyword.startsWith("Dash")
                        || keyword.startsWith("Surge") || keyword.startsWith("Transmute") || keyword.startsWith("Suspend")
                        || keyword.equals("Undaunted") || keyword.startsWith("Monstrosity") || keyword.startsWith("Embalm")
                        || keyword.startsWith("Level up") || keyword.equals("Prowess") || keyword.startsWith("Eternalize")
                        || keyword.startsWith("Reinforce") || keyword.startsWith("Champion") || keyword.startsWith("Prowl")
                        || keyword.startsWith("Amplify") || keyword.startsWith("Ninjutsu") || keyword.startsWith("Adapt")
                        || keyword.startsWith("Transfigure") || keyword.startsWith("Aura swap")
                        || keyword.startsWith("Cycling") || keyword.startsWith("TypeCycling")) {
                    // keyword parsing takes care of adding a proper description
                } else if (keyword.startsWith("CantBeBlockedByAmount")) {
                    sbLong.append(getName()).append(" can't be blocked ");
                    sbLong.append(getTextForKwCantBeBlockedByAmount(keyword));
                } else if (keyword.startsWith("CantBlock")) {
                    sbLong.append(getName()).append(" can't block ");
                    if (keyword.contains("CardUID")) {
                        sbLong.append("CardID (").append(Integer.valueOf(keyword.split("CantBlockCardUID_")[1])).append(")");
                    } else {
                        final String[] k = keyword.split(":");
                        sbLong.append(k.length > 1 ? k[1] + ".\r\n" : "");
                    }
                } else if (keyword.equals("Unblockable")) {
                    sbLong.append(getName()).append(" can't be blocked.\r\n");
                } else if (keyword.equals("AllNonLegendaryCreatureNames")) {
                    sbLong.append(getName()).append(" has all names of nonlegendary creature cards.\r\n");
                } else if (keyword.startsWith("IfReach")) {
                    String[] k = keyword.split(":");
                    sbLong.append(getName()).append(" can block ")
                    .append(CardType.getPluralType(k[1]))
                    .append(" as though it had reach.\r\n");
                } else if (keyword.startsWith("MayEffectFromOpening")) {
                    final String[] k = keyword.split(":");
                    // need to get SpellDescription from Svar
                    String desc = AbilityFactory.getMapParams(getSVar(k[1])).get("SpellDescription");
                    sbLong.append(desc);
                } else if (keyword.startsWith("Saga")) {
                    String[] k = keyword.split(":");
                    String desc = "(As this Saga enters and after your draw step, "
                        + " add a lore counter. Sacrifice after " + Strings.repeat("I", Integer.valueOf(k[1])) + ".)";
                    sbLong.append(desc);
                } else if (inst.getKeyword().equals(Keyword.COMPANION)) {
                    sbLong.append("Companion - ");
                    sbLong.append(((Companion)inst).getDescription());
                }
                else {
                    if ((i != 0) && (sb.length() != 0)) {
                        sb.append(", ");
                    }
                    sb.append(keyword);
                }
                if (sbLong.length() > 0) {
                    sbLong.append("\r\n");
                }

                i++;
            } catch (Exception e) {
                String msg = "Card:keywordToText: crash in Keyword parsing";
                Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(msg)
                    .withData("Card", this.getName()).withData("Keyword", keyword).build()
                );

                throw new RuntimeException("Error in Card " + this.getName() + " with Keyword " + keyword, e);
            }
        }
        if (sb.length() > 0) {
            sb.append("\r\n");
            if (sbLong.length() > 0) {
                sb.append("\r\n");
            }
        }
        if (sbLong.length() > 0) {
            sbLong.append("\r\n");
        }
        sb.append(sbLong);
        return sb.toString();
    }

    private static String getTextForKwCantBeBlockedByAmount(final String keyword) {
        final String restriction = keyword.split(" ", 2)[1];
        final boolean isLT = "LT".equals(restriction.substring(0,2));
        final String byClause = isLT ? "except by " : "by more than ";
        final int cnt = Integer.parseInt(restriction.substring(2));
        return byClause + Lang.nounWithNumeral(cnt, isLT ? "or more creature" : "creature");
    }

    // get the text of the abilities of a card
    public String getAbilityText() {
        return getAbilityText(currentState);
    }

    public String getAbilityText(final CardState state) {
        final CardTypeView type = state.getType();

        final StringBuilder sb = new StringBuilder();
        if (!mayPlay.isEmpty()) {
            sb.append("May be played by: ");
            sb.append(Lang.joinHomogenous(mayPlay.values()));
            sb.append("\r\n");
        }

        if (type.isInstant() || type.isSorcery()) {
            sb.append(abilityTextInstantSorcery(state));

            if (haunting != null) {
                sb.append("Haunting: ").append(haunting);
                sb.append("\r\n");
            }

            String result = sb.toString();
            while (result.endsWith("\r\n")) {
                result = result.substring(0, result.length() - 2);
            }
            return TextUtil.fastReplace(result, "CARDNAME", state.getName());
        }

        if (monstrous) {
            sb.append("Monstrous\r\n");
        }
        if (renowned) {
            sb.append("Renowned\r\n");
        }
        if (manifested) {
            sb.append("Manifested\r\n");
        }
        sb.append(keywordsToText(getUnhiddenKeywords(state)));

        // Process replacement effects first so that "enters the battlefield tapped"
        // and "as ~ enters the battlefield, choose...", etc can be printed
        // here. The rest will be printed later.
        StringBuilder replacementEffects = new StringBuilder();
        for (final ReplacementEffect replacementEffect : state.getReplacementEffects()) {
            if (!replacementEffect.isSecondary()) {
                String text = replacementEffect.getDescription();
                if (text.contains("enters the battlefield")) {
                    sb.append(text).append("\r\n");
                } else {
                    replacementEffects.append(text).append("\r\n");
                }
            }
        }

        // add As an additional cost to Permanent spells
        if (state.getFirstAbility() != null && type.isPermanent()) {
            SpellAbility first = state.getFirstAbility();
            if (first.isSpell()) {
                Cost cost = first.getPayCosts();
                if (cost != null && !cost.isOnlyManaCost()) {
                    sb.append(cost.toString());
                    sb.append("\r\n");
                }
            }
        }

        if (this.getRules() != null && state.getView().getState().equals(CardStateName.Original)) {
            // try to look which what card this card can be meld to
            // only show this info if this card does not has the meld Effect itself

            boolean hasMeldEffect = hasSVar("Meld")
                    || Iterables.any(state.getNonManaAbilities(), SpellAbilityPredicates.isApi(ApiType.Meld));
            String meld = this.getRules().getMeldWith();
            if (meld != "" && (!hasMeldEffect)) {
                sb.append("\r\n");
                sb.append("(Melds with ").append(meld).append(".)");
                sb.append("\r\n");
            }
        }

        // Give spellText line breaks for easier reading
        sb.append("\r\n");
        sb.append(text.replaceAll("\\\\r\\\\n", "\r\n"));
        sb.append("\r\n");

        // Triggered abilities
        for (final Trigger trig : state.getTriggers()) {
            if (!trig.isSecondary()) {
                String trigStr = trig.replaceAbilityText(trig.toString(), state);
                sb.append(trigStr.replaceAll("\\\\r\\\\n", "\r\n")).append("\r\n");
            }
        }

        // Replacement effects
        sb.append(replacementEffects);

        // static abilities
        for (final StaticAbility stAb : state.getStaticAbilities()) {
            if (!stAb.isSecondary()) {
                final String stAbD = stAb.toString();
                if (!stAbD.equals("")) {
                    sb.append(stAbD).append("\r\n");
                }
            }
        }

        final List<String> addedManaStrings = Lists.newArrayList();
        boolean primaryCost = true;
        boolean isNonAura = !type.hasSubtype("Aura");

        for (final SpellAbility sa : state.getSpellAbilities()) {
            // This code block is not shared by instants or sorceries. We don't need to check for permanence.
            if (sa == null || sa.isSecondary()) {
                continue;
            }

            // should not print Spelldescription for Morph
            if (sa.isCastFaceDown()) {
                continue;
            }

            boolean isNonAuraPermanent = (sa instanceof SpellPermanent) && isNonAura;
            if (isNonAuraPermanent && primaryCost) {
                // For Alt costs, make sure to display the cost!
                primaryCost = false;
                continue;
            }

            String sAbility = formatSpellAbility(sa);

            // add Adventure to AbilityText
            if (sa.isAdventure() && state.getView().getState().equals(CardStateName.Original)) {
                StringBuilder sbSA = new StringBuilder();
                sbSA.append("Adventure — ").append(getState(CardStateName.Adventure).getName());
                sbSA.append(" ").append(sa.getPayCosts().toSimpleString());
                sbSA.append(": ");
                sbSA.append(sAbility);
                sAbility = sbSA.toString();
            }

            if (sa.getManaPart() != null) {
                if (addedManaStrings.contains(sAbility)) {
                    continue;
                }
                addedManaStrings.add(sAbility);
            }

            if (isNonAuraPermanent) {
                sb.insert(0, "\r\n");
                sb.insert(0, sAbility);
            } else if (!sAbility.endsWith(state.getName() + "\r\n")) {
                sb.append(sAbility);
                sb.append("\r\n");
            }
        }

        // CantBlockBy static abilities
        if (game != null && isCreature() && isInZone(ZoneType.Battlefield)) {
            for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
                if (equals(ca)) {
                    continue;
                }
                for (final StaticAbility stAb : ca.getStaticAbilities()) {
                    if (stAb.isSecondary() ||
                            !stAb.getParam("Mode").equals("CantBlockBy") ||
                            stAb.isSuppressed() || !stAb.checkConditions() ||
                            !stAb.hasParam("ValidAttacker")) {
                        continue;
                    }
                    final Card host = stAb.getHostCard();
                    if (isValid(stAb.getParam("ValidAttacker").split(","), host.getController(), host, null)) {
                        String desc = stAb.toString();
                        desc = TextUtil.fastReplace(desc, "CARDNAME", host.getName());
                        if (host.getEffectSource() != null) {
                            desc = TextUtil.fastReplace(desc, "EFFECTSOURCE", host.getEffectSource().getName());
                        }
                        sb.append(desc);
                        sb.append("\r\n");
                    }
                }
            }
        }

        // NOTE:
        if (sb.toString().contains(" (NOTE: ")) {
            sb.insert(sb.indexOf("(NOTE: "), "\r\n");
        }
        if (sb.toString().contains("(NOTE: ") && sb.toString().contains(".) ")) {
            sb.insert(sb.indexOf(".) ") + 3, "\r\n");
        }

        if (isGoaded()) {
            sb.append("is goaded by: ").append(Lang.joinHomogenous(getGoaded()));
            sb.append("\r\n");
        }

        // replace triple line feeds with double line feeds
        final String s = "\r\n\r\n\r\n";
        int start = sb.lastIndexOf(s);
        while (start != -1) {
            sb.replace(start, start + 4, "\r\n");
            start = sb.lastIndexOf(s);
        }

        String desc = TextUtil.fastReplace(sb.toString(), "CARDNAME", state.getName());
        if (getEffectSource() != null) {
            desc = TextUtil.fastReplace(desc, "EFFECTSOURCE", getEffectSource().getName());
        }

        return desc.trim();
    }

    private StringBuilder abilityTextInstantSorcery(CardState state) {
        final StringBuilder sb = new StringBuilder();

        // Give spellText line breaks for easier reading
        sb.append(text.replaceAll("\\\\r\\\\n", "\r\n"));

        // NOTE:
        if (sb.toString().contains(" (NOTE: ")) {
            sb.insert(sb.indexOf("(NOTE: "), "\r\n");
        }
        if (sb.toString().contains("(NOTE: ") && sb.toString().endsWith(".)") && !sb.toString().endsWith("\r\n")) {
            sb.append("\r\n");
        }

        final StringBuilder sbSpell = new StringBuilder();

        // I think SpellAbilities should be displayed after Keywords
        // Add SpellAbilities
        for (final SpellAbility element : state.getSpellAbilities()) {
            if (!element.isSecondary()) {
                element.usesTargeting();
                sbSpell.append(formatSpellAbility(element));
            }
        }
        final String strSpell = sbSpell.toString();

        final StringBuilder sbBefore = new StringBuilder();
        final StringBuilder sbAfter = new StringBuilder();

        for (final KeywordInterface inst : getKeywords(state)) {
            final String keyword = inst.getOriginal();

            try {
                if (keyword.equals("Ascend")  || keyword.equals("Changeling")
                        || keyword.equals("Aftermath") || keyword.equals("Wither")
                        || keyword.equals("Convoke") || keyword.equals("Delve")
                        || keyword.equals("Improvise") || keyword.equals("Retrace")
                        || keyword.equals("Undaunted") || keyword.equals("Cascade")
                        || keyword.equals("Devoid") ||  keyword.equals("Lifelink")
                        || keyword.equals("Split second")) {
                    sbBefore.append(keyword).append(" (").append(inst.getReminderText()).append(")");
                    sbBefore.append("\r\n");
                } else if(keyword.equals("Conspire") || keyword.equals("Epic")
                        || keyword.equals("Suspend") || keyword.equals("Jump-start")) {
                    sbAfter.append(keyword).append(" (").append(inst.getReminderText()).append(")");
                    sbAfter.append("\r\n");
                } else if (keyword.startsWith("Ripple")) {
                    sbBefore.append(TextUtil.fastReplace(keyword, ":", " ")).append(" (").append(inst.getReminderText()).append(")");
                    sbBefore.append("\r\n");
                } else if (keyword.startsWith("Dredge")) {
                    sbAfter.append(TextUtil.fastReplace(keyword, ":", " ")).append(" (").append(inst.getReminderText()).append(")");
                    sbAfter.append("\r\n");
                } else if (keyword.startsWith("Escalate") || keyword.startsWith("Buyback")
                        || keyword.startsWith("Prowl")) {
                    final String[] k = keyword.split(":");
                    final String manacost = k[1];
                    final Cost cost = new Cost(manacost, false);

                    StringBuilder sbCost = new StringBuilder(k[0]);
                    if (!cost.isOnlyManaCost()) {
                        sbCost.append("—");
                    } else {
                        sbCost.append(" ");
                    }
                    sbCost.append(cost.toSimpleString());
                    sbBefore.append(sbCost).append(" (").append(inst.getReminderText()).append(")");
                    sbBefore.append("\r\n");
                } else if (keyword.startsWith("Multikicker")) {
                    if (!keyword.endsWith("Generic")) {
                        final String[] n = keyword.split(":");
                        final Cost cost = new Cost(n[1], false);
                        sbBefore.append("Multikicker ").append(cost.toSimpleString()).append(" (").append(inst.getReminderText()).append(")").append("\r\n");
                    }
                } else if (keyword.startsWith("Kicker")) {
                    if (!keyword.endsWith("Generic")) {
                        final StringBuilder sbx = new StringBuilder();
                        final String[] n = keyword.split(":");
                        sbx.append("Kicker ");
                        final Cost cost = new Cost(n[1], false);
                        sbx.append(cost.toSimpleString());
                        if (Lists.newArrayList(n).size() > 2) {
                                sbx.append(" and/or ");
                                final Cost cost2 = new Cost(n[2], false);
                            sbx.append(cost2.toSimpleString());
                        }
                        sbx.append(" (").append(inst.getReminderText()).append(")");
                        sbBefore.append(sbx).append("\r\n");
                    }
                }else if (keyword.startsWith("AlternateAdditionalCost")) {
                    final String[] k = keyword.split(":");
                    final Cost cost1 = new Cost(k[1], false);
                    final Cost cost2 = new Cost(k[2], false);
                    sbBefore.append("As an additional cost to cast ")
                            .append(state.getName()).append(", ")
                            .append(cost1.toSimpleString())
                            .append(" or pay ")
                            .append(cost2.toSimpleString())
                            .append(".\r\n");
                } else if (keyword.startsWith("Presence") || keyword.startsWith("MayFlash")) {
                    // Pseudo keywords, only print Reminder
                    sbBefore.append(inst.getReminderText());
                    sbBefore.append("\r\n");
                } else if (keyword.startsWith("Entwine") || keyword.startsWith("Madness")
                        || keyword.startsWith("Miracle") || keyword.startsWith("Recover")
                        || keyword.startsWith("Escape")) {
                    final String[] k = keyword.split(":");
                    final Cost cost = new Cost(k[1], false);

                    StringBuilder sbCost = new StringBuilder(k[0]);
                    if (!cost.isOnlyManaCost()) {
                        sbCost.append("—");
                    } else {
                        sbCost.append(" ");
                    }
                    sbCost.append(cost.toSimpleString());
                    sbAfter.append(sbCost).append(" (").append(inst.getReminderText()).append(")");
                    sbAfter.append("\r\n");
                } else if (keyword.equals("CARDNAME can't be countered.") ||
                        keyword.equals("Remove CARDNAME from your deck before playing if you're not playing for ante.")) {
                    sbBefore.append(keyword);
                    sbBefore.append("\r\n");
                } else if (keyword.startsWith("Haunt")) {
                    sbAfter.append("Haunt (");
                    sbAfter.append("When this spell card is put into a graveyard after resolving, ");
                    sbAfter.append("exile it haunting target creature.");
                    sbAfter.append(")");
                    sbAfter.append("\r\n");
                } else if (keyword.startsWith("Splice")) {
                    final String[] n = keyword.split(":");
                    final Cost cost = new Cost(n[2], false);

                    String desc;

                    if (n.length > 3) {
                        desc = n[3];
                    } else {
                        String[] k = n[1].split(",");
                        for (int i = 0; i < k.length; i++) {
                            if (CardType.isACardType(k[i])) {
                                k[i] = k[i].toLowerCase();
                            }
                        }
                        desc = StringUtils.join(k, " or ");
                    }

                    sbAfter.append("Splice onto ").append(desc).append(" ").append(cost.toSimpleString());
                    sbAfter.append(" (").append(inst.getReminderText()).append(")").append("\r\n");
                } else if (keyword.equals("Storm")) {
                    sbAfter.append("Storm (");

                    sbAfter.append("When you cast this spell, copy it for each spell cast before it this turn.");

                    if (strSpell.contains("Target") || strSpell.contains("target")) {
                        sbAfter.append(" You may choose new targets for the copies.");
                    }

                    sbAfter.append(")");
                    sbAfter.append("\r\n");
                } else if (keyword.startsWith("Replicate")) {
                    final String[] n = keyword.split(":");
                    final Cost cost = new Cost(n[1], false);
                    sbBefore.append("Replicate ").append(cost.toSimpleString());
                    sbBefore.append(" (When you cast this spell, copy it for each time you paid its replicate cost.");
                    if (strSpell.contains("Target") || strSpell.contains("target")) {
                        sbBefore.append(" You may choose new targets for the copies.");
                    }
                    sbBefore.append(")\r\n");
                }
            } catch (Exception e) {
                String msg = "Card:abilityTextInstantSorcery: crash in Keyword parsing";
                Sentry.getContext().recordBreadcrumb(
                    new BreadcrumbBuilder().setMessage(msg)
                    .withData("Card", this.getName()).withData("Keyword", keyword).build()
                );

                throw new RuntimeException("Error in Card " + this.getName() + " with Keyword " + keyword, e);
            }
        }

        sb.append(sbBefore);

        // add Spells there to main StringBuilder
        sb.append(strSpell);

        // Triggered abilities
        for (final Trigger trig : state.getTriggers()) {
            if (!trig.isSecondary()) {
                sb.append(trig.replaceAbilityText(trig.toString(), state)).append("\r\n");
            }
        }

        // Replacement effects
        for (final ReplacementEffect replacementEffect : state.getReplacementEffects()) {
            if (!replacementEffect.isSecondary()) {
                sb.append(replacementEffect.getDescription()).append("\r\n");
            }
        }

        // static abilities
        for (final StaticAbility stAb : state.getStaticAbilities()) {
            if (!stAb.isSecondary()) {
                final String stAbD = stAb.toString();
                if (!stAbD.equals("")) {
                    sb.append(stAbD).append("\r\n");
                }
            }
        }

        sb.append(sbAfter);
        return sb;
    }

    //This is the code of Michael Bowling
    public boolean entersTapped() {
        return (hasKeyword("ETBReplacement:Other:LandTapped") ||
                hasKeyword("CARDNAME enters the battlefield tapped.") ||
                hasKeyword("ETBReplacement:Other:DBTap"));
    }

    private String formatSpellAbility(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final String elementText = sa.toString();

        //Determine if a card has multiple choices, then format it in an easier to read list.
        if (ApiType.Charm.equals(sa.getApi())) {
            sb.append(CharmEffect.makeFormatedDescription(sa));
        } else {
            sb.append(elementText).append("\r\n");
        }
        return sb.toString();
    }

    public final boolean canProduceSameManaTypeWith(final Card c) {
        final FCollectionView<SpellAbility> manaAb = getManaAbilities();
        if (manaAb.isEmpty()) {
            return false;
        }
        Set<String> colors = new HashSet<>();
        for (final SpellAbility ab : c.getManaAbilities()) {
            if (ab.getApi() == ApiType.ManaReflected) {
                colors.addAll(CardUtil.getReflectableManaColors(ab));
            } else {
                colors = CardUtil.canProduce(6, ab.getManaPart(), colors);
            }
        }

        for (final SpellAbility mana : manaAb) {
            for (String s : colors) {
                if (mana.getApi() == ApiType.ManaReflected) {
                    if (CardUtil.getReflectableManaColors(mana).contains(s)) {
                        return true;
                    }
                } else {
                    if (mana.getManaPart().canProduce(MagicColor.toShortString(s))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public final void clearFirstSpell() {
        currentState.clearFirstSpell();
    }

    public final SpellAbility getFirstSpellAbility() {
        return currentState.getNonManaAbilities().isEmpty() ? null : currentState.getNonManaAbilities().getFirst();
    }

    /**
     * @return the first {@link SpellAbility} marked as a Spell with API type
     * {@link ApiType#Attach} in this {@link Card}, or {@code null} if no such
     * object exists.
     * @see SpellAbility#isSpell()
     */
    public final SpellAbility getFirstAttachSpell() {
        for (final SpellAbility sa : getSpells()) {
            if (sa.getApi() == ApiType.Attach && !sa.isSuppressed()) {
                return sa;
            }
        }
        return null;
    }

    public final SpellPermanent getSpellPermanent() {
        for (final SpellAbility sa : currentState.getNonManaAbilities()) {
            if (sa instanceof SpellPermanent) {
                return (SpellPermanent) sa;
            }
        }
        return null;
    }

    public final void addSpellAbility(final SpellAbility a) {
        addSpellAbility(a, true);
    }
    public final void addSpellAbility(final SpellAbility a, final boolean updateView) {
        a.setHostCard(this);
        if (currentState.addSpellAbility(a) && updateView) {
            currentState.getView().updateAbilityText(this, currentState);
        }
    }

    @Deprecated
    public final void removeSpellAbility(final SpellAbility a) {
        removeSpellAbility(a, true);
    }

    @Deprecated
    public final void removeSpellAbility(final SpellAbility a, final boolean updateView) {
        if (currentState.removeSpellAbility(a) && updateView) {
            currentState.getView().updateAbilityText(this, currentState);
        }
    }

    public final FCollectionView<SpellAbility> getSpellAbilities() {
        return currentState.getSpellAbilities();
    }
    public final FCollectionView<SpellAbility> getManaAbilities() {
        return currentState.getManaAbilities();
    }
    public final FCollectionView<SpellAbility> getNonManaAbilities() {
        return currentState.getNonManaAbilities();
    }

    public final boolean hasSpellAbility(final SpellAbility sa) {
        return currentState.hasSpellAbility(sa);
    }

    public final boolean hasSpellAbility(final int id) {
        return currentState.hasSpellAbility(id);
    }

    public boolean hasRemoveIntrinsic() {
        for (final CardTraitChanges ck : changedCardTraits.values()) {
            if (ck.isRemoveIntrinsic()) {
                return true;
            }
        }
        return false;
    }

    public void updateSpellAbilities(List<SpellAbility> list, CardState state, Boolean mana) {
        if (hasRemoveIntrinsic()) {
            list.clear();
        }

        // do Basic Land Abilities there
        if (null == mana || true == mana) {
            updateBasicLandAbilities(list, state);
        }

        for (final CardTraitChanges ck : changedCardTraits.values()) {
            if (ck.isRemoveNonMana()) {
                // List only has nonMana
                if (null == mana) {
                    List<SpellAbility> toRemove = Lists.newArrayList(
                            Iterables.filter(list, Predicates.not(SpellAbilityPredicates.isManaAbility())));
                    list.removeAll(toRemove);
                } else if (false == mana) {
                    list.clear();
                }
            } else if (ck.isRemoveAll()) {
                list.clear();
            }
            list.removeAll(ck.getRemovedAbilities());
            for (SpellAbility sa : ck.getAbilities()) {
                if (mana == null || mana == sa.isManaAbility()) {
                    list.add(sa);
                }
            }
        }

        // add Facedown abilities from Original state but only if this state is face down
        // need CardStateView#getState or might crash in StackOverflow
        if (isInZone(ZoneType.Battlefield)) {
            if ((null == mana || false == mana) && isFaceDown() && state.getView().getState() == CardStateName.FaceDown) {
                for (SpellAbility sa : getState(CardStateName.Original).getNonManaAbilities()) {
                    if (sa.isManifestUp() || sa.isMorphUp()) {
                        list.add(sa);
                    }
                }
            }
        } else {
            // Adenture may only be cast not from Battlefield
            if (isAdventureCard() && state.getView().getState() == CardStateName.Original) {
                for (SpellAbility sa : getState(CardStateName.Adventure).getSpellAbilities()) {
                    if (mana == null || mana == sa.isManaAbility()) {
                        list.add(sa);
                    }
                }
            }
        }

        for (KeywordInterface kw : getUnhiddenKeywords(state)) {
            for (SpellAbility sa : kw.getAbilities()) {
                if (mana == null || mana == sa.isManaAbility()) {
                    list.add(sa);
                }
            }
        }
    }

    private void updateBasicLandAbilities(List<SpellAbility> list, CardState state) {
        final CardTypeView type = state.getTypeWithChanges();

        if (!type.isLand()) {
            // no land, do nothing there
            return;
        }

        for (int i = 0; i < MagicColor.WUBRG.length; i++ ) {
            byte c = MagicColor.WUBRG[i];
            if (type.hasSubtype(MagicColor.Constant.BASIC_LANDS.get(i))) {
                SpellAbility sa = basicLandAbilities[i];

                // no Ability for this type yet, make a new one
                if (sa == null) {
                    sa = CardFactoryUtil.buildBasicLandAbility(state, c);
                    basicLandAbilities[i] = sa;
                }

                list.add(sa);
            }
        }
    }

    public final Iterable<SpellAbility> getIntrinsicSpellAbilities() {
        return currentState.getIntrinsicSpellAbilities();
    }

    public final FCollectionView<SpellAbility> getAllSpellAbilities() {
        final FCollection<SpellAbility> res = new FCollection<>();
        for (final CardStateName key : states.keySet()) {
            res.addAll(getState(key).getNonManaAbilities());
            res.addAll(getState(key).getManaAbilities());
        }
        return res;
    }

    public final FCollectionView<SpellAbility> getSpells() {
        final FCollection<SpellAbility> res = new FCollection<>();
        for (final SpellAbility sa : currentState.getNonManaAbilities()) {
            if (sa.isSpell()) {
                res.add(sa);
            }
        }
        return res;
    }

    public final FCollectionView<SpellAbility> getBasicSpells() {
        return getBasicSpells(currentState);
    }

    public final FCollectionView<SpellAbility> getBasicSpells(CardState state) {
        final FCollection<SpellAbility> res = new FCollection<>();
        for (final SpellAbility sa : state.getNonManaAbilities()) {
            if (sa.isSpell() && sa.isBasicSpell()) {
                res.add(sa);
            }
        }
        return res;
    }

    // shield = regeneration
    public final Iterable<Card> getShields() {
        return shields;
    }
    public final int getShieldCount() {
        return shields.size();
    }

    public final void addShield(final Card shield) {
        if (shields.add(shield)) {
            view.updateShieldCount(this);
        }
    }

    public final void subtractShield(final Card shield) {
        if (shields.remove(shield)) {
            view.updateShieldCount(this);
        }
    }

    public final void resetShield() {
        if (shields.isEmpty()) { return; }
        shields.clear();
        view.updateShieldCount(this);
    }

    public final void addRegeneratedThisTurn() {
        regeneratedThisTurn++;
    }

    public final int getRegeneratedThisTurn() {
        return regeneratedThisTurn;
    }
    public final void setRegeneratedThisTurn(final int n) {
        regeneratedThisTurn = n;
    }

    public final boolean canBeShielded() {
        return !hasKeyword("CARDNAME can't be regenerated.");
    }

    // is this "Card" supposed to be a token?
    public final boolean isToken() {
        return token;
    }
    public final void setToken(boolean token0) {
        if (token == token0) { return; }
        token = token0;
        view.updateToken(this);
    }

    public final Card getCopiedPermanent() {
        return copiedPermanent;
    }
    public final void setCopiedPermanent(final Card c) {
        if (copiedPermanent == c) { return; }
        copiedPermanent = c;
        currentState.getView().updateOracleText(this);
    }

    public final boolean isCopiedSpell() {
        return copiedSpell;
    }
    public final void setCopiedSpell(final boolean b) {
        copiedSpell = b;
    }

    public final boolean isFaceDown() {
        //return currentStateName == CardStateName.FaceDown;
        return facedown;
    }

    public final void setFaceDown(boolean value) {
        facedown = value;
    }

    public final boolean isFlipped() {
        return flipped;
    }

    public final void setFlipped(boolean value) {
        flipped = value;
    }

    public final void setCanCounter(final boolean b) {
        canCounter = b;
    }

    public final boolean getCanCounter() {
        return canCounter;
    }

    public final void addComesIntoPlayCommand(final GameCommand c) {
        etbCommandList.add(c);
    }

    public final void runComesIntoPlayCommands() {
        for (final GameCommand c : etbCommandList) {
            c.run();
        }
        etbCommandList.clear();
    }

    public final void addLeavesPlayCommand(final GameCommand c) {
        leavePlayCommandList.add(c);
    }

    public final void runLeavesPlayCommands() {
        for (final GameCommand c : leavePlayCommandList) {
            c.run();
        }
        leavePlayCommandList.clear();
    }

    public final void addUntapCommand(final GameCommand c) {
        untapCommandList.add(c);
    }

    public final void addUnattachCommand(final GameCommand c) {
        unattachCommandList.add(c);
    }

    public final void addFaceupCommand(final GameCommand c) {
        faceupCommandList.add(c);
    }

    public final void addFacedownCommand(final GameCommand c) {
        facedownCommandList.add(c);
    }

    public final void runUnattachCommands() {
        for (final GameCommand c : unattachCommandList) {
            c.run();
        }
    }

    public final void runFaceupCommands() {
        for (final GameCommand c : faceupCommandList) {
            c.run();
        }
        faceupCommandList.clear();
    }

    public final void runFacedownCommands() {
        for (final GameCommand c : facedownCommandList) {
            c.run();
        }
        facedownCommandList.clear();
    }

    public final void addChangeControllerCommand(final GameCommand c) {
        changeControllerCommandList.add(c);
    }

    public final void runChangeControllerCommands() {
        for (final GameCommand c : changeControllerCommandList) {
            c.run();
        }
        changeControllerCommandList.clear();
    }

    public final void setSickness(boolean sickness0) {
        if (sickness == sickness0) { return; }
        sickness = sickness0;
        view.updateSickness(this);
    }

    public final boolean isFirstTurnControlled() {
        return sickness;
    }

    public final boolean hasSickness() {
        return sickness && !hasKeyword(Keyword.HASTE);
    }

    public final boolean isSick() {
        return sickness && isCreature() && !hasKeyword(Keyword.HASTE);
    }

    public boolean hasBecomeTargetThisTurn() {
        return becameTargetThisTurn;
    }
    public void setBecameTargetThisTurn(boolean becameTargetThisTurn0) {
        becameTargetThisTurn = becameTargetThisTurn0;
    }

    public boolean hasStartedTheTurnUntapped() {
        return startedTheTurnUntapped;
    }
    public void setStartedTheTurnUntapped(boolean untapped) {
        startedTheTurnUntapped = untapped;
    }

    public boolean cameUnderControlSinceLastUpkeep() {
        return cameUnderControlSinceLastUpkeep;
    }

    public void setCameUnderControlSinceLastUpkeep(boolean underControlSinceLastUpkeep) {
        this.cameUnderControlSinceLastUpkeep = underControlSinceLastUpkeep;
    }

    public final Player getOwner() {
        return owner;
    }
    public final void setOwner(final Player owner0) {
        if (owner == owner0) { return; }
        if (owner != null && owner.getGame() != this.getGame()) {
            // Sanity check.
            throw new RuntimeException();
        }
        owner = owner0;
        view.updateOwner(this);
        view.updateController(this);
    }

    public final Player getController() {
        Entry<Long, Player> lastEntry = tempControllers.lastEntry();
        if (lastEntry != null) {
            final long lastTimestamp = lastEntry.getKey();
            if (lastTimestamp > controllerTimestamp) {
                return lastEntry.getValue();
            }
        }
        if (controller != null) {
            return controller;
        }
        return owner;
    }

    public final void setController(final Player player, final long tstamp) {
        tempControllers.clear();
        controller = player;
        controllerTimestamp = tstamp;
        view.updateController(this);
    }

    public final void addTempController(final Player player, final long tstamp) {
        tempControllers.put(tstamp, player);
        view.updateController(this);
    }

    public final void removeTempController(final long tstamp) {
        if (tempControllers.remove(tstamp) != null) {
            view.updateController(this);
        }
    }

    public final void removeTempController(final Player player) {
        // Remove each key that yields this player
        this.tempControllers.values().remove(player);
    }

    public final void clearTempControllers() {
        if (tempControllers.isEmpty()) { return; }
        tempControllers.clear();
        view.updateController(this);
    }

    public final void clearControllers() {
        if (tempControllers.isEmpty() && controller == null) { return; }
        tempControllers.clear();
        controller = null;
        view.updateController(this);
    }

    public boolean mayPlayerLook(final Player player) {
        return view.mayPlayerLook(player.getView());
    }

    public final void setMayLookAt(final Player player, final boolean mayLookAt) {
        setMayLookAt(player, mayLookAt, false);
    }
    public final void setMayLookAt(final Player player, final boolean mayLookAt, final boolean temp) {
        view.setPlayerMayLook(player, mayLookAt, temp);
    }

    public final CardPlayOption mayPlay(final StaticAbility sta) {
        if (sta == null) {
            return null;
        }
        return mayPlay.get(sta);
    }

    public final List<CardPlayOption> mayPlay(final Player player) {
        List<CardPlayOption> result = Lists.newArrayList();
        for (CardPlayOption o : mayPlay.values()) {
            if (o.getPlayer().equals(player)) {
                result.add(o);
            }
        }
        return result;
    }
    public final void setMayPlay(final Player player, final boolean withoutManaCost, final Cost altManaCost, final boolean withFlash, final boolean grantZonePermissions, final StaticAbility sta) {
        this.mayPlay.put(sta, new CardPlayOption(player, sta, withoutManaCost, altManaCost, withFlash, grantZonePermissions));
    }
    public final void removeMayPlay(final StaticAbility sta) {
        this.mayPlay.remove(sta);
    }

    public void resetMayPlayTurn() {
        for (StaticAbility sta : getStaticAbilities()) {
            sta.resetMayPlayTurn();
        }
    }

    public final CardCollectionView getEquippedBy() {
        if (this.attachedCards == null) {
            return CardCollection.EMPTY;
        }

        return CardLists.filter(attachedCards, CardPredicates.Presets.EQUIPMENT);
    }

    public final boolean isEquipped() {
        if (this.attachedCards == null) {
            return false;
        }

        return CardLists.count(attachedCards, CardPredicates.Presets.EQUIPMENT) > 0;
    }
    public final boolean isEquippedBy(Card c) {
        return this.hasCardAttachment(c);
    }
    public final boolean isEquippedBy(final String cardName) {
        return this.hasCardAttachment(cardName);
    }

    public final CardCollectionView getFortifiedBy() {
        if (this.attachedCards == null) {
            return CardCollection.EMPTY;
        }

        return CardLists.filter(attachedCards, CardPredicates.Presets.FORTIFICATION);
    }

    public final boolean isFortified() {
        if (this.attachedCards == null) {
            return false;
        }

        return CardLists.count(attachedCards, CardPredicates.Presets.FORTIFICATION) > 0;
    }
    public final boolean isFortifiedBy(Card c) {
        // 301.5e + 301.6
        return hasCardAttachment(c);
    }

    public final Card getEquipping() {
        return this.getAttachedTo();
    }
    public final boolean isEquipping() {
        return this.isAttachedToEntity();
    }

    public final boolean isFortifying() {
        return this.isAttachedToEntity();
    }

    public final void equipCard(final Card c) {
        if (!isEquipment()) {
            return;
        }

        this.attachToEntity(c);
    }

    public final void fortifyCard(final Card c) {
        if (!isFortification()) {
            return;
        }

        this.attachToEntity(c);
    }

    public final void unEquipCard(final Card c) { // equipment.unEquipCard(equippedCard);
        this.unattachFromEntity(c);
    }

    public final void unEquipAllCards() {
        if (isEquipped()) {
            for (Card c : Lists.newArrayList(getEquippedBy())) {
                c.unattachFromEntity(this);
            }
        }
    }

    public final GameEntity getEntityAttachedTo() {
        return entityAttachedTo;
    }
    public final void setEntityAttachedTo(final GameEntity e) {
        if (entityAttachedTo == e) { return; }
        entityAttachedTo = e;
        view.updateAttachedTo(this);
    }
    public final void removeAttachedTo(final GameEntity e) {
        if (entityAttachedTo == e) {
            setEntityAttachedTo(null);
        }
    }
    public final boolean isAttachedToEntity() {
        return entityAttachedTo != null;
    }

    public final Card getAttachedTo() {
        if (entityAttachedTo instanceof Card) {
            return (Card) entityAttachedTo;
        }
        return null;
    }

    public final Card getEnchantingCard() {
        return getAttachedTo();
    }
    public final Player getPlayerAttachedTo() {
        if (entityAttachedTo instanceof Player) {
            return (Player) entityAttachedTo;
        }
        return null;
    }
    public final boolean isEnchanting() {
        return isAttachedToEntity();
    }
    public final boolean isEnchantingCard() {
        return getEnchantingCard() != null;
    }

    public final void attachToEntity(final GameEntity entity) {
        if (!entity.canBeAttached(this)) {
            return;
        }

        GameEntity oldTarget = null;
        if (isAttachedToEntity()) {
            oldTarget = getEntityAttachedTo();
            // If attempting to reattach to the same object, don't do anything.
            if (oldTarget.equals(entity)) {
                return;
            }
            unattachFromEntity(oldTarget);
        }

        // They use double links... it's doubtful
        setEntityAttachedTo(entity);
        setTimestamp(getGame().getNextTimestamp());
        entity.addAttachedCard(this);

        // Play the Equip sound
        getGame().fireEvent(new GameEventCardAttachment(this, oldTarget, entity));

        // run trigger
        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.AttachSource, this);
        runParams.put(AbilityKey.AttachTarget, entity);
        getController().getGame().getTriggerHandler().runTrigger(TriggerType.Attached, runParams, false);

    }

    public final void unattachFromEntity(final GameEntity entity) {
        if (entityAttachedTo == null || !entityAttachedTo.equals(entity)) {
            return;
        }

        setEntityAttachedTo(null);
        entity.removeAttachedCard(this);

        // Handle Bestowed Aura part
        if (isBestowed()) {
            unanimateBestow();
        }
        getGame().fireEvent(new GameEventCardAttachment(this, entity, null));

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Attach, this);
        runParams.put(AbilityKey.Object, entity);
        getGame().getTriggerHandler().runTrigger(TriggerType.Unattach, runParams, false);
        runUnattachCommands();
    }

    public final void setType(final CardType type0) {
        currentState.setType(type0);
    }

    public final void addType(final String type0) {
        currentState.addType(type0);
    }

    public final void removeType(final CardType.Supertype st) {
        currentState.removeType(st);
    }

    public final void setCreatureTypes(Collection<String> ctypes) {
        currentState.setCreatureTypes(ctypes);
    }

    public final CardTypeView getType() {
        return getType(currentState);
    }

    public final CardTypeView getType(CardState state) {
        if (changedCardTypes.isEmpty()) {
            return state.getType();
        }
        return state.getType().getTypeWithChanges(getChangedCardTypes());
    }

    public Iterable<CardChangedType> getChangedCardTypes() {
        return Iterables.unmodifiableIterable(changedCardTypes.values());
    }

    public Map<Long, CardChangedType> getChangedCardTypesMap() {
        return Collections.unmodifiableMap(changedCardTypes);
    }

    public boolean clearChangedCardTypes() {
        if (changedCardTypes.isEmpty())
            return false;
        changedCardTypes.clear();
        return true;
    }

    public boolean clearChangedCardKeywords() {
        if (changedCardKeywords.isEmpty())
            return false;
        changedCardKeywords.clear();
        return true;
    }

    public boolean clearChangedCardColors() {
        if (changedCardColors.isEmpty())
            return false;
        changedCardColors.clear();
        return true;
    }

    public Map<Long, KeywordsChange> getChangedCardKeywords() {
        return changedCardKeywords;
    }

    public Map<Long, CardColor> getChangedCardColors() {
        return changedCardColors;
    }

    public final void addChangedCardTypes(final CardType addType, final CardType removeType,
            final boolean removeSuperTypes, final boolean removeCardTypes, final boolean removeSubTypes,
            final boolean removeLandTypes, final boolean removeCreatureTypes, final boolean removeArtifactTypes,
            final boolean removeEnchantmentTypes,
            final long timestamp) {
        addChangedCardTypes(addType, removeType, removeSuperTypes, removeCardTypes, removeSubTypes, removeLandTypes,
                removeCreatureTypes, removeArtifactTypes, removeEnchantmentTypes, timestamp, true);
    }

    public final void addChangedCardTypes(final CardType addType, final CardType removeType,
            final boolean removeSuperTypes, final boolean removeCardTypes, final boolean removeSubTypes,
            final boolean removeLandTypes, final boolean removeCreatureTypes, final boolean removeArtifactTypes,
            final boolean removeEnchantmentTypes,
            final long timestamp, final boolean updateView) {

        changedCardTypes.put(timestamp, new CardChangedType(
                addType, removeType, removeSuperTypes, removeCardTypes, removeSubTypes,
                removeLandTypes, removeCreatureTypes, removeArtifactTypes, removeEnchantmentTypes));
        if (updateView) {
            currentState.getView().updateType(currentState);
        }
    }

    public final void addChangedCardTypes(final Iterable<String> types, final Iterable<String> removeTypes,
            final boolean removeSuperTypes, final boolean removeCardTypes, final boolean removeSubTypes,
            final boolean removeLandTypes, final boolean removeCreatureTypes, final boolean removeArtifactTypes,
            final boolean removeEnchantmentTypes,
            final long timestamp) {
        addChangedCardTypes(types, removeTypes, removeSuperTypes, removeCardTypes, removeSubTypes,
                removeLandTypes, removeCreatureTypes, removeArtifactTypes, removeEnchantmentTypes,
                timestamp, true);
    }

    public final void addChangedCardTypes(final Iterable<String> types, final Iterable<String> removeTypes,
            final boolean removeSuperTypes, final boolean removeCardTypes, final boolean removeSubTypes,
            final boolean removeLandTypes, final boolean removeCreatureTypes, final boolean removeArtifactTypes,
            final boolean removeEnchantmentTypes,
            final long timestamp, final boolean updateView) {
        CardType addType = null;
        CardType removeType = null;
        if (types != null) {
            addType = new CardType(types);
        }

        if (removeTypes != null) {
            removeType = new CardType(removeTypes);
        }

        addChangedCardTypes(addType, removeType, removeSuperTypes, removeCardTypes, removeSubTypes,
                removeLandTypes, removeCreatureTypes, removeArtifactTypes, removeEnchantmentTypes,
                timestamp, updateView);
    }

    public final void removeChangedCardTypes(final long timestamp) {
        removeChangedCardTypes(timestamp, true);
    }

    public final void removeChangedCardTypes(final long timestamp, final boolean updateView) {
        if (changedCardTypes.remove(timestamp) != null && updateView) {
            currentState.getView().updateType(currentState);
        }
    }

    public final void addColor(final String s, final boolean addToColors, final long timestamp) {
        changedCardColors.put(timestamp, new CardColor(s, addToColors, timestamp));
        currentState.getView().updateColors(this);
        currentState.getView().updateHasChangeColors(!getChangedCardColors().isEmpty());
    }

    public final void removeColor(final long timestampIn) {
        final CardColor removeCol = changedCardColors.remove(timestampIn);

        if (removeCol != null) {
            currentState.getView().updateColors(this);
            currentState.getView().updateHasChangeColors(!getChangedCardColors().isEmpty());
        }
    }

    public final void setColor(final String color) {
        currentState.setColor(color);
    }
    public final void setColor(final byte color) {
        currentState.setColor(color);
    }

    public final ColorSet determineColor() {
        return determineColor(currentState);
    }
    public final ColorSet determineColor(CardState state) {
        final Iterable<CardColor> colorList = changedCardColors.values();
        byte colors = state.getColor();
        for (final CardColor cc : colorList) {
            if (cc.isAdditional()) {
                colors |= cc.getColorMask();
            } else {
                colors = cc.getColorMask();
            }
        }
        return ColorSet.fromMask(colors);
    }

    public final int getCurrentLoyalty() {
        return getCounters(CounterType.LOYALTY);
    }

    // values that are printed on card
    public final int getBasePower() {
        return currentState.getBasePower();
    }

    public final int getBaseToughness() {
        return currentState.getBaseToughness();
    }

    // values that are printed on card
    public final void setBasePower(final int n) {
        currentState.setBasePower(n);
    }

    public final void setBaseToughness(final int n) {
        currentState.setBaseToughness(n);
    }

    // values that are printed on card
    public final String getBasePowerString() {
        return (null == basePowerString) ? "" + getBasePower() : basePowerString;
    }

    public final String getBaseToughnessString() {
        return (null == baseToughnessString) ? "" + getBaseToughness() : baseToughnessString;
    }

    // values that are printed on card
    public final void setBasePowerString(final String s) {
        basePowerString = s;
    }

    public final void setBaseToughnessString(final String s) {
        baseToughnessString = s;
    }

    public final int getSetPower() {
        if (newPTCharacterDefining.isEmpty() && newPT.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return getLatestPT().getLeft();
    }

    public final int getSetToughness() {
        if (newPTCharacterDefining.isEmpty() && newPT.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return getLatestPT().getRight();
    }

    public final void addCloneState(CardCloneStates states, final long timestamp) {
        clonedStates.put(timestamp, states);
        updateCloneState(true);
    }

    public final boolean removeCloneState(final long timestamp) {
        if (clonedStates.remove(timestamp) != null) {
            updateCloneState(true);
            return true;
        }
        return false;
    }

    public final boolean removeCloneState(final CardTraitBase ctb) {
        boolean changed = false;
        List<Long> toRemove = Lists.newArrayList();
        for (final Entry<Long, CardCloneStates> e : clonedStates.entrySet()) {
            if (ctb.equals(e.getValue().getSource())) {
                toRemove.add(e.getKey());
                changed = true;
            }
        }
        for (final Long l : toRemove) {
            clonedStates.remove(l);
        }
        if (changed) {
            updateCloneState(true);
        }

        return changed;
    }

    public final Card getCloner() {
        CardCloneStates clStates = getLastClonedState();
        if (clStates == null) {
            return null;
        }
        return clStates.getHost();
    }

    public final boolean removeCloneStates() {
        if (clonedStates.isEmpty()) {
            return false;
        }
        clonedStates.clear();
        updateCloneState(false);
        return true;
    }

    public final Map<Long, CardCloneStates> getCloneStates() {
        return clonedStates;
    }

    public final void setCloneStates(Map<Long, CardCloneStates> val) {
        clonedStates.clear();
        clonedStates.putAll(val);
        updateCloneState(true);
    }

    private final void updateCloneState(final boolean updateView) {
        if (isFaceDown()) {
            setState(CardStateName.FaceDown, updateView, true);
        } else {
            setState(getFaceupCardStateName(), updateView, true);
        }
    }

    public final CardStateName getFaceupCardStateName() {
        if (isFlipped() && hasState(CardStateName.Flipped)) {
            return CardStateName.Flipped;
        } else if (backside && isDoubleFaced() && hasState(CardStateName.Transformed)) {
            return CardStateName.Transformed;
        } else if (backside && isMeldable() && hasState(CardStateName.Meld)) {
            return CardStateName.Meld;
        } else {
            return CardStateName.Original;
        }
    }

    private final CardCloneStates getLastClonedState() {
        if (clonedStates.isEmpty()) {
            return null;
        }
        return clonedStates.lastEntry().getValue();
    }

    public final void addTextChangeState(CardCloneStates states, final long timestamp) {
        textChangeStates.put(timestamp, states);
        updateCloneState(true);
    }

    public final boolean removeTextChangeState(final long timestamp) {
        if (textChangeStates.remove(timestamp) != null) {
            updateCloneState(true);
            return true;
        }
        return false;
    }
    public final boolean removeTextChangeStates() {
        if (textChangeStates.isEmpty()) {
            return false;
        }
        textChangeStates.clear();
        updateCloneState(false);
        return true;
    }

    private final CardCloneStates getLastTextChangeState() {
        if (textChangeStates.isEmpty()) {
            return null;
        }
        return textChangeStates.lastEntry().getValue();
    }

    public final boolean hasTextChangeState() {
        return !textChangeStates.isEmpty();
    }
    /**
     *
     * Get the latest set Power and Toughness of this Card.
     *
     * @return the latest set Power and Toughness of this {@link Card} as the
     * left and right values of a {@link Pair}, respectively. A value of Integer.MAX_VALUE
     * means that particular property has not been set.
     */
    private synchronized Pair<Integer, Integer> getLatestPT() {
        // Find latest set power
        Integer power = null, toughness = null;

        // apply CDA first
        for (Pair<Integer,Integer> pt : newPTCharacterDefining.values()) {
            if (pt.getLeft() != null)
                power = pt.getLeft();
            if (pt.getRight() != null)
                toughness = pt.getRight();
        }
        // now real PT
        for (Pair<Integer,Integer> pt : newPT.values()) {
            if (pt.getLeft() != null)
                power = pt.getLeft();
            if (pt.getRight() != null)
                toughness = pt.getRight();
        }

        if (power == null)
            power = Integer.MAX_VALUE;

        if (toughness == null)
            toughness = Integer.MAX_VALUE;

        return Pair.of(power, toughness);
    }

    public final void addNewPT(final Integer power, final Integer toughness, final long timestamp) {
        addNewPT(power, toughness, timestamp, false);
    }

    public final void addNewPT(final Integer power, final Integer toughness, final long timestamp, final boolean cda) {
        if (cda) {
            newPTCharacterDefining.put(timestamp, Pair.of(power, toughness));
        } else {
            newPT.put(timestamp, Pair.of(power, toughness));
        }
        getView().updateLethalDamage(this);
        currentState.getView().updatePower(this);
        currentState.getView().updateToughness(this);
    }

    public final void removeNewPT(final long timestamp) {
        boolean removed = false;

        removed |= newPT.remove(timestamp) != null;
        removed |= newPTCharacterDefining.remove(timestamp) != null;

        if (removed) {
            getView().updateLethalDamage(this);
            currentState.getView().updatePower(this);
            currentState.getView().updateToughness(this);
        }
    }

    public final int getCurrentPower() {
        int total = getBasePower();
        final int setPower = getSetPower();
        if (setPower != Integer.MAX_VALUE) {
            total = setPower;
        }
        return total;
    }

    public final StatBreakdown getUnswitchedPowerBreakdown() {
        return new StatBreakdown(getCurrentPower(), getTempPowerBoost(), getPowerBonusFromCounters());
    }
    public final int getUnswitchedPower() {
        return getUnswitchedPowerBreakdown().getTotal();
    }

    public final int getPowerBonusFromCounters() {
        return getCounters(CounterType.P1P1) + getCounters(CounterType.P1P2) + getCounters(CounterType.P1P0)
                - getCounters(CounterType.M1M1) + 2 * getCounters(CounterType.P2P2) - 2 * getCounters(CounterType.M2M1)
                - 2 * getCounters(CounterType.M2M2) - getCounters(CounterType.M1M0) + 2 * getCounters(CounterType.P2P0);
    }

    public final StatBreakdown getNetPowerBreakdown() {
        if (getAmountOfKeyword("CARDNAME's power and toughness are switched") % 2 != 0) {
            return getUnswitchedToughnessBreakdown();
        }
        return getUnswitchedPowerBreakdown();
    }
    public final int getNetPower() {
        if (getAmountOfKeyword("CARDNAME's power and toughness are switched") % 2 != 0) {
            return getUnswitchedToughness();
        }
        return getUnswitchedPower();
    }

    public final int getCurrentToughness() {
        int total = getBaseToughness();
        final int setToughness = getSetToughness();
        if (setToughness != Integer.MAX_VALUE) {
            total = setToughness;
        }
        return total;
    }

    public static class StatBreakdown {
        public final int currentValue;
        public final int tempBoost;
        public final int bonusFromCounters;
        public StatBreakdown() {
            this.currentValue = 0;
            this.tempBoost = 0;
            this.bonusFromCounters = 0;
        }
        public StatBreakdown(int currentValue, int tempBoost, int bonusFromCounters){
            this.currentValue = currentValue;
            this.tempBoost = tempBoost;
            this.bonusFromCounters = bonusFromCounters;
        }
        public int getTotal() {
            return currentValue + tempBoost + bonusFromCounters;
        }
        @Override
        public String toString() {
            return TextUtil.concatWithSpace("c:"+ currentValue,"tb:"+ tempBoost,"bfc:"+ bonusFromCounters);
        }
    }

    public final StatBreakdown getUnswitchedToughnessBreakdown() {
        return new StatBreakdown(getCurrentToughness(), getTempToughnessBoost(), getToughnessBonusFromCounters());
    }
    public final int getUnswitchedToughness() {
        return getUnswitchedToughnessBreakdown().getTotal();
    }

    public final int getToughnessBonusFromCounters() {
        return getCounters(CounterType.P1P1) + 2 * getCounters(CounterType.P1P2) - getCounters(CounterType.M1M1)
                + getCounters(CounterType.P0P1) - 2 * getCounters(CounterType.M0M2) + 2 * getCounters(CounterType.P2P2)
                - getCounters(CounterType.M0M1) - getCounters(CounterType.M2M1) - 2 * getCounters(CounterType.M2M2)
                + 2 * getCounters(CounterType.P0P2);
    }

    public final StatBreakdown getNetToughnessBreakdown() {
        if (getAmountOfKeyword("CARDNAME's power and toughness are switched") % 2 != 0) {
            return getUnswitchedPowerBreakdown();
        }
        return getUnswitchedToughnessBreakdown();
    }
    public final int getNetToughness() {
        return getNetToughnessBreakdown().getTotal();
    }

    public final boolean toughnessAssignsDamage() {
        return getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.toughnessAssignsDamage)
                || hasKeyword("CARDNAME assigns combat damage equal to its toughness rather than its power");
    }

    // How much combat damage does the card deal
    public final StatBreakdown getNetCombatDamageBreakdown() {
        if (hasKeyword("CARDNAME assigns no combat damage")) {
            return new StatBreakdown();
        }

        if (toughnessAssignsDamage()) {
            return getNetToughnessBreakdown();
        }
        return getNetPowerBreakdown();
    }
    public final int getNetCombatDamage() {
        return getNetCombatDamageBreakdown().getTotal();
    }

    private int multiKickerMagnitude = 0;
    public final void addMultiKickerMagnitude(final int n) { multiKickerMagnitude += n; }
    public final void setKickerMagnitude(final int n) { multiKickerMagnitude = n; }
    public final int getKickerMagnitude() {
        if (multiKickerMagnitude > 0) {
            return multiKickerMagnitude;
        }
        boolean hasK1 = costsPaid.contains(OptionalCost.Kicker1);
        return hasK1 == costsPaid.contains(OptionalCost.Kicker2) ? (hasK1 ? 2 : 0) : 1;
    }

    private int pseudoKickerMagnitude = 0;
    public final void addPseudoMultiKickerMagnitude(final int n) { pseudoKickerMagnitude += n; }
    public final void setPseudoMultiKickerMagnitude(final int n) { pseudoKickerMagnitude = n; }
    public final int getPseudoKickerMagnitude() { return pseudoKickerMagnitude; }

    // for cards like Giant Growth, etc.
    public final int getTempPowerBoost() {
        int result = 0;
        for (Pair<Integer, Integer> pair : boostPT.values()) {
            if (pair.getLeft() != null) {
                result += pair.getLeft();
            }
        }
        return result;
    }

    public final int getTempToughnessBoost() {
        int result = 0;
        for (Pair<Integer, Integer> pair : boostPT.values()) {
            if (pair.getRight() != null) {
                result += pair.getRight();
            }
        }
        return result;
    }

    public void addPTBoost(final Integer power, final Integer toughness, final long timestamp, final Integer staticId) {
        boostPT.put(staticId == null ? 0 : staticId, timestamp, Pair.of(power, toughness));
    }

    public void removePTBoost(final long timestamp, final Integer staticId) {
        boostPT.remove(staticId, timestamp);
    }

    public Table<Integer, Long, Pair<Integer, Integer>> getPTBoostTable() {
        return ImmutableTable.copyOf(boostPT);
    }

    public void setPTBoost(Table<Integer, Long, Pair<Integer, Integer>> table) {
        this.boostPT.clear();
        boostPT.putAll(table);
    }

    public final boolean isUntapped() {
        return !tapped;
    }

    public final boolean isTapped() {
        return tapped;
    }
    public final void setTapped(boolean tapped0) {
        if (tapped == tapped0) { return; }
        tapped = tapped0;
        view.updateTapped(this);
    }

    public final void tap() {
        tap(false);
    }
    public final void tap(boolean attacker) {
        if (tapped) { return; }

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
        runParams.put(AbilityKey.Attacker, attacker);
        getGame().getTriggerHandler().runTrigger(TriggerType.Taps, runParams, false);

        setTapped(true);
        getGame().fireEvent(new GameEventCardTapped(this, true));
    }

    public final void untap() {
        if (!tapped) { return; }

        // Run Replacement effects
        if (getGame().getReplacementHandler().run(ReplacementType.Untap, AbilityKey.mapFromAffected(this)) != ReplacementResult.NotReplaced) {
            return;
        }

        // Run triggers
        getGame().getTriggerHandler().runTrigger(TriggerType.Untaps, AbilityKey.mapFromCard(this), false);

        for (final GameCommand var : untapCommandList) {
            var.run();
        }
        setTapped(false);
        getGame().fireEvent(new GameEventCardTapped(this, false));
    }

    public final void addChangedCardTraits(Collection<SpellAbility> spells, Collection<SpellAbility> removedAbilities,
            Collection<Trigger> trigger, Collection<ReplacementEffect> replacements, Collection<StaticAbility> statics,
            boolean removeAll, boolean removeNonMana, boolean removeIntrinsic, long timestamp) {
        changedCardTraits.put(timestamp, new CardTraitChanges(
            spells, removedAbilities, trigger, replacements, statics, removeAll, removeNonMana, removeIntrinsic
        ));
        // update view
        updateAbilityTextForView();
    }

    public final boolean removeChangedCardTraits(long timestamp) {
        return changedCardTraits.remove(timestamp) != null;
    }

    public final Map<Long, CardTraitChanges> getChangedCardTraits() {
        return changedCardTraits;
    }

    public final void setChangedCardTraits(Map<Long, CardTraitChanges> changes) {
        changedCardTraits.clear();
        for (Entry<Long, CardTraitChanges> e : changes.entrySet()) {
            changedCardTraits.put(e.getKey(), e.getValue().copy(this, true));
        }
    }

    public boolean clearChangedCardTraits() {
        if (changedCardTraits.isEmpty()) {
            return false;
        }
        changedCardTraits.clear();
        return true;
    }

    // keywords are like flying, fear, first strike, etc...
    public final List<KeywordInterface> getKeywords() {
        return getKeywords(currentState);
    }
    public final List<KeywordInterface> getKeywords(CardState state) {
        ListKeywordVisitor visitor = new ListKeywordVisitor();
        visitKeywords(state, visitor);
        return visitor.getKeywords();
    }
    // Allows traversing the card's keywords without needing to concat a bunch
    // of lists. Optimizes common operations such as hasKeyword().
    public final void visitKeywords(CardState state, Visitor<KeywordInterface> visitor) {
        visitUnhiddenKeywords(state, visitor);
        visitHiddenExtreinsicKeywords(visitor);
    }

    @Override
    public final boolean hasKeyword(Keyword keyword) {
        return hasKeyword(keyword, currentState);
    }

    public final boolean hasKeyword(Keyword key, CardState state) {
        return state.hasKeyword(key);
    }

    @Override
    public final boolean hasKeyword(String keyword) {
        return hasKeyword(keyword, currentState);
    }

    public final boolean hasKeyword(String keyword, CardState state) {
        if (keyword.startsWith("HIDDEN")) {
            keyword = keyword.substring(7);
        }

        HasKeywordVisitor visitor = new HasKeywordVisitor(keyword, false);
        visitKeywords(state, visitor);
        return visitor.getResult();
    }

    public final void updateKeywords() {
        getCurrentState().getView().updateKeywords(this, getCurrentState());
        getView().updateLethalDamage(this);
    }

    public final void addChangedCardKeywords(final List<String> keywords, final List<String> removeKeywords,
            final boolean removeAllKeywords, final boolean removeIntrinsicKeywords, final long timestamp) {
        addChangedCardKeywords(keywords, removeKeywords, removeAllKeywords, removeIntrinsicKeywords, timestamp, true);
    }


    public final void addChangedCardKeywords(final List<String> keywords, final List<String> removeKeywords,
            final boolean removeAllKeywords, final boolean removeIntrinsicKeywords, final long timestamp, final boolean updateView) {
        // if the key already exists - merge entries
        final KeywordsChange cks = changedCardKeywords.get(timestamp);
        if (cks != null) {
            final KeywordsChange newCks = cks.merge(keywords, removeKeywords,
                    removeAllKeywords, removeIntrinsicKeywords);
            newCks.addKeywordsToCard(this);
            changedCardKeywords.put(timestamp, newCks);
        }
        else {
            final KeywordsChange newCks = new KeywordsChange(keywords, removeKeywords,
                    removeAllKeywords, removeIntrinsicKeywords);
            newCks.addKeywordsToCard(this);
            changedCardKeywords.put(timestamp, newCks);
        }

        if (updateView) {
            updateKeywords();
            if (isToken())
                game.fireEvent(new GameEventTokenStateUpdate(this));
        }
    }

    public final void addChangedCardKeywordsInternal(
            final List<KeywordInterface> keywords, final List<KeywordInterface> removeKeywords,
            final boolean removeAllKeywords, final boolean removeIntrinsicKeywords,
            final long timestamp, final boolean updateView) {
        KeywordCollection list = new KeywordCollection();
        list.insertAll(keywords);
        // if the key already exists - merge entries
        final KeywordsChange cks = changedCardKeywords.get(timestamp);
        if (cks != null) {
            final KeywordsChange newCks = cks.merge(keywords, removeKeywords,
                    removeAllKeywords, removeIntrinsicKeywords);
            newCks.addKeywordsToCard(this);
            changedCardKeywords.put(timestamp, newCks);
        }
        else {
            final KeywordsChange newCks = new KeywordsChange(keywords, removeKeywords,
                    removeAllKeywords, removeIntrinsicKeywords);
            newCks.addKeywordsToCard(this);
            changedCardKeywords.put(timestamp, newCks);
        }

        if (updateView) {
            updateKeywords();
        }
    }

    public final KeywordsChange removeChangedCardKeywords(final long timestamp) {
        return removeChangedCardKeywords(timestamp, true);
    }

    public final KeywordsChange removeChangedCardKeywords(final long timestamp, final boolean updateView) {
        KeywordsChange change = changedCardKeywords.remove(timestamp);
        if (change != null && updateView) {
            updateKeywords();
            if (isToken())
                game.fireEvent(new GameEventTokenStateUpdate(this));
        }
        return change;
    }

    public final boolean hasChangedCardKeywords(final long timestamp) {
        return changedCardKeywords.containsKey(timestamp);
    }

    public final void addChangedCardKeywordsInternal(final KeywordsChange change, final long timestamp) {
        changedCardKeywords.put(timestamp, change);
        updateKeywordsCache(currentState);
    }

    public final boolean clearChangedCardKeywords(final boolean updateView) {
        if (changedCardKeywords.isEmpty()) {
            return false;
        }
        changedCardKeywords.clear();
        if (updateView) {
            updateKeywords();
        }
        return true;
    }

    // Hidden keywords will be left out
    public final Collection<KeywordInterface> getUnhiddenKeywords() {
        return getUnhiddenKeywords(currentState);
    }
    public final Collection<KeywordInterface> getUnhiddenKeywords(CardState state) {
        return state.getCachedKeywords();
    }

    public final void updateKeywordsCache(final CardState state) {
        KeywordCollection keywords = new KeywordCollection();

        //final List<KeywordInterface> keywords = Lists.newArrayList();
        boolean removeIntrinsic = false;
        for (final KeywordsChange ck : changedCardKeywords.values()) {
            if (ck.isRemoveIntrinsicKeywords()) {
                removeIntrinsic = true;
                break;
            }
        }

        if (!removeIntrinsic) {
            keywords.insertAll(state.getIntrinsicKeywords());
        }

        // see if keyword changes are in effect
        for (final KeywordsChange ck : changedCardKeywords.values()) {
            if (ck.isRemoveAllKeywords()) {
                keywords.clear();
            }
            else if (ck.getRemoveKeywords() != null) {
                keywords.removeAll(ck.getRemoveKeywords());
            }

            keywords.removeInstances(ck.getRemovedKeywordInstances());

            if (ck.getKeywords() != null) {
                keywords.insertAll(ck.getKeywords());
            }
        }

        // remove Can't have keywords
        for (Keyword k : getCantHaveKeyword()) {
            keywords.removeAll(k);
        }

        state.setCachedKeywords(keywords);
    }
    private void visitUnhiddenKeywords(CardState state, Visitor<KeywordInterface> visitor) {
        for (KeywordInterface kw : getUnhiddenKeywords(state)) {
            if (!visitor.visit(kw)) {
                return;
            }
        }
    }

    /**
     * Replace all instances of one color word in this card's text by another.
     * @param originalWord the original color word.
     * @param newWord the new color word.
     * @throws RuntimeException if either of the strings is not a valid Magic
     *  color.
     */
    public final void addChangedTextColorWord(final String originalWord, final String newWord, final Long timestamp) {
        if (MagicColor.fromName(newWord) == 0) {
            throw new RuntimeException("Not a color: " + newWord);
        }
        changedTextColors.add(timestamp, StringUtils.capitalize(originalWord), StringUtils.capitalize(newWord));
        updateKeywordsChangedText(timestamp);
        updateChangedText();
    }

    public final void removeChangedTextColorWord(final Long timestamp) {
        changedTextColors.remove(timestamp);
        updateKeywordsOnRemoveChangedText(
                removeChangedCardKeywords(timestamp));
        updateChangedText();
    }

    /**
     * Replace all instances of one type in this card's text by another.
     * @param originalWord the original type word.
     * @param newWord the new type word.
     */
    public final void addChangedTextTypeWord(final String originalWord, final String newWord, final Long timestamp) {
        changedTextTypes.add(timestamp, originalWord, newWord);
        if (getType().hasSubtype(originalWord)) {
            addChangedCardTypes(CardType.parse(newWord), CardType.parse(originalWord),
                    false, false, false, false, false, false, false, timestamp);
        }
        updateKeywordsChangedText(timestamp);
        updateChangedText();
    }

    public final void removeChangedTextTypeWord(final Long timestamp) {
        changedTextTypes.remove(timestamp);
        removeChangedCardTypes(timestamp);
        updateKeywordsOnRemoveChangedText(
                removeChangedCardKeywords(timestamp));
        updateChangedText();
    }

    public final void removeAllChangedText(final Long timestamp) {
        changedTextTypes.removeAll();
        changedTextColors.removeAll();
        updateKeywordsOnRemoveChangedText(removeChangedCardKeywords(timestamp));
        updateChangedText();
    }

    private void updateKeywordsChangedText(final Long timestamp) {
        if (hasSVar("LockInKeywords")) {
            return;
        }

        final List<KeywordInterface> addKeywords = Lists.newArrayList();
        final List<KeywordInterface> removeKeywords = Lists.newArrayList(keywordsGrantedByTextChanges);

        for (final KeywordInterface kw : currentState.getIntrinsicKeywords()) {
            String oldtxt = kw.getOriginal();
            final String newtxt = AbilityUtils.applyKeywordTextChangeEffects(oldtxt, this);
            if (!newtxt.equals(oldtxt)) {
                KeywordInterface newKw = Keyword.getInstance(newtxt);
                addKeywords.add(newKw);
                removeKeywords.add(kw);
                keywordsGrantedByTextChanges.add(newKw);
            }
        }
        if (!addKeywords.isEmpty() || !removeKeywords.isEmpty()) {
            addChangedCardKeywordsInternal(addKeywords, removeKeywords, false, false, timestamp, true);
        }
    }

    private void updateKeywordsOnRemoveChangedText(final KeywordsChange k) {
        if (k != null) {
            keywordsGrantedByTextChanges.removeAll(k.getKeywords());
        }
    }

    /**
     * Update the changed text of the intrinsic spell abilities and keywords.
     */
    private void updateChangedText() {
        resetChangedSVars();
        currentState.updateChangedText();
        text = AbilityUtils.applyDescriptionTextChangeEffects(originalText, this);

        currentState.getView().updateAbilityText(this, currentState);
        view.updateNonAbilityText(this);
    }

    public final ImmutableMap<String, String> getChangedTextColorWords() {
        return ImmutableMap.copyOf(changedTextColors.toMap());
    }

    public final ImmutableMap<String, String> getChangedTextTypeWords() {
        return ImmutableMap.copyOf(changedTextTypes.toMap());
    }

    /**
     * Copy the color and type text changes from another {@link Card} to this
     * one. The original changes of this Card are removed.
     */
    public final void copyChangedTextFrom(final Card other) {
        changedTextColors.copyFrom(other.changedTextColors);
        changedTextTypes.copyFrom(other.changedTextTypes);
    }

    /**
     * Change a SVar due to a text change effect. Change is volatile and will be
     * reverted upon refreshing text changes (unless it is changed again at that
     * time).
     *
     * @param key the SVar name.
     * @param value the new SVar value.
     */
    public final void changeSVar(final String key, final String value) {
        originalSVars.put(key, getSVar(key));
        setSVar(key, value);
    }

    private void resetChangedSVars() {
        for (final Entry<String, String> svar : originalSVars.entrySet()) {
            setSVar(svar.getKey(), svar.getValue());
        }
        originalSVars.clear();
    }

    public final KeywordInterface addIntrinsicKeyword(final String s) {
        KeywordInterface inst = currentState.addIntrinsicKeyword(s, true);
        if (inst != null) {
            updateKeywords();
        }
        return inst;
    }

    public final void addIntrinsicKeywords(final Iterable<String> s) {
        addIntrinsicKeywords(s, true);
    }
    public final void addIntrinsicKeywords(final Iterable<String> s, boolean initTraits) {
        if (currentState.addIntrinsicKeywords(s, initTraits)) {
            updateKeywords();
        }
    }

    public final void removeIntrinsicKeyword(final String s) {
        if (currentState.removeIntrinsicKeyword(s)) {
            updateKeywords();
        }
    }

    public final void removeIntrinsicKeyword(final KeywordInterface s) {
        if (currentState.removeIntrinsicKeyword(s)) {
            updateKeywords();
        }
    }

    // Hidden Keywords will be returned without the indicator HIDDEN
    public final List<KeywordInterface> getHiddenExtrinsicKeywords() {
        ListKeywordVisitor visitor = new ListKeywordVisitor();
        visitHiddenExtreinsicKeywords(visitor);
        return visitor.getKeywords();
    }
    private void visitHiddenExtreinsicKeywords(Visitor<KeywordInterface> visitor) {
        for (KeywordInterface inst : hiddenExtrinsicKeyword.getValues()) {
            if (!visitor.visit(inst)) {
                return;
            }
        }
    }

    public final void addHiddenExtrinsicKeyword(String s) {
        if (s.startsWith("HIDDEN")) {
            s = s.substring(7);
        }
        if (hiddenExtrinsicKeyword.add(s) != null) {
            view.updateNonAbilityText(this);
            updateKeywords();
        }
    }

    public final void addHiddenExtrinsicKeyword(KeywordInterface k) {
        if (hiddenExtrinsicKeyword.insert(k)) {
            view.updateNonAbilityText(this);
            updateKeywords();
        }
    }

    public final void removeHiddenExtrinsicKeyword(String s) {
        if (s.startsWith("HIDDEN")) {
            s = s.substring(7);
        }
        if (hiddenExtrinsicKeyword.remove(s)) {
            view.updateNonAbilityText(this);
            updateKeywords();
        }
    }

    public void addCantHaveKeyword(Keyword keyword, Long timestamp) {
        cantHaveKeywords.put(timestamp, keyword);
        getView().updateCantHaveKeyword(this);
    }

    public void addCantHaveKeyword(Long timestamp, Iterable<Keyword> keywords) {
        cantHaveKeywords.putAll(timestamp, keywords);
        getView().updateCantHaveKeyword(this);
    }

    public boolean removeCantHaveKeyword(Long timestamp) {
        return removeCantHaveKeyword(timestamp, true);
    }
    public boolean removeCantHaveKeyword(Long timestamp, boolean updateView) {
        boolean change = !cantHaveKeywords.removeAll(timestamp).isEmpty();
        if (change && updateView) {
            getView().updateCantHaveKeyword(this);
            updateKeywords();
            if (isToken())
                game.fireEvent(new GameEventTokenStateUpdate(this));
        }
        return change;
    }

    public Collection<Keyword> getCantHaveKeyword() {
        return cantHaveKeywords.values();
    }

    public final void setStaticAbilities(final List<StaticAbility> a) {
        currentState.setStaticAbilities(a);
    }

    public final FCollectionView<StaticAbility> getStaticAbilities() {
        return currentState.getStaticAbilities();
    }
    public final StaticAbility addStaticAbility(final String s) {
        if (!s.trim().isEmpty()) {
            final StaticAbility stAb = new StaticAbility(s, this);
            stAb.setIntrinsic(true);
            currentState.addStaticAbility(stAb);
            return stAb;
        }
        return null;
    }
    public final StaticAbility addStaticAbility(final StaticAbility stAb) {
        currentState.addStaticAbility(stAb);
        return stAb;
    }

    @Deprecated
    public final void removeStaticAbility(StaticAbility stAb) {
        currentState.removeStaticAbility(stAb);
    }

    public void updateStaticAbilities(List<StaticAbility> list, CardState state) {
        if (hasRemoveIntrinsic()) {
            list.clear();
        }

        for (final CardTraitChanges ck : changedCardTraits.values()) {
            if (ck.isRemoveAll()) {
                list.clear();
            }
            list.addAll(ck.getStaticAbilities());
        }

        for (KeywordInterface kw : getUnhiddenKeywords(state)) {
            list.addAll(kw.getStaticAbilities());
        }
    }

    public final boolean isPermanent() {
        return !isImmutable && (isInZone(ZoneType.Battlefield) || getType().isPermanent());
    }

    public final boolean isSpell() {
        return (isInstant() || isSorcery() || (isAura() && !isInZone((ZoneType.Battlefield))));
    }

    public final boolean isEmblem()     { return getType().isEmblem(); }

    public final boolean isLand()       { return getType().isLand(); }
    public final boolean isBasicLand()  { return getType().isBasicLand(); }
    public final boolean isSnow()       { return getType().isSnow(); }

    public final boolean isTribal()     { return getType().isTribal(); }
    public final boolean isSorcery()    { return getType().isSorcery(); }
    public final boolean isInstant()    { return getType().isInstant(); }

    public final boolean isCreature()   { return getType().isCreature(); }
    public final boolean isArtifact()   { return getType().isArtifact(); }
    public final boolean isPlaneswalker()   { return getType().isPlaneswalker(); }
    public final boolean isEnchantment()    { return getType().isEnchantment(); }

    public final boolean isEquipment()  { return getType().hasSubtype("Equipment"); }
    public final boolean isFortification()  { return getType().hasSubtype("Fortification"); }
    public final boolean isCurse()          { return getType().hasSubtype("Curse"); }
    public final boolean isAura()           { return getType().hasSubtype("Aura"); }

    public final boolean isAttachment() { return isAura() || isEquipment() || isFortification(); }
    public final boolean isHistoric()   {return getType().isLegendary() || isArtifact() || getType().hasSubtype("Saga");}

    public final boolean isScheme()     { return getType().isScheme(); }
    public final boolean isPhenomenon() { return getType().isPhenomenon(); }
    public final boolean isPlane()      { return getType().isPlane(); }

    /** {@inheritDoc} */
    @Override
    public final int compareTo(final Card that) {
        if (that == null) {
            /*
             * "Here we can arbitrarily decide that all non-null Cards are
             * `greater than' null Cards. It doesn't really matter what we
             * return in this case, as long as it is consistent. I rather think
             * of null as being lowly." --Braids
             */
            return 1;
        }
        return Integer.compare(id, that.id);
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        if (getView() == null) {
            return getPaperCard().getName();
        }
        return getView().toString();
    }

    public final boolean isUnearthed() {
        return unearthed;
    }
    public final void setUnearthed(final boolean b) {
        unearthed = b;
    }

    public final boolean hasSuspend() {
        return hasKeyword(Keyword.SUSPEND) && getLastKnownZone().is(ZoneType.Exile)
                && getCounters(CounterType.TIME) >= 1;
    }

    public final boolean isPhasedOut() {
        return phasedOut;
    }
    public final void setPhasedOut(final boolean phasedOut0) {
        if (phasedOut == phasedOut0) { return; }
        phasedOut = phasedOut0;
        view.updatePhasedOut(this);
    }

    public final void phase() {
        phase(true);
    }
    public final void phase(final boolean direct) {
        final boolean phasingIn = isPhasedOut();

        if (!switchPhaseState()) {
            // Switch Phase State bails early if the Permanent can't Phase Out
            return;
        }

        if (!phasingIn) {
            setDirectlyPhasedOut(direct);
        }

        if (hasCardAttachments()) {
            for (final Card eq : getAttachedCards()) {
                if (eq.isPhasedOut() == phasingIn) {
                    eq.phase(false);
                }
            }
        }

        getGame().fireEvent(new GameEventCardPhased(this, isPhasedOut()));
    }

    private boolean switchPhaseState() {
        if (!phasedOut && hasKeyword("CARDNAME can't phase out.")) {
            return false;
        }

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);

        if (!isPhasedOut()) {
            // If this is currently PhasedIn, it's about to phase out.
            // Run trigger before it does because triggers don't work with phased out objects
            getGame().getTriggerHandler().runTrigger(TriggerType.PhaseOut, runParams, false);
        }

        setPhasedOut(!phasedOut);
        final Combat combat = getGame().getPhaseHandler().getCombat();
        if (combat != null && phasedOut) {
            combat.removeFromCombat(this);
        }

        if (!phasedOut) {
            // Just phased in, time to run the phased in trigger
            getGame().getTriggerHandler().registerActiveTrigger(this, false);
            getGame().getTriggerHandler().runTrigger(TriggerType.PhaseIn, runParams, false);
        }

        return true;
    }

    public final boolean isDirectlyPhasedOut() {
        return directlyPhasedOut;
    }
    public final void setDirectlyPhasedOut(final boolean direct) {
        directlyPhasedOut = direct;
    }

    public final boolean isReflectedLand() {
        for (final SpellAbility a : currentState.getManaAbilities()) {
            if (a.getApi() == ApiType.ManaReflected) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasStartOfKeyword(final String keyword) {
        return hasStartOfKeyword(keyword, currentState);
    }
    public final boolean hasStartOfKeyword(String keyword, CardState state) {
        HasKeywordVisitor visitor = new HasKeywordVisitor(keyword, true);
        visitKeywords(state, visitor);
        return visitor.getResult();
    }

    public final boolean hasStartOfUnHiddenKeyword(String keyword) {
        return hasStartOfUnHiddenKeyword(keyword, currentState);
    }
    public final boolean hasStartOfUnHiddenKeyword(String keyword, CardState state) {
        HasKeywordVisitor visitor = new HasKeywordVisitor(keyword, true);
        visitUnhiddenKeywords(state, visitor);
        return visitor.getResult();
    }

    public final boolean hasAnyKeyword(final Iterable<String> keywords) {
        return hasAnyKeyword(keywords, currentState);
    }
    public final boolean hasAnyKeyword(final Iterable<String> keywords, CardState state) {
        for (final String keyword : keywords) {
            if (hasKeyword(keyword, state)) {
                return true;
            }
        }
        return false;
    }

    // This counts the number of instances of a keyword a card has
    public final int getAmountOfKeyword(final String k) {
        return getAmountOfKeyword(k, currentState);
    }
    public final int getAmountOfKeyword(final String k, CardState state) {
        CountKeywordVisitor visitor = new CountKeywordVisitor(k);
        visitKeywords(state, visitor);
        return visitor.getCount();
    }

    public final int getAmountOfKeyword(final Keyword k) {
        return getAmountOfKeyword(k, currentState);
    }
    public final int getAmountOfKeyword(final Keyword k, CardState state) {
        return getKeywords(k, state).size();
    }

    public final Collection<KeywordInterface> getKeywords(final Keyword k) {
        return getKeywords(k, currentState);
    }
    public final Collection<KeywordInterface> getKeywords(final Keyword k, CardState state) {
        return state.getCachedKeyword(k);
    }

    // This is for keywords with a number like Bushido, Annihilator and Rampage.
    // It returns the total.
    public final int getKeywordMagnitude(final Keyword k) {
        return getKeywordMagnitude(k, currentState);
    }

    /**
     * use it only for real keywords and not with hidden ones
     *
     * @return Int
     */
    public final int getKeywordMagnitude(final Keyword k, CardState state) {
        int count = 0;
        for (final KeywordInterface inst : getKeywords(k, state)) {
            String kw = inst.getOriginal();
            // this can't be used yet for everything because of X values in Bushido X
            // KeywordInterface#getAmount
            // KeywordCollection#getAmount

            final String[] parse = kw.contains(":") ? kw.split(":") : kw.split(" ");
            final String s = parse[1];
            if (StringUtils.isNumeric(s)) {
               count += Integer.parseInt(s);
            } else {
                String svar = StringUtils.join(parse);
                if (state.hasSVar(svar)) {
                    count += AbilityUtils.calculateAmount(this, state.getSVar(svar), null);
                }
            }
        }
        return count;
    }

    // Takes one argument like Permanent.Blue+withFlying
    @Override
    public final boolean isValid(final String restriction, final Player sourceController, final Card source, SpellAbility spellAbility) {
        if (isImmutable() && source != null && !source.isRemembered(this) &&
                !(restriction.startsWith("Emblem") || restriction.startsWith("Effect"))) { // special case exclusion
            return false;
        }

        // Inclusive restrictions are Card types
        final String[] incR = restriction.split("\\.", 2);

        boolean testFailed = false;
        if (incR[0].startsWith("!")) {
            testFailed = true; // a bit counter logical))
            incR[0] = incR[0].substring(1); // consume negation sign
        }

        if (incR[0].equals("Spell") && !isSpell()) {
            return testFailed;
        }
        if (incR[0].equals("Permanent") && !isPermanent()) {
            return testFailed;
        }
        if (!incR[0].equals("card") && !incR[0].equals("Card") && !incR[0].equals("Spell")
                && !incR[0].equals("Permanent") && !getType().hasStringType(incR[0])) {
            return testFailed; // Check for wrong type
        }

        if (incR.length > 1) {
            final String excR = incR[1];
            final String[] exRs = excR.split("\\+"); // Exclusive Restrictions are ...
            for (String exR : exRs) {
                if (!hasProperty(exR, sourceController, source, spellAbility)) {
                    return testFailed;
                }
            }
        }
        return !testFailed;
    }

    // Takes arguments like Blue or withFlying
    @Override
    public boolean hasProperty(final String property, final Player sourceController, final Card source, SpellAbility spellAbility) {
        return CardProperty.cardHasProperty(this, property, sourceController, source, spellAbility);
    }

    public final boolean isImmutable() {
        return isImmutable;
    }
    public final void setImmutable(final boolean isImmutable0) {
        isImmutable = isImmutable0;
    }

    /*
     * there are easy checkers for Color. The CardUtil functions should be made
     * part of the Card class, so calling out is not necessary
     */
    public final boolean isOfColor(final String col) { return CardUtil.getColors(this).hasAnyColor(MagicColor.fromName(col)); }
    public final boolean isBlack() { return CardUtil.getColors(this).hasBlack(); }
    public final boolean isBlue() { return CardUtil.getColors(this).hasBlue(); }
    public final boolean isRed() { return CardUtil.getColors(this).hasRed(); }
    public final boolean isGreen() { return CardUtil.getColors(this).hasGreen(); }
    public final boolean isWhite() { return CardUtil.getColors(this).hasWhite(); }
    public final boolean isColorless() { return CardUtil.getColors(this).isColorless(); }

    public final boolean sharesNameWith(final Card c1) {
        // in a corner case where c1 is null, there is no name to share with.
        if (c1 == null) {
            return false;
        }

        // Special Logic for SpyKit
        if (c1.hasKeyword("AllNonLegendaryCreatureNames")) {
            if (hasKeyword("AllNonLegendaryCreatureNames")) {
                // with both does have this, then they share any name
                return true;
            } else if (getName().isEmpty()) {
                // if this does not have a name, then there is no name to share
                return false;
            } else {
                // check if this card has a name from a face
                // in general token creatures does not have this
                final ICardFace face = StaticData.instance().getCommonCards().getFaceByName(getName());
                if (face == null) {
                    return false;
                }
                // TODO add check if face is legal in the format of the game
                // name does need to be a non-legendary creature
                final CardType type = face.getType();
                if (type != null && type.isCreature() && !type.isLegendary())
                    return true;
            }
        }
        return sharesNameWith(c1.getName());
    }

    public final boolean sharesNameWith(final String name) {
        // the name is null or empty
        if (name == null || name.isEmpty()) {
            return false;
        }

        boolean shares = getName().equals(name);

        // Split cards has extra logic to check if it does share a name with
        if (isSplitCard()) {
            shares |= name.equals(getState(CardStateName.LeftSplit).getName());
            shares |= name.equals(getState(CardStateName.RightSplit).getName());
        }

        if (!shares && hasKeyword("AllNonLegendaryCreatureNames")) {
            // check if the name is from a face
            // in general token creatures does not have this
            final ICardFace face = StaticData.instance().getCommonCards().getFaceByName(name);
            if (face == null) {
                return false;
            }
            // TODO add check if face is legal in the format of the game
            // name does need to be a non-legendary creature
            final CardType type = face.getType();
            if (type.isCreature() && !type.isLegendary())
                return true;
        }
        return shares;
    }

    public final boolean sharesColorWith(final Card c1) {
        boolean shares;
        shares = (isBlack() && c1.isBlack());
        shares |= (isBlue() && c1.isBlue());
        shares |= (isGreen() && c1.isGreen());
        shares |= (isRed() && c1.isRed());
        shares |= (isWhite() && c1.isWhite());
        return shares;
    }

    public final boolean sharesCMCWith(final int n) {
        //need to get GameState for Discarded Cards
        final Card host = game.getCardState(this);

        //do not check for SplitCard anymore
        return host.getCMC() == n;
    }

    public final boolean sharesCMCWith(final Card c1) {
        //need to get GameState for Discarded Cards
        final Card host = game.getCardState(this);
        final Card other = game.getCardState(c1);

        //do not check for SplitCard anymore
        return host.getCMC() == other.getCMC();
    }

    public final boolean sharesCreatureTypeWith(final Card c1) {
        if (c1 == null) {
            return false;
        }

        for (final String type : getType().getCreatureTypes()) {
            if (type.equals("AllCreatureTypes") && c1.hasACreatureType()) {
                return true;
            }
            if (c1.getType().hasCreatureType(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean sharesLandTypeWith(final Card c1) {
        if (c1 == null) {
            return false;
        }

        for (final String type : getType().getLandTypes()) {
            if (c1.getType().hasSubtype(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean sharesPermanentTypeWith(final Card c1) {
        if (c1 == null) {
            return false;
        }

        for (final CoreType type : getType().getCoreTypes()) {
            if (type.isPermanent && c1.getType().hasType(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean sharesCardTypeWith(final Card c1) {
        for (final CoreType type : getType().getCoreTypes()) {
            if (c1.getType().hasType(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean sharesTypeWith(final Card c1) {
        for (final String type : getType()) {
            if (c1.getType().hasStringType(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean sharesControllerWith(final Card c1) {
        return c1 != null && getController().equals(c1.getController());
    }

    public final boolean hasACreatureType() {
        for (final String type : getType().getSubtypes()) {
            if (forge.card.CardType.isACreatureType(type) ||  type.equals("AllCreatureTypes")) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasALandType() {
        for (final String type : getType().getSubtypes()) {
            if (forge.card.CardType.isALandType(type) || forge.card.CardType.isABasicLandType(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasABasicLandType() {
        for (final String type : getType().getSubtypes()) {
            if (forge.card.CardType.isABasicLandType(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean isUsedToPay() {
        return usedToPayCost;
    }
    public final void setUsedToPay(final boolean b) {
        usedToPayCost = b;
    }

    // /////////////////////////
    //
    // Damage code
    //
    // ////////////////////////

    public final Map<Card, Integer> getReceivedDamageFromThisTurn() {
        return receivedDamageFromThisTurn;
    }
    public final void setReceivedDamageFromThisTurn(final Map<Card, Integer> receivedDamageList) {
        receivedDamageFromThisTurn = Maps.newHashMap(receivedDamageList);
    }

    public final Map<Player, Integer> getReceivedDamageFromPlayerThisTurn() {
        return receivedDamageFromPlayerThisTurn;
    }

    public final void setReceivedDamageFromPlayerThisTurn(final Map<Player, Integer> receivedDamageList) {
        receivedDamageFromPlayerThisTurn = Maps.newHashMap(receivedDamageList);
    }

    public int getReceivedDamageByPlayerThisTurn(final Player p) {
        if (receivedDamageFromPlayerThisTurn.containsKey(p)) {
            return receivedDamageFromPlayerThisTurn.get(p);
        }
        return 0;
    }

    public final void addReceivedDamageFromThisTurn(final Card c, final int damage) {
        int currentDamage = 0;
        if (receivedDamageFromThisTurn.containsKey(c)) {
            currentDamage = receivedDamageFromThisTurn.get(c);
        }
        receivedDamageFromThisTurn.put(c, damage+currentDamage);

        Player p = c.getController();
        if (p != null) {
            currentDamage = 0;
            if (receivedDamageFromPlayerThisTurn.containsKey(p)) {
                currentDamage = receivedDamageFromPlayerThisTurn.get(p);
            }
            receivedDamageFromPlayerThisTurn.put(p, damage+currentDamage);
        }
    }
    public final void resetReceivedDamageFromThisTurn() {
        receivedDamageFromThisTurn.clear();
        receivedDamageFromPlayerThisTurn.clear();
    }

    public final int getTotalDamageRecievedThisTurn() {
        int total = 0;
        for (int damage : receivedDamageFromThisTurn.values()) {
            total += damage;
        }
        return total;
    }

    // TODO: Combine getDealtDamageToThisTurn with addDealtDamageToPlayerThisTurn using GameObject, Integer
    public final Map<Card, Integer> getDealtDamageToThisTurn() {
        return dealtDamageToThisTurn;
    }
    public final void setDealtDamageToThisTurn(final Map<Card, Integer> dealtDamageList) {
        dealtDamageToThisTurn = dealtDamageList;
    }
    public final void addDealtDamageToThisTurn(final Card c, final int damage) {
        int currentDamage = 0;
        if (dealtDamageToThisTurn.containsKey(c)) {
            currentDamage = dealtDamageToThisTurn.get(c);
        }
        dealtDamageToThisTurn.put(c, damage+currentDamage);
    }
    public final void resetDealtDamageToThisTurn() {
        dealtDamageToThisTurn.clear();
    }

    public final Map<String, Integer> getDealtDamageToPlayerThisTurn() {
        return dealtDamageToPlayerThisTurn;
    }
    public final void setDealtDamageToPlayerThisTurn(final Map<String, Integer> dealtDamageList) {
        dealtDamageToPlayerThisTurn = dealtDamageList;
    }
    public final void addDealtDamageToPlayerThisTurn(final String player, final int damage) {
        int currentDamage = 0;
        if (dealtDamageToPlayerThisTurn.containsKey(player)) {
            currentDamage = dealtDamageToPlayerThisTurn.get(player);
        }
        dealtDamageToPlayerThisTurn.put(player, damage+currentDamage);
    }
    public final void resetDealtDamageToPlayerThisTurn() {
        dealtDamageToPlayerThisTurn.clear();
    }

    public final boolean hasDealtDamageToOpponentThisTurn() {
        for (final GameEntity e : getDamageHistory().getThisTurnDamaged()) {
            if (e instanceof Player) {
                final Player p = (Player) e;
                if (getController().isOpponentOf(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    // this is the amount of damage a creature needs to receive before it dies
    public final int getLethal() {
        if (hasKeyword("Lethal damage dealt to CARDNAME is determined by its power rather than its toughness.")) {
            return getNetPower(); }
        else {
            return getNetToughness(); }
    }

    // this is the minimal damage a trampling creature has to assign to a blocker
    public final int getLethalDamage() {
        return getLethal() - getDamage() - getTotalAssignedDamage();
    }

    public final int getDamage() {
        return damage;
    }
    public final void setDamage(int damage0) {
        if (damage == damage0) { return; }
        damage = damage0;
        view.updateDamage(this);
        getGame().fireEvent(new GameEventCardStatsChanged(this));
    }

    public final boolean hasBeenDealtDeathtouchDamage() {
        return hasBeenDealtDeathtouchDamage;
    }
    public final void setHasBeenDealtDeathtouchDamage(final boolean hasBeenDealtDeatchtouchDamage) {
        this.hasBeenDealtDeathtouchDamage = hasBeenDealtDeatchtouchDamage;
    }

    public final Map<Card, Integer> getAssignedDamageMap() {
        return assignedDamageMap;
    }

    public final void addAssignedDamage(int assignedDamage0, final Card sourceCard) {
        if (assignedDamage0 < 0) {
            assignedDamage0 = 0;
        }
        Log.debug(this + " - was assigned " + assignedDamage0 + " damage, by " + sourceCard);
        if (!assignedDamageMap.containsKey(sourceCard)) {
            assignedDamageMap.put(sourceCard, assignedDamage0);
        }
        else {
            assignedDamageMap.put(sourceCard, assignedDamageMap.get(sourceCard) + assignedDamage0);
        }
        if (assignedDamage0 > 0) {
            view.updateAssignedDamage(this);
        }
    }
    public final void clearAssignedDamage() {
        if (assignedDamageMap.isEmpty()) { return; }
        assignedDamageMap.clear();
        view.updateAssignedDamage(this);
    }

    public final int getTotalAssignedDamage() {
        int total = 0;
        for (Integer assignedDamage : assignedDamageMap.values()) {
            total += assignedDamage;
        }
        return total;
    }

    public final void addCombatDamage(final Map<Card, Integer> map, final CardDamageMap damageMap, final CardDamageMap preventMap, GameEntityCounterTable counterTable) {
        for (final Entry<Card, Integer> entry : map.entrySet()) {
            addCombatDamage(entry.getValue(), entry.getKey(), damageMap, preventMap, counterTable);
        }
    }

    /*
     * (non-Javadoc)
     * @see forge.game.GameEntity#addCombatDamageBase(int, forge.game.card.Card, forge.game.card.CardDamageMap, forge.game.GameEntityCounterTable)
     */
    @Override
    protected int addCombatDamageBase(final int damage, final Card source, CardDamageMap damageMap, GameEntityCounterTable counterTable) {
        if (isInPlay()) {
            return super.addCombatDamageBase(damage, source, damageMap, counterTable);
        }
        return 0;
    }

    public final boolean canDamagePrevented(final boolean isCombat) {
        CardCollection list = new CardCollection(getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES));
        list.add(this);
        for (final Card ca : list) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.applyAbility("CantPreventDamage", this, isCombat)) {
                    return false;
                }
            }
        }

        return true;
    }

    // This is used by the AI to forecast an effect (so it must not change the game state)
    public final int staticDamagePrevention(final int damage, final int possiblePrevention, final Card source, final boolean isCombat) {
        if (!source.canDamagePrevented(isCombat)) {
            return damage;
        }

        for (final Card ca : getGame().getCardsIn(ZoneType.Battlefield)) {
            for (final ReplacementEffect re : ca.getReplacementEffects()) {
                Map<String, String> params = re.getMapParams();
                if (!re.getMode().equals(ReplacementType.DamageDone) || !params.containsKey("PreventionEffect")) {
                    continue;
                }
                if (params.containsKey("ValidSource")
                        && !source.isValid(params.get("ValidSource"), ca.getController(), ca, null)) {
                    continue;
                }
                if (params.containsKey("ValidTarget")
                        && !isValid(params.get("ValidTarget"), ca.getController(), ca, null)) {
                    continue;
                }
                if (params.containsKey("IsCombat")) {
                    if (params.get("IsCombat").equals("True")) {
                        if (!isCombat) {
                            continue;
                        }
                    } else {
                        if (isCombat) {
                            continue;
                        }
                    }
                }
                return 0;
            }
        }
        return staticDamagePrevention(damage - possiblePrevention, source, isCombat, true);
    }

    // This should be also usable by the AI to forecast an effect (so it must not change the game state)
    @Override
    public final int staticDamagePrevention(final int damageIn, final Card source, final boolean isCombat, final boolean isTest) {
        if (damageIn <= 0) {
            return 0;
        }

        if (!source.canDamagePrevented(isCombat)) {
            return damageIn;
        }

        if (isCombat && getGame().getPhaseHandler().isPreventCombatDamageThisTurn()) {
            return 0;
        }

        int restDamage = damageIn;

        if (hasProtectionFromDamage(source)) {
            return 0;
        }

        // Prevent Damage static abilities
        for (final Card ca : getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                restDamage = stAb.applyAbility("PreventDamage", source, this, restDamage, isCombat, isTest);
            }
        }
        return restDamage > 0 ? restDamage : 0;
    }

    protected int preventShieldEffect(final int damage) {
        if (damage <= 0) {
            return 0;
        }

        int restDamage = damage;

        boolean DEBUGShieldsWithEffects = false;
        while (!getPreventNextDamageWithEffect().isEmpty() && restDamage != 0) {
            Map<Card, Map<String, String>> shieldMap = getPreventNextDamageWithEffect();
            CardCollectionView preventionEffectSources = new CardCollection(shieldMap.keySet());
            Card shieldSource = preventionEffectSources.get(0);
            if (preventionEffectSources.size() > 1) {
                Map<String, Card> choiceMap = Maps.newTreeMap();
                List<String> choices = Lists.newArrayList();
                for (final Card key : preventionEffectSources) {
                    String effDesc = shieldMap.get(key).get("EffectString");
                    int descIndex = effDesc.indexOf("SpellDescription");
                    effDesc = effDesc.substring(descIndex + 18);
                    String shieldDescription = key.toString() + " - " + shieldMap.get(key).get("ShieldAmount")
                            + " shields - " + effDesc;
                    choices.add(shieldDescription);
                    choiceMap.put(shieldDescription, key);
                }
                shieldSource = getController().getController().chooseProtectionShield(this, choices, choiceMap);
            }
            if (DEBUGShieldsWithEffects) {
                System.out.println("Prevention shield source: " + shieldSource);
            }

            int shieldAmount = Integer.valueOf(shieldMap.get(shieldSource).get("ShieldAmount"));
            int dmgToBePrevented = Math.min(restDamage, shieldAmount);
            if (DEBUGShieldsWithEffects) {
                System.out.println("Selected source initial shield amount: " + shieldAmount);
                System.out.println("Incoming damage: " + restDamage);
                System.out.println("Damage to be prevented: " + dmgToBePrevented);
            }

            //Set up ability
            SpellAbility shieldSA;
            String effectAbString = shieldMap.get(shieldSource).get("EffectString");
            effectAbString = TextUtil.fastReplace(effectAbString, "PreventedDamage", Integer.toString(dmgToBePrevented));
            effectAbString = TextUtil.fastReplace(effectAbString, "ShieldEffectTarget", shieldMap.get(shieldSource).get("ShieldEffectTarget"));
            if (DEBUGShieldsWithEffects) {
                System.out.println("Final shield ability string: " + effectAbString);
            }
            shieldSA = AbilityFactory.getAbility(effectAbString, shieldSource);
            if (shieldSA.usesTargeting()) {
                System.err.println(shieldSource + " - Targeting for prevention shield's effect should be done with initial spell");
            }

            boolean apiIsEffect = (shieldSA.getApi() == ApiType.Effect);
            CardCollectionView cardsInCommand = null;
            if (apiIsEffect) {
                cardsInCommand = getGame().getCardsIn(ZoneType.Command);
            }

            getController().getController().playSpellAbilityNoStack(shieldSA, true);
            if (apiIsEffect) {
                CardCollection newCardsInCommand = (CardCollection)getGame().getCardsIn(ZoneType.Command);
                newCardsInCommand.removeAll(cardsInCommand);
                if (!newCardsInCommand.isEmpty()) {
                    newCardsInCommand.get(0).setSVar("PreventedDamage", "Number$" + dmgToBePrevented);
                }
            }
            subtractPreventNextDamageWithEffect(shieldSource, restDamage);
            restDamage = restDamage - dmgToBePrevented;

            if (DEBUGShieldsWithEffects) {
                System.out.println("Remaining shields: "
                    + (shieldMap.containsKey(shieldSource) ? shieldMap.get(shieldSource).get("ShieldAmount") : "all shields used"));
                System.out.println("Remaining damage: " + restDamage);
            }
        }
        return restDamage;
    }

    // This is used by the AI to forecast an effect (so it must not change the game state)
    @Override
    public final int staticReplaceDamage(final int damage, final Card source, final boolean isCombat) {

        int restDamage = damage;
        for (Card c : getGame().getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Sulfuric Vapors")) {
                if (source.isSpell() && source.isRed()) {
                    restDamage += 1;
                }
            } else if (c.getName().equals("Pyromancer's Swath")) {
                if (c.getController().equals(source.getController()) && (source.isInstant() || source.isSorcery())
                        && isCreature()) {
                    restDamage += 2;
                }
            } else if (c.getName().equals("Furnace of Rath")) {
                if (isCreature()) {
                    restDamage += restDamage;
                }
            } else if (c.getName().equals("Dictate of the Twin Gods")) {
                restDamage += restDamage;
            } else if (c.getName().equals("Gratuitous Violence")) {
                if (c.getController().equals(source.getController()) && source.isCreature() && isCreature()) {
                    restDamage += restDamage;
                }
            } else if (c.getName().equals("Fire Servant")) {
                if (c.getController().equals(source.getController()) && source.isRed()
                        && (source.isInstant() || source.isSorcery())) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Gisela, Blade of Goldnight")) {
                if (!c.getController().equals(getController())) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Inquisitor's Flail")) {
                if (isCombat && c.getEquipping() != null
                        && (c.getEquipping().equals(this) || c.getEquipping().equals(source))) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Ghosts of the Innocent")) {
                if (isCreature()) {
                    restDamage = restDamage / 2;
                }
            } else if (c.getName().equals("Benevolent Unicorn")) {
                if (source.isSpell() && isCreature()) {
                   restDamage -= 1;
                }
            } else if (c.getName().equals("Divine Presence")) {
                if (restDamage > 3 && isCreature()) {
                    restDamage = 3;
                }
            } else if (c.getName().equals("Lashknife Barrier")) {
                if (c.getController().equals(getController()) && isCreature()) {
                    restDamage -= 1;
                }
            }
        }

        // TODO: improve such that this can be predicted from the replacement effect itself
        // (+ move this function out into ComputerUtilCombat?)
        for (Card c : getGame().getCardsIn(ZoneType.Command)) {
            if (c.getName().equals("Insult Effect")) {
                if (c.getController().equals(source.getController())) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Mishra")) {
                if (c.isCreature() && c.getController().equals(source.getController())) {
                    restDamage *= 2;
                }
            }
        }

        if (getName().equals("Phytohydra")) {
            return 0;
        }
        return restDamage;
    }

    public final void addDamage(final Map<Card, Integer> sourcesMap, CardDamageMap damageMap, GameEntityCounterTable counterTable) {
        for (final Entry<Card, Integer> entry : sourcesMap.entrySet()) {
            // damage prevention is already checked!
            addDamageAfterPrevention(entry.getValue(), entry.getKey(), true, damageMap, counterTable);
        }
    }

    /**
     * This function handles damage after replacement and prevention effects are
     * applied.
     */
    @Override
    public final int addDamageAfterPrevention(final int damageIn, final Card source, final boolean isCombat, CardDamageMap damageMap, GameEntityCounterTable counterTable) {

        if (damageIn == 0) {
            return 0; // Rule 119.8
        }

        addReceivedDamageFromThisTurn(source, damageIn);
        source.addDealtDamageToThisTurn(this, damageIn);

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.DamageSource, source);
        runParams.put(AbilityKey.DamageTarget, this);
        runParams.put(AbilityKey.DamageAmount, damageIn);
        runParams.put(AbilityKey.IsCombatDamage, isCombat);
        if (!isCombat) {
            runParams.put(AbilityKey.SpellAbilityStackInstance, game.stack.peek());
        }
        // Defending player at the time the damage was dealt
        runParams.put(AbilityKey.DefendingPlayer, game.getCombat() != null ? game.getCombat().getDefendingPlayerRelatedTo(source) : null);
        getGame().getTriggerHandler().runTrigger(TriggerType.DamageDone, runParams, false);

        GameEventCardDamaged.DamageType damageType = DamageType.Normal;
        if (isPlaneswalker()) {
            subtractCounter(CounterType.LOYALTY, damageIn);
        }
        if (isCreature()) {
            final Game game = source.getGame();

            boolean wither = (game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.alwaysWither)
                    || source.hasKeyword(Keyword.WITHER) || source.hasKeyword(Keyword.INFECT));

            if (isInPlay()) {
                if (wither) {
                    addCounter(CounterType.M1M1, damageIn, source.getController(), true, counterTable);
                    damageType = DamageType.M1M1Counters;
                }
                else {
                    damage += damageIn;
                    view.updateDamage(this);
                }
            }

            if (source.hasKeyword(Keyword.DEATHTOUCH) && isCreature()) {
                setHasBeenDealtDeathtouchDamage(true);
                damageType = DamageType.Deathtouch;
            }

            // Play the Damage sound
            game.fireEvent(new GameEventCardDamaged(this, source, damageIn, damageType));
        }

        if (damageIn > 0) {
            damageMap.put(source, this, damageIn);
        }

        return damageIn;
    }

    public final String getSetCode() {
        return currentState.getSetCode();
    }
    public final void setSetCode(final String setCode) {
        currentState.setSetCode(setCode);
    }

    public final CardRarity getRarity() {
        return currentState.getRarity();
    }
    public final void setRarity(CardRarity r) {
        currentState.setRarity(r);
    }

    public final String getMostRecentSet() {
        return StaticData.instance().getCommonCards().getCard(getPaperCard().getName()).getEdition();
    }

    public final String getImageKey() {
        return getCardForUi().currentState.getImageKey();
    }
    public final void setImageKey(final String iFN) {
        getCardForUi().currentState.setImageKey(iFN);
    }

    public String getImageKey(CardStateName state) {
        CardState c = getCardForUi().states.get(state);
        return (c != null ? c.getImageKey() : "");
    }


    public final boolean isTributed() { return tributed; }

    public final void setTributed(final boolean b) {
        tributed = b;
    }

    public final boolean isEmbalmed() {
        return embalmed;
    }
    public final void setEmbalmed(final boolean b) {
        embalmed = b;
    }

    public final boolean isEternalized() {
        return eternalized;
    }
    public final void setEternalized(final boolean b) {
        eternalized = b;
    }

    public final int getExertedThisTurn() {
        return exertThisTurn;
    }

    public void exert() {
        exertedByPlayer.add(getController());
        exertThisTurn++;
        view.updateExertedThisTurn(this, true);
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
        runParams.put(AbilityKey.Player, getController());
        game.getTriggerHandler().runTrigger(TriggerType.Exerted, runParams, false);
    }

    public boolean isExertedBy(final Player player) {
        return exertedByPlayer.contains(player);
    }

    public void removeExertedBy(final Player player) {
        exertedByPlayer.remove(player);
        view.updateExertedThisTurn(this, getExertedThisTurn() > 0);
    }

    protected void resetExtertedThisTurn() {
        exertThisTurn = 0;
        view.updateExertedThisTurn(this, false);
    }

    public boolean isMadness() {
        return madness;
    }
    public void setMadness(boolean madness0) {
        madness = madness0;
    }
    public boolean getMadnessWithoutCast() { return madnessWithoutCast; }
    public void setMadnessWithoutCast(boolean state) { madnessWithoutCast = state; }

    public final boolean isMonstrous() {
        return monstrous;
    }
    public final void setMonstrous(final boolean monstrous0) {
        monstrous = monstrous0;
    }

    public final boolean isRenowned() {
        return renowned;
    }
    public final void setRenowned(final boolean renowned0) {
        renowned = renowned0;
    }

    public final boolean isManifested() {
        return manifested;
    }
    public final void setManifested(final boolean manifested) {
        this.manifested = manifested;
        final String image = manifested ? ImageKeys.MANIFEST_IMAGE : ImageKeys.MORPH_IMAGE;
        // Note: This should only be called after state has been set to CardStateName.FaceDown,
        // so the below call should be valid since the state should have been created already.
        getState(CardStateName.FaceDown).setImageKey(ImageKeys.getTokenKey(image));
        if (!manifested) {
            // remove Manifest Up abilities from Original State
            CardState original = getState(CardStateName.Original);
            for (SpellAbility sa : original.getNonManaAbilities()) {
                if (sa.isManifestUp()) {
                    original.removeSpellAbility(sa);
                }
            }
        }
    }

    public final void animateBestow() {
        animateBestow(true);
    }

    public final void animateBestow(final boolean updateView) {
        bestowTimestamp = getGame().getNextTimestamp();
        addChangedCardTypes(new CardType(Collections.singletonList("Aura")),
                new CardType(Collections.singletonList("Creature")),
                false, false, false, false, false, false, true, bestowTimestamp, updateView);
        addChangedCardKeywords(Collections.singletonList("Enchant creature"), Lists.newArrayList(),
                false, false, bestowTimestamp, updateView);
    }

    public final void unanimateBestow() {
        unanimateBestow(true);
    }

    public final void unanimateBestow(final boolean updateView) {
        removeChangedCardKeywords(bestowTimestamp, updateView);
        removeChangedCardTypes(bestowTimestamp, updateView);
        bestowTimestamp = -1;
    }

    public final boolean isBestowed() {
        return bestowTimestamp != -1;
    }

    public final long getTimestamp() {
        return timestamp;
    }
    public final void setTimestamp(final long t) {
        timestamp = t;
    }
    public boolean equalsWithTimestamp(Card c) {
        return equals(c) && c.getTimestamp() == timestamp;
    }

    /**
     * Assign a random foil finish depending on the card edition.
     */
    public final void setRandomFoil() {
        setFoil(CardEdition.getRandomFoil(getSetCode()));
    }

    public final void setFoil(final int f) {
        currentState.setSVar("Foil", Integer.toString(f));
    }

    public final CardCollectionView getHauntedBy() {
        return CardCollection.getView(hauntedBy);
    }
    public final boolean isHaunted() {
        return FCollection.hasElements(hauntedBy);
    }
    public final boolean isHauntedBy(Card c) {
        return FCollection.hasElement(hauntedBy, c);
    }
    public final void addHauntedBy(Card c, final boolean update) {
        hauntedBy = view.addCard(hauntedBy, c, TrackableProperty.HauntedBy);
        if (c != null && update) {
            c.setHaunting(this);
        }
    }
    public final void addHauntedBy(Card c) {
        addHauntedBy(c, true);
    }
    public final void removeHauntedBy(Card c) {
        hauntedBy = view.removeCard(hauntedBy, c, TrackableProperty.HauntedBy);
    }

    public final Card getHaunting() {
        return haunting;
    }
    public final void setHaunting(final Card c) {
        haunting = view.setCard(haunting, c, TrackableProperty.Haunting);
    }

    public final Card getPairedWith() {
        return pairedWith;
    }
    public final void setPairedWith(final Card c) {
        pairedWith = view.setCard(pairedWith, c, TrackableProperty.PairedWith);
    }
    public final boolean isPaired() {
        return pairedWith != null;
    }

    public Card getMeldedWith() {   return meldedWith;  }

    public void setMeldedWith(Card meldedWith) {    this.meldedWith = meldedWith;   }

    public final int getDamageDoneThisTurn() {
        int sum = 0;
        for (final Card c : dealtDamageToThisTurn.keySet()) {
            sum += dealtDamageToThisTurn.get(c);
        }

        return sum;
    }

    public final int getDamageDoneToPlayerBy(final String player) {
        int sum = 0;
        for (final String p : dealtDamageToPlayerThisTurn.keySet()) {
            if (p.equals(player)) {
                sum += dealtDamageToPlayerThisTurn.get(p);
            }
        }
        return sum;
    }

    /**
     * Gets the total damage done by card this turn (after prevention and redirects).
     *
     * @return the damage done to player p this turn
     */
    public final int getTotalDamageDoneBy() {
        int sum = 0;
        for (final Card c : dealtDamageToThisTurn.keySet()) {
            sum += dealtDamageToThisTurn.get(c);
        }
        for (final String p : dealtDamageToPlayerThisTurn.keySet()) {
            sum += dealtDamageToPlayerThisTurn.get(p);
        }
        return sum;
    }


    public boolean hasProtectionFrom(final Card source) {
        return hasProtectionFrom(source, false, false);
    }

    public boolean hasProtectionFromDamage(final Card source) {
        return hasProtectionFrom(source, false, true);
    }

    @Override
    public boolean hasProtectionFrom(final Card source, final boolean checkSBA) {
        return hasProtectionFrom(source, checkSBA, false);
    }

    public boolean hasProtectionFrom(final Card source, final boolean checkSBA, final boolean damageSource) {
        if (source == null) {
            return false;
        }

        if (isImmutable()) {
            return true;
        }

        // Protection only works on the Battlefield
        if (!isInZone(ZoneType.Battlefield)) {
            return false;
        }

        final boolean colorlessDamage = damageSource && source.hasKeyword("Colorless Damage Source");

        for (final KeywordInterface inst : getKeywords()) {
            String kw = inst.getOriginal();
            if (!kw.startsWith("Protection")) {
                continue;
            }
            if (kw.equals("Protection from white")) {
                if (source.isWhite() && !colorlessDamage) {
                    return true;
                }
            } else if (kw.equals("Protection from blue")) {
                if (source.isBlue() && !colorlessDamage) {
                    return true;
                }
            } else if (kw.equals("Protection from black")) {
                if (source.isBlack() && !colorlessDamage) {
                    return true;
                }
            } else if (kw.equals("Protection from red")) {
                if (source.isRed() && !colorlessDamage) {
                    return true;
                }
            } else if (kw.equals("Protection from green")) {
                if (source.isGreen() && !colorlessDamage) {
                    return true;
                }
            } else if (kw.equals("Protection from monocolored")) {
                if (CardUtil.getColors(source).isMonoColor() && !colorlessDamage) {
                    return true;
                }
            } else if (kw.equals("Protection from multicolored")) {
                if (CardUtil.getColors(source).isMulticolor() && !colorlessDamage) {
                    return true;
                }
            } else if (kw.equals("Protection from all colors")) {
                if (!source.isColorless() && !colorlessDamage) {
                    return true;
                }
            } else if (kw.equals("Protection from colorless")) {
                if (source.isColorless() || colorlessDamage) {
                    return true;
                }
            } else if (kw.equals("Protection from creatures")) {
                if (source.isCreature()) {
                    return true;
                }
            } else if (kw.equals("Protection from artifacts")) {
                if (source.isArtifact()) {
                    return true;
                }
            } else if (kw.equals("Protection from enchantments")) {
                if (source.isEnchantment()) {
                    return true;
                }
            } else if (kw.equals("Protection from everything")) {
                return true;
            } else if (kw.startsWith("Protection:")) { // uses isValid; Protection:characteristic:desc:exception
                final String[] kws = kw.split(":");
                String characteristic = kws[1];

                if (characteristic.startsWith("Player")) {
                    // TODO need to handle that better in CardProperty
                    if (source.getController().isValid(characteristic.split(","), getController(), this, null)) {
                        return true;
                    }
                } else {
                    // if colorlessDamage then it does only check damage color..
                    if (colorlessDamage) {
                        if (characteristic.endsWith("White") || characteristic.endsWith("Blue")
                            || characteristic.endsWith("Black") || characteristic.endsWith("Red")
                            || characteristic.endsWith("Green") || characteristic.endsWith("Colorless")
                            || characteristic.endsWith("ChosenColor")) {
                            characteristic += "Source";
                        }
                    }

                    final String[] characteristics = characteristic.split(",");
                    final String exception = kws.length > 3 ? kws[3] : null; // check "This effect cannot remove sth"
                    if (source.isValid(characteristics, getController(), this, null)
                            && (!checkSBA || exception == null || !source.isValid(exception, getController(), this, null))) {
                        return true;
                    }
                }
            } else if (kw.equals("Protection from colored spells")) {
                if (source.isSpell() && !source.isColorless()) {
                    return true;
                }
            } else if (kw.startsWith("Protection from opponent of ")) {
                final String playerName = kw.substring("Protection from opponent of ".length());
                if (source.getController().isOpponentOf(playerName)) {
                    return true;
                }
            } else if (kw.startsWith("Protection from ")) {
                final String protectType = CardType.getSingularType(kw.substring("Protection from ".length()));
                if (source.getType().hasStringType(protectType)) {
                    return true;
                }
            }
        }
        return false;
    }
    public String getProtectionKey() {
        String protectKey = "";
        boolean pR = false; boolean pG = false; boolean pB = false; boolean pU = false; boolean pW = false;
        for (final KeywordInterface inst : getKeywords()) {
            String kw = inst.getOriginal();
            if (!kw.startsWith("Protection")) {
                continue;
            }
            if (kw.equals("Protection from red")) {
                if (!pR) {
                    pR = true;
                    protectKey += "R";
                }
            } else if (kw.equals("Protection from green")) {
                if (!pG) {
                    pG = true;
                    protectKey += "G";
                }
            } else if (kw.equals("Protection from black")) {
                if (!pB) {
                    pB = true;
                    protectKey += "B";
                }
            } else if (kw.equals("Protection from blue")) {
                if (!pU) {
                    pU = true;
                    protectKey += "U";
                }
            } else if (kw.equals("Protection from white")) {
                if (!pW) {
                    pW = true;
                    protectKey += "W";
                }
            } else if (kw.equals("Protection from monocolored")) {
                protectKey += "monocolored:";
            } else if (kw.equals("Protection from multicolored")) {
                protectKey += "multicolored:";
            } else if (kw.equals("Protection from all colors")) {
                protectKey += "allcolors:";
            } else if (kw.equals("Protection from colorless")) {
                protectKey += "colorless:";
            } else if (kw.equals("Protection from creatures")) {
                protectKey += "creatures:";
            } else if (kw.equals("Protection from artifacts")) {
                protectKey += "artifacts:";
            } else if (kw.equals("Protection from enchantments")) {
                protectKey += "enchantments:";
            } else if (kw.equals("Protection from everything")) {
                protectKey += "everything:";
            } else if (kw.equals("Protection from colored spells")) {
                protectKey += "coloredspells:";
            } else if (kw.startsWith("Protection")) {
                protectKey += "generic";
            }
        }
        return protectKey;
    }
    public String getHexproofKey() {
        String hexproofKey = "";
        boolean hR = false; boolean hG = false; boolean hB = false; boolean hU = false; boolean hW = false;
        for (final KeywordInterface inst : getKeywords()) {
            String kw = inst.getOriginal();
            if (!kw.startsWith("Hexproof")) {
                continue;
            }
            if (kw.equals("Hexproof")) {
                hexproofKey += "generic:";
            }
            if (kw.startsWith("Hexproof:")) {
                String[] k = kw.split(":");
                if (k[2].toString().equals("red")) {
                    if (!hR) {
                        hR = true;
                        hexproofKey += "R:";
                    }
                } else if (k[2].toString().equals("green")) {
                    if (!hG) {
                        hG = true;
                        hexproofKey += "G:";
                    }
                } else if (k[2].toString().equals("black")) {
                    if (!hB) {
                        hB = true;
                        hexproofKey += "B:";
                    }
                } else if (k[2].toString().equals("blue")) {
                    if (!hU) {
                        hU = true;
                        hexproofKey += "U:";
                    }
                } else if (k[2].toString().equals("white")) {
                    if (!hW) {
                        hW = true;
                        hexproofKey += "W:";
                    }
                } else if (k[2].toString().equals("monocolored")) {
                    hexproofKey += "monocolored:";
                }
            }
        }
        return hexproofKey;
    }
    public String getKeywordKey() {
        List<String> ability = new ArrayList<>();
        for (final KeywordInterface inst : getKeywords()) {
            ability.add(inst.getOriginal());
        }
        Collections.sort(ability);
        return StringUtils.join(ability.toArray(), ","); //fix nosuchmethod on some android devices...
    }
    public Zone getZone() {
        return currentZone;
    }
    public void setZone(Zone zone) {
        if (currentZone == zone) { return; }
        currentZone = zone;
        view.updateZone(this);
    }

    public boolean isInZone(final ZoneType zone) {
        Zone z = this.getLastKnownZone();
        return z != null && z.is(zone);
    }

    public final boolean canBeDestroyed() {
        return isInPlay() && (!hasKeyword(Keyword.INDESTRUCTIBLE) || (isCreature() && getNetToughness() <= 0));
    }

    public final boolean canBeSacrificed() {
        return isInPlay() && !this.isPhasedOut() && !hasKeyword("CARDNAME can't be sacrificed.");
    }

    @Override
    public final boolean canBeTargetedBy(final SpellAbility sa) {
        if (sa == null) {
            return true;
        }

        // CantTarget static abilities
        for (final Card ca : getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.applyAbility("CantTarget", this, sa)) {
                    return false;
                }
            }
        }

        // keywords don't work outside battlefield
        if (!isInZone(ZoneType.Battlefield)) {
            return true;
        }

        if (hasProtectionFrom(sa.getHostCard())) {
            return false;
        }

        if (isPhasedOut()) {
            return false;
        }

        final Card source = sa.getHostCard();

        if (sa.isSpell()) {
            for(KeywordInterface inst : source.getKeywords()) {
                String kw = inst.getOriginal();
                if(!kw.startsWith("SpellCantTarget")) {
                    continue;
                }
                final String[] k = kw.split(":");
                final String[] restrictions = k[1].split(",");
                if (isValid(restrictions, source.getController(), source, null)) {
                    return false;
                }
            }
        }
        return true;
    }

    public final boolean canBeControlledBy(final Player newController) {
        return !(hasKeyword("Other players can't gain control of CARDNAME.") && !getController().equals(newController));
    }

    @Override
    protected final boolean canBeEnchantedBy(final Card aura) {
        SpellAbility sa = aura.getFirstAttachSpell();
        TargetRestrictions tgt = null;
        if (sa != null) {
            tgt = sa.getTargetRestrictions();
        }

        if (tgt != null) {
            boolean zoneValid = false;
            // check the zone types
            for (final ZoneType zt : tgt.getZone()) {
                if (isInZone(zt)) {
                    zoneValid = true;
                    break;
                }
            }
            if (!zoneValid) {
                return false;
            }

            // check valid
            return isValid(tgt.getValidTgts(), aura.getController(), aura, sa);
        }

        return true;
    }

    @Override
    protected final boolean canBeEquippedBy(final Card equip) {
        return isCreature() && isInPlay();
    }

    @Override
    protected boolean canBeFortifiedBy(final Card fort) {
        return isLand() && isInPlay() && !fort.isLand();
    }

    /* (non-Javadoc)
     * @see forge.game.GameEntity#canBeAttached(forge.game.card.Card, boolean)
     */
    @Override
    public boolean canBeAttached(Card attach, boolean checkSBA) {
        // phase check there
        if (isPhasedOut() && !attach.isPhasedOut()) {
            return false;
        }

        return super.canBeAttached(attach, checkSBA);
    }

    public FCollectionView<ReplacementEffect> getReplacementEffects() {
        return currentState.getReplacementEffects();
    }

    public ReplacementEffect addReplacementEffect(final ReplacementEffect replacementEffect) {
        currentState.addReplacementEffect(replacementEffect);
        return replacementEffect;
    }

    @Deprecated
    public void removeReplacementEffect(ReplacementEffect replacementEffect) {
        currentState.removeReplacementEffect(replacementEffect);
    }

    public void updateReplacementEffects(List<ReplacementEffect> list, CardState state) {
        if (hasRemoveIntrinsic()) {
            list.clear();
        }

        for (final CardTraitChanges ck : changedCardTraits.values()) {
            if (ck.isRemoveAll()) {
                list.clear();
            }
            list.addAll(ck.getReplacements());
        }
        for (KeywordInterface kw : getUnhiddenKeywords(state)) {
            list.addAll(kw.getReplacements());
        }
    }

    public boolean hasReplacementEffect(final ReplacementEffect re) {
        return currentState.hasReplacementEffect(re);
    }
    public boolean hasReplacementEffect(final int id) {
        return currentState.hasReplacementEffect(id);
    }

    public ReplacementEffect getReplacementEffect(final int id) {
        return currentState.getReplacementEffect(id);
    }

    /**
     * Returns what zone this card was cast from (from what zone it was moved to the stack).
     */
    public ZoneType getCastFrom() {
        return castFrom;
    }
    public void setCastFrom(final ZoneType castFrom0) {
        castFrom = castFrom0;
    }

    public SpellAbility getCastSA() {
        return castSA;
    }

    public void setCastSA(SpellAbility castSA) {
        this.castSA = castSA;
    }

    public CardDamageHistory getDamageHistory() {
        return damageHistory;
    }

    public Card getEffectSource() {
        if (effectSourceAbility != null) {
            return effectSourceAbility.getHostCard();
        }
        return effectSource;
    }

    public SpellAbility getEffectSourceAbility() {
        return effectSourceAbility;
    }

    public void setEffectSource(Card src) {
        effectSource = src;
    }

    public void setEffectSource(SpellAbility sa) {
        effectSourceAbility = sa;
    }

    public boolean isStartsGameInPlay() {
        return startsGameInPlay;
    }
    public void setStartsGameInPlay(boolean startsGameInPlay0) {
        startsGameInPlay = startsGameInPlay0;
    }

    public boolean isInPlay() {
        return isInZone(ZoneType.Battlefield);
    }

    public void onCleanupPhase(final Player turn) {
        setDamage(0);
        setHasBeenDealtDeathtouchDamage(false);
        resetPreventNextDamage();
        resetPreventNextDamageWithEffect();
        resetReceivedDamageFromThisTurn();
        resetDealtDamageToThisTurn();
        resetDealtDamageToPlayerThisTurn();
        getDamageHistory().newTurn();
        setRegeneratedThisTurn(0);
        resetShield();
        setBecameTargetThisTurn(false);
        clearMustAttackEntity(turn);
        clearMustBlockCards();
        getDamageHistory().setCreatureAttackedLastTurnOf(turn, getDamageHistory().getCreatureAttackedThisTurn());
        getDamageHistory().setCreatureAttackedThisTurn(false);
        getDamageHistory().setCreatureAttacksThisTurn(0);
        getDamageHistory().setCreatureBlockedThisTurn(false);
        getDamageHistory().setCreatureGotBlockedThisTurn(false);
        clearBlockedByThisTurn();
        clearBlockedThisTurn();
        resetMayPlayTurn();
        resetExtertedThisTurn();
    }

    public boolean hasETBTrigger(final boolean drawbackOnly) {
        for (final Trigger tr : getTriggers()) {
            final Map<String, String> params = tr.getMapParams();
            if (tr.getMode() != TriggerType.ChangesZone) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard") && !params.get("ValidCard").contains("Self")) {
                continue;
            }
            if (drawbackOnly && params.containsKey("Execute")){
                String exec = this.getSVar(params.get("Execute"));
                if (exec.contains("AB$")) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }

    public boolean hasETBReplacement() {
        for (final ReplacementEffect re : getReplacementEffects()) {
            final Map<String, String> params = re.getMapParams();
            if (!(re instanceof ReplaceMoved)) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard") && !params.get("ValidCard").contains("Self")) {
                continue;
            }
            return true;
        }
        return false;
    }

    public int getCMC() {
        return getCMC(SplitCMCMode.CurrentSideCMC);
    }

    public int getCMC(SplitCMCMode mode) {
        if (isToken() && getCopiedPermanent() == null) {
            return 0;
        }
        if (lkiCMC >= 0) {
            return lkiCMC; // a workaround used by getLKICopy
        }

        int xPaid = 0;

        // 2012-07-22 - If a card is on the stack, count the xManaCost in with it's CMC
        if (getGame().getCardsIn(ZoneType.Stack).contains(this) && getManaCost() != null) {
            xPaid = getXManaCostPaid() * getManaCost().countX();
        }

        int requestedCMC = 0;

        if (isSplitCard()) {
            switch(mode) {
                case CurrentSideCMC:
                    // TODO: test if this returns combined CMC for the full face (then get rid of CombinedCMC mode?)
                    requestedCMC = getManaCost().getCMC() + xPaid;
                    break;
                case LeftSplitCMC:
                    requestedCMC = getState(CardStateName.LeftSplit).getManaCost().getCMC() + xPaid;
                    break;
                case RightSplitCMC:
                    requestedCMC = getState(CardStateName.RightSplit).getManaCost().getCMC() + xPaid;
                    break;
                case CombinedCMC:
                    requestedCMC += getState(CardStateName.LeftSplit).getManaCost().getCMC();
                    requestedCMC += getState(CardStateName.RightSplit).getManaCost().getCMC();
                    requestedCMC += xPaid;
                    break;
                default:
                    System.out.println(TextUtil.concatWithSpace("Illegal Split Card CMC mode", mode.toString(),"passed to getCMC!"));
                    break;
            }
        } else if (currentStateName == CardStateName.Transformed) {
            // Except in the cases were we clone the back-side of a DFC.
            requestedCMC = getState(CardStateName.Original).getManaCost().getCMC();
        } else if (currentStateName == CardStateName.Meld) {
            // Melded creatures have a combined CMC of each of their parts
            requestedCMC = getState(CardStateName.Original).getManaCost().getCMC() + this.getMeldedWith().getManaCost().getCMC();
        } else {
            requestedCMC = getManaCost().getCMC() + xPaid;
        }
        return requestedCMC;
    }

    public final void setLKICMC(final int cmc) {
        this.lkiCMC = cmc;
    }

    public final boolean isLKI() {
        return this.lkiCMC >= 0;
    }

    public final boolean canBeSacrificedBy(final SpellAbility source) {
        if (isImmutable()) {
            System.out.println("Trying to sacrifice immutables: " + this);
            return false;
        }
        if (!canBeSacrificed()) {
            return false;
        }

        if (source == null){
            return true;
        }

        if (isCreature() && source.getActivatingPlayer().hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
            Cost srcCost = source.getPayCosts();
            if (srcCost != null) {
                if (srcCost.hasSpecificCostType(CostSacrifice.class)) {
                    return false;
                }
            }
        }

        return getController().canSacrificeBy(source);
    }

    public CardRules getRules() {
        return cardRules;
    }
    public void setRules(CardRules r) {
        cardRules = r;
        currentState.getView().updateRulesText(r, getType());
        currentState.getView().updateOracleText(this);
    }

    public boolean isCommander() {
        if (this.getMeldedWith() != null && this.getMeldedWith().isCommander())
            return true;
        return isCommander;
    }
    public void setCommander(boolean b) {
        if (isCommander == b) { return; }
        isCommander = b;
        view.updateCommander(this);
    }

    public void setSplitStateToPlayAbility(final SpellAbility sa) {
        if (isAdventureCard()) {
            if (sa.isAdventure()) {
                setState(CardStateName.Adventure, true);
            }
            return;
        }
        if (!isSplitCard()) {
            return; // just in case
        }
        // Split card support
        if (sa.isLeftSplit()) {
            setState(CardStateName.LeftSplit, true);
        } else if (sa.isRightSplit()) {
            setState(CardStateName.RightSplit, true);
        }
    }

    // Optional costs paid
    private final EnumSet<OptionalCost> costsPaid = EnumSet.noneOf(OptionalCost.class);
    public void clearOptionalCostsPaid() { costsPaid.clear(); }
    public void addOptionalCostPaid(OptionalCost cost) { costsPaid.add(cost); }
    public Iterable<OptionalCost> getOptionalCostsPaid() { return costsPaid; }
    public boolean isOptionalCostPaid(OptionalCost cost) { return costsPaid.contains(cost); }

    @Override
    public Game getGame() {
        return game;
    }

    public List<SpellAbility> getAllPossibleAbilities(final Player player, final boolean removeUnplayable) {
        // this can only be called by the Human
        final List<SpellAbility> abilities = Lists.newArrayList();
        for (SpellAbility sa : getSpellAbilities()) {
            //add alternative costs as additional spell abilities
            abilities.add(sa);
            abilities.addAll(GameActionUtil.getAlternativeCosts(sa, player));
        }

        if (isFaceDown() && isInZone(ZoneType.Exile)) {
            for (final SpellAbility sa : getState(CardStateName.Original).getSpellAbilities()) {
                abilities.addAll(GameActionUtil.getAlternativeCosts(sa, player));
            }
        }

        final Collection<SpellAbility> toRemove = Lists.newArrayListWithCapacity(abilities.size());
        for (final SpellAbility sa : abilities) {
            sa.setActivatingPlayer(player);
            // fix things like retrace
            // check only if SA can't be cast normally
            if (sa.canPlay(true)) {
                continue;
            }
            if ((removeUnplayable && !sa.canPlay()) || !sa.isPossible()) {
                toRemove.add(sa);
            }
        }
        abilities.removeAll(toRemove);

        if (getState(CardStateName.Original).getType().isLand() && !getLastKnownZone().is(ZoneType.Battlefield)) {
            LandAbility la = new LandAbility(this, player, null);
            if (la.canPlay()) {
                abilities.add(la);
            }

            Card source = this;
            boolean lkicheck = false;

            // if Card is Facedown, need to check if MayPlay still applies
            if (isFaceDown()) {
                lkicheck = true;
                source = CardUtil.getLKICopy(source);
                source.turnFaceUp(false, false);
            }

            if (lkicheck) {
                // double freeze tracker, so it doesn't update view
                game.getTracker().freeze();
                CardCollection preList = new CardCollection(source);
                game.getAction().checkStaticAbilities(false, Sets.newHashSet(source), preList);
            }

            // extra for MayPlay
            for (CardPlayOption o : source.mayPlay(player)) {
                la = new LandAbility(this, player, o.getAbility());
                if (la.canPlay()) {
                    abilities.add(la);
                }
            }

            // reset static abilities
            if (lkicheck) {
                game.getAction().checkStaticAbilities(false);
                // clear delayed changes, this check should not have updated the view
                game.getTracker().clearDelayed();
                // need to unfreeze tracker
                game.getTracker().unfreeze();
            }
        }

        return abilities;
    }

    public static Card fromPaperCard(IPaperCard pc, Player owner) {
        return CardFactory.getCard(pc, owner, owner == null ? null : owner.getGame());
    }
    public static Card fromPaperCard(IPaperCard pc, Player owner, Game game) {
        return CardFactory.getCard(pc, owner, game);
    }

    private static final Map<PaperCard, Card> cp2card = Maps.newHashMap();
    public static Card getCardForUi(IPaperCard pc) {
        if (pc instanceof PaperCard) {
            Card res = cp2card.get(pc);
            if (res == null) {
                res = fromPaperCard(pc, null);
                cp2card.put((PaperCard) pc, res);
            }
            return res;
        }
        return fromPaperCard(pc, null);
    }

    //safe way to get card for ui if card may be null
    public static Card getCardForUi(Card c) {
        if (c == null) { return null; }
        return c.getCardForUi();
    }

    public IPaperCard getPaperCard() {
        IPaperCard cp = paperCard;
        if (cp != null) {
            return cp;
        }

        final String name = getName();
        final String set = getSetCode();

        if (StringUtils.isNotBlank(set)) {
            cp = StaticData.instance().getVariantCards().getCard(name, set);
            return cp == null ? StaticData.instance().getCommonCards().getCard(name, set) : cp;
        }
        cp = StaticData.instance().getVariantCards().getCard(name);
        return cp == null ? StaticData.instance().getCommonCards().getCardFromEdition(name, SetPreference.Latest) : cp;
    }

    /**
     * Update Card instance for the given PaperCard if any
     */
    public static void updateCard(PaperCard pc) {
        Card res = cp2card.get(pc);
        if (res != null) {
            cp2card.put(pc, fromPaperCard(pc, null));
        }
    }

    public List<Object[]> getStaticCommandList() {
        return staticCommandList;
    }

    public void addStaticCommandList(Object[] objects) {
        staticCommandList.add(objects);
    }

    //allow special cards to override this function to return another card for the sake of UI logic
    public Card getCardForUi() {
        return this;
    }

    public String getOracleText() {
        CardRules rules = cardRules;
        if (copiedPermanent != null) { //return oracle text of copied permanent if applicable
            rules = copiedPermanent.getRules();
        }
        return rules != null ? rules.getOracleText() : oracleText;
    }
    public void setOracleText(final String oracleText0) {
        oracleText = oracleText0;
        currentState.getView().updateOracleText(this);
    }

    @Override
    public CardView getView() {
        return view;
    }

    // Counts number of instances of a given keyword.
    private static final class CountKeywordVisitor extends Visitor<KeywordInterface> {
        private String keyword;
        private int count;
        private boolean startOf;

        private CountKeywordVisitor(String keyword) {
            this.keyword = keyword;
            this.count = 0;
            this.startOf = false;
        }

        private CountKeywordVisitor(String keyword, boolean startOf) {
            this(keyword);
            this.startOf = startOf;
        }

        @Override
        public boolean visit(KeywordInterface inst) {
            final String kw = inst.getOriginal();
            if ((startOf && kw.startsWith(keyword)) || kw.equals(keyword)) {
                count++;
            }
            return true;
        }

        public int getCount() {
            return count;
        }
    }

    private static final class HasKeywordVisitor extends Visitor<KeywordInterface> {
        private String keyword;
        private final MutableBoolean result = new MutableBoolean(false);

        private boolean startOf;
        private HasKeywordVisitor(String keyword, boolean startOf) {
            this.keyword = keyword;
            this.startOf = startOf;
        }

        @Override
        public boolean visit(KeywordInterface inst) {
            final String kw = inst.getOriginal();
            if ((startOf && kw.startsWith(keyword)) || kw.equals(keyword)) {
                result.setTrue();
            }
            return result.isFalse();
        }

        public boolean getResult() {
            return result.isTrue();
        }
    }

    // Collects all the keywords into a list.
    private static final class ListKeywordVisitor extends Visitor<KeywordInterface> {
        private List<KeywordInterface> keywords = Lists.newArrayList();

        @Override
        public boolean visit(KeywordInterface kw) {
            keywords.add(kw);
            return true;
        }

        public List<KeywordInterface> getKeywords() {
            return keywords;
        }
    }

    public void setChangedCardTypes(Map<Long, CardChangedType> changedCardTypes) {
        this.changedCardTypes.clear();
        for (Entry<Long, CardChangedType> entry : changedCardTypes.entrySet()) {
            this.changedCardTypes.put(entry.getKey(), entry.getValue());
        }
    }

    public void setChangedCardKeywords(Map<Long, KeywordsChange> changedCardKeywords) {
        this.changedCardKeywords.clear();
        for (Entry<Long, KeywordsChange> entry : changedCardKeywords.entrySet()) {
            this.changedCardKeywords.put(entry.getKey(), entry.getValue().copy(this, true));
        }
    }

    public void setChangedCardColors(Map<Long, CardColor> changedCardColors) {
        this.changedCardColors.clear();
        for (Entry<Long, CardColor> entry : changedCardColors.entrySet()) {
            this.changedCardColors.put(entry.getKey(), entry.getValue());
        }
    }

    public void ceaseToExist() {
        getGame().getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        getZone().remove(this);
        getGame().getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
    }

    public void forceTurnFaceUp() {
        getGame().getTriggerHandler().suppressMode(TriggerType.TurnFaceUp);
        turnFaceUp(false, false);
        getGame().getTriggerHandler().clearSuppression(TriggerType.TurnFaceUp);
    }

    public final void addGoad(Long timestamp, final Player p) {
        goad.put(timestamp, p);
        updateAbilityTextForView();
    }

    public final void removeGoad(Long timestamp) {
        if (goad.remove(timestamp) != null) {
            updateAbilityTextForView();
        }
    }

    public final boolean isGoaded() {
        return !goad.isEmpty();
    }

    public final boolean isGoadedBy(final Player p) {
        return goad.containsValue(p);
    }

    public final Collection<Player> getGoaded() {
        return goad.values();
    }

    /**
     * Returns the last known zone information for the card. If the card is a LKI copy of another card,
     * then it stores the relevant information in savedLastKnownZone, which is returned. If the card is
     * not a LKI copy (e.g. an ordinary card in the game), it does not have this information and then
     * the last known zone is assumed to be the current zone the card is currently in.
     * @return last known zone of the card (either LKI, if present, or the current zone).
     */
    public final Zone getLastKnownZone() {
        return this.savedLastKnownZone != null ? this.savedLastKnownZone : getZone();
    }

    /**
     * Sets the last known zone information for the card. Should only be used by LKI copies of cards
     * obtained via CardUtil::getLKICopy. Otherwise should be null, which means that current zone the
     * card is in is the last known zone.
     * @param zone last known zone information for the card.
     */
    public final void setLastKnownZone(Zone zone) {
        this.savedLastKnownZone = zone;
    }

    /**
     * ETBCounters are only used between replacementEffects
     * and when the Card really enters the Battlefield with the counters
     * @return map of counters
     */
    public final void addEtbCounter(CounterType type, Integer val, final Player source) {
        int old = etbCounters.contains(source, type) ? etbCounters.get(source, type) : 0;
        etbCounters.put(source, type, old + val);
    }

    public final void clearEtbCounters() {
        etbCounters.clear();
    }

    public final Set<Table.Cell<Player, CounterType, Integer>> getEtbCounters() {
        return etbCounters.cellSet();
    }

    public final boolean putEtbCounters(GameEntityCounterTable table) {
        boolean changed = false;
        for (Table.Cell<Player, CounterType, Integer> e : etbCounters.cellSet()) {
            CounterType ct = e.getColumnKey();
            if (this.isLKI()) {
                if (canReceiveCounters(ct)) {
                    setCounters(ct, getCounters(ct) + e.getValue());
                    changed = true;
                }
            } else {
                changed |= addCounter(ct, e.getValue(), e.getRowKey(), true, table) > 0;
            }
        }
        return changed;
    }

    public final void clearTemporaryVars() {
        // Add cleanup for all variables that are set temporarily but that need
        // to be restored to their original value if a card changes zones

        removeSVar("PayX"); // Temporary AI X announcement variable
        removeSVar("IsCastFromPlayEffect"); // Temporary SVar indicating that the spell is cast indirectly via AF Play
        setSunburstValue(0); // Sunburst
        setXManaCostPaidByColor(null);
        setKickerMagnitude(0);
        setPseudoMultiKickerMagnitude(0);
    }

    public final int getFinalChapterNr() {
        int n = 0;
        for (final Trigger t : getTriggers()) {
            SpellAbility sa = t.getOverridingAbility();
            if (sa != null && sa.isChapter()) {
                n = Math.max(n, sa.getChapter());
            }
        }
        return n;
    }

    public boolean withFlash(Player p) {
        if (hasKeyword(Keyword.FLASH)) {
            return true;
        }
        return withFlash.containsValue(p);
    }

    public void addWithFlash(Long timestamp, Iterable<Player> players) {
        withFlash.putAll(timestamp, players);
    }

    public void removeWithFlash(Long timestamp) {
        withFlash.removeAll(timestamp);
    }

    public boolean canBeDiscardedBy(SpellAbility sa) {
        if (!isInZone(ZoneType.Hand)) {
            return false;
        }

        return getOwner().canDiscardBy(sa);
    }

    public void addAbilityActivated(SpellAbility ability) {
        SpellAbility original = ability.getOriginalAbility();
        if (original == null) {
            original = ability;
        }

        int turnActivated = getAbilityActivatedThisTurn(ability);
        int gameActivated = getAbilityActivatedThisGame(ability);
        if (ability.getGrantorStatic() != null) {
            numberTurnActivationsStatic.put(original, ability.getGrantorStatic(), turnActivated + 1);
            numberGameActivationsStatic.put(original, ability.getGrantorStatic(), gameActivated + 1);
        } else {
            numberTurnActivations.put(original, turnActivated + 1);
            numberGameActivations.put(original, gameActivated + 1);
        }

        if (ability.isPwAbility()) {
            addPlaneswalkerAbilityActivated();
        }
    }

    public int getAbilityActivatedThisTurn(SpellAbility ability) {
        SpellAbility original = ability.getOriginalAbility();
        if (original == null) {
            original = ability;
        }

        if (ability.getGrantorStatic() != null) {
            if (numberTurnActivationsStatic.contains(original, ability.getGrantorStatic())) {
                return numberTurnActivationsStatic.get(original, ability.getGrantorStatic());
            }
            return 0;
        }
        return numberTurnActivations.containsKey(original) ? numberTurnActivations.get(original) : 0;
    }

    public int getAbilityActivatedThisGame(SpellAbility ability) {
        SpellAbility original = ability.getOriginalAbility();
        if (original == null) {
            original = ability;
        }

        if (ability.getGrantorStatic() != null) {
            if (numberGameActivationsStatic.contains(original, ability.getGrantorStatic())) {
                return numberGameActivationsStatic.get(original, ability.getGrantorStatic());
            }
            return 0;
        }
        return numberGameActivations.containsKey(original) ? numberGameActivations.get(original) : 0;
    }

    public void resetTurnActivations() {
        numberTurnActivations.clear();
        numberTurnActivationsStatic.clear();
    }

    public int getPlaneswalkerAbilityActivated() {
        return planeswalkerAbilityActivated;
    }

    public void addPlaneswalkerAbilityActivated() {
        planeswalkerAbilityActivated++;
    }

    public void resetActivationsPerTurn() {
        planeswalkerAbilityActivated = 0;
        numberTurnActivations.clear();
        numberTurnActivationsStatic.clear();
    }

    public void addCanBlockAdditional(int n, long timestamp) {
        if (n <= 0) {
            return;
        }
        canBlockAdditional.put(timestamp, n);
        getView().updateBlockAdditional(this);
    }

    public boolean removeCanBlockAdditional(long timestamp) {
        boolean result = canBlockAdditional.remove(timestamp) != null;
        if (result) {
            getView().updateBlockAdditional(this);
        }
        return result;
    }

    public int canBlockAdditional() {
        int result = 0;
        for (Integer v : canBlockAdditional.values()) {
            result += v;
        }
        return result;
    }

    public void addCanBlockAny(long timestamp) {
        canBlockAny.add(timestamp);
        getView().updateBlockAdditional(this);
    }

    public boolean removeCanBlockAny(long timestamp) {
        boolean result = canBlockAny.remove(timestamp);
        if (result) {
            getView().updateBlockAdditional(this);
        }
        return result;
    }

    public boolean canBlockAny() {
        return !canBlockAny.isEmpty();
    }

    public boolean removeChangedState() {
        boolean updateState = false;
        updateState |= removeCloneStates();
        updateState |= removeTextChangeStates();

        updateState |= clearChangedCardTypes();
        updateState |= clearChangedCardKeywords();
        updateState |= clearChangedCardColors();
        updateState |= clearChangedCardTraits();

        newPT.clear();
        newPTCharacterDefining.clear();

        clearEtbCounters();

        return updateState;
    }
}
