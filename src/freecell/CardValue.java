package freecell;

/**
 * Enumerated card values.
 */
public enum CardValue
{
   ACE(1), TWO(2), THREE(3), FOUR(4),
   FIVE(5), SIX(6), SEVEN(7), EIGHT(8), NINE(9), TEN(10), JACK(11),
   QUEEN(12), KING(13);
   final private int myValue;

   private CardValue(final int myValue)
   {
      this.myValue = myValue;
   }

   /** UNTESTED
    * 
    * @return 
    */
   public CardValue getNext() {
      return this.ordinal() < CardValue.values().length - 1
          ? CardValue.values()[this.ordinal() + 1]
          : null;
    //Shamelessly taken from:
    //  http://digitaljoel.nerd-herders.com/2011/04/05/get-the-next-value-in-a-java-enum/
    }
   
   /** UNTESTED
    * 
    * @return 
    */
   public CardValue getPrevious() {
      return this.ordinal() > 0  //Should it be > 1 ?
          ? CardValue.values()[this.ordinal() - 1]
          : null;
      //Adapted from getNext
    }
   /**
    * @return Value of the card. Stored as an int, so it's a deep
    * clone.
    */
   public int value()
   {
      return myValue;
   }

//modify this to do its thing to cardName???
   @Override
   public String toString()
   {
      //only capitalize the first letter
      String s = super.toString();
      if ((this.myValue == 1) || (this.myValue == 8))
      {
         return "An " + s.substring(0, 1) + s.substring(1).toLowerCase();
      }
      else
      {
         return "A " + s.substring(0, 1) + s.substring(1).toLowerCase();
      }
   }

}
