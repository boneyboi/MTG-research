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
package forge.game;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import forge.card.CardRarity;
import forge.card.CardStateName;
import forge.card.CardType.Supertype;
import forge.game.ability.AbilityKey;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.event.Event;
import forge.game.event.GameEventGameOutcome;
import forge.game.phase.Phase;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.phase.Untap;
import forge.game.player.*;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.CostPaymentStack;
import forge.game.zone.MagicStack;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.trackable.Tracker;
import forge.util.Aggregates;
import forge.util.MyRandom;
import forge.util.Visitor;

import java.util.*;

/**
 * Represents the state of a <i>single game</i>, a new instance is created for each game.
 */
public class Game {
    private final GameRules rules;
    private final PlayerCollection allPlayers = new PlayerCollection();
    private final PlayerCollection ingamePlayers = new PlayerCollection();
    private final PlayerCollection lostPlayers = new PlayerCollection();

    private List<Card> activePlanes = null;

    public final Phase cleanup;
    public final Phase endOfCombat;
    public final Phase endOfTurn;
    public final Untap untap;
    public final Phase upkeep;
    public final MagicStack stack;
    public final CostPaymentStack costPaymentStack = new CostPaymentStack();
    private final PhaseHandler phaseHandler;
    private final StaticEffects staticEffects = new StaticEffects();
    private final TriggerHandler triggerHandler = new TriggerHandler(this);
    private final ReplacementHandler replacementHandler = new ReplacementHandler(this);
    private final EventBus events = new EventBus("game events");
    private final GameLog gameLog = new GameLog();

    private final Zone stackZone = new Zone(ZoneType.Stack, this);

    private CardCollection lastStateBattlefield = new CardCollection();
    private CardCollection lastStateGraveyard = new CardCollection();

    private Map<Player, PlayerCollection> attackedThisTurn = Maps.newHashMap();
    private Map<Player, PlayerCollection> attackedLastTurn = Maps.newHashMap();

    private Player monarch = null;
    private Player monarchBeginTurn = null;

    private Direction turnOrder = Direction.getDefaultDirection();

    private long timestamp = 0;
    public final GameAction action;
    private final Match match;
    private GameStage age = GameStage.BeforeMulligan;
    private GameOutcome outcome;

    private final GameView view;
    private final Tracker tracker = new Tracker();

    public Player getMonarch() {
        return monarch;
    }

    public void setMonarch(final Player p) {
        monarch = p;
    }

    public Player getMonarchBeginTurn() {
        return monarchBeginTurn;
    }

    public void setMonarchBeginTurn(Player monarchBeginTurn) {
        this.monarchBeginTurn = monarchBeginTurn;
    }

    public Map<Player, PlayerCollection> getPlayersAttackedThisTurn() {
        return attackedThisTurn;
    }

    public Map<Player, PlayerCollection> getPlayersAttackedLastTurn() {
        return attackedLastTurn;
    }

    public void addPlayerAttackedThisTurn(Player attacker, Player defender) {
        PlayerCollection atk = attackedThisTurn.get(attacker);
        if (atk == null) {
            attackedThisTurn.put(attacker, new PlayerCollection());
        }
        attackedThisTurn.get(attacker).add(defender);
    }

    public void resetPlayersAttackedOnNextTurn() {
        attackedLastTurn.clear();
        attackedLastTurn.putAll(attackedThisTurn);
        attackedThisTurn.clear();
    }

    public CardCollectionView getLastStateBattlefield() {
        return lastStateBattlefield;
    }
    public CardCollectionView getLastStateGraveyard() {
        return lastStateGraveyard;
    }

    public void copyLastState() {
        lastStateBattlefield.clear();
        lastStateGraveyard.clear();
        for (final Player p : getPlayers()) {
            lastStateBattlefield.addAll(p.getZone(ZoneType.Battlefield).getLKICopy());
            lastStateGraveyard.addAll(p.getZone(ZoneType.Graveyard).getLKICopy());
        }
    }

