package forge.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.deck.ArchetypeDeckGenerator;
import forge.deck.CardThemedDeckGenerator;
import forge.deck.CommanderDeckGenerator;
import forge.deck.DeckProxy;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.item.IPaperCard;
import forge.item.InventoryItem;
import forge.model.FModel;
import forge.planarconquest.ConquestCommander;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.MatchController;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDialog;
import forge.toolbox.FOverlay;
import forge.util.Localizer;
import forge.util.collect.FCollectionView;
import forge.util.Utils;

public class CardZoom extends FOverlay {
    private static final float REQ_AMOUNT = Utils.AVG_FINGER_WIDTH;

    private static final CardZoom cardZoom = new CardZoom();
    private static final ForgePreferences prefs = FModel.getPreferences();
    private static List<?> items;
    private static int currentIndex, initialIndex;
    private static CardView currentCard, prevCard, nextCard;
    private static boolean zoomMode = true;
    private static boolean oneCardView = prefs.getPrefBoolean(FPref.UI_SINGLE_CARD_ZOOM);
    private float totalZoomAmount;
    private static ActivateHandler activateHandler;
    private static String currentActivateAction;
    private static Rectangle flipIconBounds;
    private static boolean showAltState;

    public static void show(Object item) {
        List<Object> items0 = new ArrayList<>();
        items0.add(item);
        show(items0, 0, null);
    }
    public static void show(FCollectionView<?> items0, int currentIndex0, ActivateHandler activateHandler0) {
        show((List<?>)items0, currentIndex0, activateHandler0);
    }
    public static void show(final List<?> items0, int currentIndex0, ActivateHandler activateHandler0) {
        items = items0;
        activateHandler = activateHandler0;
        currentIndex = currentIndex0;
        initialIndex = currentIndex0;
        currentCard = getCardView(items.get(currentIndex));
        prevCard = currentIndex > 0 ? getCardView(items.get(currentIndex - 1)) : null;
        nextCard = currentIndex < items.size() - 1 ? getCardView(items.get(currentIndex + 1)) : null;
        onCardChanged();
        cardZoom.show();
    }

    public static boolean isOpen() {
        return cardZoom.isVisible();
    }

    public static void hideZoom() {
        cardZoom.hide();
    }

    private CardZoom() {
    }

    @Override
    public void setVisible(boolean visible0) {
        if (this.isVisible() == visible0) { return; }

        super.setVisible(visible0);

        //update selected index when hidden if current index is different than initial index
        if (!visible0 && activateHandler != null && currentIndex != initialIndex) {
            activateHandler.setSelectedIndex(currentIndex);
        }
    }

    private static void incrementCard(int dir) {
        if (dir > 0) {
            if (currentIndex == items.size() - 1) { return; }
            currentIndex++;

            prevCard = currentCard;
            currentCard = nextCard;
            nextCard = currentIndex < items.size() - 1 ? getCardView(items.get(currentIndex + 1)) : null;
        }
        else {
            if (currentIndex == 0) { return; }
            currentIndex--;

            nextCard = currentCard;
            currentCard = prevCard;
            prevCard = currentIndex > 0 ? getCardView(items.get(currentIndex - 1)) : null;
        }
        onCardChanged();
    }

    private static void onCardChanged() {
        if (activateHandler != null) {
            currentActivateAction = activateHandler.getActivateAction(currentIndex);
        }
        if (MatchController.instance.mayFlip(currentCard)) {
            flipIconBounds = new Rectangle();
        } else {
            flipIconBounds = null;
        }
        showAltState = false;
    }

    private static CardView getCardView(Object item) {
        if (item instanceof Entry) {
            item = ((Entry<?, ?>)item).getKey();
        }
        if (item instanceof CardView) {
            return (CardView)item;
        }
        if (item instanceof DeckProxy) {
            if (item instanceof CardThemedDeckGenerator){
                return CardView.getCardForUi(((CardThemedDeckGenerator)item).getPaperCard());
            }else if (item instanceof CommanderDeckGenerator){
                return CardView.getCardForUi(((CommanderDeckGenerator)item).getPaperCard());
            }else if (item instanceof ArchetypeDeckGenerator){
                return CardView.getCardForUi(((ArchetypeDeckGenerator)item).getPaperCard());
            }else{
                DeckProxy deck = ((DeckProxy)item);
                return new CardView(-1, null, deck.getName(), null, deck.getImageKey(false));
            }

        }
        if (item instanceof IPaperCard) {
            return CardView.getCardForUi((IPaperCard)item);
        }
        if (item instanceof ConquestCommander) {
            return CardView.getCardForUi(((ConquestCommander)item).getCard());
        }
        if (item instanceof InventoryItem) {
            InventoryItem ii = (InventoryItem)item;
            return new CardView(-1, null, ii.getName(), null, ii.getImageKey(false));
        }
        return new CardView(-1, null, item.toString());
    }

    @Override
    public boolean tap(float x, float y, int count) {
        if (flipIconBounds != null && flipIconBounds.contains(x, y)) {
            showAltState = !showAltState;
            return true;
        }
        hide();
        return true;
    }

