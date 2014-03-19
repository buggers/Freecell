package freecell;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/* Known bugs:
 * 
 * -Default onClickPressed flash animation still present.
 * 
 * Future features/tests:
 * 
 -Add deal button so you can replay if you have no moves left
     -(Don't auto-exit upon win.)
 * -Double clicking a playstack button moves it to a freecell
 * -Auto-move of stacks if cells are available? (some would not like this)
 * -Auto-move of aces and other cards when it would have no detrimental
 * game impact (some would not like this, so this could be an option)
 * -Add testing.
 * -Better graphics
 * -Card moving animation
   -Check selection visibility on a PC
 * -Make buttons moveable. This might require a redesign. Hence do it first.
 * 
 * Fix .equals in Card, I threw it together.
 * 
 * Possible future directions:
 * AI to solve all games (or declare them unwinnable)
 * 
 */
public class Game
{
   final static Color niftyColor = new Color(0, 250, 100, 50); 
  //20 alpha shows up fine on a Mac as green, too light on a PC.
  //50 alpha shows up as grey on a PC but at least shows up.
   
   private interface CardStack
   {
      boolean isValidAdd(Card aCard);

      boolean isValidRemove();

      boolean isSelectable();

      boolean isOccupied();

      Card peek();

      Card pop();

      Card push(Card aCard);

      ImageIcon getDefaultImage();
//pop, peek,push
   }

   /**
    * Not going to implement an actual logger, this program is too
    * small to bother.
    */
   public final static boolean DEBUGGING = true;

   static  class GameWindow extends JFrame
   {

      public static final int NUMBER_PLAY_STACKS = 8;
      public static final int NUMBER_ACE_STACKS = 4;
      public static final int GAP = 20;
      public static final int WINDOW_WIDTH = 12 * RetrieveIcon.getImageWidth();
      public static final int TOP_HEIGHT = (int) (((double) RetrieveIcon.getImageHeight()) * 1.6D);
      public static final int BOTTOM_HEIGHT = 3 * TOP_HEIGHT;
//SETTING THE HEIGHT/BOUNDS WRONG CAN LEAD TO WEIRD PROBLEMS
      public static final int HEIGHT_OFFSET = (int) (0.25D * (double) RetrieveIcon.getImageHeight());
      private static final Point origin = new Point(
              RetrieveIcon.getImageWidth(), 3 * GAP);

      private CardStack[] myAceStacks;
      private CardButton[] aceStacksButtons;
      private CardStack[] myFreecells;
      private CardButton[] freecellsButtons;
      private CardStack[] myPlayStacks;
      private List<List<CardButton>> playStacksButtons;
      private CardStack currentlySelected;
      private boolean somethingSelected;
      private CardButton currentlySelectedButton;
      private JButton deal = new JButton();
//Not implemented
      private JPanel freecellArea; //belongs in freeAceArea
      
      
      private JPanel aceStackArea; //belongs in freeAceArea
      private JPanel freeAceArea;
      private JLayeredPane playStackArea;
      private ClickListener[] playStackListener, freecellListener, aceStackListener;

      void deal()
      {
         Shoe aShoe = new Shoe(1);
         boolean empty = false;
         Card aCard;
         int iteration = 0;

         while (!empty)
         {
            for (int i = 0; i < myPlayStacks.length; i++)
            {
               aCard = aShoe.drawRandom();
               ((PlayStack) (myPlayStacks[i])).forcePush(aCard);

               addButtonToThisPlayStack(aCard, i);

               if (aShoe.numberOfCards() == 0)
               {
                  empty = true;

                  break;
               }
            }
            iteration++;

         }

      }

