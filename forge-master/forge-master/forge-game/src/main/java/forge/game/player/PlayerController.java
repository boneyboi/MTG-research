package forge.game.player;

import com.google.common.base.Predicate;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import forge.LobbyPlayer;
import forge.card.ColorSet;
import forge.card.ICardFace;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.GameOutcome.AnteResult;
import forge.game.GameType;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.*;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.ITriggerEvent;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 */
public abstract class PlayerController {

    public enum ManaPaymentPurpose {
        DeclareAttacker,
        DeclareBlocker,
        Echo,
        Multikicker,
        CumulativeUpkeep,
    }

    public enum BinaryChoiceType {
        HeadsOrTails, // coin
        TapOrUntap,
        PlayOrDraw,
        OddsOrEvens,
        UntapOrLeaveTapped,
        UntapTimeVault,
        LeftOrRight,
        AddOrRemove,
    }

    protected final Game game;

    protected final Player player;
    protected final LobbyPlayer lobbyPlayer;

    public PlayerController(Game game0, Player p, LobbyPlayer lp) {
        game = game0;
        player = p;
        lobbyPlayer = lp;
    }

    public boolean isAI() {
        return false;
    }

    public Game getGame() { return game; }
    public Player getPlayer() { return player; }
    public LobbyPlayer getLobbyPlayer() { return lobbyPlayer; }

    public void tempShowCards(final Iterable<Card> cards) { } // show cards in UI until ended
    public void endTempShowCards() { }