    @Override
    public boolean fling(float velocityX, float velocityY) {
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            incrementCard(velocityX > 0 ? -1 : 1);
            return true;
        }
        if (velocityY > 0) {
            zoomMode = !zoomMode;
            return true;
        }
        if (currentActivateAction != null && activateHandler != null) {
            hide();
            activateHandler.activate(currentIndex);
            return true;
        }
        return false;
    }

    private void setOneCardView(boolean oneCardView0) {
        if (oneCardView == oneCardView0 || Forge.isLandscapeMode()) { return; } //don't allow changing this when in landscape mode

        oneCardView = oneCardView0;
        prefs.setPref(FPref.UI_SINGLE_CARD_ZOOM, oneCardView0);
        prefs.save();
    }

    @Override
    public boolean zoom(float x, float y, float amount) {
        totalZoomAmount += amount;

        if (totalZoomAmount >= REQ_AMOUNT) {
            setOneCardView(true);
            totalZoomAmount = 0;
        }
        else if (totalZoomAmount <= -REQ_AMOUNT) {
            setOneCardView(false);
            totalZoomAmount = 0;
        }
        return true;
    }

    @Override
    public boolean longPress(float x, float y) {
        setOneCardView(!oneCardView);
        return true;
    }

    @Override
    public void drawOverlay(Graphics g) {
        final GameView gameView = MatchController.instance.getGameView();

        float w = getWidth();
        float h = getHeight();
        float messageHeight = FDialog.MSG_HEIGHT;
        float AspectRatioMultiplier;
        switch (Forge.extrawide) {
            case "default":
                AspectRatioMultiplier = 3; //good for tablets with 16:10 or similar
                break;
            case "wide":
                AspectRatioMultiplier = 2.5f;
                break;
            case "extrawide":
                AspectRatioMultiplier = 2; //good for tall phones with 21:9 or similar
                break;
            default:
                AspectRatioMultiplier = 3;
                break;
        }
        float maxCardHeight = h - AspectRatioMultiplier * messageHeight; //maxheight of currently zoomed card

        float cardWidth, cardHeight, y;
        
        if (oneCardView && !Forge.isLandscapeMode()) {

            cardWidth = w;
            cardHeight = FCardPanel.ASPECT_RATIO * cardWidth;
            
            boolean rotateSplit = FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ROTATE_SPLIT_CARDS);
            if (currentCard.isSplitCard() && rotateSplit) {
                // card will be rotated.  Make sure that the height does not exceed the width of the view
                if (cardHeight > Gdx.graphics.getWidth())
                {
                    cardHeight = Gdx.graphics.getWidth();
                    cardWidth = cardHeight / FCardPanel.ASPECT_RATIO;
                }
            }
        }
        else {
            
            cardWidth = w * 0.5f;
            cardHeight = FCardPanel.ASPECT_RATIO * cardWidth;

            float maxSideCardHeight = maxCardHeight * 5 / 7;
            if (cardHeight > maxSideCardHeight) { //prevent card overlapping message bars
                cardHeight = maxSideCardHeight;
                cardWidth = cardHeight / FCardPanel.ASPECT_RATIO;
            }
            y = (h - cardHeight) / 2;
            if (prevCard != null) {
                CardImageRenderer.drawZoom(g, prevCard, gameView, false, 0, y, cardWidth, cardHeight, getWidth(), getHeight(), false);
            }
            if (nextCard != null) {
                CardImageRenderer.drawZoom(g, nextCard, gameView, false, w - cardWidth, y, cardWidth, cardHeight, getWidth(), getHeight(), false);
            }

            cardWidth = w * 0.7f;
            cardHeight = FCardPanel.ASPECT_RATIO * cardWidth;
        }

        if (cardHeight > maxCardHeight) { //prevent card overlapping message bars
            cardHeight = maxCardHeight;
            cardWidth = cardHeight / FCardPanel.ASPECT_RATIO;
        }
        float x = (w - cardWidth) / 2;
        y = (h - cardHeight) / 2;
        if (zoomMode) {
            CardImageRenderer.drawZoom(g, currentCard, gameView, showAltState, x, y, cardWidth, cardHeight, getWidth(), getHeight(), true);
        }
        else {
            CardImageRenderer.drawDetails(g, currentCard, gameView, showAltState, x, y, cardWidth, cardHeight);
        }

        if (flipIconBounds != null) {
            float imageWidth = cardWidth / 2;
            if (Forge.hdbuttons){
                float imageHeight = imageWidth * FSkinImage.HDFLIPCARD.getHeight() / FSkinImage.HDFLIPCARD.getWidth();
                flipIconBounds.set(x + (cardWidth - imageWidth) / 2, y + (cardHeight - imageHeight) / 2, imageWidth, imageHeight);
                g.drawImage(FSkinImage.HDFLIPCARD, flipIconBounds.x, flipIconBounds.y, flipIconBounds.width, flipIconBounds.height);
            } else {
                float imageHeight = imageWidth * FSkinImage.FLIPCARD.getHeight() / FSkinImage.FLIPCARD.getWidth();
                flipIconBounds.set(x + (cardWidth - imageWidth) / 2, y + (cardHeight - imageHeight) / 2, imageWidth, imageHeight);
                g.drawImage(FSkinImage.FLIPCARD, flipIconBounds.x, flipIconBounds.y, flipIconBounds.width, flipIconBounds.height);
            }
        }

        if (currentActivateAction != null) {
            g.fillRect(FDialog.MSG_BACK_COLOR, 0, 0, w, messageHeight);
            g.drawText(Localizer.getInstance().getMessage("lblSwipeUpTo").replace("%s", currentActivateAction), FDialog.MSG_FONT, FDialog.MSG_FORE_COLOR, 0, 0, w, messageHeight, false, Align.center, true);
        }
        g.fillRect(FDialog.MSG_BACK_COLOR, 0, h - messageHeight, w, messageHeight);
        g.drawText(zoomMode ? Localizer.getInstance().getMessage("lblSwipeDownDetailView") : Localizer.getInstance().getMessage("lblSwipeDownPictureView"), FDialog.MSG_FONT, FDialog.MSG_FORE_COLOR, 0, h - messageHeight, w, messageHeight, false, Align.center, true);
    }

    @Override
    protected void doLayout(float width, float height) {
    }

    public interface ActivateHandler {
        String getActivateAction(int index);
        void setSelectedIndex(int index);
        void activate(int index);
    }
}