      /**
       * Takes a Card and index of a play stack and creates a button
       * on top of that playstack.
       *
       * @param aCard
       * @param playStackIndex
       */
      void addButtonToThisPlayStack(final Card aCard,
                                    final int playStackIndex)
      {
         CardButton scratch;
         final int indexAddedElement = playStacksButtons.get(
                 playStackIndex).size();
         //setToTopOfPlayStack adds one element to this array

         scratch = new CardButton(RetrieveIcon.getIcon(aCard), aCard);
         scratch.addActionListener(playStackListener[playStackIndex]);
         setButtonSettings(scratch, aCard);
         setToTopOfPlayStack(scratch, playStackIndex); //Adds the button to the playstack
         playStackArea.add(playStacksButtons.get(playStackIndex).get(
                 indexAddedElement), new Integer(indexAddedElement));

      }

      /**
       * Helper function for addButtonToThisPlayStack
       *
       * @param scratch
       * @param i The index of the PlayStack to which you want to move
       * this button (or set its bounds)
       *
       */
      private void setToTopOfPlayStack(CardButton scratch, int i)
      {
         final int xPos, yPos, width, height;
         xPos = origin.x + (RetrieveIcon.getImageWidth() + GAP) * i;
         yPos = origin.y + HEIGHT_OFFSET * playStacksButtons.get(i).size();
         width = RetrieveIcon.getImageWidth();
         height = RetrieveIcon.getImageHeight();
         scratch.setBounds(xPos,
                           yPos,
                           width,
                           height);
         playStacksButtons.get(i).add(scratch);
         // System.out.println("For " + i + ", I get: xPos = " + xPos + ", yPos is + " +yPos+
         //      ", width is " + width + ", and height is: " + height);
         //This function is funky
      }

      private void initialize()
      {
         int i;
         myAceStacks = new AceStack[NUMBER_ACE_STACKS];
         aceStacksButtons = new CardButton[NUMBER_ACE_STACKS];
         aceStackListener = new ClickListener[NUMBER_ACE_STACKS];

         myFreecells = new Freecell[NUMBER_ACE_STACKS];
         freecellsButtons = new CardButton[NUMBER_ACE_STACKS];
         freecellListener = new ClickListener[NUMBER_ACE_STACKS];

         myPlayStacks = new PlayStack[NUMBER_PLAY_STACKS];


         playStacksButtons = new ArrayList<List<CardButton>>();
         for (i = 0; i < NUMBER_PLAY_STACKS; i++)
         {
            playStacksButtons.add(new ArrayList<CardButton>());
         }

         playStackListener = new ClickListener[NUMBER_PLAY_STACKS];

         for (i = 0; i < myAceStacks.length; i++)
         {
            myAceStacks[i] = new AceStack();
            aceStackListener[i] = new ClickListener(myAceStacks[i]);
            myFreecells[i] = new Freecell();
            freecellListener[i] = new ClickListener(myFreecells[i]);
         }
         for (i = 0; i < myPlayStacks.length; i++)
         {
            myPlayStacks[i] = new PlayStack();
            playStackListener[i] = new ClickListener(myPlayStacks[i]);
         }

         somethingSelected = false;
         currentlySelected = null;
         currentlySelectedButton = null;


      }

      /**
       * Helper function for setButtonSettings. Don't call me alone.
       *
       *
       * @param aButton
       */
      private void standardButtonSettings(JButton aButton)
      {
         //Assumes you've already said aButton = new JButton(someIcon);
         aButton.setBorderPainted(false);
         aButton.setBorder(null);
         //aButton.setBackground(Color.WHITE); Should inherit the background of the pane.
         //? but doesn't hurt

         aButton.setFocusPainted(false);  //Bingo! This disables the stupid box.
         aButton.setOpaque(true);

      }

      private void setButtonSettings(JButton aButton, Card aCard)
      {
         standardButtonSettings(aButton);

///and here's where it get a bit dicey.
         aButton.setSelectedIcon(RetrieveIcon.getSelectedImage(aCard));

      }

      /**
       * *
       *
       * @param aButton
       * @param cardcode A final static int from the RetrieveIcon
       * class.
       *
       */
      private void setButtonSettings(JButton aButton, int cardcode)
      {
         standardButtonSettings(aButton);
         aButton.setSelectedIcon(RetrieveIcon.getSelectedImage(
                 cardcode));
      }

