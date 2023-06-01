package Server;

public class testing {
    public static void main(String... args) {
        GameState game = new GameState("jason is cool lmao");
        game.join("jasonid");
        game.join("henryid");

        System.out.println(game.move(2, 2, "jasonid"));
        System.out.println(game.displayBoard());
        System.out.println(game.move(1, 1, "henryid"));
        System.out.println(game.move(1, 3, "jasonid"));
        System.out.println(game.move(3, 3, "henryid"));
        System.out.println(game.move(3, 1, "jasonid"));
        System.out.println(game.displayBoard());
        System.out.println(game.getStatus());
        System.out.println(game.getWinner());
    }
}
