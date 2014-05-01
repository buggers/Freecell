package freecell;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

/* 

Known bugs:
* -Default onClickPressed flash animation still present.
* - Laggy, and controls aren't always predictable (needs better diagnosis before
    this can be fixed)
  -Dragging a card outside of the window is very slow
  -Random crashes? Needs better diagnosis.
   
* Future features:
* - Give user option to enable or disable auto moves to ace stacks
 * -Add custom graphics when moving a stack, so it's clear what you're moving
*  -Unshare the transfer handlers, or at least change the icon each time so that
   -the transfer image matches the card being moved; optionally, set the cardbutton
itself to be invisible so it looks like you're moving it.
     -Add deal button so you can replay if you have no moves left
     -(Don't auto-exit upon win.) This'd require nulling out/clearing a large
     -amount of variables.
 * -Double clicking a playstack button moves it to a freecell
 * -Add testing. 
 * -Better graphics
 * -Card moving animation
   -Review JavaDocs
 
 * Possible future directions:
 * AI to solve all games (or declare them unwinnable)
 * Separate logging output for verbosity
 * Check if there are no possible moves or one possible move

Finished  (v. 0.16a)
* Added pixel to cardwidth -- seemes to fix the clipping issue
* Changed card color slightly

Finished:  (v. 0.16)
* Fixed drag-and-drop-any-card bug, miscellaneous bugs
* Auto-move of aces and other cards when it would have no detrimental
  game impact (some would not like this, so this could be an option)
* Auto-move of stacks if cells are available via drag-and-drop

Finished: (v. 0.15)
-Implemented drag and drop

 */
public class Game
{
   
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
      /**
       * 
       * 
       * @return 0 for an ace stack, 1 for an occupied freecell stack,
       * a variable number for a playstack
       */
      //int getNumCardsInSequence();
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
      private boolean autoMoveToAceStacks;
      /**
       * Used only by NiftyTransferHandler so it knows the source stack.
       * Set by DragClickListener.
       * The equivalent of currentlySelectd
       */
      private CardStack stackBeingDragged;
      private CardButton currentlySelectedButton;
      private JButton deal = new JButton();
//Not implemented
      private JPanel freecellArea; //belongs in freeAceArea
      
      
      private JPanel aceStackArea; //belongs in freeAceArea
      private JPanel freeAceArea;
      private JLayeredPane playStackArea;
      private DragClickListener[] playStackListener, freecellListener, aceStackListener;
      private MoveAids myMoveAids;
       
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
       * I'm adding a bunch of code to this, it's now untested.
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

         scratch = new CardButton(RetrieveIcon.getIcon(aCard), aCard, myPlayStacks[playStackIndex]);
         scratch.addMouseListener(playStackListener[playStackIndex]);
                  
         setButtonSettings(scratch, aCard);
         //If the Card knows its stack, then...why did I do all that?
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
         myMoveAids = new MoveAids();
         myAceStacks = new AceStack[NUMBER_ACE_STACKS];
         aceStacksButtons = new CardButton[NUMBER_ACE_STACKS];
         aceStackListener = new DragClickListener[NUMBER_ACE_STACKS];

         myFreecells = new Freecell[NUMBER_ACE_STACKS];
         freecellsButtons = new CardButton[NUMBER_ACE_STACKS];
         freecellListener = new DragClickListener[NUMBER_ACE_STACKS];

         myPlayStacks = new PlayStack[NUMBER_PLAY_STACKS];


         playStacksButtons = new ArrayList<List<CardButton>>();
         for (i = 0; i < NUMBER_PLAY_STACKS; i++)
         {
            playStacksButtons.add(new ArrayList<CardButton>());
         }

         playStackListener = new DragClickListener[NUMBER_PLAY_STACKS];

         for (i = 0; i < myAceStacks.length; i++)
         {
            myAceStacks[i] = new AceStack();
            aceStackListener[i] = new DragClickListener(myAceStacks[i]);
            myFreecells[i] = new Freecell();
            freecellListener[i] = new DragClickListener(myFreecells[i]);
         }
         for (i = 0; i < myPlayStacks.length; i++)
         {
            myPlayStacks[i] = new PlayStack();
            playStackListener[i] = new DragClickListener(myPlayStacks[i]);
         }