      /**
       * This does NOT set the selected image -- so maybe it won't
       * ever be used.
       *
       * @param aButton
       */
      private void setButtonSettings(JButton aButton)
      {
         standardButtonSettings(aButton);
      }

      private void createLayout()
      {
         int i;

         aceStackArea = new JPanel(new FlowLayout(FlowLayout.CENTER, GAP/2, GAP/2));
         freecellArea = new JPanel(new FlowLayout(FlowLayout.CENTER, GAP/2, GAP/2));
         freeAceArea = new JPanel();
         playStackArea = new JLayeredPane();

//FlowLayout(int align, int hgap, int vgap)
         freeAceArea.setLayout(new GridLayout(1, 2, GAP, GAP));
//GridLayout(int rows, int cols, int hgap, int vgap)

         freeAceArea.setPreferredSize(new Dimension(WINDOW_WIDTH,
                                                    TOP_HEIGHT));

         // aceStackArea = new JPanel(new FlowLayout(FlowLayout.TRAILING) );

         //playStackArea.setPreferredSize(new Dimension(WINDOW_WIDTH, TOP_HEIGHT));
         //SHOULD BE THIS:
         playStackArea.setPreferredSize(new Dimension(WINDOW_WIDTH,
                                                      BOTTOM_HEIGHT));


         for (i = 0; i < NUMBER_ACE_STACKS; i++)
         {
            freecellsButtons[i] = new CardButton(RetrieveIcon.getIcon(
                    RetrieveIcon.EMPTY_SPACE));
            assert RetrieveIcon.getIcon(RetrieveIcon.EMPTY_SPACE) != null;
            freecellsButtons[i].addActionListener(freecellListener[i]);
            setButtonSettings(freecellsButtons[i],
                              RetrieveIcon.EMPTY_SPACE);
            freecellArea.add(freecellsButtons[i]);


            aceStacksButtons[i] = new CardButton(RetrieveIcon.getIcon(
                    RetrieveIcon.JOKER));
            assert RetrieveIcon.getIcon(RetrieveIcon.JOKER) != null;
            aceStacksButtons[i].addActionListener(aceStackListener[i]);
            setButtonSettings(aceStacksButtons[i], RetrieveIcon.JOKER);
            aceStackArea.add(aceStacksButtons[i]);
         }

         deal();

         /*
          Shoe aShoe = new Shoe(1);
          Card aCard = aShoe.drawRandom();

          fun = new JButton(RetrieveIcon.getIcon(RetrieveIcon.EMPTY_SPACE));
          //scratch.addActionListener(playStackListener[i]);
          //setButtonSettings(fun, aCard);
          //setToTopOfPlayStack(scratch, i);
            
          //you must always set the bounds of an object before you put it in a JLayeredPane.
          fun.setBounds(10,10, 40,40);
          if (fun == null)
          throw new NullPointerException();
          //yes, got it to flicker!...like the rest.
          playStackArea.add(fun);
          */

         freeAceArea.add(freecellArea, BorderLayout.WEST);
         freeAceArea.add(aceStackArea, BorderLayout.EAST);

         add(freeAceArea, BorderLayout.NORTH);
         add(playStackArea, BorderLayout.SOUTH);
         // add(maybe, BorderLayout.SOUTH);
         //playStackArea.setVisible(true); 

         //???
         //It is NOT correctly displaying these two. But it does display this bottom one.
         //add(freecellArea);
      }

      GameWindow(String title)
      {
         super(title);
         setLocationRelativeTo(null);
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         setSize(WINDOW_WIDTH, BOTTOM_HEIGHT + TOP_HEIGHT);
         initialize();  //?use .pack?
         createLayout();

         Card aCard;
         if (DEBUGGING)
         {
            printAllStacks();
         }

         this.pack(); // ???
         this.setVisible(true);

      }

      final void printAllStacks()
      {  // DEBUGGING
         Card aCard;
         for (int i = 0; i < playStacksButtons.size(); i++)
         {
            System.out.println("The buttons in this stack, in order:");
            for (int j = 0; j < playStacksButtons.get(i).size(); j++)
            {
               if (playStacksButtons.get(i).get(j).isCard())
               {
                  aCard = playStacksButtons.get(i).get(j).getCard();
                  System.out.println(aCard);
               }
               else
               {
                  System.out.println("Not a card.");
               }
            }
         }

      }

