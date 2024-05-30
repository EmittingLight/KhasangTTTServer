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
                            if (opponentHandler != null) {
                                this.opponent = opponentHandler;
                                opponentHandler.opponent = this;
                                out.println("START X");
                                opponentHandler.out.println("START O");
                            } else {
                                out.println("ERROR: Player not found");
                            }
                        }
                    } else if (opponent != null && message.matches("\\d+")) {
                        opponent.out.println(message);
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
                String playerList = "PLAYER_LIST " + String.join(",", playerMap.keySet());
                for (ClientHandler handler : clientHandlers) {
                    handler.out.println(playerList);
                }
            }
        }
    }
}





