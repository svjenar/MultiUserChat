package com.jen;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class ServerWorker extends  Thread {
    private final Socket clientSocket;
    private final Server server;
    private  String login=null;
    private OutputStream outputStream;
    private HashSet<String> topicSet=new HashSet<>();


    public ServerWorker(Server server, Socket clientSocket) {
        this.server=server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handelClientSocket(clientSocket);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    private void handelClientSocket(Socket clientSocket) throws IOException, InterruptedException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

        BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream));
        String text;
        while ((text=reader.readLine()) !=null)
        {
            String[] tokens= StringUtils.split(text);
            if (tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if ("logoff".equalsIgnoreCase(cmd) || "quit".equalsIgnoreCase(cmd)) {
                    handleLogoff();
                    break;
                } else if ("login".equalsIgnoreCase(cmd)) {
                    handleLogin(outputStream, tokens);
                } else if ("msg".equalsIgnoreCase(cmd)) {
                    String[] tokensMsg = StringUtils.split(text, null, 3);
                    handelMessage(tokensMsg);
                } else if("join".equalsIgnoreCase(cmd)){
                    handelJoin(tokens);
                } else if ("leave".equalsIgnoreCase(cmd)) {
                    handelLeave(tokens);
                } else {
                    String msg="unknown "+cmd+"\n";
                    outputStream.write(msg.getBytes());
                }
            }
        }

        clientSocket.close();
    }

    private void handelLeave(String[] tokens) {
        if(tokens.length>1)
        {
            String topic= tokens[1];
            topicSet.remove(topic);
        }
    }

    public boolean isMemberOfTopic(String topic){
        return topicSet.contains(topic);
    }

    private void handelJoin(String[] tokens) {
        if(tokens.length>1)
        {
            String topic= tokens[1];
            topicSet.add(topic);
        }
    }

    //format "msg" "login" msg.....
    //format "msg "#topic" msg.....
    private void handelMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];

        boolean isTopic = sendTo.charAt(0) == '#';

        List<ServerWorker> workerList = server.getWorkerList();
        for(ServerWorker worker : workerList){
            if (isTopic){
                if (worker.isMemberOfTopic(sendTo)){
                    String outMsg = "msg " + sendTo +":"+login + " " + body + "\n";
                    worker.send(outMsg);
                }
            }else {

                if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                    String outMsg = "msg " + login + " " + body + "\n";
                    worker.send(outMsg);
                }
            }
        }

        }

    private void handleLogoff() throws IOException {
        server.removeWorker(this);
        List<ServerWorker> workerList = server.getWorkerList();
        String onlineMsg="offline "+login+"\n";

        for(ServerWorker worker : workerList){
            if (!login.equals(worker.getLogin())) {
                worker.send(onlineMsg);
            }
        }


        clientSocket.close();
    }

    public String getLogin() {
        return login;
    }

    private void handleLogin(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 3){
            String login=tokens[1];
            String password=tokens[2];

            if ((login.equalsIgnoreCase("jenar") && password.equalsIgnoreCase("jenar"))||(login.equalsIgnoreCase("nippy") && password.equalsIgnoreCase("nippy"))){
                String msg="ok login\n";
                outputStream.write(msg.getBytes());
                this.login=login;
                System.out.println("user login succesful: "+login);

                List<ServerWorker> workerList = server.getWorkerList();

                //sending current user about others
                for(ServerWorker worker : workerList){
                    if (!login.equals(worker.getLogin())) {
                        if(worker.getLogin()!= null) {
                            String msg2 = "online " + worker.getLogin() + "\n";
                            send(msg2);
                        }
                    }
                }
                //sending other about current user
                String onlineMsg="online "+login+"\n";

                for(ServerWorker worker : workerList){
                    if (!login.equals(worker.getLogin())) {
                        worker.send(onlineMsg);
                    }
                }

            } else {
                String msg="error in login\n";
                outputStream.write(msg.getBytes());
                System.err.println("login failed "+login);
            }
        }
    }

    private void send(String msg) throws IOException {
        if (login!= null) {
            outputStream.write(msg.getBytes());
        }
    }
}
