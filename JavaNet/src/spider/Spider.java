package spider;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Spider {
    public List<String> words = new ArrayList<String>();
    private JTextArea textArea;
    private JButton openButton;
    //JTextArea receiveField = new JTextArea(25, 45); // 这里的20是列数，可以根据需要调整
    String s = "";
    public String url;
    public boolean canRun = true;
    public boolean canPut = true;
    public Set<String> urlSet = new HashSet<String>();
    public int num = 0;
    public String PATH = "D://net//";

    public static void main(String[] args) throws IOException, BadLocationException {
        //new Spider().new Get("https://fanyi.youdao.com/#/").start();
        //new Spider().new Get("http://www.jjckb.cn/xinpi/xinpipt.htm").start();
        new Spider().new Gui();
    }

    public class Gui {
        JFrame jf = new JFrame("爬虫敏感词");
        public Gui() throws IOException, BadLocationException {
            // 创建 JFrame 实例
            jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            jf.setSize(670, 500);


            // 使用JTextPane显示文本
            JTextPane textPane = new JTextPane();
            textPane.setEditable(false); // 设置为不可编辑

            JScrollPane js = new JScrollPane(textPane);
            js.setBounds(150,50,500,400);
            js.setViewportView(textPane);
            // 插入原始文本
            StyledDocument doc = textPane.getStyledDocument();
            jf.add(js);


            JPanel jp = new JPanel();

            // 创建文本框
            JTextField sendField = new JTextField(30); // 这里的20是列数，可以根据需要调整

            //receiveField.setLineWrap(true); // 启用自动换行
            //receiveField.setWrapStyleWord(true); // 只在单词边界处换行
            // 创建一个JScrollPane实例，并将JTextArea作为参数传递给它
            // 默认情况下，JScrollPane会根据需要添加水平和垂直滚动条
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setBounds(150,50,500,400);//自定义该面板位置并设置大小为100*50
            //scrollPane.setViewportView(receiveField);

            // （可选）设置滚动条的策略
            // 例如，仅当内容超出视口时才显示垂直滚动条
            //scrollPane.setVerticalScrollBarPolicy(VerticalScrollBarPolicy.AS_NEEDED);


            // 创建一个按钮，点击时获取文本框内容
            JButton sendButton = new JButton("开始");
            sendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // 获取文本框中的文本
                    canRun = true;
                    canPut = true;
                    String input = sendField.getText();
                    url = input;
                    new Get(url).start();
                    JOptionPane.showMessageDialog(null, "正在爬取");
                }
            });

            // 创建一个按钮，点击时获取文本框内容
            JButton stopButton = new JButton("停止");
            stopButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canRun = false;
                    canPut = false;
                    try {
                        doc.remove(0, doc.getLength());
                        doc.insertString(doc.getLength(), s, null);
                    } catch (BadLocationException ex) {
                        throw new RuntimeException(ex);
                    }
                    //receiveField.setText(s);
                }
            });

            // 创建文本区域用于显示文件内容
            textArea = new JTextArea(10, 30);
            textArea.setEditable(false); // 设置文本区域为不可编辑
            JScrollPane scrollPane1 = new JScrollPane();
            scrollPane1.setViewportView(textArea);

            // 创建按钮并添加动作监听器
            openButton = new JButton("导入敏感词");
            openButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openFile();
                }
            });

            // 创建按钮并添加动作监听器
            JButton wordButton = new JButton("高亮");
            wordButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        doc.remove(0, doc.getLength());
                    } catch (BadLocationException ex) {
                        throw new RuntimeException(ex);
                    }
                    try {
                        doc.insertString(doc.getLength(), s, null);
                    } catch (BadLocationException ex) {
                        throw new RuntimeException(ex);
                    }
                    try {
                        highlightWords(doc, words);
                    } catch (BadLocationException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            // 将组件添加到窗口
            jp.add(openButton);
            jp.add(wordButton);
            scrollPane1.setBounds(10,50 , 130, 400);
            jf.add(scrollPane1);


            //jp.add(receiveField);
            jp.add(sendField);
            jp.add(sendButton);
            jp.add(stopButton);

            //jp.setBounds(5, 0, 400, 500);

            //jp.setBounds(0, 0, 100, 50);
            //jp.setBounds(0, 0, 400, 500);
            jf.add(jp);

            // 将JScrollPane（包含JTextArea）添加到JFrame中
            jf.add(scrollPane);

            //jp.setBounds(0, 0, 400, 500);
            jf.add(jp);

            // 设置窗口可见
            jf.setVisible(true);
        }

        // 高亮显示文本中的特定单词
        private void highlightWords(StyledDocument doc, List<String> words) throws BadLocationException {
            for (String word : words) {
                String patternString = Pattern.quote(word);
                Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(doc.getText(0, doc.getLength()));

                int lastEnd = 0;
                while (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();

                    // 防止高亮重叠
                    if (start < lastEnd) {
                        continue;
                    }

                    // 创建一个简单的属性集来高亮文本
                    SimpleAttributeSet attrs = new SimpleAttributeSet();
                    StyleConstants.setForeground(attrs, Color.RED); // 设置前景色为红色

                    // 应用高亮
                    doc.setCharacterAttributes(start, end - start, attrs, false);

                    lastEnd = end;
                }
            }
        }

        private void openFile() {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(jf); // 弹出文件选择对话框

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                    String line;
                    textArea.setText(""); // 清空文本区域
                    while ((line = reader.readLine()) != null) {
                        words.add(line);
                        textArea.append(line + "\n"); // 逐行读取并添加到文本区域
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(jf, "读取文件时发生错误：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public class Get extends Thread {
        public String u;

        public Get(String u) {
            this.u = u;
        }

        public void run() {
            try {
                get();
            } catch (IOException e) {
                //throw new RuntimeException(e);
            } catch (InterruptedException e) {
                //throw new RuntimeException(e);
            }
        }

        public void get() throws IOException, InterruptedException {
            if (urlSet.contains(u)) {
                return;
            }
            urlSet.add(u);
            Document doc = getHtml(u);
            Elements links = doc.select("a[href]"); // a标签带有href属性的

            if (doc.html().contains("<html>")) {
                save(doc.html(), PATH + "html//" + num++ + ".html");
                save(doc.select("p").text(), PATH + "text//" + num++ + ".txt");
            }
            if (canPut) {
                s += doc.select("p").text();
            }
            // 遍历找到的链接
            for (Element link : links) {
                // 获取href属性的值
                String href = link.attr("href");

                // 过滤掉非HTTP/HTTPS协议的URL
                if (isValidHttpUrl(href) && canRun) {
                    Thread.sleep(100);
                    new Get(href).start();
                }
            }
        }

        // 辅助方法：检查URL是否是有效的HTTP/HTTPS URL
        private static boolean isValidHttpUrl(String urlStr) {
            try {
                URL url = new URL(urlStr);
                String protocol = url.getProtocol();
                return "http".equals(protocol) || "https".equals(protocol);
            } catch (MalformedURLException e) {
                // 如果URL格式不正确，则认为它不是有效的HTTP/HTTPS URL
                return false;
            }
        }

        public Document getHtml(String url) throws IOException, InterruptedException {
            return Jsoup.connect(url).get();
        }

        public void save(String s, String path) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
                writer.write(s);
            } catch (IOException e) {
                //System.err.println("发生错误：" + e.getMessage());
            }
        }
    }
}