      void removeFromPlayStackButtons(Card aCard)
      {
         final int index = getIndexOfPlayStackButtons(aCard);
         final int lastElement = playStacksButtons.get(index).size() - 1;
         playStacksButtons.get(index).remove(lastElement);
         //I have not implemented .equals and hence will not use remove(Object)
      }

      /**
       * Returns the index of the playStack whose top card corresponds
       * to topCard. If there is no playstack those top card is
       * topCard, returns a negative number.
       *
       * @param topCard
       * @return
       */
      int getIndexOfPlayStackButtons(final Card topCard)
      {
         int i;
         CardButton scratch;
         for (i = 0; i < playStacksButtons.size(); i++)
         {
            scratch = playStacksButtons.get(i).get(
                    playStacksButtons.get(i).size() - 1);
            if (!scratch.isCard());
            else if (scratch.getCard().equals(topCard))
            {
               return i;
            }


         }
         return -100000;
         //I am doing this so I can : (A) remove the old card from the right arraylist of cardbuttons
         //
         // (B) Add the card to the right arraylist of buttons.
      }

      private class ClickListener implements ActionListener
      {
         CardStack myStack; //all click listeners point to a stack
         //They can use the JFrame's objects, so they know what, if anything,
         //is selected
         //I only need 1 clicklistener object for each stack -- I can reuse the
         //same one for multiple cards in the stack.

         /**
          * This is clunkier than it should be because of
          * architechtural failings. I need to link CardStacks with
          * the arrays of JButtons they refer to. That way I wouldn't
          * have to look stuff up and use ad hoc solutions.
          *
          * @param from
          * @param to
          */
         private void makeMove(CardStack from, CardStack to,
                               CardButton myTop)
         {
            final boolean toWasNotOccupied;
            final Card lastCardInToStack;
            if (to.isOccupied())
            {
               toWasNotOccupied = false;
               lastCardInToStack = to.peek();
            }
            else
            {
               toWasNotOccupied = true;
               lastCardInToStack = null;
            }
            Card movedCard = from.pop();
            to.push(movedCard);
            if (DEBUGGING)
            {
               System.out.println(
                       "I am trying to a move a " + movedCard);
            }
            //UPDATE FROM STACK
            final boolean fromIsOccupied = from.isOccupied();

            //REMOVE CARD
            if (!fromIsOccupied)
            {
               //The stack you're moving from has no cards left in it. Change CardButton.
               currentlySelectedButton.setIcon(from.getDefaultImage());
               currentlySelectedButton.setBorder(null);
               currentlySelectedButton.setNoCard();
               //Chains up to the layout when I call validate.

            }
            else
            {
               //There are cards left in the current stack. It must be a PlayStack
               //must also remove from PlayStackButtons array.
               currentlySelectedButton.setVisible(false); //Bingo! This works!
               //Must remove from panel that it belongs to, not whole JFram:
               playStackArea.remove(currentlySelectedButton);
               removeFromPlayStackButtons(movedCard);

            }
            //UPDATE TO STACK
            if ((toWasNotOccupied) || to.isValidRemove() == false)
            {//The second part only is true if the stack is currently empty OR it's an ace stack.
               if (DEBUGGING)
               {
                  System.out.println(
                          "The stack I am moving to has nothing in it.");
               }
               
              
               
               //setButtonSettings (myTop, movedCard); -- does NOT do any of these:
                myTop.setCard(movedCard);
               myTop.setIcon(RetrieveIcon.getIcon(movedCard)); 
               myTop.setSelectedIcon(RetrieveIcon.getSelectedImage(movedCard));
               myTop.setBorder(null); 
               
               //change icon only, don't need to add a button.
            }
            else
            { //The to stack has been occupied previously. It must be a playstack.
               //Add one button to the last card. Arg math.

               final int playStackIndex = getIndexOfPlayStackButtons(
                       lastCardInToStack);
               if (DEBUGGING)
               {
                  System.out.println(
                          "Adding a card to a playStack whose last card is "
                          + lastCardInToStack + ", and whose index is " + playStackIndex);
               }
               addButtonToThisPlayStack(movedCard, playStackIndex);
            }

         }

