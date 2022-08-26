package ui;



import burp.BurpExtender;
import burp.IHttpRequestResponse;
import burp.IRequestInfo;
import common.util.PrintUtil;
import common.util.SysParamUtil;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DownLoadWindow {

    public JLabel urlLabel;
    public JLabel  pathInputText;
    public JButton pathBtn;
    public JLabel barText;
    public JProgressBar bar;
    public JButton startBtn;
    public JFrame jf;

    public ExecutorService executor;

    IRequestInfo reqInfo;

    Future future;


    public DownLoadWindow(IRequestInfo reqInfo){
        this.reqInfo = reqInfo;
        executor = Executors.newSingleThreadExecutor();
        init();
        registerListener();
    }
    private void init(){
        try {
            URI uri = reqInfo.getUrl().toURI();
            urlLabel = new JLabel("request url: "+uri.toString());
        }catch (Exception e){
            urlLabel = new JLabel("promlem occurred!");
        }
        pathInputText = new JLabel("save path: ");  // 文字：本地保存路径
        pathBtn=  new JButton("choose save path");   // 保存路径输入框
        barText = new JLabel("process");         // 文字：进度条
        bar = new JProgressBar();       // 进度条
        startBtn = new JButton("Get");
        jf = new JFrame("downlaod window");
        jf.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        jf.setSize(512, 321);
        jf.setLayout(new GridLayout(8, 1));
        jf.add(urlLabel);
        jf.add(pathInputText);
        jf.add(pathBtn);
        jf.add(barText);
        jf.add(bar);
        jf.add(startBtn);
        jf.setVisible(true);
    }

    private void registerListener(){
        pathBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int intRes = chooser.showOpenDialog(pathBtn);
                if (intRes == JFileChooser.APPROVE_OPTION){
                    pathInputText.setText(chooser.getSelectedFile().getPath());
                }
            }
        });
        startBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if ("Get".equals(startBtn.getText())){
                    startBtn.setText("Cancel");
                    bar.setValue(0);
                    future = executor.submit(()->{
                        InputStream in = null;
                        FileOutputStream out=null;
                        try {
                            SSLContext sslContext = new SSLContextBuilder()
                                    .loadTrustMaterial(null, (certificate, authType) -> true).build();
                            HttpClient httpClient = HttpClients.custom()
                                    .setSSLContext(sslContext)
                                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                                    .build();

                            URI uri = reqInfo.getUrl().toURI();
                            HttpGet httpGet = new HttpGet(uri.toString());
                            PrintUtil.print(BurpExtender.burpCallbacks, "uri: "+uri);
                            Map<String, String> headersMap = parseHeaders(reqInfo.getHeaders());
                            for (Map.Entry<String, String> entry : headersMap.entrySet()) {
                                httpGet.addHeader(entry.getKey(), entry.getValue());

                            }
                            //发送请求
                            HttpResponse response = httpClient.execute(httpGet);
                            int code = response.getStatusLine().getStatusCode();
                            if (code == HttpStatus.SC_OK) {
                                HttpEntity entity = response.getEntity();
                                in = entity.getContent();
                                String contentDisposition = response.getHeaders("Content-Disposition")[0].getValue();
                                String[] property = contentDisposition.split(";");
                                String fileName=null;
                                //解析本地存储路径
                                for (String p : property) {
                                    PrintUtil.print(BurpExtender.burpCallbacks, "p: "+p);
                                    if (p.startsWith("fileName")){
                                        fileName = p.split("=")[1];
                                    }
                                }
                                String saveURL = pathInputText.getText()+"\\"+fileName;
                                //文件下载
                                long srcSize = response.getEntity().getContentLength();
                                if (srcSize<SysParamUtil.BUFFER_SIZE){
                                    srcSize = SysParamUtil.BUFFER_SIZE;
                                }
                                //2.IO流拷贝
                                out = new FileOutputStream(saveURL);
                                byte[] buffer = new byte[1024];
                                int len;
                                float section;
                                long readedBytes=0l;
                                while ((len = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, len);
                                    readedBytes+=len;
                                    section = readedBytes/srcSize;
                                    int procedure = (int) section * 100;
                                    PrintUtil.print(BurpExtender.burpCallbacks,"procedure: "+procedure);
                                    refreshBar(procedure, bar);
                                }
                                out.flush();
                                startBtn.setText("Get");
                            }
                        }catch (Exception e){
                            PrintUtil.print(BurpExtender.burpCallbacks, e.getMessage());
                        }finally {
                            try {
                                if (null != in){
                                    in.close();
                                }
                                if (null != out){
                                    out.close();
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                }else {
                    if (null != future && !future.isCancelled()){
                        future.cancel(true);
                        startBtn.setText("Get");
                    }
                }
            }
        });
    }

    private void refreshBar(int procedure, JProgressBar bar) {
        bar.setValue(procedure);
    }

    public static Map<String, String> parseHeaders(List<String> headers){
        Map<String, String> res = new HashMap<>();
        for (String header : headers) {
            int splitIndex = header.indexOf(":");
            res.put(header.substring(0, splitIndex), header.substring(splitIndex+1));
        }
        return res;
    }


}
