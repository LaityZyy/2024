package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
    DefaultTableModel model = new DefaultTableModel();
    public Map<String, String> msg = new HashMap<>();
    public String toName = "公共聊天室";
    public JTextArea receiveField = new JTextArea(25, 45); // 这里的20是列数，可以根据需要调整
    public String user;
    public Scanner sc = new Scanner(System.in);
    DatagramSocket ds = new DatagramSocket();

    public Client() throws IOException, InterruptedException {
        // 初始化用户
        user = JOptionPane.showInputDialog(null, "请输入用户名");

        while (!connect(user)) {
            // 初始化用户
            user = JOptionPane.showInputDialog(null, "用户已存在，请输入新的用户名: ");
            user = sc.nextLine();
        }
        //System.out.println("连接成功");
        new Gui();
        Receive r = new Receive();
        r.start();
    }


    public boolean connect(String user) throws IOException {
        // 创建要发送的数据
        byte[] sendData = user.getBytes();
        // 指定目标地址和端口
        InetAddress serverAddress = InetAddress.getByName("localhost");
        int serverPort = 8888;
        // 创建数据报
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
        // 发送数据报
        ds.send(sendPacket);

        // 接受响应
        // 创建接收数据的缓冲区
        byte[] receiveData = new byte[1024];
        // 创建接收数据报
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        // 接收数据报
        ds.receive(receivePacket);
        // 解析接收到的数据
        String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
        //System.out.println(message);
        if (message.equals("true")) {
            return true;
        } else {
            return false;
        }
    }

    public class Receive extends Thread {
        public void run() {
            try {
                receive();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void receive() throws IOException {
            while (true) {
                // 创建接收数据的缓冲区
                byte[] receiveData = new byte[1024];
                // 创建接收数据报
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                // 接收数据报
                ds.receive(receivePacket);
                // 解析接收到的数据
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                //System.out.println(message);
                if (message.split("#")[0].equals("kill")) {
                    if (message.split("#")[1].equals(user)) {
                        JOptionPane.showMessageDialog(null, "你已被踢出系统");
                        System.exit(0);
                    } else {
                        String s = msg.get("公共聊天室");
                        s += message.split("#")[1] + "已被踢出聊天室" + "\n";
                        msg.put("公共聊天室", s);
                        toName = "公共聊天室";
                        receiveField.setText(s);
                        for (int row = 0; row < model.getRowCount(); row++) {
                            // 遍历每一行的所有列
                            for (int col = 0; col < model.getColumnCount(); col++) {
                                // 获取并打印单元格的值
                                Object value = model.getValueAt(row, col);
                                if (((String)value).equals(message.split("#")[1])) {
                                    model.removeRow(row);
                                }
                            }
                        }
                    }
                } else if (message.split("#")[0].equals("newUser")) {
                    model.addRow(new Object[]{message.split("#")[1]});
                    //System.out.println(message.split("#")[1]);
                    msg.put(message.split("#")[1], "");
                    String s = msg.get("公共聊天室");
                    s += message.split("#")[1] + "来到了聊天室\n";
                    //System.out.println(s);
                    msg.put("公共聊天室", s);
                    receiveField.setText(s);
                } else if (message.split("#")[0].equals("newUser1")) {
                    if (message.split("#")[1].equals(user)) {
                        String s = msg.get("公共聊天室");
                        s += message.split("#")[1] + "来到了聊天室\n";
                        //System.out.println(s);
                        msg.put("公共聊天室", s);
                        receiveField.setText(s);
                    } else {
                        model.addRow(new Object[]{message.split("#")[1]});
                        msg.put(message.split("#")[1], "");
                    }
                } else if (message.split("#")[0].equals("Server")) {
                    String s = msg.get("公共聊天室");
                    message = message.replace("#", ":");
                    s += message + "\n";
                    msg.put("公共聊天室", s);
                    toName = "公共聊天室";
                    receiveField.setText(s);
                } else if (!message.split("&")[1].equals("公共聊天室")){
                    String t = message.split("#")[0];
                    String s = msg.get(t);
                    //System.out.println(s);
                    message = message.replace("#", "说:");
                    s += message.split("&")[0] + "\n";
                    toName = t;
                    msg.put(t, s);
                    receiveField.setText(s);
                } else {
                    toName = "公共聊天室";
                    String s = msg.get("公共聊天室");
                    message = message.replace("#", "说:");
                    s += message.split("&")[0] + "\n";
                    msg.put("公共聊天室", s);
                    receiveField.setText(s);
                }
                //receiveField.setText(receiveMsg);
            }
        }
    }

    public class Gui {
        public Gui() {
            // 创建 JFrame 实例
            JFrame jf = new JFrame("用户:" + user);
            jf.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            jf.setSize(670, 500);

            jf.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    try {
                        send("leave");
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    System.exit(0);
                }
            });

            JPanel jp = new JPanel();

            // 创建文本框
            JTextField sendField = new JTextField(30); // 这里的20是列数，可以根据需要调整
            receiveField.setLineWrap(true); // 启用自动换行
            receiveField.setWrapStyleWord(true); // 只在单词边界处换行

            // 创建一个按钮，点击时获取文本框内容
            JButton sendButton = new JButton("发送");
            sendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // 获取文本框中的文本
                    String input = sendField.getText();
                    try {
                        send(input);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    sendField.setText("");
                }
            });

            jp.add(receiveField);
            jp.add(sendField);
            jp.add(sendButton);

            //jp.setBounds(5, 0, 400, 500);

            model.addColumn("在线用户");
            model.addRow(new Object[]{"公共聊天室"});
            msg.put("公共聊天室", "");

            JTable table = new JTable(model);
            //table.getColumnModel().getColumn(0).setPreferredWidth(50);
            //table.getColumnModel().getColumn(1).setPreferredWidth(50);
            table.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        Object value = table.getValueAt(row, col);
                        toName = (String) value;
                        //System.out.println(toName);
                        receiveField.setText(msg.get(toName));
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(table);
            //scrollPane.setBounds(new Rectangle(10, 10));
            scrollPane.setBounds(0, 0, 100, 500);
            jf.add(scrollPane);

            //jp.setBounds(0, 0, 400, 500);
            jf.add(jp);

            // 设置窗口可见
            jf.setVisible(true);
        }

        public void send(String m) throws IOException {
            String s;
            //System.out.println("ads" + toName);
            s = msg.get(toName);
            System.out.println(s);
            s += user + "说:" + m + "\n";
            System.out.println(s);
            if (!toName.equals("公共聊天室")) {
                msg.put(toName, s);
            }
            m = user + "#" + m + "&" + toName;
            byte[] sendData = m.getBytes();
            // 指定目标地址和端口
            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 8889;
            // 创建数据报
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
            // 发送数据报
            ds.send(sendPacket);
            receiveField.setText(s);
        }
    }

}