         /**
          * This implementation sucks, and CardButton is a ghetto fix.
          * Alas. This should get fixed. Maybe the CardStacks always
          * contain a reference to their top and bottom buttons.
          *
          * @param topCard
          * @param mySource
          * @throws IllegalStateException if topCard is not actually
          * on the top of any playstack
          * @return CardButton at the top of the stack
          */
         public CardButton getTopButton(final Card topCard,
                                        final CardButton mySource)
         {

            Card clickedCard = mySource.getCard();
            if (topCard.equals(clickedCard))
            {
               return mySource;
            }

//Otherwise...you clicked on something not at the top. Only possible in the playStackButtons.
            int size;
            Card otherTopCard;
            CardButton otherTopButton;
            for (int i = 0; i < playStacksButtons.size(); i++)
            {
               size = playStacksButtons.get(i).size();
               otherTopButton = playStacksButtons.get(i).get(size - 1);
               if (otherTopButton.isCard())
               {
                  otherTopCard = otherTopButton.getCard();
                  if (otherTopCard.equals(topCard))
                  {
                     return playStacksButtons.get(i).get(size - 1);
                  }
               }
               /* catch (NullPointerException e)
                { System.err.println("My CardButton source is associated with card " + clickedCard + ".");
                System.err.println("I was looking at the following stack of cards: " + "");
                e.printStackTrace();
                } */

            }

//It should be impossible to get here.
            throw new IllegalStateException(
                    "Top button not found that corresponds to " + topCard);
         }

         /**
          * This can be expanded to move aces up automatically, move
          * other cards up automatically, or anything else.
          *
          */
         void postMoveChecks()
         {
            boolean gameFinished = true;
            for (int i = 0; (gameFinished) && (i < myAceStacks.length); i++)
            {
               if (!myAceStacks[i].isOccupied())
               {
                  gameFinished = false;
               }
               else if (myAceStacks[i].peek().getCardValue() != CardValue.KING)
               {
                  gameFinished = false;
               }
            }

            if (gameFinished)
            {
               JOptionPane.showMessageDialog(null,
                                             "Congratulations, you win! Have a nice day.",
                                             "Yay!",
                                             JOptionPane.PLAIN_MESSAGE);
               System.exit(0);
            }
         }

         /**
          *
          * @param aStack NOT CLONED so be careful.
          */
         ClickListener(CardStack aStack)
         {
            super();
            myStack = aStack; //Note lack of cloning
         }

         private void deselect()
         {
            currentlySelectedButton.setBackground(null);
            currentlySelectedButton.setSelected(false);

            somethingSelected = false;
            currentlySelected = null;
            currentlySelectedButton = null;
            if (DEBUGGING)
            {
               System.out.println(
                       "Stack that has been selected has been deselected.");
            }
         }

