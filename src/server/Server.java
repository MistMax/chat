package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Server {
    public static void main(String[] args) {
        {
            ArrayList<User> users = new ArrayList<>();
            try {
                ServerSocket serverSocket = new ServerSocket(8188); // Создаём серверный сокет
                System.out.println("Сервер запущен");
                while (true){ // Бесконечный цикл для ожидания родключения клиентов
                    Socket socket = serverSocket.accept(); // Ожидаем подключения клиента
                    System.out.println("Клиент подключился");
                    User currentUser = new User(socket);
                    users.add(currentUser);
                    DataInputStream in = new DataInputStream(currentUser.getSocket().getInputStream()); // Поток ввода
                    DataOutputStream out = new DataOutputStream(currentUser.getSocket().getOutputStream()); // Поток вывода
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                out.writeUTF("Добро пожаловать на сервер");
                                out.writeUTF("Введите ваше имя: ");
                                String userName = in.readUTF(); // Ожидаем имя от клиента
                                while (checkFreeName(users, userName)){
                                    out.writeUTF("Имя: "+userName+" занято, попробуйте ввести другое");
                                    userName = in.readUTF();
                                }
                                currentUser.setUserName(userName);
                                for (User user : users) {
                                    DataOutputStream out = new DataOutputStream(user.getSocket().getOutputStream());
                                    out.writeUTF(currentUser.getUserName()+" присоединился к беседе ");

                                }
                                while (true){
                                    String request = in.readUTF(); // Ждём сообщение от пользователя
                                    System.out.println(currentUser.getUserName()+": "+request);
                                    if (request.startsWith("/m ")){
                                        String replacedRequest = request.replaceFirst("/m ", "");
                                        sendPrivateMessage(replacedRequest,users,currentUser);
                                    } else {
                                        sendPublicMessage(request, users, currentUser);
                                    }
                                }
                            }catch (IOException e){
                                userExit(users, currentUser);
                            }
                        }
                    });
                    thread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static void sendPublicMessage(String request, ArrayList<User> users, User currentUser) throws IOException {
        for (User user : users) {
            if (users.indexOf(user) == users.indexOf(currentUser)) continue;
            DataOutputStream out = new DataOutputStream(user.getSocket().getOutputStream());
            out.writeUTF(currentUser.getUserName() + ": " + request);
        }
    }

    private static void sendPrivateMessage(String request, ArrayList<User> users, User currentUser) throws IOException {
        User recipient = null;
        String[] splitted = request.split(" ", 2);
        String name = "";
        String message = "";
        DataOutputStream out = new DataOutputStream(currentUser.getSocket().getOutputStream());
        try {
            name = splitted[0];
            message = splitted[1];
        } catch (IndexOutOfBoundsException ex) {
            out.writeUTF("Сообщение введено не корректно");
        }

        for (User user : users) {
            if (user.getUserName().equals(name)) {
                recipient = user;
                break;
            }
        }
        if (recipient != null) {
            DataOutputStream recipientOut = new DataOutputStream(recipient.getSocket().getOutputStream());
            recipientOut.writeUTF(currentUser.getUserName() + ": " + message);
        } else {
            out.writeUTF("Пользователь с именем: " + name + " отсутствует");
        }
    }
    private static void userExit(ArrayList<User> users, User currentUser) {
        users.remove(currentUser);
        for (User user : users) {
            try {
               DataOutputStream out = new DataOutputStream(user.getSocket().getOutputStream());
               out.writeUTF(currentUser.getUserName() + " покинул чат");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
    public static boolean checkFreeName(ArrayList<User> users, String name) {
        for (User user : users) {
            if (user.getUserName() != null && user.getUserName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
