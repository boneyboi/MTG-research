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
package forge.screens.deckeditor.controllers;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import forge.UiCommand;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.gui.framework.FScreen;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.ItemManagerConfig;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.deckeditor.AddBasicLandsDialog;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.match.controllers.CDetailPicture;
import forge.toolbox.FComboBox;
import forge.util.ItemPool;
import forge.util.Localizer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Child controller for constructed deck editor UI.
 * This is the least restrictive mode;
 * all cards are available.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 * 
 * @author Forge
 * @version $Id: CEditorConstructed.java 24868 2014-02-17 05:08:05Z drdev $
 */
public final class CEditorConstructed extends CDeckEditor<Deck> {
    private DeckController<Deck> controller;
    private final List<DeckSection> allSections = new ArrayList<>();
    private ItemPool<PaperCard> normalPool, avatarPool, planePool, schemePool, conspiracyPool, commanderPool;

    Predicate<CardRules> commanderFilter;

    CardManager catalogManager;
    CardManager deckManager;

    //=========== Constructor
    /**
     * Child controller for constructed deck editor UI.
     * This is the least restrictive mode;
     * all cards are available.
     */
    @SuppressWarnings("serial")
    public CEditorConstructed(final CDetailPicture cDetailPicture0) {
        this(cDetailPicture0, GameType.Constructed);
    }

