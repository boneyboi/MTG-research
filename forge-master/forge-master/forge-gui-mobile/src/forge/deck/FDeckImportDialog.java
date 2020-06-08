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
package forge.deck;

import java.util.List;

import com.google.common.collect.ImmutableList;

import forge.FThreads;
import forge.Forge;
import forge.Graphics;
import forge.deck.DeckRecognizer.TokenType;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FDialog;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextArea;
import forge.util.Callback;
import forge.util.Localizer;
import forge.util.gui.SOptionPane;


public class FDeckImportDialog extends FDialog {
    private final Callback<Deck> callback;

    private final FTextArea txtInput = add(new FTextArea(true));
    private final FCheckBox newEditionCheck = add(new FCheckBox(Localizer.getInstance().getMessage("lblImportLatestVersionCard"), true));
    private final FCheckBox dateTimeCheck = add(new FCheckBox(Localizer.getInstance().getMessage("lblUseOnlySetsReleasedBefore"), false));
    /*setting onlyCoreExpCheck to false allow the copied cards to pass the check of deck contents
      forge-core\src\main\java\forge\deck\Deck.javaDeck.java starting @ Line 320 which is called by
      forge-gui-mobile\src\forge\deck\FDeckEditor.java starting @ Line 373
      (as of latest commit: 8e6655e3ee67688cff66b422d4722c58392eaa7e)
    */
    private final FCheckBox onlyCoreExpCheck = add(new FCheckBox(Localizer.getInstance().getMessage("lblUseOnlyCoreAndExpansionSets"), false));

    private final FComboBox<String> monthDropdown = add(new FComboBox<>()); //don't need wrappers since skin can't change while this dialog is open
    private final FComboBox<Integer> yearDropdown = add(new FComboBox<>());

    private final boolean showOptions;
    private final DeckImportController controller;

    private final static ImmutableList<String> importOrCancel = ImmutableList.of(Localizer.getInstance().getMessage("lblImport"), Localizer.getInstance().getMessage("lblCancel"));

    public FDeckImportDialog(final boolean replacingDeck, final Callback<Deck> callback0) {
        super(Localizer.getInstance().getMessage("lblImportFromClipboard"), 2);

        callback = callback0;
        controller = new DeckImportController(replacingDeck, newEditionCheck, dateTimeCheck, onlyCoreExpCheck, monthDropdown, yearDropdown);
        txtInput.setText(Forge.getClipboard().getContents()); //just pull import directly off the clipboard

        initButton(0, Localizer.getInstance().getMessage("lblImport"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                FThreads.invokeInBackgroundThread(new Runnable() {
                    @Override
                    public void run() {
                        List<DeckRecognizer.Token> tokens = controller.parseInput(txtInput.getText()); //ensure deck updated based on any changes to options

                        //if there are any unknown cards, let user know this and give them the option to cancel
                        StringBuilder sb = new StringBuilder();
                        for (DeckRecognizer.Token token : tokens) {
                            if (token.getType() == TokenType.UnknownCard) {
                                if (sb.length() > 0) {
                                    sb.append("\n");
                                }
                                sb.append(token.getNumber()).append(" ").append(token.getText());
                            }
                        }
                        if (sb.length() > 0) {
                            if (SOptionPane.showOptionDialog(Localizer.getInstance().getMessage("lblFollowingCardsCannotBeImported") + "\n\n" + sb.toString(), Localizer.getInstance().getMessage("lblImportRemainingCards"), SOptionPane.INFORMATION_ICON, importOrCancel) == 1) {
                                return;
                            }
                        }

                        final Deck deck = controller.accept(); //must accept in background thread in case a dialog is shown
                        if (deck == null) { return; }

                        FThreads.invokeInEdtLater(new Runnable() {
                            @Override
                            public void run() {
                                hide();
                                callback.run(deck);
                            }
                        });
                    }
                });
            }
        });
        initButton(1, Localizer.getInstance().getMessage("lblCancel"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                hide();
            }
        });

        List<DeckRecognizer.Token> tokens = controller.parseInput(txtInput.getText());

        //ensure at least one known card found on clipboard
        for (DeckRecognizer.Token token : tokens) {
            if (token.getType() == TokenType.KnownCard) {
                showOptions = true;

                dateTimeCheck.setCommand(new FEventHandler() {
                    @Override
                    public void handleEvent(FEvent e) {
                        updateDropDownEnabled();
                    }
                });
                updateDropDownEnabled();
                return;
            }
        }

        showOptions = false;
        setButtonEnabled(0, false);
        txtInput.setText(Localizer.getInstance().getMessage("lblNoKnownCardsOnClipboard"));
    }

    private void updateDropDownEnabled() {
        boolean enabled = dateTimeCheck.isSelected();
        monthDropdown.setEnabled(enabled);
        yearDropdown.setEnabled(enabled);
    }

    @Override
    public void drawOverlay(Graphics g) {
        super.drawOverlay(g);
        if (showOptions) {
            float y = txtInput.getTop() - FOptionPane.PADDING;
            g.drawLine(BORDER_THICKNESS, BORDER_COLOR, 0, y, getWidth(), y);
        }
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FOptionPane.PADDING;
        float x = padding;
        float y = padding;
        float w = width - 2 * padding;
        float h;
        if (showOptions) {
            h = monthDropdown.getHeight();
            float fieldPadding = padding / 2;

            newEditionCheck.setBounds(x, y, w, h);
            y += h + fieldPadding;
            dateTimeCheck.setBounds(x, y, w, h);
            y += h + fieldPadding;
            
            float dropDownWidth = (w - fieldPadding) / 2;
            monthDropdown.setBounds(x, y, dropDownWidth, h);
            yearDropdown.setBounds(x + dropDownWidth + fieldPadding, y, dropDownWidth, h);
            y += h + fieldPadding;

            onlyCoreExpCheck.setBounds(x, y, w, h);
            y += h + 2 * padding;
        }
        h = txtInput.getPreferredHeight(w);
        float maxTextBoxHeight = maxHeight - y - padding;
        if (h > maxTextBoxHeight) {
            h = maxTextBoxHeight;
        }
        txtInput.setBounds(x, y, w, h);
        y += h + padding;
        if (showOptions) {
            h = newEditionCheck.getHeight();
        }
        return y;
    }
}