         @Override
         public void actionPerformed(ActionEvent event)
         {
            CardButton mySource = (CardButton) event.getSource();

            if (DEBUGGING)
            {
               if (myStack.isOccupied())
               {
                  if (mySource.isCard())
                  {
                     System.out.println(
                             "This card has been clicked on: " + mySource.getCard());
                  }
                  else
                  {
                     System.out.println(
                             "I have clicked on an empty space.");
                  }
               }
               else if (!somethingSelected)
               { //If the clicked-on stack is empty and nothing else is selected, do nothing.
                  System.out.println("This stack is empty.");
                  return;
               }
            }
            if (!somethingSelected)
            {
               //Is it legal to select this stack? If not, do nothing. If yes, select it.
               //In either case, return.
               if (myStack.isSelectable())
               {
                  //set as selected the TOP BUTTON in mySource
                  if (DEBUGGING)
                  {
                     System.out.println(
                             "My stack is selectable, and its top card is " + myStack.peek());
                  }

                  CardButton theTop = getTopButton(myStack.peek(),
                                                   mySource);
                  theTop.setSelected(true);
                  theTop.setBackground(niftyColor);
                  //no effect on JLayedPane. Creates a background on a JPanel though.

                  //theTop.setOpaque(true);

                  //doesn't work here.
                  somethingSelected = true;
                  currentlySelected = myStack;
                  currentlySelectedButton = theTop; // 
                  validate();
               }
               return;
            }

            //Something has been previously selected.

            if (myStack == currentlySelected)
            { //Deselect and return
               deselect();
               validate();
               return;
            }


            //At this point, I know that something else has been previously selected.
            //So test if the move is valid, then make it, or deselect if not legal.
            if (currentlySelected.isValidRemove() && myStack.isValidAdd(
                    currentlySelected.peek()))
            {
               CardButton myTop;
               if (!mySource.isCard())
               {
                  myTop = mySource;
               }
               else
               //Can't peek on an empty stack.
               {
                  myTop = getTopButton(myStack.peek(), mySource);
               }
               makeMove(currentlySelected, myStack, myTop);
               deselect();
               validate();
               postMoveChecks();
               return;
            }

            deselect();
            validate();
         }

      }

   }

   static public class Freecell implements CardStack
   {
      private Card aCard;
      boolean isOccupied;

      Freecell()
      {
         isOccupied = false;
      }

      @Override
      public boolean isSelectable()
      {
         if (isOccupied)
         {
            return true;
         }
         return false;
      }

      public boolean isOccupied()
      {
         return isOccupied;
      }

      @Override
      public boolean isValidAdd(Card aCard)
      {
         return !isValidRemove();
      }

      @Override
      public boolean isValidRemove()
      {
         if (isOccupied)
         {
            return true;
         }
         return false;
      }

      /**
       *
       * @return The card in the free cell.
       * @throws NPE if there is no card in the free cell.
       *
       */
      @Override
      public Card peek()
      {
         if (isOccupied)
         {
            return new Card(aCard);
         }
         else
         {
            throw new NullPointerException();
         }
      }

      /**
       * @throws NPE if there is no card in the free cell.
       * @return
       */
      @Override
      public Card pop()
      {
         if (!isOccupied)
         {
            throw new EmptyStackException();
         }
         else
         {
            isOccupied = false;
         }
         Card temp = new Card(aCard);
         aCard = null;
         return temp;
      }

      @Override
      public Card push(Card otherCard)
      {
         if (isOccupied)
         {
            assert false;
            throw new UnsupportedOperationException(
                    "Free cell is already occupied by " + aCard);
         }
         else
         {
            aCard = new Card(otherCard);
            isOccupied = true;
            return otherCard;
         }

      }

      @Override
      public ImageIcon getDefaultImage()
      {
         return RetrieveIcon.getIcon(RetrieveIcon.EMPTY_SPACE);
      }

   }

   static public class AceStack implements CardStack
   {
      boolean isStarted;
      Card topCard;

      /**
       *
       * UNTESTED
       *
       * @param aCard
       * @return true if the card is an ace and the stack is empty. If
       * the stack has been started, then true if the card is one
       * higher in value than the next card and the same suit. False
       * otherwise.
       *
       */
      @Override
      public boolean isValidAdd(Card aCard)
      {
         if (!isStarted)
         {
            if (aCard.getCardValue() != CardValue.ACE)
            {
               return false;
            }
            return true;
         }
         else if ((topCard.getSuit() == aCard.getSuit())
                 && ((aCard.value() - 1) == topCard.value()))
         {
            return true;
         }
         return false;
      }

      @Override
      public boolean isValidRemove()
      {
         return false;
      }

      @Override
      public Card peek()
      {
         if (!isStarted)
         {
            throw new NullPointerException();
         }
         else
         {
            return new Card(topCard);
         }
      }

      /**
       * You can't remove a card from this stack ever.
       *
       * @return
       */
      @Override
      public Card pop()
      {
         throw new UnsupportedOperationException();
      }

