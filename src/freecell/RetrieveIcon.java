package freecell;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class RetrieveIcon {


private final static Suit[] suitOrder = { Suit.CLUBS, Suit.SPADES, Suit.HEARTS, Suit.DIAMONDS};

private final static String directory = "images" + File.separator;

private static boolean allLoaded;

private final static boolean FOR_CLASS = true;
/*
 * NOT IMPLEMENTED YET
 */
final public static int EMPTY_SPACE = 53;
final public static int JOKER = 54;

private static final int NUMBER_OF_ICONS = 55; //for now, eventually an extra for the top

private static ImageIcon[] cardFronts = new ImageIcon[NUMBER_OF_ICONS];
private static ImageIcon[] selectedCards = new ImageIcon[NUMBER_OF_ICONS];

final public static int CLASS_IMAGE_WIDTH = 72;
final public static int CLASS_IMAGE_HEIGHT = 97;

final public static int HEIGHT_OFFSET = 17;

public static int getImageHeight()
{ if (FOR_CLASS)
   return CLASS_IMAGE_HEIGHT;
  else
   throw new UnsupportedOperationException();

   
}
    
public static int getImageWidth()
{if (FOR_CLASS)
   return CLASS_IMAGE_WIDTH;
  else
   throw new UnsupportedOperationException();
   
}

public static ImageIcon getIcon(Card aCard)
{
   loadAllIcons();
  assert getCardCode(aCard) <= 52;
     //
  
  return cardFronts[ getCardCode(aCard)];
}

private static int getCardCode(Card aCard)
{
   return getSuitValue(aCard.getSuit()) + (4* (aCard.value()-1));
   
   
}

/**
 * 
 * @param cardCode EMPTY_SPACE
 * @return 
 */
public static ImageIcon getIcon(int cardCode)  
{ loadAllIcons();
   return cardFronts[ cardCode];
   //this'll throw the exception for me.
}

private static int getSuitValue(Suit aSuit)
{
   switch(aSuit)
   { case CLUBS: return 1;
   case SPADES: return 2;
   case HEARTS:return 3;
   case DIAMONDS: return 4;
   default:
   assert false;
   return -10000000;
   }
}

private static void loadAllIcons()
{
         if (allLoaded)
         {
            return;
         }

 String filename;
 int cardCode;
 Card aCard;
 String extension= FOR_CLASS ? ".gif" : ".png";
         
for (CardValue myCV : CardValue.values())
{
  for (Suit mySuit: suitOrder )
{  aCard =new Card(mySuit,myCV);
   cardCode = getCardCode(aCard );
   if (!FOR_CLASS) {
    filename = String.valueOf(cardCode);
   }
   else {
      filename = getFileNameForClass(aCard);
   }

   cardFronts[cardCode] = new ImageIcon(directory + filename + extension);
     //CODE TO LOAD IMAGE GOES HERE
   selectedCards[cardCode] = makeASelectedImage(aCard, extension);
   //CREATE ALL SELECTED IMAGES HERE AND STORE THEM
   //System.out.println(directory + filename + extension);
   
}
}

if (!FOR_CLASS)
{
   cardFronts[EMPTY_SPACE] = new ImageIcon (directory + "56" + extension);
   cardFronts[JOKER] = new ImageIcon(directory + "54" + extension);
}
else
{
   cardFronts[EMPTY_SPACE] = new ImageIcon (directory + "BK" + extension);
   cardFronts[JOKER] = new ImageIcon(directory +"XH" + extension);
}

         allLoaded = true;
}
 

/** NEEDS TESTING
 * 
 * 
 * @param aCard
 * @return 
 */
static private String getFileNameForClass(Card aCard)
{ StringBuilder s = new StringBuilder();

//number then suit
switch(aCard.value())
{ case 10: s.append("T"); break;
  case 11: s.append("J"); break;
  case 12: s.append("Q"); break;
  case 13: s.append("K"); break;
  case 1: s.append("A"); break;
  default: s.append(aCard.value());
}
s.append(aCard.suit().charAt(0));
return s.toString();
   // scratch = directory + toCardValue(i)
//                       + toCardSuit(j) + extension;

   
}

/**
 * Helper function for loadAllIcons
 * This function needs work, no idea how to fix it.
 * 
 * @param aCard
 * @param extension
 * @return 
 */
static private ImageIcon makeASelectedImage(Card aCard, String extension)
{
   final String completeFileName;
   if (!FOR_CLASS)
     completeFileName = directory +String.valueOf(getCardCode(aCard)) + extension;
   else
      completeFileName = directory + getFileNameForClass(aCard) + extension;
     BufferedImage newImage;
      
   try {
    newImage = ImageIO.read(new File(completeFileName));
    
    //THIS BY DEFAULT LOOKS IN THE ROOT OF THE PROJECT
} catch (IOException e) {
    System.err.println("Problem reading file: " + completeFileName);
    e.printStackTrace();
    return null;
}
   if (newImage == null)
   {
    System.err.println("Problem reading file: " + completeFileName);
    return null;
   }

   //OK. GHETTO STYLE. This obviously isn't the best way to do it, but
   //I can't waste more time on it.
   for (int i = newImage.getMinTileX(); i <newImage.getWidth(); i++)
      for (int y = newImage.getMinTileY(); y < newImage.getHeight(); y++)
      {  if ( (( i+y) % 2) == 0 )
           newImage.setRGB(i, y, Game.niftyColor.getRGB());
      }
   return new ImageIcon(newImage);
   
 /*  CODE GRAVEYARD - of potential future use
   Graphics2D whizBang = newImage.createGraphics();

    whizBang.setColor(GameWindow.niftyColor);
  // whizBang.setXORMode(GameWindow.niftyColor);
whizBang.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OVER,
   0.5f));
   
//   AlphaComposite.

   whizBang.drawImage(newImage,0,0, GameWindow.niftyColor, null);   
  // whizBang.drawImage(newImage,0,0, null); //No diff between this line and the above line
   ImageIcon done = new ImageIcon(newImage);
    whizBang.dispose();  // ????? Should do but does it mess it up?
   return done;
*/

}

static public ImageIcon getSelectedImage(int cardcode)
{
   loadAllIcons();
   return selectedCards[cardcode];
}

    static public ImageIcon getSelectedImage(Card aCard)
    {
          loadAllIcons();
      return selectedCards[getCardCode(aCard)];
    }
}


