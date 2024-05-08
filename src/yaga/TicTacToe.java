package yaga;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.*;

public class TicTacToe extends JFrame {
    private JButton[] buttons = new JButton[9];
    private char currentPlayer = 'X';
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public TicTacToe() {
        setTitle("Tic Tac Toe");
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
                    buttonClicked(index);
                }
            });
            add(buttons[i]);
        }
    }

    private void buttonClicked(int index) {
        if (buttons[index].getText().equals("") && currentPlayer != ' ') {
            buttons[index].setText(String.valueOf(currentPlayer));
            out.println(index);
            checkForWin();
            currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
        }
    }

    private void checkForWin() {
        // Проверка на выигрыш
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
                        if (message.equals("START")) {
                            // Начать игру
                        } else {
                            int index = Integer.parseInt(message);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    buttons[index].setText(String.valueOf(currentPlayer));
                                    checkForWin();
                                    currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
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
                new TicTacToe();
            }
        });
    }
}

