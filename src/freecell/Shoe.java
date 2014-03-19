package freecell;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;



/**
 * Contains all cards in a dealing shoe.
 *
 */
 public class Shoe
{
   private ArrayList<Card> myCards = new ArrayList<Card>();
   /**
    * This random number generator is overkill; change it if it's
    * going too slow. Alternately, create another thread whose sole
    * job is to generate random numbers, then use a synchronized
    * queue. That'd be cool!
    */
   private Random generator = new SecureRandom();


   public int numberOfCards()
   {
      return myCards.size();

   }

   /**
    * Constructs a shoe with the specified number of decks. For
    * example, Shoe(2) would create a new shoe with 2 decks.
    */
   public Shoe(int numberOfDecks)
   {
      if (numberOfDecks <= 0)
      {
         throw new IllegalArgumentException();
      }
      int i;
      for (i = 0; i < numberOfDecks; i++)
      {
         for (Suit theSuit : Suit.values())
         {
            for (CardValue cValue : CardValue.values())
            {
               myCards.add(new Card(theSuit, cValue));
            }
         }
      }
   }


   public void printContents()
   {
      System.out.println("The shoe contains a/an: ");
      for (int i = 0; i < myCards.size(); i++)
      {
         System.out.println(
                 myCards.get(i).getCardValue() + " of " + myCards.get(
                 i).getSuit());

      }
      System.out.println("for a total of " + myCards.size() + " cards.");

   }





   /**
    *
    * @return A random card from this Shoe, which has been pulled from
    * the Shoe.
    */
   Card drawRandom() 
   {
      if (myCards.size() == 0)
      {
         throw new UnsupportedOperationException("No cards left in shoe.");
      }
      final int index = (int) (generator.nextDouble() * (double) myCards.size());
      //As long as Java always rounds down when typecasting, I'm good.
      Card pulledCard = myCards.get(index);
      // I assume it doesn't matter that the deck is always in perfect order.
      myCards.remove(index);
      //does myCard still exist?
      return pulledCard;
   }


   

   /**
    * Utterly untested
    *
    * @param Card to be added.
    */
   public void addCard(final Card thisCard)
   {
      myCards.add(new Card(thisCard));
   }



}