      /**
       * Tests the move for validity first.
       *
       * @param aCard
       * @return FALSE if the move is not legal.
       * @throws UnsupportedOperationException if you try to make an
       * invalid move
       */
      @Override
      public Card push(Card aCard)
      {
         if (!isValidAdd(aCard))
         {
            throw new UnsupportedOperationException();
         }
         topCard = new Card(aCard);
         isStarted = true;
         return aCard;
      }

      @Override
      public boolean isSelectable()
      {
         return false;
      }

      @Override
      public boolean isOccupied()
      {
         if (isStarted)
         {
            return true;
         }
         else
         {
            return false;
         }
      }

      /* FUTURE: Eventually make it return a different image by adding an int emptyStackGraphicCode
       which tells you which image to load.
       * */
      @Override
      public ImageIcon getDefaultImage()
      {
         return RetrieveIcon.getIcon(RetrieveIcon.JOKER);
      }

   }

   static public class PlayStack extends Stack<Card> implements CardStack
   {
      public boolean isRed(Card someCard)
      {
         if ((someCard.getSuit() == Suit.DIAMONDS)
                 || (someCard.getSuit() == Suit.HEARTS))
         {
            return true;
         }
         else
         {
            return false;
         }
      }

      @Override
      public boolean isValidAdd(Card aCard)
      {
         if (super.size() == 0)
         {
            return true;
         }
         Card topCard = peek();
         if (aCard.value() + 1 != topCard.value())
         {
            return false;
         }
         if (isRed(aCard) != isRed(topCard))
         {
            return true;
         }
         else
         {
            return false;
         }

      }

      @Override
      public boolean isValidRemove()
      {
         if (super.size() == 0)
         {
            return false;
         }
         else
         {
            return true;
         }
      }

      @Override
      public Card peek()
      {
         return super.peek();
      }

      @Override
      public Card pop()
      {
         return super.pop();
      }

      @Override
      public Card push(Card aCard)
      {
         if (!isValidAdd(aCard))
         {
            throw new UnsupportedOperationException(
                    "I can't add a " + aCard + " to a " + super.peek());
         }
         else
         {
            super.push(aCard);
         }
         return aCard;
      }

      /**
       * Used to initialize the stack when dealing.
       */
      void forcePush(Card aCard)
      {
         super.push(aCard);
      }

      @Override
      public boolean isSelectable()
      {
         if (super.empty())
         {
            return false;
         }
         return true;

      }

      @Override
      public boolean isOccupied()
      {
         if (this.isEmpty())
         {
            return false;
         }
         else
         {
            return true;
         }
      }

      @Override
      public ImageIcon getDefaultImage()
      {
         return RetrieveIcon.getIcon(RetrieveIcon.EMPTY_SPACE);
      }

   }

   
   static private class CardButton extends JButton
{
Card aCard;
boolean isCard;


/**
 *  Should make this just take a card as an argument since I can just 
 * get the icon anyway. it'd save code.
 * @param icon
 * @param someCard 
 */
CardButton(ImageIcon icon, Card someCard)
{ super(icon);
   aCard = new Card(someCard);
   isCard = true;
}

CardButton(ImageIcon icon)
{ super(icon);
   isCard = false;
}

boolean isCard ()
{ return isCard;}
        
Card getCard() throws NullPointerException
{ if (!isCard)
    throw new NullPointerException("This CardButton has no card associated with it.");
   return new Card(aCard);

}

boolean setCard(Card aCard)
{ //Can easily be misused.
 isCard =true;
 this.aCard = new Card(aCard);
 return true;
   
}

boolean setNoCard()
{ isCard = false;
        aCard = null;
   return true;
}

}
   
   public static void main(String[] args)
   {

      SwingUtilities.invokeLater(new Runnable()
      {
         @Override
         public void run()
         {
            // Here, we can safely update the GUI
            // because we'll be called from the
            // event dispatch thread
            GameWindow myWindow = new GameWindow(
                    "Freecell. No frills.");

         }

      });
   }
   
   
}
