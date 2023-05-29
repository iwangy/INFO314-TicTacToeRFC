package Server;

public class GameState {
    /*
    0 : waiting for 2nd player
    1 : game currently in progress
    2 : game is over
     */
    private int gameStatus;
    private String[] players;
    private String[] playerids;
    private int turn;
    private String[][] board;
    private int winner;

    /*
    constructs the gamestate object
     */
    public GameState() {
        gameStatus = 0;
        players = new String[2];
        playerids = new String[2];
        turn = 0;
        board = new String[3][3];
        winner = -1;
    }

    /*
    pre:    a player name
    post:   adds the player into the game and returns a 0
            else, returns a 1
    */
    public int join(String playerName, String playerId) {
        if (players[0] == null) {
            players[0] = playerName;
            playerids[0] = playerId;
            return 0;
        } else if (players[1] == null) {
            players[1] = playerName;
            playerids[1] = playerId;
            return 0;
        } else {
            System.out.println("game is full");
            return 1;

        }
    }

    /*
    pre:    x and y coordinates on the tic tac toe board
    post:   makes the player move,  and return a 0 status code.
            if coordinates are out of bounds, return 1
            if spot alrady taken, return 2
     */
    public int move(int x, int y) {
        // check if the x or y coordinates are out of bounds
        if (x < 1 || x > 3) {
            return 1;
        }
        if (y < 1 || y > 3) {
            return 1;
        }

        // subtract 1 to x and y since arrays are 0 indexed
        x--;
        y--;

        // check if the spot is already taken
        if (this.board[x][y] != null) return 2;

        String marker = turn == 0 ? "X" : "O";

        // make move
        this.board[x][y] = marker;


        return 0;
    }

    public int getStatus() {
        return gameStatus;
    }

}
