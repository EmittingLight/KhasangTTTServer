package yaga;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class TicTacToe extends JFrame {
    private JButton[] buttons = new JButton[9];
    private char mySymbol;
    private char opponentSymbol;
    private boolean isMyTurn = false;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String playerName;
    private JComboBox<String> playerList;

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
            buttons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (isMyTurn && buttons[index].getText().equals("")) {
                        buttons[index].setText(String.valueOf(mySymbol));
                        out.println(index);
                        isMyTurn = false;
                    } else if (!isMyTurn) {
                        JOptionPane.showMessageDialog(TicTacToe.this, "Это не ваш ход. Пожалуйста, подождите.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            boardPanel.add(buttons[i]);
        }

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        playerList = new JComboBox<>();
        JButton challengeButton = new JButton("Вызвать на игру");
        challengeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String opponentName = (String) playerList.getSelectedItem();
                if (opponentName != null && !opponentName.equals(playerName)) {
                    out.println("CHALLENGE " + opponentName);
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

    private void updatePlayerList(String[] players) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                playerList.removeAllItems();
                for (String player : players) {
                    playerList.addItem(player);
                }
            }
        });
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
                } else {
                    out.println("DECLINE " + challengerName);
                }
            }
        });
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
                            isMyTurn = (mySymbol == 'X'); // X начинает
                        } else if (message.startsWith("PLAYER_LIST")) {
                            String[] parts = message.substring(12).split(",");
                            updatePlayerList(parts);
                        } else if (message.startsWith("INVITATION ")) {
                            String challengerName = message.substring(11);
                            showInvitationDialog(challengerName);
                        } else if (message.startsWith("DECLINED ")) {
                            String opponentName = message.substring(9);
                            JOptionPane.showMessageDialog(TicTacToe.this, opponentName + " отклонил ваше приглашение.", "Приглашение отклонено", JOptionPane.INFORMATION_MESSAGE);
                        } else if (message.matches("\\d+")) {
                            int index = Integer.parseInt(message);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    buttons[index].setText(String.valueOf(opponentSymbol));
                                    isMyTurn = true;
                                }
                            });
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                while (true) {
                    String playerName = JOptionPane.showInputDialog(null, "Введите ваше имя:");
                    if (playerName != null && !playerName.trim().isEmpty()) {
                        new TicTacToe(playerName);
                        break;
                    } else {
                        JOptionPane.showMessageDialog(null, "Имя не может быть пустым. Пожалуйста, введите ваше имя.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }
}



