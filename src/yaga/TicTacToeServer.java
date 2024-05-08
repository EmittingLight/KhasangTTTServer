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

    public TicTacToeServer() {
        try {
            serverSocket = new ServerSocket(5000);
            System.out.println("Server is running...");
            player1Socket = serverSocket.accept();
            System.out.println("Player 1 connected.");
            player1In = new BufferedReader(new InputStreamReader(player1Socket.getInputStream()));
            player1Out = new PrintWriter(player1Socket.getOutputStream(), true);

            player2Socket = serverSocket.accept();
            System.out.println("Player 2 connected.");
            player2In = new BufferedReader(new InputStreamReader(player2Socket.getInputStream()));
            player2Out = new PrintWriter(player2Socket.getOutputStream(), true);

            player1Out.println("START");
            player2Out.println("START");

            playGame();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playGame() {
        try {
            while (true) {
                String messageFromPlayer1 = player1In.readLine();
                if (messageFromPlayer1 != null) {
                    player2Out.println(messageFromPlayer1);
                    player1Out.println(messageFromPlayer1);
                }

                String messageFromPlayer2 = player2In.readLine();
                if (messageFromPlayer2 != null) {
                    player1Out.println(messageFromPlayer2);
                    player2Out.println(messageFromPlayer2);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new TicTacToeServer();
    }
}



