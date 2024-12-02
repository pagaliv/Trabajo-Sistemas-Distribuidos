package MD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Deck {
    private  ArrayList<Card> listaDeCards;
    public Deck(){
        //Pre:
        //Pos: Make a new Baraja empty;
        this.listaDeCards = new ArrayList<>();
    }
    public void shuffle(){
        //Pre:
        //Pos: Do random ordering in the Deck
            Collections.shuffle(this.listaDeCards);
    }
    private ArrayList<Card> getListaDeCartas(){
        //Pre:
        //Pos: return the array with the Card´s list
        return this.listaDeCards;
    }
    public int getNumeroCartas(){
        //Pre:
        //Pos: return the numbers of cards of us deck
        return this.listaDeCards.size();
    }
    public boolean addCard(Card cardAdded){
        //Pre:
        //Pos: add cardAdded in the Deck
        return this.listaDeCards.add(cardAdded);
    }
    public Card addCardInPosition(Card cardAdded, int position){
        //Pre: cardAdded was initialized
        //Pos: add the card in the position and return the card that was in this position
        if(position < 0){
            position =0;
        }
        if(position >= this.getNumeroCartas()){
            position = this.getNumeroCartas()-1;
        }
        return (Card) this.listaDeCards.set(position,cardAdded);
    }
    public boolean appears(Card cardToCompare){
        //Pre:
        //Pos: return true if cardToCompare is in the deck and false in the other case
        return this.listaDeCards.contains(cardToCompare);
    }
    public boolean deleteCard(Card deletedCard){
        //Pre:
        //Pos: delete the card deletedCard in the deck
        //return this.listaDeCartas.remove(deletedCard);
        return this.listaDeCards.remove(deletedCard);
    }
    public Card deleteCardInPosition(int posicion){
        //Pre: need a int.
        //Pos: Delete the card that is in this position and return the this card.
        if(posicion < 0){
            posicion = 0;
        }
        if(posicion >= this.getNumeroCartas()){
            posicion = this.getNumeroCartas()-1;
        }
        return (Card) this.listaDeCards.remove(posicion);
    }
    public Deck cloneBaraja(){
        //Pre:
        //Pos: return a Deck´s clone

        Deck copiaBaraja = new Deck();
        for (Card c : this.listaDeCards){
            copiaBaraja.addCard(c);
        }
        return copiaBaraja;
    }
    public Card extractCard(int posicion){
        //Pre:
        //Pos: return the card of this position
        if(posicion < 0){
            posicion = 0;
        }
        if(posicion >= this.getNumeroCartas()){
            posicion = this.getNumeroCartas()-1;
        }
        return this.listaDeCards.get(posicion);
    }
    public Card extractCard() {
        // Si el mazo tiene cartas, extraemos la carta en la primera posición y la eliminamos
        if (!listaDeCards.isEmpty()) {
            return listaDeCards.remove(0); // Elimina y devuelve la carta en la primera posición
        }
        return null; // Si no hay cartas, retorna null
    }

    public void showBaraja(){
        //Pre:
        //Pos: show the Dick
        System.out.println(this.toString());
    }
    public String toString() {
        //Pre:
        //Pos: return a string of the deck
        String cadena = "";
        for (Card c : this.listaDeCards) {
            cadena = cadena + c.toString();
        }
        return cadena;
    }
    public void clearBaraja(){
        //Pre:
        //Pos: clear the dick
        this.listaDeCards.clear();
    }
    public void fillBarajaEspanola(){
        //Pre:
        //Pos: Complete the deck with all the cards from the Spanish deck
        this.fillPaloEspanol(Suit.oros);
        this.fillPaloEspanol(Suit.bastos);
        this.fillPaloEspanol(Suit.espadas);
        this.fillPaloEspanol(Suit.copas);
    }
    public void fillPaloEspanol(Suit suit){
        //Pre: palo is a suit from palo
        //Pos: complete the deck with all the cards of the Spanish deck of a suit
        for(int i=1;i<=10;i++){
            Card cardAAnadir = new Card(i, suit);
            this.addCard(cardAAnadir);
        }
    }
    public void permuteCards(int i, int j){
        //Pre: 0<=i<=j< number of cards
        //Pos: permute the positions of i and j
        Card cartai = this.extractCard(i);
        Card cartaj = this.extractCard(j);

        this.addCardInPosition(cartai,j);
        this.addCardInPosition(cartaj,i);
    }
    public void ordenarBarajaAutomatico (){
        this.listaDeCards.sort(new Compara());
    }



    public int countPointCardsPlayer(){
        //Pre:
        //Pos: The method return the Player´s points, but we thought that the valor of king, horse and jack are 10, 9  and 8 because we haven`t got this cards in the deck
        int n=0;
        for (Card c1:this.listaDeCards
        ) {
            n+=c1.getCardNum();

        }
        return n;
    }
}