         somethingSelected = false;
         currentlySelected = null;
         currentlySelectedButton = null;
         autoMoveToAceStacks = true;

      }

      /**
       * Helper function for setButtonSettings. Don't call me alone.
       *
       *
       * @param aButton
       */
      private void standardButtonSettings(CardButton aButton)
      {
         //Assumes you've already said aButton = new JButton(someIcon);
         
         aButton.setFocusable(false); //Needed for drag/drop to work.
         //I might need to declare this before the other stuff, too.
         
         aButton.setBorder(null);
         //aButton.setBackground(Color.WHITE); Should inherit the background of the pane.
         //? but doesn't hurt

         /*Can't be used on a JLabel:
         aButton.setFocusPainted(false);  //Bingo! This disables the stupid box.
         aButton.setBorderPainted(false);
         //
         */
         aButton.setOpaque(true);
         CardStack myStack = aButton.getMyCardStack();
         NiftyTransferHandler myHandler = new NiftyTransferHandler("icon", myStack);
         
         
         ImageIcon dragImage = RetrieveIcon.getIcon(RetrieveIcon.JOKER);
         
       myHandler.setDragImage(dragImage.getImage());
         
         aButton.setTransferHandler(myHandler);


      }

      private void setButtonSettings(CardButton aButton, Card aCard)
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
      private void setButtonSettings(CardButton aButton, int cardcode)
      {
         standardButtonSettings(aButton);
         aButton.setSelectedIcon(RetrieveIcon.getSelectedImage(
                 cardcode));
         
      }

      public boolean setAutoMoveToAceStacks(boolean value)
      {
          autoMoveToAceStacks = value;
          return true;
      }
      /**
       * This does NOT set the selected image -- so maybe it won't
       * ever be used.
       *
       * @param aButton
       */
      private void setButtonSettings(CardButton aButton)
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
                    RetrieveIcon.EMPTY_SPACE), myFreecells[i]);
            assert RetrieveIcon.getIcon(RetrieveIcon.EMPTY_SPACE) != null;
            freecellsButtons[i].addMouseListener(freecellListener[i]);
            setButtonSettings(freecellsButtons[i],
                              RetrieveIcon.EMPTY_SPACE);
            freecellArea.add(freecellsButtons[i]);


            aceStacksButtons[i] = new CardButton(RetrieveIcon.getIcon(
                    RetrieveIcon.JOKER), myAceStacks[i]);
            assert RetrieveIcon.getIcon(RetrieveIcon.JOKER) != null;
            aceStacksButtons[i].addMouseListener(aceStackListener[i]);
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
/**Call me in the context of moveAids.removeButton.
 * I think it's implicit that you're removing the top card.
 * Removes the button specified by the Card from the playstacks area.
 * Should not be used on the last button in a stack.
 * @param aCard 
 */
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

      
      /*
      @Override
      public int getNumCardsInSequence()
      {
          if (isOccupied())
             return 1;
          else
             return 0;
      }*/
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

      /*
      @Override
      public int getNumCardsInSequence()
      {
         return 0;
      }*/
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

      
      /**
       * TOTALLY UNTESTED
       * 
       * @param aCard
       * @param maxReachableCardDepth
       * @return False if the card is not reachable OR if the card is not in the stack
       *
       */
      //thePlayStack.isReachable(mySource.getCard(), maxMoveableCards)
      public boolean isReachable(Card aCard, int maxReachableCardDepth)
      {
          if (DEBUGGING)
              System.out.println("Entering PlayStack.isReachable");
          if (!isOccupied())
          {  assert false: "PlayStack.isReachable called on an empty stack.";
             return false;
          }
          
          final int searchDepth ; //1 more than how far you can go
          if (size() > maxReachableCardDepth)
              searchDepth = size();
          else searchDepth = maxReachableCardDepth;
          
          //OK, try popping them off?? to avoid redoing this class
          //int sequentialCards = 1, i;
          PlayStack scratch = new PlayStack();
          boolean success = false;
          for (int i = 0; i < searchDepth; i++)
          { 
              if (peek().equals(aCard))
              { success = true;
                break;
                  
              }
          else
              {
          Card currentCard = pop(); //Take the top card off
          scratch.forcePush(currentCard); //Store it
              if (isValidAdd(currentCard)) //if it's legal to re-add then it's in sequence
                  ;
              else
              { assert (!success);
                break; //Next card is not in sequence, and I didn't find anything
              }
              
              
              }

          }
           //Put all the cards back
          while (!scratch.isEmpty())
             forcePush (scratch.pop()); //put all the cards back
          
          if (DEBUGGING)
              System.out.println("PlayStack.isReachable:exiting with result " + success);
          return success;
          
          
      }
      
      /** UNTESTED (and unused)
       * 
       * 
       * @return The number of cards in sequential order in this stack
       */
      private int getNumCardsInSequence()
      {
          if (!isOccupied())
             return 0;
          //OK, try popping them off?? to avoid redoing this class
          int sequentialCards = 1, i;
          PlayStack scratch = new PlayStack();
          boolean doneFlag = false;
          while(!doneFlag)
          { 
          Card currentCard = pop(); //Take the top card off
          scratch.forcePush(currentCard); //Store it
              if (isValidAdd(currentCard)) //if it's legal to re-add then it's in sequence
                  sequentialCards++;
              else
              {
                  doneFlag = true;
              }
              if (size() == 0 )
                  doneFlag = true;
// 0 ->size
          //elementAt
          }
          while (scratch.peek() != null)
             forcePush (scratch.pop()); //put all the cards back
          
          return sequentialCards;
      }
      
   }

   
   
   class DragClickListener extends MouseAdapter {

CardStack myStack;//I'll probably need this.

DragClickListener(CardStack myStack)
{
  super();
  this.myStack = myStack;
}
        
  private CardButton getCB(MouseEvent e)
  {
      CardButton mySource = null;
            if (e.getSource() instanceof CardButton)
              mySource = (CardButton) e.getSource();
            else {
                System.out.println("Class DragClickListener associated with something"
                         + "that's not a CardButton.");
             if (DEBUGGING)
                 throw new RuntimeException();
             else return null; //Silent error
            }
            //Or throw a checked class cast exception
      return mySource;
  }
/**
 * Well I'll be, I do get here sometimes.
 * 
 * @param e 
 */
        @Override
        public void mouseClicked(MouseEvent e)
        {
            CardButton mySource = getCB(e);
            if (DEBUGGING)
            {    System.out.println("mouseClicked: on ");
                    if (mySource.isCard())
                     System.out.print(mySource.getCard());
                    else 
                        System.out.print (" an empty space.");
            System.out.println();
            }
        myMoveAids.singleClickOn(mySource, myStack);
        }
        
        /**
         * This seems to kill mouseClicked.
         * 
         * @param e 
         */
        @Override
        public void mousePressed(MouseEvent e)
        {
            CardButton mySource = getCB(e);
            
            TransferHandler handler = mySource.getTransferHandler();
            if (DEBUGGING)
            {
             if (mySource.isCard())
            System.out.println("DragClickListener.mousepressed: moving " + mySource.getCard());
             else 
                 System.out.println("DragClickListener.mousepressed: On a Freecell or Acestack.");
            }
            
            if (mySource == null)
                throw new NullPointerException("mySource is null");
            if (e == null)
                throw new NullPointerException("E is null");
            if (handler == null)
                throw new NullPointerException("Handler is null");

//Don't drag if something's already been selected
            
if (somethingSelected)
{
    if (DEBUGGING)
        System.out.println("\nDCL.mousePressed:I'm pressing on a stack when something"
                + "has previously beeen selected.");
//myMoveAids.singleClickOn(mySource, myStack);
    
//Something has already been selected
//interpret as a move and return -- theoretically handled by mouseClicked
    //HOWEVER, if there is a tiny drag, it will be handled below.
    //I haven't taken care of that case yet.s
return;
}

//Don't export if not selectable 

       if (myStack.isSelectable()) //Doesn't actually select it
       { 
        stackBeingDragged = myStack; //except when do I unselect it? Do I need to 
        //Note that if I do not press and hold, or move the mouse, it doesn't register as
        //drag.
        //Do this step in a deep playstack only if I have the tools to handle it.
        if (!(myStack instanceof PlayStack))
           handler.exportAsDrag(mySource, e, TransferHandler.COPY);
        else
        {
            final int maxMoveableCards = myMoveAids.maxCardsInMoveableStack();
            PlayStack thePlayStack = (PlayStack) myStack;
            //Is this card in the sequence??? takes a card in the stack, the
            //max number of moveable cards, and then returns true if it's reachable and false
            //if not.
            if (thePlayStack.isReachable(mySource.getCard(), maxMoveableCards))
                handler.exportAsDrag(mySource, e, TransferHandler.COPY);
            
        }
       }
       else
       {
           //OK. You can't select it. But it might be a valid move if you're just clicking on it.
          
       }
        }
        
}


 class MoveAids {

         void singleClickOn(CardButton mySource, CardStack myStack)
         {

            if (DEBUGGING)
            {
               if (myStack.isOccupied())
               {
                  if (mySource.isCard())
                  {
                     System.out.println(
                             "singleClickOn:This card has been clicked on: " + 
                      mySource.getCard());
                  }
                  else
                  {
                     System.out.println(
                             "singleClickOn: I have clicked on an empty space.");
                  }
               }
               else if (!somethingSelected)
               { //If the clicked-on stack is empty and nothing else is 
//selected, do nothing.
                  System.out.println("singleClickOn: This stack is empty.");
                  return;
               }
            }
            if (!somethingSelected)
            {
               myMoveAids.attemptSelection(myStack, mySource);
               return;
            }

            //Something has been previously selected.

            if (myStack == currentlySelected)
            { //Deselect and return
               deselect(true);
               
               return;
            }


   //At this point, I know that something else has been previously selected.
//So test if the move is valid, then make it, or deselect if not legal.
            myMoveAids.doMoveIfPossible(mySource, myStack, currentlySelected, currentlySelectedButton);

         }

         /** It's assumed that you have enough empty spaces to move the entire stack
          * from sourceButton on down, and that all those are in sequential order.
          * 
          * @param targetButton
          * @param targetStack
          * @param sourceStack
          * @param sourceButton
          * @return 
          */
         public boolean moveStackIfPossible(CardButton targetButton, PlayStack targetStack, PlayStack sourceStack,
                 CardButton sourceButton)
         {
             //Call doMoveIfPossible. With a fromButton of null.?
             //Separate out the button removal from a given stack.
             final Card topCard = sourceButton.getCard();
             if (!targetStack.isValidAdd(topCard))
                 return false;

             //Add all to scratch stack; remove them from the visible stack
             PlayStack scratchStack = new PlayStack(); //An invisible construct, used to hold
             //cards
             Card pulledCard;
             boolean removingLastCardFromStack = false;
             //If you are removing the last card from a stack, that has to be 
             //done in the normal way; otherwise this will demolish the button
             while (true)
             {
                 pulledCard = sourceStack.peek();
                 if (sourceStack.size() == 1 )
                 {
                     removingLastCardFromStack = true;
                     //Make move here.
                     break;
                 }
                 else {
                      removeButton ( getTopButton(pulledCard,sourceButton ));
                      sourceStack.pop();
                      //Now it's not at the top any more
                      scratchStack.forcePush(pulledCard);
                      }
                 if (!sourceStack.isOccupied() || pulledCard.equals(sourceButton.getCard()))
                     break;
             }
             if (removingLastCardFromStack)
             {
                 CardButton fromButton = getTopPlayStackButton(sourceStack.peek());
                 if (!doMoveIfPossible(targetButton, targetStack, sourceStack, fromButton))
                 { throw new IllegalStateException("Attempting to move the last card from"
                         + "a stack, but it failed: trying to move a " + fromButton.getCard() +
                         " to a " + targetButton.getCard() );
                 }
                 
             }
             
             //Now add all from scratchStack to the targetStack. Use doMove. If it returns
             //false then I messed up.
             while (scratchStack.isOccupied())
                 if (!doMoveIfPossible(targetButton, targetStack, scratchStack, null))
                 { throw new IllegalStateException("Failed to move " + scratchStack.peek() +
                         " to a " + targetButton.getCard() );
                 }
             
             
             return true;
         }
         
         
         //sourceStack.pop();
         /**
          * Call this to make a move.
          * 
          * @param myTarget
          * @param targetStack
          * @param sourceStack
          * @param fromButton Can be null if it doesn't exist.
          * @return 
          */
   boolean doMoveIfPossible(CardButton myTarget, CardStack targetStack, CardStack sourceStack, CardButton fromButton)
       {  
           final boolean buttonExists; 
           if (DEBUGGING)
       {
           System.out.print("\ndoMoveIfPossible: trying to move to a ");
           if (myTarget.isCard())
              System.out.print(myTarget.getCard() );
           else System.out.println("space that is not a card.");
                   
       }
            if (sourceStack.isValidRemove() && targetStack.isValidAdd(
                    sourceStack.peek()))
            {  if (DEBUGGING)
                System.out.println("doMoveIfPossible: The move was judged valid.");
               CardButton myTop;
               if (!myTarget.isCard())
               {
                  myTop = myTarget;
               }
               else
               //Can't peek on an empty stack.
               {
                  myTop = getTopButton(targetStack.peek(), myTarget);
               }
               //I need the from button
               
               buttonExists = makeMove(sourceStack, targetStack, myTop, fromButton);
               deselect(buttonExists);
               
               postMoveChecks();
               if (DEBUGGING)
                   System.out.println("doMoveIfPosible: Exiting and returning true.");
               return true;
            }
            buttonExists = (fromButton != null);
            deselect(buttonExists);
            
            return false;
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

//Otherwise...you clicked on something not at the top. Only possible in the 
//playStackButtons.
            return getTopPlayStackButton(topCard);
            
            
            }

            
         
/** Given a card, returns the CardButton which corresponds to that card, if
 * the card is the top card of a PlayStack stack. Else, throws an IllegalStateException
 * This function needs some kind of type-checking.
 * @param topCard
 * @return 
 */
private CardButton getTopPlayStackButton(final Card topCard)
            {
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
                { System.err.println("My CardButton source is associated with 
card " + clickedCard + ".");
                System.err.println("I was looking at the following stack of 
cards: " + "");
                e.printStackTrace();
                } */

            }
            throw new IllegalStateException(
                    "Top button not found that corresponds to " + topCard);
            }

/** Deselects the selected card.
 * 
 */
private void deselect(boolean buttonExists)
         {
            
             if (somethingSelected)
             {
            if (buttonExists)
            { currentlySelectedButton.setBackground(null);
              currentlySelectedButton.setSelected(false);
            }
            somethingSelected = false;
            currentlySelected = null;
            currentlySelectedButton = null;
                        if (DEBUGGING)
            {
               System.out.println(
                       "deselect: Stack that has been selected has been deselected.");
            }
            validate();
             }
            
         }

/**
*Moves topCard to the ace stack it belongs to, or an empty one if topCard is an Ace.
* This function assumes that topCard is the top card of the sourceStack and that there
* is an ace stack to which you can move topCard.
* 
* @param topCard the card to be moved
* @param sourceStack the stack topCard belongs to
* @boolean isFromPlayStack true if sourceStack is a PlayStack, false if it's a FreecellStack
* 
*/
private void autoMoveToAceStack(Card topCard, CardStack sourceStack, boolean
        isFromPlayStack)//From Card, From Stack
                     //Note that in some cards the scratchCard is an ace card and can
                     //be moved to any stack
{ assert (sourceStack.isOccupied());
if (DEBUGGING) 
    System.out.println("Entering autoMoveToAceStack with card " + topCard);
CardButton fromButton = null, targetButton = null;
CardStack targetStack = null;
int i;
if (isFromPlayStack)
        {
            fromButton = getTopPlayStackButton(topCard);
        }
else
  {
    for (i = 0; i < freecellsButtons.length; i++)
    {   if (freecellsButtons[i].isCard())
           if (topCard.equals( freecellsButtons[i].getCard()))
           { fromButton = freecellsButtons[i];  
           break;
           }   
    }
    
    
   }
assert (fromButton != null);
final boolean isAce;
Card targetCard = null;
isAce = (topCard.getCardValue() == CardValue.ACE);
if (!isAce)
 targetCard = new Card(topCard.getSuit(), topCard.getCardValue().getPrevious());
for (i = 0; i < aceStacksButtons.length; i++)
{   if (isAce && !aceStacksButtons[i].isCard())
    {   targetButton = aceStacksButtons[i];
        targetStack = myAceStacks[i];
        break;
    }
    if (!isAce && aceStacksButtons[i].getCard().equals(targetCard))
    {
        targetButton = aceStacksButtons[i];
        targetStack = myAceStacks[i];
        break;
        
    }
    
}
assert (targetStack != null);
assert (targetButton != null);
//Go through Ace stacks to find the one that's one less than topCard, or one that's
//empty if it's an ace. then set targetbutton and targetStack
 
   //Need to get CardButtons. Hard to do from CardStacks alone.
           //Perhaps have a getTopButton function and setTopButton????
    //What function should I call, either makeMove or doMoveIfPossible??
           
//boolean doMoveIfPossible(CardButton myTarget, CardStack targetStack, 
           //CardStack sourceStack, CardButton fromButton)
  if (!doMoveIfPossible(targetButton, targetStack, sourceStack, fromButton))
  {
      //Logic error if I get here.
   assert false: "Move ruled impossible in moveAids.autoMoveToAceStack. Card " +
           fromButton.getCard() + " is the card being moved.";
  }
  
}

         /**
          * This can be expanded to move aces up automatically, move
          * other cards up automatically, or anything else.
          *
          */
         void postMoveChecks()
         {
             boolean automaticMovesLeft;
             int i,j;
             Card scratchCard, topCard, topAceCard;
             CardValue scratchCV;
             ArrayList<Card> autoMoveCards = fillAutoMoveCards();
             boolean isFromStackPlayStack;
             System.out.println("Entering postMovechecks");
             
          if (autoMoveToAceStacks)
          {   
             if (DEBUGGING)
             {
                 System.out.println("postMoveChecks: I can move " + autoMoveCards.size()
                         + " cards up automatically: ");
                 for (i =0; i < autoMoveCards.size(); i++)
                         System.out.print(autoMoveCards.get(i) +"; ");
             }
             for (int k = 0; k < autoMoveCards.size(); k++ )
             {
                 scratchCard = autoMoveCards.get(k); //Card eligible for automatic move.
             //Now iterate over all free cells and card stacks and look for card matches
                 
             for (j=0; j < myFreecells.length; j++)
             {
                 if (myFreecells[j].isOccupied() && myFreecells[j].peek().equals(scratchCard)) 
                 { 
                     isFromStackPlayStack = false;
                     myMoveAids.autoMoveToAceStack(scratchCard, myFreecells[j], isFromStackPlayStack);//From Card, From Stack
                     //Note that in some cards the scratchCard is an ace card and can
                     //be moved to any stack
                     return; // this function will automatically be called again by autoMove
                     
                 }
                                  
             }
             for (j = 0; j <myPlayStacks.length; j++)
             {
                 if (myPlayStacks[j].isOccupied() && myPlayStacks[j].peek().equals(scratchCard))
                 { 
                     isFromStackPlayStack = true;
                     autoMoveToAceStack(scratchCard, myPlayStacks[j], isFromStackPlayStack);//From Card, From Stack
                     return;// this function will automatically be called again by autoMove
                 }
                 
             }    
             
             
             }
          }   
            boolean gameFinished = true;
            for (i = 0; (gameFinished) && (i < myAceStacks.length); i++)
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


/** Totally untested.
 * Helper function for postMoveChecks.
 * Takes the array of cards that could be moved to the ace stacks,
 * and returns the array of cards that can be moved without adversely impacting
 * gameplay.
 * @param potentialMoveCards
 * @return 
 */
private ArrayList<Card> fillAutoMoveCards ()
{ ArrayList<Card> autoMoveCards = new ArrayList<>();
int i,j;
int numberOppSuitBelow;
Card scratchCard, topCard;
CardValue scratchCV;
ArrayList<Card> potentialMoveCards = new ArrayList<>();
             
 for (j= 0; j < myAceStacks.length; j++)
 {
   if (myAceStacks[j].isOccupied()) //If there's a card already there
  {
   scratchCard = myAceStacks[j].peek();
   scratchCV = scratchCard.getCardValue();
   if (scratchCV != CardValue.KING) //and it's not a king
    {   scratchCard = (new Card(scratchCard.getSuit(), scratchCV.getNext()));
        if (scratchCard.getCardValue() == CardValue.TWO)
            autoMoveCards.add(scratchCard); //If you can move a two up, you should
        //since you can't use them to build on (since aces automatically go up)
        else
            potentialMoveCards.add(scratchCard);
       
    }
                     
  }
                 
}
             
if (DEBUGGING)
{
    System.out.println("fillAutoMoveCards: Potentially moveable cards: ");
    for (j=0 ; j< potentialMoveCards.size(); j++)
    {
        System.out.print(potentialMoveCards.get(j));
    }
    
}

for (Suit s: Suit.values())
{
  autoMoveCards.add(new Card(s, CardValue.ACE));
}



//Next, see if the two opposite colored cards below it in value have
//already been moved to the ace stacks. (won't work for Twos)
             for (j = 0; j < potentialMoveCards.size(); j++)
             { scratchCard = potentialMoveCards.get(j);
                 numberOppSuitBelow = 0;
               for (i=0; i < myAceStacks.length; i++)
               { 
                   if (myAceStacks[i].isOccupied())
                   {    topCard = myAceStacks[i].peek(); 
                      if (topCard.getCardValue().value() >= scratchCard.getCardValue().getPrevious().value() )
                      {  if (scratchCard.isRed() && topCard.isBlack() )
                              numberOppSuitBelow++;       
                          else if  (scratchCard.isBlack() && topCard.isRed() )
                              numberOppSuitBelow++;
                      } 
                   }
               }
                if (numberOppSuitBelow >= 2)
                  autoMoveCards.add ( potentialMoveCards.get(j));
             
             }
return autoMoveCards;                         
            
}
         


void attemptSelection(CardStack myStack, CardButton mySource)
{
               //Is it legal to select this stack? If not, do nothing. If yes, 
// select it.
               //In either case, return.
               if (myStack.isSelectable())
               {
                  //set as selected the TOP BUTTON in mySource
                  if (DEBUGGING)
                  {
                     System.out.println(
             "attemptSelection: selecting " + myStack.peek());
                  }

                  CardButton theTop = getTopButton(myStack.peek(),
                                                   mySource);
                  theTop.setSelected(true);
                  theTop.setBackground(RetrieveIcon.niftyColor);
                  //no effect on JLayedPane. Creates a background on a JPanel 
                  //though.

                  //theTop.setOpaque(true);

                  //doesn't work here.
                  somethingSelected = true;
                  currentlySelected = myStack;
                  currentlySelectedButton = theTop; // 
                  validate();
               }
               return;
}

/** This function returns the maximum amount of cards you can grab from a playstack, 
 * based on the number of free cells and empty playstacks. Needs testing.
 * Of course, you can only grab cards that are already in sequential order. But that's
 * the province of some other function.
 */
private int maxCardsInMoveableStack()
{ int numberFreeCells = 0;
  int numberFreeStacks = 0;
  int i;
  for (i=0; i < myFreecells.length; i++)
      if (!myFreecells[i].isOccupied())
          numberFreeCells++;
  for (i=0; i <myPlayStacks.length; i++)
      if (!myPlayStacks[i].isOccupied())
          numberFreeStacks++;
  final int totalOpen = numberFreeCells + numberFreeStacks;
  int maxCardMoveable=1; // You can always do 1
  for (i=1; i <= numberFreeStacks; i++)
      maxCardMoveable += (totalOpen + 1 - i); 
  maxCardMoveable += numberFreeCells;
  return maxCardMoveable;
    
}

         /**
          * This is clunkier than it should be because of
          * architectural failings. I need to link CardStacks with
          * the arrays of JButtons they refer to. That way I wouldn't
          * have to look stuff up and use ad hoc solutions.
          *
          * @param from
          * @param to
          * @param myTop
          * @param fromButton Can be null if the the CardStack you're moving from does not
          * exist in the layout.
          * @return True if fromButton exists post-removal, false if not.
          */
         private boolean makeMove(CardStack from, CardStack to,
                               CardButton myTop, CardButton fromButton)
                 
         {
            final boolean toWasNotOccupied;
            final Card lastCardInToStack;
            //The selected button will exist under what cases.
            //Move from and to the (non-visual) cardstacks
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
                       "makeMove: I have moved a " + movedCard);
            }
            
            
            //Remove button from layout
            final boolean buttonExists = removeButton(fromButton);
            //true if the button exists post-removal
            
            //UPDATE TO STACK
            if ((toWasNotOccupied) || to.isValidRemove() == false)
            {//The second part only is true if the stack is currently empty OR it's an ace stack.
               if (DEBUGGING)
               {
                  System.out.println(
                          "makeMove:The stack I am moving to has nothing in it.");
               }
               
              
               
               //setButtonSettings (myTop, movedCard); -- does NOT do any of these:
               myTop.setCard(movedCard);
               myTop.setIcon(RetrieveIcon.getIcon(movedCard)); 
               myTop.setSelectedIcon(RetrieveIcon.getSelectedImage(movedCard));
               myTop.setBorder(null); 
               myTop.invalidate();
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
                          "makeMove: Adding a card to a playStack whose last card is "
                          + lastCardInToStack + ", and whose index is " + playStackIndex);
               }
               addButtonToThisPlayStack(movedCard, playStackIndex);
            }
                if (DEBUGGING)
                    System.out.println("Exiting makeMove");
                return buttonExists;
         }

