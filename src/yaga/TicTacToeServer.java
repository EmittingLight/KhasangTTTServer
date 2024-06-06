package yaga;

import java.io.*;
import java.net.*;
import java.util.*;

public class TicTacToeServer {
    private static final int PORT = 5050;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static Map<String, ClientHandler> playerMap = new HashMap<>();

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
                synchronized (playerMap) {
                    playerMap.put(playerName, this);
                }
                sendPlayerList();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("CHALLENGE ")) {
                        String opponentName = message.substring(10);
                        synchronized (playerMap) {
                            ClientHandler opponentHandler = playerMap.get(opponentName);
                            if (opponentHandler != null && !opponentHandler.awaitingResponse) {
                                this.awaitingResponse = true;
                                opponentHandler.awaitingResponse = true;
                                opponentHandler.out.println("INVITATION " + playerName);
                            } else {
                                out.println("ERROR: Player not found or already in a game");
                            }
                        }
                    } else if (message.startsWith("ACCEPT ")) {
                        String challengerName = message.substring(7);
                        synchronized (playerMap) {
                            ClientHandler challengerHandler = playerMap.get(challengerName);
                            if (challengerHandler != null && challengerHandler.awaitingResponse) {
                                this.opponent = challengerHandler;
                                challengerHandler.opponent = this;
                                this.awaitingResponse = false;
                                challengerHandler.awaitingResponse = false;
                                challengerHandler.out.println("START X");
                                this.out.println("START O");
                                challengerHandler.out.println("CONFIRMED " + playerName);
                                sendPlayerList();  // обновить список игроков
                            }
                        }
                    } else if (message.startsWith("DECLINE ")) {
                        String challengerName = message.substring(8);
                        synchronized (playerMap) {
                            ClientHandler challengerHandler = playerMap.get(challengerName);
                            if (challengerHandler != null && challengerHandler.awaitingResponse) {
                                this.awaitingResponse = false;
                                challengerHandler.awaitingResponse = false;
                                challengerHandler.out.println("DECLINED " + playerName);
                            }
                        }
                    } else if (opponent != null && message.matches("\\d+")) {
                        opponent.out.println(message);
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
                synchronized (playerMap) {
                    playerMap.remove(playerName);
                }
                clientHandlers.remove(this);
                sendPlayerList();
            }
        }

        private void sendPlayerList() {
            synchronized (playerMap) {
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
}