    public void updateLastStateForCard(Card c) {
        if (c == null || c.getZone() == null) {
            return;
        }

        ZoneType zone = c.getZone().getZoneType();
        CardCollection lookup = zone.equals(ZoneType.Battlefield) ? lastStateBattlefield
                : zone.equals(ZoneType.Graveyard) ? lastStateGraveyard
                : null;

        if (lookup != null && lookup.remove(c)) {
            lookup.add(CardUtil.getLKICopy(c));
        }
    }

    private final GameEntityCache<Player, PlayerView> playerCache = new GameEntityCache<>();
    public Player getPlayer(PlayerView playerView) {
        return playerCache.get(playerView);
    }
    public void addPlayer(int id, Player player) {
        playerCache.put(Integer.valueOf(id), player);
    }

    // methods that deal with saving, retrieving and clearing LKI information about cards on zone change
    private final HashMap<Integer, Card> changeZoneLKIInfo = new HashMap<>();
    public final void addChangeZoneLKIInfo(Card c) {
        if (c == null) {
            return;
        }
        changeZoneLKIInfo.put(c.getId(), CardUtil.getLKICopy(c));
    }
    public final Card getChangeZoneLKIInfo(Card c) {
        if (c == null) {
            return null;
        }
        return changeZoneLKIInfo.containsKey(c.getId()) ? changeZoneLKIInfo.get(c.getId()) : c;
    }
    public final void clearChangeZoneLKIInfo() {
        changeZoneLKIInfo.clear();
    }

    public Game(List<RegisteredPlayer> players0, GameRules rules0, Match match0) { /* no more zones to map here */
        rules = rules0;
        match = match0;

        int highestTeam = -1;
        for (RegisteredPlayer psc : players0) {
            // Track highest team number for auto assigning unassigned teams
            int teamNum = psc.getTeamNumber();
            if (teamNum > highestTeam) {
                highestTeam = teamNum;
            }
        }

        int plId = 0;
        for (RegisteredPlayer psc : players0) {
            IGameEntitiesFactory factory = (IGameEntitiesFactory)psc.getPlayer();
            Player pl = factory.createIngamePlayer(this, plId++);
            allPlayers.add(pl);
            ingamePlayers.add(pl);

            pl.setStartingLife(psc.getStartingLife());
            pl.setMaxHandSize(psc.getStartingHand());
            pl.setStartingHandSize(psc.getStartingHand());

            int teamNum = psc.getTeamNumber();
            if (teamNum == -1) {
                // RegisteredPlayer doesn't have an assigned team, set it to 1 higher than the highest found team number
                teamNum = ++highestTeam;
                psc.setTeamNumber(teamNum);
            }

            pl.setTeam(teamNum);
        }

        action = new GameAction(this);
        stack = new MagicStack(this);
        phaseHandler = new PhaseHandler(this);

        untap = new Untap(this);
        upkeep = new Phase(PhaseType.UPKEEP);
        cleanup = new Phase(PhaseType.CLEANUP);
        endOfCombat = new Phase(PhaseType.COMBAT_END);
        endOfTurn = new Phase(PhaseType.END_OF_TURN);

        view = new GameView(this);

        subscribeToEvents(gameLog.getEventVisitor());
    }

    public GameView getView() {
        return view;
    }

    public Tracker getTracker() {
        return tracker;
    }

    /**
     * Gets the players who are still fighting to win.
     */
    public final PlayerCollection getPlayers() {
        return ingamePlayers;
    }

    public final PlayerCollection getLostPlayers() {
        return lostPlayers;
    }

    /**
     * Gets the players who are still fighting to win, in turn order.
     */
    public final PlayerCollection getPlayersInTurnOrder() {
        if (turnOrder.isDefaultDirection()) {
            return ingamePlayers;
        }
        final PlayerCollection players = new PlayerCollection(ingamePlayers);
        Collections.reverse(players);
        return players;
    }