    public CEditorConstructed(final CDetailPicture cDetailPicture0, final GameType gameType0) {
        super(FScreen.DECK_EDITOR_CONSTRUCTED, cDetailPicture0, gameType0);

        boolean wantUnique = false;

        allSections.add(DeckSection.Main);
        allSections.add(DeckSection.Sideboard);

        switch (this.gameType) {
            case Constructed:
                allSections.add(DeckSection.Avatar);
                allSections.add(DeckSection.Schemes);
                allSections.add(DeckSection.Planes);
                allSections.add(DeckSection.Conspiracy);

                normalPool = ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getAllCards(), PaperCard.class);
                avatarPool = ItemPool.createFrom(FModel.getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_VANGUARD, PaperCard.FN_GET_RULES)), PaperCard.class);
                planePool = ItemPool.createFrom(FModel.getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_PLANE_OR_PHENOMENON, PaperCard.FN_GET_RULES)), PaperCard.class);
                schemePool = ItemPool.createFrom(FModel.getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_SCHEME, PaperCard.FN_GET_RULES)), PaperCard.class);
                conspiracyPool = ItemPool.createFrom(FModel.getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_CONSPIRACY, PaperCard.FN_GET_RULES)), PaperCard.class);

                break;
            case Commander:
            case TinyLeaders:
                allSections.add(DeckSection.Commander);

                commanderFilter = CardRulesPredicates.Presets.CAN_BE_TINY_LEADERS_COMMANDER;
                commanderPool = ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getAllCards(Predicates.compose(commanderFilter, PaperCard.FN_GET_RULES)), PaperCard.class);
                normalPool = ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getAllCards(), PaperCard.class);

                wantUnique = true;
                break;
            case Oathbreaker:
                allSections.add(DeckSection.Commander);

                commanderFilter = Predicates.or(CardRulesPredicates.Presets.CAN_BE_OATHBREAKER, CardRulesPredicates.Presets.CAN_BE_SIGNATURE_SPELL);
                commanderPool = ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getAllCards(Predicates.compose(commanderFilter, PaperCard.FN_GET_RULES)), PaperCard.class);
                normalPool = ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getAllCards(), PaperCard.class);

                wantUnique = true;
                break;
            case Brawl:
                allSections.add(DeckSection.Commander);

                commanderFilter = CardRulesPredicates.Presets.CAN_BE_BRAWL_COMMANDER;
                commanderPool = ItemPool.createFrom(FModel.getMagicDb().getCommonCards().getAllCards(Predicates.and(
                        FModel.getFormats().get("Brawl").getFilterPrinted(), Predicates.compose(commanderFilter, PaperCard.FN_GET_RULES))), PaperCard.class);
                normalPool = ItemPool.createFrom(FModel.getFormats().get("Brawl").getAllCards(), PaperCard.class);

                wantUnique = true;
                break;
        }

        catalogManager = new CardManager(getCDetailPicture(), wantUnique, false);
        deckManager = new CardManager(getCDetailPicture(), wantUnique, false);

        final Localizer localizer = Localizer.getInstance();

        catalogManager.setCaption(localizer.getMessage("lblCatalog"));

        this.setCatalogManager(catalogManager);
        this.setDeckManager(deckManager);

        final Supplier<Deck> newCreator = new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        };

        switch (this.gameType) {
            case Constructed:
                this.controller = new DeckController<>(FModel.getDecks().getConstructed(), this, newCreator);
                break;
            case Commander:
                this.controller = new DeckController<>(FModel.getDecks().getCommander(), this, newCreator);
                break;
            case Oathbreaker:
                this.controller = new DeckController<>(FModel.getDecks().getOathbreaker(), this, newCreator);
                break;
            case Brawl:
                this.controller = new DeckController<>(FModel.getDecks().getBrawl(), this, newCreator);
                break;
            case TinyLeaders:
                this.controller = new DeckController<>(FModel.getDecks().getTinyLeaders(), this, newCreator);
                break;
        }

        getBtnAddBasicLands().setCommand(new UiCommand() {
            @Override
            public void run() {
                CEditorConstructed.addBasicLands(CEditorConstructed.this);
            }
        });
    }

    //=========== Overridden from ACEditorBase

    @Override
    protected CardLimit getCardLimit() {
        if (sectionMode == DeckSection.Avatar) {
            return CardLimit.Singleton;
        }
        if (FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            switch (this.gameType) {
                case Constructed:
                    return CardLimit.Default;
                case Commander:
                case Oathbreaker:
                case TinyLeaders:
                case Brawl:
                    return CardLimit.Singleton;
            }
        }
        return CardLimit.None; //if not enforcing deck legality, don't enforce default limit
    }

    public static void onAddItems(ACEditorBase<PaperCard, Deck> editor, Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        DeckSection sectionMode = editor.sectionMode;
        DeckController<Deck> controller = editor.getDeckController();

        switch (sectionMode) {
        case Commander:
            int count = editor.getDeckManager().getItemCount();
            if (count > 0) {
                PaperCard newCard = items.iterator().next().getKey(); //can only add one card at a time when on commander section
                if (editor.gameType == GameType.Oathbreaker) { //replace oathbreaker or signature spell if needed
                    for (Entry<PaperCard, Integer> entry : editor.getDeckManager().getPool()) {
                        if (entry.getKey().getRules().canBeOathbreaker() == newCard.getRules().canBeOathbreaker()) {
                            editor.getDeckManager().removeItem(entry.getKey(), entry.getValue());
                            break;
                        }
                    }
                }
                else { //replace existing commander unless new commander is valid partner commander
                    if (count == 1 && newCard.getRules().canBePartnerCommander()) { //replace existing partners regardless
                        PaperCard commander = editor.getDeckManager().getPool().toFlatList().get(0);
                        if (!commander.getRules().canBePartnerCommander()) {
                            editor.getDeckManager().removeAllItems();
                        }
                    }
                    else {
                        editor.getDeckManager().removeAllItems();
                    }
                }
            }
            break;
        case Avatar:
            editor.getDeckManager().removeAllItems();
            break;
        default:
            break;
        }

        ItemPool<PaperCard> itemsToAdd = editor.getAllowedAdditions(items);
        if (itemsToAdd.isEmpty()) { return; }

        if (toAlternate) {
            switch (sectionMode) {
            case Main:
                controller.getModel().getOrCreate(DeckSection.Sideboard).addAll(itemsToAdd);
                break;
            default:
                return; //no other sections should support toAlternate
            }
        }
        else {
            editor.getDeckManager().addItems(itemsToAdd);
        }

        if (editor.getCatalogManager().isInfinite()) {
            //select all added cards in Catalog if infinite
            editor.getCatalogManager().selectItemEntrys(itemsToAdd);
        }
        else {
            //remove all added cards from Catalog if not infinite
            editor.getCatalogManager().removeItems(items);
        }

        controller.notifyModelChanged();
    }

    public static void onRemoveItems(ACEditorBase<PaperCard, Deck> editor, Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        DeckSection sectionMode = editor.sectionMode;
        DeckController<Deck> controller = editor.getDeckController();

        if (toAlternate) {
            switch (sectionMode) {
            case Main:
                controller.getModel().getOrCreate(DeckSection.Sideboard).addAll(items);
                break;
            case Sideboard:
                controller.getModel().get(DeckSection.Main).addAll(items);
                break;
            default:
                break; //no other sections should support toAlternate
            }
        }
        else {
            editor.getCatalogManager().addItems(items);
        }
        editor.getDeckManager().removeItems(items);

        controller.notifyModelChanged();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onAddItems()
     */
    @Override
    protected void onAddItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        onAddItems(this, items, toAlternate);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#onRemoveItems()
     */
    @Override
    protected void onRemoveItems(Iterable<Entry<PaperCard, Integer>> items, boolean toAlternate) {
        onRemoveItems(this, items, toAlternate);
    }

    public static void buildAddContextMenu(EditorContextMenuBuilder cmb, DeckSection sectionMode, GameType gameType) {
        final Localizer localizer = Localizer.getInstance();
        switch (sectionMode) {
        case Main:
            cmb.addMoveItems(localizer.getMessage("lblAdd"), localizer.getMessage("lbltodeck"));
            cmb.addMoveAlternateItems(localizer.getMessage("lblAdd"), localizer.getMessage("lbltosideboard"));
            break;
        case Sideboard:
            cmb.addMoveItems(localizer.getMessage("lblAdd"), localizer.getMessage("lbltosideboard"));
            break;
        case Commander:
            if (gameType == GameType.Oathbreaker) {
                PaperCard pc = cmb.getItemManager().getSelectedItem();
                if (pc != null && pc.getRules().canBeSignatureSpell()) {
                    cmb.addMoveItems(localizer.getMessage("lblSet2"), localizer.getMessage("lblassignaturespell"));
                }
                else {
                    cmb.addMoveItems(localizer.getMessage("lblSet2"), localizer.getMessage("lblasoathbreaker"));
                }
            }
            else {
                cmb.addMoveItems(localizer.getMessage("lblSet2"), localizer.getMessage("lblascommander"));
            }
            break;
        case Avatar:
            cmb.addMoveItems(localizer.getMessage("lblSet2"), localizer.getMessage("lblasavatar"));
            break;
        case Schemes:
            cmb.addMoveItems(localizer.getMessage("lblAdd"), localizer.getMessage("lbltoschemedeck"));
            break;
        case Planes:
            cmb.addMoveItems(localizer.getMessage("lblAdd"), localizer.getMessage("lbltoplanardeck"));
            break;
        case Conspiracy:
            cmb.addMoveItems(localizer.getMessage("lblAdd"), localizer.getMessage("lbltoconspiracydeck"));
            break;
        }
    }

    public static void buildRemoveContextMenu(EditorContextMenuBuilder cmb, DeckSection sectionMode, boolean foilAvailable) {
        final Localizer localizer = Localizer.getInstance();
        switch (sectionMode) {
        case Main:
            cmb.addMoveItems(localizer.getMessage("lblRemove"), localizer.getMessage("lblfromdeck"));
            cmb.addMoveAlternateItems(localizer.getMessage("lblMove"), localizer.getMessage("lbltosideboard"));
            break;
        case Sideboard:
            cmb.addMoveItems(localizer.getMessage("lblRemove"), localizer.getMessage("lblfromsideboard"));
            cmb.addMoveAlternateItems("Move", "to deck");
            break;
        case Commander:
            cmb.addMoveItems(localizer.getMessage("lblRemove"), localizer.getMessage("lblascommander"));
            break;
        case Avatar:
            cmb.addMoveItems(localizer.getMessage("lblRemove"), localizer.getMessage("lblasavatar"));
            break;
        case Schemes:
            cmb.addMoveItems(localizer.getMessage("lblRemove"), localizer.getMessage("lblfromschemedeck"));
            break;
        case Planes:
            cmb.addMoveItems(localizer.getMessage("lblRemove"), localizer.getMessage("lblfromplanardeck"));
            break;
        case Conspiracy:
            cmb.addMoveItems(localizer.getMessage("lblRemove"), localizer.getMessage("lblfromconspiracydeck"));
            break;
        }
        if (foilAvailable) {
            cmb.addMakeFoils();
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#buildAddContextMenu()
     */
    @Override
    protected void buildAddContextMenu(EditorContextMenuBuilder cmb) {
        buildAddContextMenu(cmb, sectionMode, gameType);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#buildRemoveContextMenu()
     */
    @Override
    protected void buildRemoveContextMenu(EditorContextMenuBuilder cmb) {
        buildRemoveContextMenu(cmb, sectionMode, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#resetTables()
     */
    @Override
    public void resetTables() {
        // Constructed mode can use all cards, no limitations.
        this.sectionMode = DeckSection.Main;
        this.getCatalogManager().setPool(normalPool, true);
        this.getDeckManager().setPool(this.controller.getModel().getMain());
    }

    @Override
    protected Boolean isSectionImportable(DeckSection section) {
        return allSections.contains(section);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#getController()
     */
    @Override
    public DeckController<Deck> getDeckController() {
        return this.controller;
    }

    /**
     * Switch between the main deck and the sideboard editor.
     */
    public void setEditorMode(DeckSection sectionMode) {
        if (sectionMode == null) {
            return;
        }
        switch(this.gameType) {
            case Constructed:
                switch(sectionMode) {
                    case Main:
                        this.getCatalogManager().setup(ItemManagerConfig.CARD_CATALOG);
                        this.getCatalogManager().setPool(normalPool, true);
                        this.getCatalogManager().setAllowMultipleSelections(true);
                        this.getDeckManager().setPool(this.controller.getModel().getMain());
                        break;
                    case Sideboard:
                        this.getCatalogManager().setup(ItemManagerConfig.CARD_CATALOG);
                        this.getCatalogManager().setPool(normalPool, true);
                        this.getCatalogManager().setAllowMultipleSelections(true);
                        this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Sideboard));
                        break;
                    case Avatar:
                        this.getCatalogManager().setup(ItemManagerConfig.AVATAR_POOL);
                        this.getCatalogManager().setPool(avatarPool, true);
                        this.getCatalogManager().setAllowMultipleSelections(false);
                        this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Avatar));
                        break;
                    case Planes:
                        this.getCatalogManager().setup(ItemManagerConfig.PLANAR_POOL);
                        this.getCatalogManager().setPool(planePool, true);
                        this.getCatalogManager().setAllowMultipleSelections(true);
                        this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Planes));
                        break;
                    case Schemes:
                        this.getCatalogManager().setup(ItemManagerConfig.SCHEME_POOL);
                        this.getCatalogManager().setPool(schemePool, true);
                        this.getCatalogManager().setAllowMultipleSelections(true);
                        this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Schemes));
                        break;
                    case Commander:
                        break; //do nothing for Commander here
                    case Conspiracy:
                        this.getCatalogManager().setup(ItemManagerConfig.CONSPIRACY_DECKS);
                        this.getCatalogManager().setPool(conspiracyPool, true);
                        this.getCatalogManager().setAllowMultipleSelections(true);
                        this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Conspiracy));
                }
            case Commander:
            case Oathbreaker:
            case TinyLeaders:
            case Brawl:
                switch(sectionMode) {
                    case Main:
                        this.getCatalogManager().setup(ItemManagerConfig.CARD_CATALOG);
                        this.getCatalogManager().setPool(normalPool, true);
                        this.getCatalogManager().setAllowMultipleSelections(true);
                        this.getDeckManager().setPool(this.controller.getModel().getMain());
                        break;
                    case Sideboard:
                        this.getCatalogManager().setup(ItemManagerConfig.CARD_CATALOG);
                        this.getCatalogManager().setPool(normalPool, true);
                        this.getCatalogManager().setAllowMultipleSelections(true);
                        this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Sideboard));
                        break;
                    case Commander:
                        this.getCatalogManager().setup(ItemManagerConfig.COMMANDER_POOL);
                        this.getCatalogManager().setPool(commanderPool, true);
                        this.getCatalogManager().setAllowMultipleSelections(false);
                        this.getDeckManager().setPool(this.controller.getModel().getOrCreate(DeckSection.Commander));
                        break;
                    default:
                        break;
                }
        }

        this.sectionMode = sectionMode;
        this.controller.updateCaptions();
    }

    public static void addBasicLands(ACEditorBase<PaperCard, Deck> editor) {
        Deck deck = editor.getDeckController().getModel();
        if (deck == null) { return; }

        AddBasicLandsDialog dialog = new AddBasicLandsDialog(deck);
        CardPool landsToAdd = dialog.show();
        if (landsToAdd != null) {
            editor.onAddItems(landsToAdd, false);
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#show(forge.Command)
     */
    @SuppressWarnings("serial")
    @Override
    public void update() {
        this.getCatalogManager().setup(ItemManagerConfig.CARD_CATALOG);
        this.getDeckManager().setup(ItemManagerConfig.DECK_EDITOR);

        resetUI();

        this.getCbxSection().removeAllItems();
        for (DeckSection section : allSections) {
            this.getCbxSection().addItem(section);
        }
        this.getCbxSection().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                FComboBox cb = (FComboBox)actionEvent.getSource();
                DeckSection ds = (DeckSection)cb.getSelectedItem();
                setEditorMode(ds);
            }
        });
        this.getCbxSection().setVisible(true);

        this.controller.refreshModel();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#canSwitchAway()
     */
    @Override
    public boolean canSwitchAway(boolean isClosing) {
        return SEditorIO.confirmSaveChanges(FScreen.DECK_EDITOR_CONSTRUCTED, false); //ignore isClosing since screen can't close
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#resetUIChanges()
     */
    @Override
    public void resetUIChanges() {
    }
}