    public final SpellAbility getAbilityToPlay(final Card hostCard, final List<SpellAbility> abilities) { return getAbilityToPlay(hostCard, abilities, null); }
    public abstract SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent);

    //public abstract void playFromSuspend(Card c);
    public abstract void playSpellAbilityForFree(SpellAbility copySA, boolean mayChoseNewTargets);
    public abstract void playSpellAbilityNoStack(SpellAbility effectSA, boolean mayChoseNewTargets);

    public abstract List<PaperCard> sideboard(final Deck deck, GameType gameType, String message);
    public abstract List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses);

    public abstract Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers, int damageDealt, GameEntity defender, boolean overrideOrder);

    public abstract Integer announceRequirements(SpellAbility ability, String announce, boolean allowZero);
    public abstract CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message);
    public abstract CardCollectionView choosePermanentsToDestroy(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message);
    public abstract TargetChoices chooseNewTargetsFor(SpellAbility ability);
    public abstract boolean chooseTargetsFor(SpellAbility currentAbility); // this is bad a function for it assigns targets to sa inside its body 

    // Specify a target of a spell (Spellskite)
    public abstract Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility sa, List<Pair<SpellAbilityStackInstance, GameObject>> allTargets);

    // Q: why is there min/max and optional at once? A: This is to handle cases like 'choose 3 to 5 cards or none at all'  
    public abstract CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional);
    
    public final <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, SpellAbility sa, String title) { return chooseSingleEntityForEffect(optionList, null, sa, title, false, null); }
    public final <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, SpellAbility sa, String title, boolean isOptional) { return chooseSingleEntityForEffect(optionList, null, sa, title, isOptional, null); } 
    public abstract <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player relatedPlayer);
    public abstract SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, String title,
            Map<String, Object> params);

    public abstract <T extends GameEntity> List<T> chooseEntitiesForEffect(FCollectionView<T> optionList, int min, int max, DelayedReveal delayedReveal, SpellAbility sa, String title, Player relatedPlayer);

    public abstract boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message);
    public abstract boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode bidlife, String string, int bid, Player winner);
    public abstract boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message);
    public abstract boolean confirmTrigger(WrappedAbility sa, Map<String, String> triggerParams, boolean isMandatory);
    public abstract Player chooseStartingPlayer(boolean isFirstGame);

    public abstract CardCollection orderBlockers(Card attacker, CardCollection blockers);
    public abstract List<Card> exertAttackers(List<Card> attackers);
    /**
     * Add a card to a pre-existing blocking order.
     * @param attacker the attacking creature.
     * @param blocker the new blocker.
     * @param oldBlockers the creatures already blocking the attacker (in order).
     * @return The new order of creatures blocking the attacker.
     */
    public abstract CardCollection orderBlocker(final Card attacker, final Card blocker, final CardCollection oldBlockers);
    public abstract CardCollection orderAttackers(Card blocker, CardCollection attackers);

    /** Shows the card to this player*/
    public final void reveal(CardCollectionView cards, ZoneType zone, Player owner) {
        reveal(cards, zone, owner, null);
    }
    public abstract void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix);
    public abstract void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix);

    /** Shows message to player to reveal chosen cardName, creatureType, number etc. AI must analyze API to understand what that is */
    public abstract void notifyOfValue(SpellAbility saSource, GameObject realtedTarget, String value);
    public abstract ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN);
    public abstract ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(CardCollection topN);

    public abstract boolean willPutCardOnTop(Card c);
    public final CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone) {
        return orderMoveToZoneList(cards, destinationZone, null);
    }
    public abstract CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, SpellAbility source);

    /** p = target player, validCards - possible discards, min cards to discard */
    public abstract CardCollectionView chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa, CardCollection validCards, int min, int max);

    public abstract CardCollectionView chooseCardsToDelve(int genericAmount, CardCollection grave);
    public abstract CardCollectionView chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid);
    public abstract CardCollectionView chooseCardsToDiscardUnlessType(int min, CardCollectionView hand, String param, SpellAbility sa);
    public abstract List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand);
    public abstract Mana chooseManaFromPool(List<Mana> manaChoices);

    public abstract String chooseSomeType(String kindOfType, SpellAbility sa, Collection<String> validTypes, List<String> invalidTypes, boolean isOptional);
    public final String chooseSomeType(String kindOfType, SpellAbility sa, Collection<String> validTypes, List<String> invalidTypes) {
        return chooseSomeType(kindOfType, sa, validTypes, invalidTypes, false);
    }

    public abstract Object vote(SpellAbility sa, String prompt, List<Object> options, ListMultimap<Object, Player> votes, Player forPlayer);
    public abstract boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, String question);

    public abstract CardCollectionView getCardsToMulligan(Player firstPlayer);
    public abstract boolean mulliganKeepHand(Player player, int cardsToReturn);
    public abstract CardCollectionView londonMulliganReturnCards(Player mulliganingPlayer, int cardsToReturn);

    public abstract void declareAttackers(Player attacker, Combat combat);
    public abstract void declareBlockers(Player defender, Combat combat);
    public abstract List<SpellAbility> chooseSpellAbilityToPlay();
    public abstract void playChosenSpellAbility(SpellAbility sa);

    public abstract CardCollection chooseCardsToDiscardToMaximumHandSize(int numDiscard);
    public abstract boolean payManaOptional(Card card, Cost cost, SpellAbility sa, String prompt, ManaPaymentPurpose purpose);

    public abstract int chooseNumberForKeywordCost(SpellAbility sa, Cost cost, KeywordInterface keyword, String prompt, int max);
    public boolean addKeywordCost(SpellAbility sa, Cost cost, KeywordInterface keyword, String prompt) {
        return chooseNumberForKeywordCost(sa, cost, keyword, prompt, 1) == 1;
    }

    public abstract int chooseNumber(SpellAbility sa, String title, int min, int max);
    public abstract int chooseNumber(SpellAbility sa, String title, List<Integer> values, Player relatedPlayer);
    public int chooseNumber(SpellAbility sa, String string, int min, int max, Map<String, Object> params) {
        return chooseNumber(sa, string, min, max);
    }

    public final boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice) { return chooseBinary(sa, question, kindOfChoice, (Boolean) null); }
    public abstract boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultChioce);
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Map<String, Object> params)  { return chooseBinary(sa, question, kindOfChoice); }
    
    public abstract boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call);
    public abstract Card chooseProtectionShield(GameEntity entityBeingDamaged, List<String> options, Map<String, Card> choiceMap);

    public abstract List<AbilitySub> chooseModeForAbility(SpellAbility sa, int min, int num, boolean allowRepeat);

    public abstract byte chooseColor(String message, SpellAbility sa, ColorSet colors);
    public abstract byte chooseColorAllowColorless(String message, Card c, ColorSet colors);

    public abstract ICardFace chooseSingleCardFace(SpellAbility sa, String message, Predicate<ICardFace> cpp, String name);
    public abstract List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options);

    public abstract CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, String prompt,
            Map<String, Object> params);

    public abstract boolean confirmPayment(CostPart costPart, String string, SpellAbility sa);
    public abstract ReplacementEffect chooseSingleReplacementEffect(String prompt, List<ReplacementEffect> possibleReplacers);
    public abstract String chooseProtectionType(String string, SpellAbility sa, List<String> choices);

    // these 4 need some refining.
    public abstract boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, FCollectionView<Player> allPayers);
    public abstract void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs);
    public abstract void playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory);

    public abstract boolean playSaFromPlayEffect(SpellAbility tgtSA);
    public abstract boolean chooseCardsPile(SpellAbility sa, CardCollectionView pile1, CardCollectionView pile2, String faceUp);

    public abstract void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards);

    // These 2 are for AI
    public CardCollectionView cheatShuffle(CardCollectionView list) { return list; }
    public Collection<? extends PaperCard> complainCardsCantPlayWell(Deck myDeck) { return null; }

    public abstract void resetAtEndOfTurn(); // currently used by the AI to perform card memory cleanup

    public final boolean payManaCost(CostPartMana costPartMana, SpellAbility sa, String prompt, boolean isActivatedAbility) {
        return payManaCost(costPartMana, sa, prompt, null, isActivatedAbility);
    }

    public final boolean payManaCost(CostPartMana costPartMana, SpellAbility sa, String prompt, ManaConversionMatrix matrix, boolean isActivatedAbility) {
        return payManaCost(costPartMana.getManaCostFor(sa), costPartMana, sa, prompt, matrix, isActivatedAbility);
    }
    public abstract boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt, ManaConversionMatrix matrix, boolean isActivatedAbility);

    public abstract Map<Card, ManaCostShard> chooseCardsForConvokeOrImprovise(SpellAbility sa, ManaCost manaCost, CardCollectionView untappedCards, boolean improvise);

    public abstract List<Card> chooseCardsForSplice(SpellAbility sa, List<Card> cards);

    public abstract String chooseCardName(SpellAbility sa, Predicate<ICardFace> cpp, String valid, String message);

    public abstract String chooseCardName(SpellAbility sa, List<ICardFace> faces, String message);
    // better to have this odd method than those if playerType comparison in ChangeZone  
    public abstract Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional, Player decider);

    public abstract List<Card> chooseCardsForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, int min, int max, DelayedReveal delayedReveal, String selectPrompt, Player decider);

    public abstract void autoPassCancel();

    public abstract void awaitNextInput();

    public abstract void cancelAwaitNextInput();

    public boolean isGuiPlayer() {
        return false;
    }

    public boolean canPlayUnlimitedLands() {
        return false;
    }

    public AnteResult getAnteResult() {
        return game.getOutcome().anteResult.get(player.getRegisteredPlayer());
    }

    public abstract List<OptionalCostValue> chooseOptionalCosts(SpellAbility choosen, List<OptionalCostValue> optionalCostValues);

    public abstract boolean confirmMulliganScry(final Player p);

    public abstract CardCollection chooseCardsForEffectMultiple(Map<String, CardCollection> validMap,
            SpellAbility sa, String title, boolean isOptional);
}