    /**
     * Gets the nonactive players who are still fighting to win, in turn order.
     */
    public final PlayerCollection getNonactivePlayers() {
        // Don't use getPlayersInTurnOrder to prevent copying the player collection twice
        final PlayerCollection players = new PlayerCollection(ingamePlayers);
        players.remove(phaseHandler.getPlayerTurn());
        if (!turnOrder.isDefaultDirection()) {
            Collections.reverse(players);
        }
        return players;
    }

    /**
     * Gets the players who participated in match (regardless of outcome).
     * <i>Use this in UI and after match calculations</i>
     */
    public final PlayerCollection getRegisteredPlayers() {
        return allPlayers;
    }

    public final Untap getUntap() {
        return untap;
    }
    public final Phase getUpkeep() {
        return upkeep;
    }
    public final Phase getEndOfCombat() {
        return endOfCombat;
    }
    public final Phase getEndOfTurn() {
        return endOfTurn;
    }
    public final Phase getCleanup() {
        return cleanup;
    }

    public final PhaseHandler getPhaseHandler() {
        return phaseHandler;
    }
    public final void updateTurnForView() {
        view.updateTurn(phaseHandler);
    }
    public final void updatePhaseForView() {
        view.updatePhase(phaseHandler);
    }
    public final void updatePlayerTurnForView() {
        view.updatePlayerTurn(phaseHandler);
    }

    public final MagicStack getStack() {
        return stack;
    }
    public final void updateStackForView() {
        view.updateStack(stack);
    }

    public final StaticEffects getStaticEffects() {
        return staticEffects;
    }

    public final TriggerHandler getTriggerHandler() {
        return triggerHandler;
    }

    public final Combat getCombat() {
        return getPhaseHandler().getCombat();
    }
    public final void updateCombatForView() {
        view.updateCombat(getCombat());
    }

    public final GameLog getGameLog() {
        return gameLog;
    }
    public final void updateGameLogForView() {
        view.updateGameLog(gameLog);
    }

    public final Zone getStackZone() {
        return stackZone;
    }