/**
 *  This does NOT alter the stacks. It only removes the visible button from the layout.
   Returns true if the card continues to exist in the layout afterwards. False otherwise.
 * @param aButton
 * @returns true if the button exists after "removal" (it was the final card in a stack)
 * ; false if the button does not(or should not) exist
 */
         private boolean removeButton(CardButton aButton)
         {   if (aButton == null)
             return false;
         Card movedCard = aButton.getCard();
         CardStack from = aButton.getMyCardStack();
             final boolean fromIsOccupied = from.isOccupied();
              if (!fromIsOccupied)
              {
               //The stack you're moving from has no cards left in it. Change CardButton.
                if (DEBUGGING)
                {
                    System.out.println("myMoveAids.removeButton: Removing a card from an empty stack.");
                }
                
                setButtonSettings(aButton); 
                aButton.setNoCard();
                aButton.setIcon(from.getDefaultImage());
                aButton.setBorder(null);
                
                aButton.invalidate(); // ?
                return true;
               //Chains up to the layout when I call validate.

              }
             else
              {
               //There are cards left in the current stack. It must be a PlayStack
               //must also remove from PlayStackButtons array.
               aButton.setVisible(false); //Bingo! This works!
               //Must remove from panel that it belongs to, not whole JFram:
               playStackArea.remove(aButton);
               removeFromPlayStackButtons(movedCard);
               return false;
              }
            
         }
         
       }
 
 
 /** Problem: There is no means of changing myStack, even though the cards move around.
  * Note: No, I don't think they move. I just add and remove them.
  * Also, I need to associate these with the CardButtons, in a central manner.
  * 
  */
 class NiftyTransferHandler extends TransferHandler {
      // final String owner; //For debugging only
       
       final String stringRepJLabel = DataFlavor.javaJVMLocalObjectMimeType +
   ";class=javax.swing.JLabel";
  final DataFlavor mysteryFlavor;
final CardStack myStack;
        NiftyTransferHandler(String msg, CardStack myStack)
        { super(msg);
        this.myStack = myStack;
        //this.owner = owner;
        try {
        mysteryFlavor = new DataFlavor(stringRepJLabel);
        //System.out.println("The mystery flavor has correctly found the class.");
            }
            catch (ClassNotFoundException e)
                    {
                        throw new RuntimeException(e);
                    }
        }        
        
/**protected Transferable createTransferable(JComponent c)
This is the key function that I needed to override --
Creates a Transferable to use as the source for a data transfer.
* Returns the representation of the data to be transferred, or null 
* if the component's property is null
         */
@Override
        protected Transferable createTransferable(JComponent source)
        {JLabel aLabel;
            if (source instanceof JLabel)
            {
                aLabel = (JLabel) source;
                return new buttonTransferable(aLabel);
            }
            else return super.createTransferable(source);
        }

        
        /** When dragging from A to B, B's importData function is activated
         * 
         * (When dragging from A to B, A's exportDone function is activated.)
         * This only works from CardButton to CardButton. Otherwise it returns false.
         * @param support
         * @return 
         */
        @Override
        public boolean importData(TransferHandler.TransferSupport support)
        {
            if (DEBUGGING)
            System.out.println("importData: entering function");
            CardButton myTarget, sourceLabel;
            if (support.getComponent() instanceof CardButton)
                myTarget = (CardButton)support.getComponent();
            else {
                if (DEBUGGING)
                    System.out.println("NTH.importData: Released drag on a non-CardButton.");
                return false;
            }
            
            sourceLabel = null;
            
            if (hasMysteryFlavor(support.getDataFlavors()))
            { 
                try {
            if (support.getTransferable().getTransferData(mysteryFlavor) instanceof
                    CardButton )
                sourceLabel = (CardButton) 
                     support.getTransferable().getTransferData(mysteryFlavor);
            else 
            {  System.out.println("NTH.importData: My source label claims not to be a CardButton.");
                return false;
            }
            
                }
                catch (IOException | UnsupportedFlavorException e)
                 {  e.printStackTrace();
                    return false;
                }
                
              if (DEBUGGING) 
              {    System.out.println("importData: Trying to move "
              + sourceLabel.getCard() +" to ");
              if (myTarget.isCard)
                  System.out.print(myTarget.getCard());
              else System.out.print ("an ace or freecell stack.");
              }
              //myMoveAids.singleClickOn(mySource, myStack);
              if (sourceLabel == myTarget)
              {
                  myMoveAids.singleClickOn(sourceLabel, myStack);
                  return false;
              }                  
//Well, you can drag to yourself
              //Dragging to yourself is equivalent to selecting, if possible

// if they do = each other, just deselect them both.
              
              //OK. I know the sourceLabel is selectable.
              
              
              //If my source button is in a playstack, but not at the top, I need
              //to do something different
              final boolean moveDone;
              if (stackBeingDragged.peek().equals(sourceLabel.getCard()))
              moveDone = 
              myMoveAids.doMoveIfPossible(myTarget, myStack, stackBeingDragged, sourceLabel);
              else
              { 
              assert (stackBeingDragged instanceof PlayStack);
              if  (!(myStack instanceof PlayStack)) //you can't drag a stack to anything but
                   return false; //another playstack
              //OK I am dragging from midway up a playstack to another playstack
                  //I know I have enough free cells to do this
                  moveDone = myMoveAids.moveStackIfPossible(myTarget, (PlayStack) myStack, (PlayStack) stackBeingDragged,sourceLabel);
                  
              }
              
              
              stackBeingDragged = null;
              //This code is where I have access to both JLabels. Right frickin' here.
              //myTarget.setText("I've been dragged!");
              //sourceLabel.setText("I've been dropped!");

              return moveDone;
              
            }
         if (DEBUGGING)
         {
             System.out.println("NTH.importData: my target does not contain the right data flavor.");
             
         }
        return false;
                    
            
        }
        
        
        
    private boolean hasMysteryFlavor(DataFlavor[] flavors) 
    {
        if (mysteryFlavor == null) {
             return false;
        }
        for (int i = 0; i < flavors.length; i++) 
            if (mysteryFlavor.equals(flavors[i])) 
            { //System.out.println("Returning true from hasMysteryFlavor.");
                return true;
            }

        return false;

    }

    /** You need to override this.
     * 
     * 
     */
    @Override
        public boolean canImport(JComponent c, DataFlavor[] flavors) {
            return hasMysteryFlavor(flavors);
    }

        
          class buttonTransferable implements Transferable {
//It has to be transferring icons too???????
    protected final DataFlavor[] supportedFlavors = {
        mysteryFlavor,
            //,dragLabelFlavor
    };
    
    JLabel myLabel;
    
    public buttonTransferable(JLabel aLabel)
    {this.myLabel = aLabel;
    }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            return supportedFlavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
        if ( flavor.equals(mysteryFlavor))
           return true;
        return false;
        }
        
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
        {  if (flavor.equals(mysteryFlavor))
            return myLabel;
          else
            throw new UnsupportedFlavorException(flavor);
        }
        
        
        
    }    
    }
 
   }
   
   
   
   
   
   /** Extends JLabel.
    * 
    */
   static private class CardButton extends JLabel
{
Card aCard;
boolean isCard;
Icon selectedImage;
boolean selected;
Icon standardIcon;
CardStack myStack;


/** Sets whether or not this card is selected. Changes the icons if the
 * button represents a card.
 * 
 * 
 * @param value 
 */
public void setSelected(boolean value)
{
    if (selected == value)
        return;
    selected = value;
    assert (!selectedImage.equals(standardIcon));
    
    //You can deselect a button whose card is null.
    if (isCard)
    {
        
        //ERROR CHECKING
    if (!this.standardIcon.equals(RetrieveIcon.getIcon(aCard)))
    {
        System.out.println("CB.setselected: My card is " + aCard + ", but its icon is "
                + "for something else.");
        assert false;
    }
    if (!this.selectedImage.equals(RetrieveIcon.getSelectedImage(aCard)))
    {
        System.out.println("CB.setselected: My card is " + aCard + ", but its selected icon is "
                + "for something else.");
        assert false;
        
    }
    
    if (value && selectedImage != null)
       this.setIcon(selectedImage);
    if (!value)
        this.setIcon(standardIcon);
    
    }
    
    if (DEBUGGING & !value)
        System.out.println("CardButton.setSelected: Deselecting "+ aCard);
    this.invalidate(); 
    //Do I have to validate here?? Or invalidate??
}

/*Why am I overriding this??

@Override
public void setIcon(Icon anIcon)
{
    super.setIcon(anIcon);
    
}*/
/**
 *  Should make this just take a card as an argument since I can just 
 * get the icon anyway. it'd save code.
 * @param icon
 * @param someCard 
 */
CardButton(ImageIcon icon, Card someCard, CardStack myStack)
{ super(icon);
//you can create an ImageIcon from an Image.
   aCard = new Card(someCard);
   isCard = true;
   selectedImage = null;
   selected =false;
   this.myStack = myStack;
   standardIcon = icon;
}


CardButton(ImageIcon icon, CardStack myStack)
{ super(icon);
standardIcon = icon;
   isCard = false;
   selectedImage = null;
   selected = false;
   this.myStack = myStack;
}

/** 
 *  UNTESTED
 * @return 
 */
CardStack getMyCardStack()
{
 return myStack;
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

boolean setSelectedIcon (ImageIcon selectIcon)
{
 selectedImage = selectIcon;
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
