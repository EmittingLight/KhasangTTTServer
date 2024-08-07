package yaga;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

public class TicTacToe extends JFrame {
    private JButton[] buttons = new JButton[9];
    private char mySymbol;
    private char opponentSymbol;
    private boolean isMyTurn = false;
    private boolean gameInProgress = false;
    private boolean awaitingConfirmation = false;
    private boolean confirmedNewGame = false;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String playerName;
    private JComboBox<String> playerList;
    private Set<String> inGamePlayers = new HashSet<>();

    private static final int[][] WIN_COMBINATIONS = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
    };

    public TicTacToe(String playerName) {
        this.playerName = playerName;
        setTitle("Крестики-нолики - " + playerName);
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        initializeComponents();
        connectToServer();
        setResizable(false);
        setVisible(true);
    }

    private void initializeComponents() {
        JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(3, 3));
        for (int i = 0; i < 9; i++) {
            final int index = i;
            buttons[i] = new JButton();
            buttons[i].setFont(new Font(Font.SANS_SERIF, Font.BOLD, 50));
            buttons[i].setEnabled(false);
            buttons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (gameInProgress && isMyTurn && !awaitingConfirmation && confirmedNewGame && buttons[index].getText().isEmpty()) {
                        buttons[index].setText(String.valueOf(mySymbol));
                        out.println(index);
                        isMyTurn = false;
                        if (checkWin(mySymbol)) {
                            JOptionPane.showMessageDialog(TicTacToe.this, playerName + " выиграли символом " + mySymbol + "!", "Победа", JOptionPane.INFORMATION_MESSAGE);
                            out.println("WIN");
                            endGame();
                        } else if (checkDraw()) {
                            JOptionPane.showMessageDialog(TicTacToe.this, "Ничья!", "Ничья", JOptionPane.INFORMATION_MESSAGE);
                            out.println("DRAW");
                            endGame();
                        }
                    } else if (!isMyTurn) {
                        JOptionPane.showMessageDialog(TicTacToe.this, "Это не ваш ход. Пожалуйста, подождите.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    } else if (awaitingConfirmation || !confirmedNewGame) {
                        JOptionPane.showMessageDialog(TicTacToe.this, "Ожидание подтверждения от обоих игроков для начала новой игры.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            buttons[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!buttons[index].isEnabled()) {
                        JOptionPane.showMessageDialog(TicTacToe.this, "Выберите игрока из списка или ожидайте подтверждения", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            boardPanel.add(buttons[i]);
        }

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        playerList = new JComboBox<>();
        playerList.addItem("Нажмите, чтобы выбрать игрока");
        JButton challengeButton = new JButton("Приглашение на игру");
        challengeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String opponentName = (String) playerList.getSelectedItem();
                if (opponentName != null && !opponentName.equals(playerName) && !opponentName.equals("Нажмите, чтобы выбрать игрока") && !inGamePlayers.contains(opponentName)) {
                    out.println("CHALLENGE " + opponentName);
                } else {
                    JOptionPane.showMessageDialog(TicTacToe.this, "Вы не можете выбрать сами себя, начальный элемент или игрока, который уже в игре.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        controlPanel.add(playerList, BorderLayout.CENTER);
        controlPanel.add(challengeButton, BorderLayout.EAST);

        add(boardPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.NORTH);
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5050);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(playerName);
            new Thread(new ServerListener()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disconnectFromServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                out.println("DISCONNECT");
                socket.close();
                in.close();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestPlayerListUpdate();
    }

    private void updatePlayerList(String[] players) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                playerList.removeAllItems();
                playerList.addItem("Нажмите, чтобы выбрать игрока");
                for (String player : players) {
                    if (!player.equals(playerName) && !inGamePlayers.contains(player)) {
                        playerList.addItem(player);
                    }
                }
            }
        });
    }

    private void addInGamePlayer(String player) {
        inGamePlayers.add(player);
        playerList.setEnabled(false);
        requestPlayerListUpdate();
    }

    private void removeInGamePlayer(String player) {
        inGamePlayers.remove(player);
        clearBoard();
        requestPlayerListUpdate();
        playerList.setEnabled(true);
        enableButtons(false);
    }

    private void requestPlayerListUpdate() {
        if (!playerList.isEnabled()) {
            return;
        }
        out.println("REQUEST_PLAYER_LIST");
    }

    private void showInvitationDialog(String challengerName) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int option = JOptionPane.showOptionDialog(
                        TicTacToe.this,
                        challengerName + " приглашает вас на игру. Принять?",
                        "Приглашение",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new Object[]{"Принять", "Отклонить"},
                        JOptionPane.YES_OPTION
                );

                if (option == JOptionPane.YES_OPTION) {
                    out.println("ACCEPT " + challengerName);
                    addInGamePlayer(challengerName);
                    startNewGame();
                } else {
                    out.println("DECLINE " + challengerName);
                }
            }
        });
    }

    private boolean checkWin(char symbol) {
        for (int[] combination : WIN_COMBINATIONS) {
            if (buttons[combination[0]].getText().equals(String.valueOf(symbol)) &&
                    buttons[combination[1]].getText().equals(String.valueOf(symbol)) &&
                    buttons[combination[2]].getText().equals(String.valueOf(symbol))) {
                return true;
            }
        }
        return false;
    }

    private boolean checkDraw() {
        for (JButton button : buttons) {
            if (button.getText().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private class ServerListener implements Runnable {
        public void run() {
            try {
                while (true) {
                    String message = in.readLine();
                    if (message != null) {
                        if (message.startsWith("START")) {
                            String[] parts = message.split(" ");
                            mySymbol = parts[1].charAt(0);
                            opponentSymbol = (mySymbol == 'X') ? 'O' : 'X';
                            isMyTurn = (mySymbol == 'X');
                            gameInProgress = true;
                            awaitingConfirmation = false;
                            confirmedNewGame = true;
                            enableButtons(true);
                        } else if (message.startsWith("PLAYER_LIST")) {
                            String[] parts = message.substring(12).split(",");
                            updatePlayerList(parts);
                        } else if (message.startsWith("INVITATION ")) {
                            String challengerName = message.substring(11);
                            showInvitationDialog(challengerName);
                        } else if (message.startsWith("DECLINED ")) {
                            String opponentName = message.substring(9);
                            JOptionPane.showMessageDialog(TicTacToe.this, opponentName + " отклонил ваше приглашение.", "Приглашение отклонено", JOptionPane.INFORMATION_MESSAGE);
                            inGamePlayers.remove(opponentName);
                            clearBoard();
                            gameInProgress = false;
                            enableButtons(false);
                            playerList.setEnabled(true);
                        } else if (message.startsWith("CONFIRMED ")) {
                            String opponentName = message.substring(10);
                            JOptionPane.showMessageDialog(TicTacToe.this, opponentName + " принял ваше приглашение.", "Приглашение принято", JOptionPane.INFORMATION_MESSAGE);
                            addInGamePlayer(opponentName);
                            String firstTurnMessage = isMyTurn
                                    ? "Вы ходите первым, так как играете за X."
                                    : "Ваш противник ходит первым, так как он играет за X.";
                            JOptionPane.showMessageDialog(
                                    TicTacToe.this,
                                    firstTurnMessage,
                                    "Начало игры",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                            gameInProgress = true;
                        } else if (message.matches("\\d+")) {
                            int index = Integer.parseInt(message);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    if (gameInProgress && !awaitingConfirmation && confirmedNewGame) {
                                        buttons[index].setText(String.valueOf(opponentSymbol));
                                        isMyTurn = true;
                                    }
                                    if (checkWin(opponentSymbol)) {
                                        JOptionPane.showMessageDialog(TicTacToe.this, playerName + " вы проиграли! Победил символ " + opponentSymbol, "Поражение", JOptionPane.INFORMATION_MESSAGE);
                                        out.println("LOSE");
                                        endGame();
                                    } else if (checkDraw()) {
                                        JOptionPane.showMessageDialog(TicTacToe.this, "Ничья!", "Ничья", JOptionPane.INFORMATION_MESSAGE);
                                        out.println("DRAW");
                                        endGame();
                                    }
                                }
                            });
                        } else if (message.equals("WIN")) {
                            JOptionPane.showMessageDialog(TicTacToe.this, "Вы победили!", "Победа", JOptionPane.INFORMATION_MESSAGE);
                            removeInGamePlayer(playerName);
                            endGame();
                        } else if (message.equals("DRAW")) {
                            JOptionPane.showMessageDialog(TicTacToe.this, "Ничья!", "Ничья", JOptionPane.INFORMATION_MESSAGE);
                            removeInGamePlayer(playerName);
                            endGame();
                        } else if (message.equals("LOSE")) {
                            JOptionPane.showMessageDialog(TicTacToe.this, "Вы проиграли!", "Поражение", JOptionPane.INFORMATION_MESSAGE);
                            removeInGamePlayer(playerName);
                            endGame();
                        } else if (message.equals("GAME_ENDED")) {
                            JOptionPane.showMessageDialog(TicTacToe.this, "Игра закончена, так как оппонент отказался продолжать игру.", "Игра завершена", JOptionPane.INFORMATION_MESSAGE);
                            removeInGamePlayer(playerName);
                            disconnectFromServer();
                            resetGame();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void endGame() {
        clearBoard();
        gameInProgress = false;
        awaitingConfirmation = true;
        confirmedNewGame = false;
        enableButtons(false);
        int option = JOptionPane.showOptionDialog(
                TicTacToe.this,
                "Хотите начать новую игру?",
                "Новая игра",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"Да", "Отмена"},
                JOptionPane.YES_OPTION
        );

        if (option == JOptionPane.YES_OPTION) {
            out.println("NEW_GAME");
        } else if (option == JOptionPane.NO_OPTION || option == JOptionPane.CLOSED_OPTION) {
            out.println("END_GAME");
            inGamePlayers.clear();
            requestPlayerListUpdate();
            disconnectFromServer();
            resetGame();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String playerName = JOptionPane.showInputDialog(null, "Введите ваше имя:");
                if (playerName == null || playerName.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Имя не может быть пустым. Приложение будет закрыто.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                } else {
                    new TicTacToe(playerName);
                }
            }
        });
    }

    private void clearBoard() {
        for (JButton button : buttons) {
            button.setText("");
        }
    }

    private void resetGame() {
        isMyTurn = false;
        mySymbol = '\0';
        opponentSymbol = '\0';
        inGamePlayers.clear();
        requestPlayerListUpdate();
        gameInProgress = false;
        awaitingConfirmation = false;
        confirmedNewGame = false;
        playerList.setEnabled(true);
        enableButtons(false);
        connectToServer();
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window instanceof JDialog) {
                JDialog dialog = (JDialog) window;
                if (dialog.getTitle().equals("Новая игра")) {
                    dialog.dispose();
                    break;
                }
            }
        }
    }

    private void startNewGame() {
        clearBoard();
        gameInProgress = true;
        awaitingConfirmation = false;
        confirmedNewGame = true;
        enableButtons(true);
    }

    private void enableButtons(boolean enable) {
        for (JButton button : buttons) {
            button.setEnabled(enable);
        }
    }
}





