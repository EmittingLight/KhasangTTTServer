package yaga;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TicTacToeServer {
    private static final int PORT = 5050;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static Map<String, ClientHandler> playerMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер запущен...");
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String playerName;
        private ClientHandler opponent;
        private boolean awaitingResponse = false;
        private boolean awaitingConfirmation = false;
        private boolean confirmedNewGame = false;
        private boolean isMyTurn = false;
        private char mySymbol;
        private char opponentSymbol;
        private static final String CHALLENGE_PREFIX = "CHALLENGE ";
        private static final String ACCEPT_PREFIX = "ACCEPT ";

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("ENTER_NAME");
                playerName = in.readLine();
                playerMap.put(playerName, this);
                sendPlayerList();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith(CHALLENGE_PREFIX)) {
                        String opponentName = message.substring(CHALLENGE_PREFIX.length());
                        ClientHandler opponentHandler = playerMap.get(opponentName);
                        if (opponentHandler != null && !opponentHandler.awaitingResponse) {
                            this.awaitingResponse = true;
                            opponentHandler.awaitingResponse = true;
                            opponentHandler.out.println("INVITATION " + playerName);
                        } else {
                            out.println("ERROR: Player not found or already in a game");
                        }
                    } else if (message.startsWith(ACCEPT_PREFIX)) {
                        String challengerName = message.substring(ACCEPT_PREFIX.length());
                        ClientHandler challengerHandler = playerMap.get(challengerName);
                        if (challengerHandler != null && challengerHandler.awaitingResponse) {
                            this.opponent = challengerHandler;
                            challengerHandler.opponent = this;
                            this.awaitingResponse = false;
                            challengerHandler.awaitingResponse = false;
                            this.mySymbol = 'O';
                            this.opponentSymbol = 'X';
                            challengerHandler.mySymbol = 'X';
                            challengerHandler.opponentSymbol = 'O';
                            challengerHandler.out.println("START X");
                            this.out.println("START O");
                            challengerHandler.out.println("CONFIRMED " + playerName);
                            this.out.println("CONFIRMED " + challengerName);
                            sendPlayerList();
                        }
                    } else if (message.startsWith("DECLINE ")) {
                        String challengerName = message.substring(8);
                        ClientHandler challengerHandler = playerMap.get(challengerName);
                        if (challengerHandler != null && challengerHandler.awaitingResponse) {
                            this.awaitingResponse = false;
                            challengerHandler.awaitingResponse = false;
                            challengerHandler.out.println("DECLINED " + playerName);
                        }
                    } else if (message.equals("NEW_GAME")) {
                        if (opponent != null) {
                            this.awaitingConfirmation = true;
                            opponent.awaitingConfirmation = true;
                            this.confirmedNewGame = true;
                            if (opponent.confirmedNewGame) {
                                this.out.println("START " + mySymbol);
                                opponent.out.println("START " + opponentSymbol);
                                this.awaitingConfirmation = false;
                                opponent.awaitingConfirmation = false;
                                this.confirmedNewGame = false;
                                opponent.confirmedNewGame = false;
                            }
                        }
                    } else if (message.equals("END_GAME")) {
                        endGame();
                    } else if (opponent != null && message.matches("\\d+")) {
                        opponent.out.println(message);
                        this.isMyTurn = false;
                        opponent.isMyTurn = true;
                    } else if (message.equals("REQUEST_PLAYER_LIST")) {
                        sendPlayerList();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                playerMap.remove(playerName);
                clientHandlers.remove(this);
                sendPlayerList();
            }
        }

        private void endGame() {
            if (opponent != null) {
                opponent.out.println("GAME_ENDED");
                this.out.println("GAME_ENDED");
                opponent.opponent = null;
                this.opponent = null;
            }
            sendPlayerList();
        }

        private void sendPlayerList() {
            List<String> availablePlayers = new ArrayList<>();
            for (String player : playerMap.keySet()) {
                ClientHandler handler = playerMap.get(player);
                if (handler.opponent == null) {
                    availablePlayers.add(player);
                }
            }
            String playerList = "PLAYER_LIST " + String.join(",", availablePlayers);
            for (ClientHandler handler : clientHandlers) {
                handler.out.println(playerList);
            }
        }
    }
}
