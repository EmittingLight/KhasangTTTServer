package yaga;

import java.io.*;
import java.net.*;

public class TicTacToeServer {
    private ServerSocket serverSocket;
    private Socket player1Socket;
    private Socket player2Socket;
    private BufferedReader player1In;
    private PrintWriter player1Out;
    private BufferedReader player2In;
    private PrintWriter player2Out;
    private char[] board = new char[9];
    private char currentPlayer;
    private int playerCount = 0;

    public TicTacToeServer() {
        try {
            serverSocket = new ServerSocket(5050);
            System.out.println("Сервер запущен...");

            while (true) {
                if (playerCount == 0) {
                    player1Socket = serverSocket.accept();
                    player1In = new BufferedReader(new InputStreamReader(player1Socket.getInputStream()));
                    player1Out = new PrintWriter(player1Socket.getOutputStream(), true);
                    playerCount++;
                    System.out.println("Игрок 1 подключился.");
                } else if (playerCount == 1) {
                    player2Socket = serverSocket.accept();
                    player2In = new BufferedReader(new InputStreamReader(player2Socket.getInputStream()));
                    player2Out = new PrintWriter(player2Socket.getOutputStream(), true);
                    playerCount++;
                    System.out.println("Игрок 2 подключился.");
                    startNewGame();
                    runGame();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runGame() {
        try {
            while (true) {
                if (currentPlayer == 'O') {
                    String messageFromPlayer1 = player1In.readLine();
                    if (messageFromPlayer1 != null) {
                        if (messageFromPlayer1.equals("NEW_GAME")) {
                            startNewGame();
                        } else {
                            processMove(messageFromPlayer1, 'O');
                            player2Out.println(messageFromPlayer1);
                            currentPlayer = 'X';
                        }
                    }
                } else {
                    String messageFromPlayer2 = player2In.readLine();
                    if (messageFromPlayer2 != null) {
                        if (messageFromPlayer2.equals("NEW_GAME")) {
                            startNewGame();
                        } else {
                            processMove(messageFromPlayer2, 'X');
                            player1Out.println(messageFromPlayer2);
                            currentPlayer = 'O';
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processMove(String message, char symbol) {
        int index = Integer.parseInt(message);
        board[index] = symbol;
        String winner = checkForWin();
        if (winner != null) {
            player1Out.println("WINNER " + winner);
            player2Out.println("WINNER " + winner);
        } else if (isBoardFull()) {
            player1Out.println("DRAW");
            player2Out.println("DRAW");
        }
    }

    private String checkForWin() {
        String[][] lines = new String[][]{
                {"" + board[0] + board[1] + board[2], "012"},
                {"" + board[3] + board[4] + board[5], "345"},
                {"" + board[6] + board[7] + board[8], "678"},
                {"" + board[0] + board[3] + board[6], "036"},
                {"" + board[1] + board[4] + board[7], "147"},
                {"" + board[2] + board[5] + board[8], "258"},
                {"" + board[0] + board[4] + board[8], "048"},
                {"" + board[2] + board[4] + board[6], "246"}
        };
        for (String[] lineData : lines) {
            String line = lineData[0];
            if (line.equals("XXX")) {
                return "X";
            } else if (line.equals("OOO")) {
                return "O";
            }
        }
        return null;
    }

    private boolean isBoardFull() {
        for (char c : board) {
            if (c == '\0') {
                return false;
            }
        }
        return true;
    }

    private void resetBoard() {
        for (int i = 0; i < board.length; i++) {
            board[i] = '\0';
        }
    }

    private void startNewGame() {
        resetBoard();
        player1Out.println("RESET");
        player2Out.println("RESET");

        if (playerCount % 2 == 0) { // четный игрок
            player1Out.println("START O");
            player2Out.println("START X");
            currentPlayer = 'O';
        } else { // нечетный игрок
            player1Out.println("START X");
            player2Out.println("START O");
            currentPlayer = 'O';
        }
    }

    public static void main(String[] args) {
        new TicTacToeServer();
    }
}