    public CardCollectionView getCardsPlayerCanActivateInStack() {
        return CardLists.filter(stackZone.getCards(), new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                for (final SpellAbility sa : c.getSpellAbilities()) {
                    final ZoneType restrictZone = sa.getRestrictions().getZone();
                    if (ZoneType.Stack == restrictZone) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    /**
     * The Direction in which the turn order of this Game currently proceeds.
     */
    public final Direction getTurnOrder() {
    	return turnOrder;
    }
    public final void reverseTurnOrder() {
    	turnOrder = turnOrder.getOtherDirection();
    }
    public final void resetTurnOrder() {
    	turnOrder = Direction.getDefaultDirection();
    }

    /**
     * Create and return the next timestamp.
     */
    public final long getNextTimestamp() {
        timestamp = getTimestamp() + 1;
        return getTimestamp();
    }
    public final long getTimestamp() {
        return timestamp;
    }

    public final GameOutcome getOutcome() {
        return outcome;
    }

    public ReplacementHandler getReplacementHandler() {
        return replacementHandler;
    }

    public synchronized boolean isGameOver() {
        return age == GameStage.GameOver;
    }

    public synchronized void setGameOver(GameEndReason reason) {
        age = GameStage.GameOver;
        for (Player p : allPlayers) {
            p.setMindSlaveMaster(null); // for correct totals
        }

        for (Player p : getPlayers()) {
            p.onGameOver();
        }

        final GameOutcome result = new GameOutcome(reason, getRegisteredPlayers());
        result.setTurnsPlayed(getPhaseHandler().getTurn());

        outcome = result;
        match.addGamePlayed(this);

        view.updateGameOver(this);

        // The log shall listen to events and generate text internally
        fireEvent(new GameEventGameOutcome(result, match.getPlayedGames()));
    }

    public Zone getZoneOf(final Card card) {
        return card.getLastKnownZone();
    }

    public synchronized CardCollectionView getCardsIn(final ZoneType zone) {
        if (zone == ZoneType.Stack) {
            return getStackZone().getCards();
        }
        return getPlayers().getCardsIn(zone);
    }

    public CardCollectionView getCardsIncludePhasingIn(final ZoneType zone) {
        if (zone == ZoneType.Stack) {
            return getStackZone().getCards();
        }
        else {
            CardCollection cards = new CardCollection();
            for (final Player p : getPlayers()) {
                cards.addAll(p.getCardsIncludePhasingIn(zone));
            }
            return cards;
        }
    }

    public CardCollectionView getCardsIn(final Iterable<ZoneType> zones) {
        CardCollection cards = new CardCollection();
        for (final ZoneType z : zones) {
            cards.addAll(getCardsIn(z));
        }
        return cards;
    }

    public boolean isCardExiled(final Card c) {
        return getCardsIn(ZoneType.Exile).contains(c);
    }

    public boolean isCardInPlay(final String cardName) {
        for (final Player p : getPlayers()) {
            if (p.isCardInPlay(cardName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCardInCommand(final String cardName) {
        for (final Player p : getPlayers()) {
            if (p.isCardInCommand(cardName)) {
                return true;
            }
        }
        return false;
    }

    public CardCollectionView getColoredCardsInPlay(final String color) {
        final CardCollection cards = new CardCollection();
        for (Player p : getPlayers()) {
            cards.addAll(p.getColoredCardsInPlay(color));
        }
        return cards;
    }

    private static class CardStateVisitor extends Visitor<Card> {
        Card found = null;
        Card old = null;

        private CardStateVisitor(final Card card) {
            this.old = card;
        }

        @Override
        public boolean visit(Card object) {
            if (object.equals(old)) {
                found = object;
            }
            return found == null;
        }

        public Card getFound(final Card notFound) {
            return found == null ? notFound : found;
        }
    }

    public Card getCardState(final Card card) {
        return getCardState(card, card);
    }

    public Card getCardState(final Card card, final Card notFound) {
        CardStateVisitor visit = new CardStateVisitor(card);
        this.forEachCardInGame(visit);
        return visit.getFound(notFound);
    }

    private static class CardIdVisitor extends Visitor<Card> {
        Card found = null;
        int id;

        private CardIdVisitor(final int id) {
            this.id = id;
        }

        @Override
        public boolean visit(Card object) {
            if (this.id == object.getId()) {
                found = object;
            }
            return found == null;
        }

        public Card getFound() {
            return found;
        }
    }

    public Card findById(int id) {
        CardIdVisitor visit = new CardIdVisitor(id);
        this.forEachCardInGame(visit);
        return visit.getFound();
    }

    // Allows visiting cards in game without allocating a temporary list.
    public void forEachCardInGame(Visitor<Card> visitor) {
        for (final Player player : getPlayers()) {
            if (!visitor.visitAll(player.getZone(ZoneType.Graveyard).getCards())) {
                return;
            }
            if (!visitor.visitAll(player.getZone(ZoneType.Hand).getCards())) {
                return;
            }
            if (!visitor.visitAll(player.getZone(ZoneType.Library).getCards())) {
                return;
            }
            if (!visitor.visitAll(player.getZone(ZoneType.Battlefield).getCards(false))) {
                return;
            }
            if (!visitor.visitAll(player.getZone(ZoneType.Exile).getCards())) {
                return;
            }
            if (!visitor.visitAll(player.getZone(ZoneType.Command).getCards())) {
                return;
            }
            if (!visitor.visitAll(player.getInboundTokens())) {
                return;
            }
        }
        visitor.visitAll(getStackZone().getCards());
    }
    public CardCollectionView getCardsInGame() {
        final CardCollection all = new CardCollection();
        Visitor<Card> visitor = new Visitor<Card>() {
            @Override
            public boolean visit(Card card) {
                all.add(card);
                return true;
            }
        };
        forEachCardInGame(visitor);
        return all;
    }

    public final GameAction getAction() {
        return action;
    }

    public final Match getMatch() {
        return match;
    }

    /**
     * Get the player whose turn it is after a given player's turn, taking turn
     * order into account.
     * @param playerTurn a {@link Player}, or {@code null}.
     * @return A {@link Player}, whose turn comes after the current player, or
     * {@code null} if there are no players in the game.
     */
    public Player getNextPlayerAfter(final Player playerTurn) {
        return getNextPlayerAfter(playerTurn, turnOrder);
    }

    /**
     * Get the player whose turn it is after a given player's turn, taking turn
     * order into account.
     * @param playerTurn a {@link Player}, or {@code null}.
     * @param turnOrder a {@link Direction}
     * @return A {@link Player}, whose turn comes after the current player, or
     * {@code null} if there are no players in the game.
     */
    public Player getNextPlayerAfter(final Player playerTurn, final Direction turnOrder) {
        int iPlayer = ingamePlayers.indexOf(playerTurn);

        if (ingamePlayers.isEmpty()) {
            return null;
        }

        final int shift = turnOrder.getShift();
        if (-1 == iPlayer) { // if playerTurn has just lost
        	final int totalNumPlayers = allPlayers.size();
            int iAlive;
            iPlayer = allPlayers.indexOf(playerTurn);
            do {
                iPlayer = (iPlayer + shift) % totalNumPlayers;
                if (iPlayer < 0) {
                	iPlayer += totalNumPlayers;
                }
                iAlive = ingamePlayers.indexOf(allPlayers.get(iPlayer));
            } while (iAlive < 0);
            iPlayer = iAlive;
        }
        else { // for the case playerTurn hasn't died
        	final int numPlayersInGame = ingamePlayers.size();
        	iPlayer = (iPlayer + shift) % numPlayersInGame;
        	if (iPlayer < 0) {
        		iPlayer += numPlayersInGame;
        	}
        }

        return ingamePlayers.get(iPlayer);
    }

    public int getPosition(Player player, Player startingPlayer) {
        int startPosition = ingamePlayers.indexOf(startingPlayer);
        int myPosition = ingamePlayers.indexOf(player);
        if (startPosition > myPosition) {
            myPosition += ingamePlayers.size();
        }

        return myPosition - startPosition + 1;
    }

    public void onPlayerLost(Player p) {
        // Rule 800.4 Losing a Multiplayer game
        CardCollectionView cards = this.getCardsInGame();
        boolean planarControllerLost = false;
        boolean isMultiplayer = this.getPlayers().size() > 2;

        for(Card c : cards) {
            if (c.getController().equals(p) && (c.isPlane() || c.isPhenomenon())) {
                planarControllerLost = true;
            }

            if(isMultiplayer) {
                if (c.getOwner().equals(p)) {
                    c.ceaseToExist();
                } else {
                    c.removeTempController(p);
                    if (c.getController().equals(p)) {
                        this.getAction().exile(c, null);
                    }
                }
            } else {
                c.forceTurnFaceUp();
            }
        }

        // 901.6: If the current planar controller would leave the game, instead the next player
        // in turn order that wouldn’t leave the game becomes the planar controller, then the old
        // planar controller leaves
        // 901.10: When a player leaves the game, all objects owned by that player except abilities
        // from phenomena leave the game. (See rule 800.4a.) If that includes a face-up plane card
        // or phenomenon card, the planar controller turns the top card of his or her planar deck face up.the game.
        if (planarControllerLost) {
            getNextPlayerAfter(p).initPlane();
        }

        if (p != null && p.equals(getMonarch())) {
            // if the player who lost was the Monarch, someone else will be the monarch
            if(p.equals(getPhaseHandler().getPlayerTurn())) {
                getAction().becomeMonarch(getNextPlayerAfter(p));
            } else {
                getAction().becomeMonarch(getPhaseHandler().getPlayerTurn());
            }
        }

        // Remove leftover items from
        this.getStack().removeInstancesControlledBy(p);

        ingamePlayers.remove(p);
        lostPlayers.add(p);

        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Player, p);
        getTriggerHandler().runTrigger(TriggerType.LosesGame, runParams, false);
    }

    /**
     * Fire only the events after they became real for gamestate and won't get replaced.<br>
     * The events are sent to UI, log and sound system. Network listeners are under development.
     */
    public void fireEvent(final Event event) {
        events.post(event);
    }
    public void subscribeToEvents(final Object subscriber) {
        events.register(subscriber);
    }

    public GameRules getRules() {
        return rules;
    }

    public List<Card> getActivePlanes() {
        return activePlanes;
    }
    public void setActivePlanes(List<Card> activePlane0) {
        activePlanes = activePlane0;
    }

    public void archenemy904_10() {
        //904.10. If a non-ongoing scheme card is face up in the
        //command zone, and it isn't the source of a triggered ability
        //that has triggered but not yet left the stack, that scheme card
        //is turned face down and put on the bottom of its owner's scheme
        //deck the next time a player would receive priority.
        //(This is a state-based action. See rule 704.)

        for (int i = 0; i < getCardsIn(ZoneType.Command).size(); i++) {
            Card c = getCardsIn(ZoneType.Command).get(i);
            if (c.isScheme() && !c.getType().hasSupertype(Supertype.Ongoing)) {
                boolean foundonstack = false;
                for (SpellAbilityStackInstance si : stack) {
                    if (si.getSourceCard().equals(c)) {
                        foundonstack = true;
                        break;
                    }
                }
                if (!foundonstack) {
                    getTriggerHandler().suppressMode(TriggerType.ChangesZone);
                    c.getController().getZone(ZoneType.Command).remove(c);
                    i--;
                    getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

                    c.getController().getZone(ZoneType.SchemeDeck).add(c);
                }
            }
        }
    }

    public GameStage getAge() {
        return age;
    }

    public void setAge(GameStage value) {
        age = value;
    }

    private int cardIdCounter = 0, hiddenCardIdCounter = 0;
    public int nextCardId() {
        return ++cardIdCounter;
    }
    public int nextHiddenCardId() {
        return ++hiddenCardIdCounter;
    }

    public Multimap<Player, Card> chooseCardsForAnte(final boolean matchRarity) {
        Multimap<Player, Card> anteed = ArrayListMultimap.create();

        if (matchRarity) {

            boolean onePlayerHasTimeShifted = false;

            List<CardRarity> validRarities = new ArrayList<>(Arrays.asList(CardRarity.values()));
            for (final Player player : getPlayers()) {
                final Set<CardRarity> playerRarity = getValidRarities(player.getCardsIn(ZoneType.Library));
                if (!onePlayerHasTimeShifted) {
                    onePlayerHasTimeShifted = playerRarity.contains(CardRarity.Special);
                }
                validRarities.retainAll(playerRarity);
            }

            if (validRarities.size() == 0) { //If no possible rarity matches were found, use the original method to choose antes
                for (Player player : getPlayers()) {
                    chooseRandomCardsForAnte(player, anteed);
                }
                return anteed;
            }

            //If possible, don't ante basic lands
            if (validRarities.size() > 1) {
                validRarities.remove(CardRarity.BasicLand);
            }

            if (validRarities.contains(CardRarity.Special)) {
                onePlayerHasTimeShifted = false;
            }

            CardRarity anteRarity = validRarities.get(MyRandom.getRandom().nextInt(validRarities.size()));

            System.out.println("Rarity chosen for ante: " + anteRarity.name());

            for (final Player player : getPlayers()) {
                CardCollection library = new CardCollection(player.getCardsIn(ZoneType.Library));
                CardCollection toRemove = new CardCollection();

                //Remove all cards that aren't of the chosen rarity
                for (Card card : library) {
                    if (onePlayerHasTimeShifted && card.getRarity() == CardRarity.Special) {
                        //Since Time Shifted cards don't have a traditional rarity, they're wildcards
                        continue;
                    } else if (anteRarity == CardRarity.MythicRare || anteRarity == CardRarity.Rare) {
                        //Rare and Mythic Rare cards are considered the same rarity, just as in booster packs
                        //Otherwise it's possible to never lose Mythic Rare cards if you choose opponents carefully
                        //It also lets you win Mythic Rare cards when you don't have any to ante
                        if (card.getRarity() != CardRarity.MythicRare && card.getRarity() != CardRarity.Rare) {
                            toRemove.add(card);
                        }
                    } else {
                        if (card.getRarity() != anteRarity) {
                            toRemove.add(card);
                        }
                    }
                }

                library.removeAll(toRemove);

                if (library.size() > 0) { //Make sure that matches were found. If not, use the original method to choose antes
                    Card ante = library.get(MyRandom.getRandom().nextInt(library.size()));
                    anteed.put(player, ante);
                } else {
                    chooseRandomCardsForAnte(player, anteed);
                }

            }
        }
        else {
            for (Player player : getPlayers()) {
                chooseRandomCardsForAnte(player, anteed);
            }
        }
        return anteed;
    }

    private void chooseRandomCardsForAnte(final Player player, final Multimap<Player, Card> anteed) {
        final CardCollectionView lib = player.getCardsIn(ZoneType.Library);
        Predicate<Card> goodForAnte = Predicates.not(CardPredicates.Presets.BASIC_LANDS);
        Card ante = Aggregates.random(Iterables.filter(lib, goodForAnte));
        if (ante == null) {
            getGameLog().add(GameLogEntryType.ANTE, "Only basic lands found. Will ante one of them");
            ante = Aggregates.random(lib);
        }
        anteed.put(player, ante);
    }

    private static Set<CardRarity> getValidRarities(final Iterable<Card> cards) {
        final Set<CardRarity> rarities = new HashSet<>();
        for (final Card card : cards) {
            if (card.getRarity() == CardRarity.Rare || card.getRarity() == CardRarity.MythicRare) {
                //Since both rare and mythic rare are considered the same, adding both rarities
                //massively increases the odds chances of the game picking rare cards to ante.
                //This is a little unfair, so we add just one of the two.
                rarities.add(CardRarity.Rare);
            } else {
                rarities.add(card.getRarity());
            }
        }
        return rarities;
    }

    public void clearCaches() {

        lastStateBattlefield.clear();
        lastStateGraveyard.clear();
        //playerCache.clear();
    }

    // Does the player control any cards that care about the order of cards in the graveyard?
    public boolean isGraveyardOrdered(final Player p) {
        for (Card c : p.getAllCards()) {
            if (c.hasSVar("NeedsOrderedGraveyard")) {
                return true;
            } else if (c.getOriginalState(CardStateName.Original).hasSVar("NeedsOrderedGraveyard")) {
                return true;
            }
        }
        for (Card c : p.getOpponents().getCardsIn(ZoneType.Battlefield)) {
            // Bone Dancer is important when an opponent has it active on the battlefield
            if ("opponent".equalsIgnoreCase(c.getSVar("NeedsOrderedGraveyard"))) {
                return true;
            }
        }
        return false;
    }

    public Player getControlVote() {
        Player result = null;
        long maxValue = 0;
        for (Player p : getPlayers()) {
            Long v = p.getHighestControlVote();
            if (v != null && v > maxValue) {
                maxValue = v;
                result = p;
            }
        }
        return result;
    }
}
