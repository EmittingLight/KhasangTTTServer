package yaga;

import java.io.*;
import java.net.*;
import java.util.Random;

public class TicTacToeServer {
    private ServerSocket serverSocket;
    private Socket player1Socket;
    private Socket player2Socket;
    private BufferedReader player1In;
    private PrintWriter player1Out;
    private BufferedReader player2In;
    private PrintWriter player2Out;
    private char[] board = new char[9];
    private Random random = new Random();

    public TicTacToeServer() {
        try {
            serverSocket = new ServerSocket(5000);
            System.out.println("Сервер запущен...");
            player1Socket = serverSocket.accept();
            System.out.println("Игрок 1 подключился.");
            player1In = new BufferedReader(new InputStreamReader(player1Socket.getInputStream()));
            player1Out = new PrintWriter(player1Socket.getOutputStream(), true);

            player2Socket = serverSocket.accept();
            System.out.println("Игрок 2 подключился.");
            player2In = new BufferedReader(new InputStreamReader(player2Socket.getInputStream()));
            player2Out = new PrintWriter(player2Socket.getOutputStream(), true);

            // Определяем, кто начинает игру
            if (random.nextBoolean()) {
                player1Out.println("START X");
                player2Out.println("START O");
            } else {
                player1Out.println("START O");
                player2Out.println("START X");
            }

            играть();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void играть() {
        try {
            while (true) {
                String messageFromPlayer1 = player1In.readLine();
                if (messageFromPlayer1 != null) {
                    processMove(messageFromPlayer1, 'X');
                    player2Out.println(messageFromPlayer1);
                }

                String messageFromPlayer2 = player2In.readLine();
                if (messageFromPlayer2 != null) {
                    processMove(messageFromPlayer2, 'O');
                    player1Out.println(messageFromPlayer2);
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
            resetBoard();
        } else if (isBoardFull()) {
            player1Out.println("DRAW");
            player2Out.println("DRAW");
            resetBoard();
        }
    }

    private String checkForWin() {
        String[] lines = new String[]{
                "" + board[0] + board[1] + board[2],
                "" + board[3] + board[4] + board[5],
                "" + board[6] + board[7] + board[8],
                "" + board[0] + board[3] + board[6],
                "" + board[1] + board[4] + board[7],
                "" + board[2] + board[5] + board[8],
                "" + board[0] + board[4] + board[8],
                "" + board[2] + board[4] + board[6]
        };
        for (String line : lines) {
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

    public static void main(String[] args) {
        new TicTacToeServer();
    }
}



