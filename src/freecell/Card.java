package freecell;

/**
 * An individual card. Contains a suit and value.
 *
 */
public class Card
{
   private final Suit mySuit;
   private final CardValue myValue;

   /**
    * Untested
    *
    *
    * @param mySuit
    * @param myValue
    */
   public Card(final Suit mySuit, final CardValue myValue)
   {

      this.mySuit = mySuit;
      this.myValue = myValue;
   }

   public Card(final Suit mySuit, final CardValue myValue,
               final boolean noclone)
   {
      this.mySuit = mySuit;
      this.myValue = myValue;

   }

   /**
    * Ideally, a copy constructor (deep clone). Untested, so beware.
    * It hinges on enum encapsulation.
    *
    * @param toBeCloned Card to be cloned.
    *
    */
   public Card(final Card toBeCloned)
   {
      this.mySuit = toBeCloned.getSuit();
      this.myValue = toBeCloned.getCardValue();

   }

   public String suit()
   {
      return mySuit.name();
   }

   public int value()
   {
      return myValue.value();
   }

   public CardValue getCardValue()
   {
      return myValue;
   }

   /**
    * Gives a deep clone. Yay.
    *
    *
    * @return
    */
   public Suit getSuit()
   { //public enum Suit { SPADES, CLUBS, DIAMONDS, HEARTS ;
      return mySuit;
   }

   @Override
   public String toString()
   {
      return this.myValue.toString() + " of " + this.mySuit.toString();

   }

   @Override
   public boolean equals(Object something)
   {
      Card otherCard;
      if (something instanceof Card)
      {
         otherCard = (Card) something;
      }
      else
      {
         return false;
      }
      if (otherCard == this)
      {
         return true;
      }

      if ((otherCard.getSuit() == this.mySuit)
              && (otherCard.getCardValue() == this.myValue))
      {
         return true;
      }

      return false;
   }

}
