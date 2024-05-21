package yaga;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
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

    public TicTacToe(String playerName) {
        this.playerName = playerName;
        setTitle("Крестики-нолики - " + playerName);
        setSize(300, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 3));
        initializeButtons();
        connectToServer();
        setVisible(true);
    }

    private void initializeButtons() {
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
            add(buttons[i]);
        }
    }

    private void showEndGameDialog(String message) {
        int option = JOptionPane.showOptionDialog(this, message + "\nХотите начать новую игру?", "Игра завершена",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Да", "Нет"}, JOptionPane.YES_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            out.println("NEW_GAME");
            resetGame();
        } else {
            System.exit(0);
        }
    }

    private void resetGame() {
        for (JButton button : buttons) {
            button.setText("");
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5050);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            new Thread(new ServerListener()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                        } else if (message.startsWith("WINNER")) {
                            String[] parts = message.split(" ");
                            showEndGameDialog("Победитель: " + parts[1]);
                        } else if (message.equals("DRAW")) {
                            showEndGameDialog("Ничья!");
                        } else if (message.equals("RESET")) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    resetGame();
                                }
                            });
                        } else {
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
        String playerName = JOptionPane.showInputDialog(null, "Введите ваше имя:");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new TicTacToe(playerName);
            }
        });
    }
}

