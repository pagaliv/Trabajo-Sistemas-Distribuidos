package MD;

public class Card {
    //Atributes
    private int valor;
    private Suit suit;

    //Constructor
    public Card(int num, Suit suitCard) {
        //Pre:
        //Pos: We make a card with this atributes
        this.suit = suitCard;
        this.valor = num;
    }

    //we make a card without atributes
    public Card(){
        //Pre:
        //Pos: creamos a card aleatory
        this.suit = Suit.oros;
        this.valor = 5;
    }

    //Méthods
    public int getCardNum() {
        //Pre:
        //Pos: return the Card Number
        return this.valor;
    }

    public Suit getSuit() {
        //Pre:
        //Pos: return the card Suit
        return this.suit;
    }

    public String toString() {
        //Pre: the card was initialized;
        //Pos: return aa string with the number ande suit
        String cadena = "";
        switch(this.valor){
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                cadena = "["+this.valor+" de "+this.suit +"] ";break;
            case 8:
                cadena = "[Sota de "+this.suit +"] ";break;
            case 9:
                cadena = "[Caballo de "+this.suit +"] ";break;
            case 10:
                cadena = "[Rey de "+this.suit +"] ";break;
        }
        return cadena;
    }

    //Pre:
    //Post: Compare de cards and use the o1.copareTo
    public int compare(Card o1, Card o2) {
        return o1.compareTo(o2);
    }

    @Override
    public boolean equals(Object o) {
        boolean esIgual = false;
        if (o instanceof Card){
            Card cardObjeto = (Card) o;
            esIgual = this.equals(cardObjeto);
        }
        return esIgual;
    }
    public boolean equals(Card c) {
        //Pre: The card c was initialized;
        //Pos: return true if are the same card and false in the other case
        return (c.valor == this.valor && c.suit == this.suit);
    }

    public void show() {
        //Pre:
        //Pos: show the card
        System.out.println(this.toString());
    }

    public int compareTo(Card o2) {
        //Pre: the card o2 was initialized
        //Pos:  if the card o2 is biger return a negative number, if is the same return 0 and if is smaller return a positive number
        //The order: Bastos > Oros > Espadas > Copas, in case of same suit, the bigest is the card with bigger number

        //Inicializamos nuestra variable valor
        int valor = 0;


        if(this.suit == o2.suit){
            //Si son del mismo palo, comparamos sus valores
            valor = o2.valor - this.valor;
        }
        else if(this.suit == Suit.bastos){
            //Si nuestro palo es el de bastos, nuestra carta es mayor.
            valor = 1;
        }
        else if (o2.suit == Suit.bastos){
            //Si su palo es el de bastos, su carta es superior.
            valor = -1;
        }
        else if (this.suit == Suit.oros){
            //Si nuestro palo es el de oros, nuestra carta es mayor.
            valor = 1;
        }
        else if(o2.suit == Suit.oros){
            //Si su palo es el de oros, su carta es mayor.
            valor = -1;
        }
        else if (this.suit == Suit.espadas){
            //Si nuestro palo es el de espadas, nuestra carta es mayor.
            valor = 1;
        }
        else if (o2.suit == Suit.espadas){
            //Si su palo es el de espadas, su carta es superior.
            valor = -1;
        }
        return valor;
    }
    public int compareToReverse(Card o2) {
        //Pre:
        //Pos:
        return -this.compareTo(o2);
    }
}
