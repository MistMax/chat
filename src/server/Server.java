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
            ArrayList<String> usersName = new ArrayList<>();
            try {
                ServerSocket serverSocket = new ServerSocket(8188); // Создаём серверный сокет
                System.out.println("Сервер запущен");
                while (true){ // Бесконечный цикл для ожидания родключения клиентов
                    Socket socket = serverSocket.accept(); // Ожидаем подключения клиента
                    System.out.println("Клиент подключился");
                    User currentUser = new User(socket);
                    users.add(currentUser);
                    DataInputStream in = new DataInputStream(currentUser.getSocket().getInputStream()); // Поток ввода
                    ObjectOutputStream oos = new ObjectOutputStream(currentUser.getSocket().getOutputStream()); // Поток вывода
                    currentUser.setOos(oos);
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                currentUser.getOos().writeObject("Добро пожаловать на сервер");
                                currentUser.getOos().writeObject("Введите ваше имя: ");
                                String userName = in.readUTF(); // Ожидаем имя от клиента
                                while (checkFreeName(users, userName)){
                                    currentUser.getOos().writeObject("Имя: "+userName+" занято, попробуйте ввести другое");
                                    userName = in.readUTF();
                                }
                                currentUser.setUserName(userName);
                                usersName.add(currentUser.getUserName()); // Добавляем имя пользователя в коллекцию
                                for (User user : users) {
                                    user.getOos().writeObject(currentUser.getUserName()+" присоединился к беседе");
                                    user.getOos().writeObject(new ArrayList<>(usersName)); // Отправляем список пользователей клиентам
                                    System.out.println("Отправляем список пользователей" + usersName); // Пказываем, что отправили
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
                                userExit(users, currentUser, usersName);
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
    private static void userExit(ArrayList<User> users, User currentUser, ArrayList<String> usersName) {
        users.remove(currentUser);
        usersName.remove(currentUser.getUserName());
        for (User user : users) {
            try {
                user.getOos().writeObject(currentUser.getUserName()+" покинул чат");
                user.getOos().writeObject(new ArrayList<>(usersName));
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
