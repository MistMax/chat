package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;

public class Server {
    public static void main(String[] args) {
        {
            ArrayList<User> users = new ArrayList<>();
            try {
                ServerSocket serverSocket = new ServerSocket(8188); // Создаём серверный сокет
                System.out.println("Сервер запущен");
                while (true) { // Бесконечный цикл для ожидания родключения клиентов
                        try {
                            Socket socket = serverSocket.accept(); // Ожидаем подключения клиента
                            System.out.println("Клиент подключился");
                            User currentUser = new User(socket);
                            users.add(currentUser);
                            DataInputStream in = new DataInputStream(socket.getInputStream()); // Поток ввода
                            DataOutputStream out = new DataOutputStream(socket.getOutputStream()); // Поток вывода
                            Thread client = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        out.writeUTF("Добро пожаловать на сервер");
                                        out.writeUTF("Введите ваше имя: ");
                                        String userName = in.readUTF();
                                        currentUser.setUserName(userName);
                                        for (User user : users) {
                                            DataOutputStream out = new DataOutputStream(user.getSocket().getOutputStream());
                                            out.writeUTF(currentUser.getUserName()+" присоединился к беседе");
                                        }
                                        while (true) {
                                            String request = in.readUTF(); // Ждём сообщение от пользователя
                                            System.out.println(currentUser.getUserName()+": "+request);
                                            for (User user : users) {
                                                if (users.indexOf(user) == users.indexOf(currentUser)) continue;
                                                DataOutputStream out = new DataOutputStream(user.getSocket().getOutputStream());
                                                out.writeUTF(currentUser.getUserName()+": "+request);
                                            }
                                        }
                                    } catch (IOException e) {
                                        users.remove(currentUser);
                                        for (User user : users) {
                                            DataOutputStream out = new DataOutputStream(user.getSocket().getOutputStream());
                                            out.writeUTF(currentUser.getUserName() + " присоединился к беседе");
                                        }
                                    }
                                }
                            });
                            client.start();
                        } catch (Exception e) {
                        }
                    }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
