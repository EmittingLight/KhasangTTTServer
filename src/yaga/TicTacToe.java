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
        setTitle("Tic Tac Toe - " + playerName);
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
                        buttons[index].setText(String.valueOf(mySymbol)); // Устанавливаем символ текущего игрока
                        out.println(index);
                        isMyTurn = false; // Блокируем возможность сделать еще один ход
                        checkForWin();
                    }
                }
            });
            add(buttons[i]);
        }
    }

    private void checkForWin() {
        // Проверяем наличие победителя
        for (int i = 0; i < 8; i++) {
            String line = null;
            switch (i) {
                case 0:
                    line = buttons[0].getText() + buttons[1].getText() + buttons[2].getText();
                    break;
                case 1:
                    line = buttons[3].getText() + buttons[4].getText() + buttons[5].getText();
                    break;
                case 2:
                    line = buttons[6].getText() + buttons[7].getText() + buttons[8].getText();
                    break;
                case 3:
                    line = buttons[0].getText() + buttons[3].getText() + buttons[6].getText();
                    break;
                case 4:
                    line = buttons[1].getText() + buttons[4].getText() + buttons[7].getText();
                    break;
                case 5:
                    line = buttons[2].getText() + buttons[5].getText() + buttons[8].getText();
                    break;
                case 6:
                    line = buttons[0].getText() + buttons[4].getText() + buttons[8].getText();
                    break;
                case 7:
                    line = buttons[2].getText() + buttons[4].getText() + buttons[6].getText();
                    break;
            }
            if (line.equals(mySymbol + "" + mySymbol + "" + mySymbol)) {
                JOptionPane.showMessageDialog(this, "Победитель: " + mySymbol);
                resetGame(); // Сбрасываем состояние игры
                return; // Завершаем метод, чтобы избежать дополнительных действий
            }
        }

        // Проверяем наличие ничьей
        if (isBoardFull()) {
            JOptionPane.showMessageDialog(this, "Ничья!");
            resetGame(); // Сбрасываем состояние игры
            return; // Завершаем метод
        }
    }

    private boolean isBoardFull() {
        for (JButton button : buttons) {
            if (button.getText().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void resetGame() {
        for (JButton button : buttons) {
            button.setText("");
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5000);
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
                        } else {
                            int index = Integer.parseInt(message);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    buttons[index].setText(String.valueOf(opponentSymbol));
                                    checkForWin();
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
