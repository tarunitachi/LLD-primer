package org.tarunitachi.snakeandladder;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Data
@AllArgsConstructor
class Entity{
    int startPos;
    int endPos;
}


class Snake extends Entity{
    public Snake(int startPos, int endPos) {
        super(startPos, endPos);
    }
}

class Ladder extends Entity{
    public Ladder(int startPos, int endPos) {
        super(startPos, endPos);
    }
}

@Data
@AllArgsConstructor
class Dice{
    int generateRandom(){
        return new Random().nextInt(6);
    }
}

@Data
@AllArgsConstructor
class Board{
    int size;
}

@Data
@AllArgsConstructor
class Game{
    private Board board;
    private Dice dice;
    private List<Player> players;
    public void launchGame(){

    }
}

@Data
@AllArgsConstructor
class Player{
    int id;
    String name;
    int position;
}

public class SnakeAndLadderLauncher {
    public static void main(String[] args) {

        Player player1 = new Player(1, "Tarun", 0);
        Player player2 = new Player(2, "Kiran", 0);
        Player player3 = new Player(3, "Sai", 0);
        Player player4 = new Player(4, "Mounika", 0);
        Board board = new Board(10);
        Dice dice = new Dice();
        Game game = new Game(board, dice, Arrays.asList(player1, player2, player3, player4));

    }
}
