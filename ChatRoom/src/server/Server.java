package server;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class Server {
    public String name = "";
    public int row = -1;
    public DefaultTableModel model = new DefaultTableModel();
    JTable table = new JTable(model);
    public class User {
        public String name;
        public InetAddress IPAddress;
        public int p;

        public User(String name,InetAddress IPAddress, int p) {
            this.name = name;
            this.IPAddress = IPAddress;
            this.p = p;
        }
    }

    public ArrayList<User> users = new ArrayList<>();

    public Server() throws IOException {
        Listen l = new Listen();
        l.start();
        //System.out.println("hello");
        Forward f = new Forward();
        f.start();
        new Gui();
    }

    public class Listen extends Thread {
        public void run() {
            try {
                listen();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        public void listen() throws IOException {
            int port = 8888;
            DatagramSocket ds = new DatagramSocket(port);
            //创建一个数据包，用于接收数据
            byte[] bys = new byte[1024];
            DatagramPacket dp = new DatagramPacket(bys, bys.length);
            while (true) {
                //调用DatagramSocket对象的方法接收数据
                ds.receive(dp);
                //解析数据包
                String user = new String(dp.getData(), 0, dp.getLength());
                String returnMsg;
                if (!contain(user)) {
                    users.add(new User(user,dp.getAddress(),dp.getPort()));
                    model.addRow(new Object[]{user});
                    returnMsg = "true";
                } else {
                    returnMsg = "false";
                }
                InetAddress IPAddress = dp.getAddress();
                int p = dp.getPort();
                byte[] sendData = returnMsg.getBytes();

                //System.out.println(returnMsg);
                // 发送响应
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, p);
                ds.send(sendPacket);
                if (returnMsg.equals("true")) {
                    newUser(user);
                }
            }
        }
    }

    public void newUser(String name) throws IOException {
        DatagramSocket ds = new DatagramSocket();
        //解析数据包
        String m = "newUser" + "#" + name;
        byte[] sendData = m.getBytes();

        for (User u : users) {
            if (u.name.equals(name)) {
                for (User u2 : users) {
                    byte[] sendData1 = ("newUser1#" + u2.name).getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData1, sendData1.length, u.IPAddress, u.p);
                    ds.send(sendPacket);
                }
                continue;
            }
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, u.IPAddress, u.p);
            ds.send(sendPacket);
        }
    }

    public boolean contain(String name) {
        for (User user : users) {
            if (user.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public class Forward extends Thread {
        public void run() {
            try {
                forward();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void forward() throws IOException {
            int port = 8889;
            DatagramSocket ds = new DatagramSocket(port);
            //创建一个数据包，用于接收数据
            byte[] bys = new byte[1024];
            DatagramPacket dp = new DatagramPacket(bys, bys.length);
            while (true) {
                //调用DatagramSocket对象的方法接收数据
                ds.receive(dp);
                //解析数据包
                String msg = new String(dp.getData(), 0, dp.getLength());
                if (msg.split("#")[1].split("&")[0].equals("leave")) {
                    String name = msg.split("#")[0];
                    remove(name);
                    // 遍历所有的行
                    for (int row = 0; row < model.getRowCount(); row++) {
                        // 遍历每一行的所有列
                        for (int col = 0; col < model.getColumnCount(); col++) {
                            // 获取并打印单元格的值
                            Object value = model.getValueAt(row, col);
                            if (((String)value).equals(name)) {
                                model.removeRow(row);
                            }
                        }
                    }
                    //DatagramSocket ds = new DatagramSocket();
                    String m = "Server#" + name + "离开了聊天室";
                    for (User u : users) {
                        byte[] sendData = m.getBytes();
                        // 创建数据报
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, u.IPAddress, u.p);
                        // 发送数据报
                        ds.send(sendPacket);
                    }
                    continue;
                }
                //System.out.println(msg);
                //String m = msg.replace("#", "说:").split("&")[0];
                String toName = msg.replace("#", "说：").split("&")[1];
                //System.out.println(m);
                byte[] sendData = msg.getBytes();

                for (User u : users) {
                    if (u.name.equals(toName) || toName.equals("公共聊天室")) {
                        // 发送响应
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, u.IPAddress, u.p);
                        ds.send(sendPacket);
                    }
                }
            }
        }
    }

    public void remove(String name) {
        int i = 0;
        for (User user : users) {
            if (user.name.equals(name)) {
                break;
            }
            i++;
        }
        users.remove(i);
    }

    public class Gui {
        public Gui() {
            // 创建 JFrame 实例
            JFrame jf = new JFrame();
            jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            jf.setSize(700, 500);

            JPanel jp = new JPanel();

            // 创建文本框
            JTextField sendField = new JTextField(30); // 这里的20是列数，可以根据需要调整
            //receiveField.setLineWrap(true); // 启用自动换行
            //receiveField.setWrapStyleWord(true); // 只在单词边界处换行

            // 创建一个按钮，点击时获取文本框内容
            JButton sendButton = new JButton("系统消息");
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

            JButton nameButton = new JButton("踢出此人");
            nameButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (name.equals("")) {
                        return;
                    }
                    DatagramSocket ds = null;
                    try {
                        ds = new DatagramSocket();
                    } catch (SocketException ex) {
                        throw new RuntimeException(ex);
                    }
                    String m = "kill#" + name;
                    System.out.println(name);
                    for (User u : users) {
                        byte[] sendData = m.getBytes();
                        // 创建数据报
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, u.IPAddress, u.p);
                        // 发送数据报
                        try {
                            ds.send(sendPacket);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    ds.close();
                    model.removeRow(row);
                    remove(name);
                }
            });

            jp.add(nameButton);

            //jp.add(receiveField);
            jp.add(sendField);
            jp.add(sendButton);

            //jp.setBounds(5, 0, 400, 500);

            model.addColumn("在线用户");


            table.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    int r = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    row = r;
                    if (r >= 0 && col >= 0) {
                        Object value = table.getValueAt(r, col);
                        name = (String) value;
                        //System.out.println(toName);
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(table);
            //scrollPane.setBounds(new Rectangle(10, 10));
            scrollPane.setBounds(0, 0, 100, 500);
            //scrollPane.add(nameButton);
            jf.add(scrollPane);

            //jp.setBounds(0, 0, 400, 500);
            jf.add(jp);

            // 设置窗口可见
            jf.setVisible(true);
        }

        public void send(String m) throws IOException {
            DatagramSocket ds = new DatagramSocket();
            m = "Server#" + m;
            for (User u : users) {
                byte[] sendData = m.getBytes();
                // 创建数据报
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, u.IPAddress, u.p);
                // 发送数据报
                ds.send(sendPacket);
            }
            //receiveField.setText(s);
        }
    }
}
