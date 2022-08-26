package burp;


import common.util.PrintUtil;
import common.util.UUIDUtil;
import listener.HttpListener;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import ui.DownLoadWindow;

import javax.net.ssl.SSLContext;
import javax.swing.JFileChooser;

import javax.swing.JMenuItem;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URI;


import java.util.*;
import java.util.List;

/**
 * <p>
 *
 * </p>
 *
 * @author: dxl
 * @Time: 2022/8/24  21:12
 */
public class BurpExtender implements IBurpExtender, ITab,IContextMenuFactory {
    public static IBurpExtenderCallbacks burpCallbacks;
    public static IContextMenuInvocation menuInvocation;
    public static IExtensionHelpers burpHelpers;

    public static Map<String, String> typeHandler = new HashMap<>();


    public static final String BOUNDARY_PREFIX="--";
    private static final String LINE_END = "\r\n";
    private static final String UPLOAD_NAME = "Upload File";
    private static final String DOWNLOAD_NAME = "Download File";

    static {
        typeHandler.put("pdf","application/pdf");
        typeHandler.put("txt","text/plain");
        typeHandler.put("html","text/html");
        typeHandler.put("xml","text/xml");
        typeHandler.put("png","image/png");
        typeHandler.put("xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        typeHandler.put("docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        PrintUtil.print(callbacks, "loading extender.....");
        burpCallbacks = callbacks;
        burpCallbacks.registerContextMenuFactory(this);
        burpHelpers = burpCallbacks.getHelpers();

        callbacks.registerHttpListener(new HttpListener());
    }



    @Override
    public String getTabCaption() {
        return null;
    }

    @Override
    public Component getUiComponent() {
        return null;
    }

    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        List<JMenuItem> menuItemList = new ArrayList<>();
        menuInvocation = invocation;
        if (menuInvocation.getInvocationContext() == IContextMenuInvocation.CONTEXT_PROXY_HISTORY ||
                menuInvocation.getInvocationContext()
                        == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST ||
                menuInvocation.getInvocationContext()
                        == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_RESPONSE ||
                menuInvocation.getInvocationContext()
                        == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST ||
                menuInvocation.getInvocationContext()
                        == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_RESPONSE
        ){
            JMenuItem upLoadButton = new JMenuItem(UPLOAD_NAME);
            upLoadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand().equals(UPLOAD_NAME)){
                        IHttpRequestResponse selectedMessages = menuInvocation.getSelectedMessages()[0];
                        IRequestInfo reqInfo = burpHelpers.analyzeRequest(selectedMessages.getRequest());
                        JFileChooser chooser = new JFileChooser();
                        chooser.setMultiSelectionEnabled(true);
                        int returnVal = chooser.showOpenDialog(null);
                        File file = chooser.getSelectedFiles()[0];
                        int bodyOffset = reqInfo.getBodyOffset();
                        try {
                            byte[] newReq = uploadFileToRequset(selectedMessages,file);
                            selectedMessages.setRequest(newReq);
                        }catch (Exception exception){
                            throw new RuntimeException(exception);
                        }
                    }
                }
            });

            JMenuItem downLoadButton = new JMenuItem(DOWNLOAD_NAME);
            downLoadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand().equals(DOWNLOAD_NAME)){
                        IHttpRequestResponse selectedMessage = menuInvocation.getSelectedMessages()[0];
                        downloadFileByRequest(selectedMessage);
                    }
                }
            });

            menuItemList.add(upLoadButton);
            menuItemList.add(downLoadButton);
            return menuItemList;
        }
        return null;
    }

    public static void downloadFileByRequest(IHttpRequestResponse httpRequestResponse) {
        IRequestInfo reqInfo = burpHelpers.analyzeRequest(httpRequestResponse);
        DownLoadWindow downLoadWindow = new DownLoadWindow(reqInfo);

//        new Thread(()->{
//            InputStream in = null;
//            FileOutputStream out=null;
//            try {
//                IRequestInfo reqInfo = burpHelpers.analyzeRequest(httpRequestResponse);
//
//
//                SSLContext sslContext = new SSLContextBuilder()
//                        .loadTrustMaterial(null, (certificate, authType) -> true).build();
//                HttpClient httpClient = HttpClients.custom()
//                        .setSSLContext(sslContext)
//                        .setSSLHostnameVerifier(new NoopHostnameVerifier())
//                        .build();
//
//                URI uri = reqInfo.getUrl().toURI();
//                HttpGet httpGet = new HttpGet(uri.toString());
//                PrintUtil.print(burpCallbacks, "uri: "+uri);
//                Map<String, String> headersMap = parseHeaders(reqInfo.getHeaders());
//                for (Map.Entry<String, String> entry : headersMap.entrySet()) {
//                    httpGet.addHeader(entry.getKey(), entry.getValue());
//
//                }
//                HttpResponse response = httpClient.execute(httpGet);
//                int code = response.getStatusLine().getStatusCode();
//                if (code == HttpStatus.SC_OK) {
//                    HttpEntity entity = response.getEntity();
//                    in = entity.getContent();
//                    String contentDisposition = response.getHeaders("Content-Disposition")[0].getValue();
//                    String[] property = contentDisposition.split(";");
//                    String fileName=null;
//                    for (String p : property) {
//                        PrintUtil.print(burpCallbacks, "p: "+p);
//                        if (p.startsWith("fileName")){
//                            fileName = p.split("=")[1];
//                        }
//                    }
//                    PrintUtil.print(burpCallbacks, "filename: "+fileName);
//                    out = new FileOutputStream("D:\\a.pdf");
//                    byte[] buffer = new byte[1024];
//                    int len;
//                    while ((len = in.read(buffer)) != -1) {
//                        out.write(buffer, 0, len);
//                    }
//                    out.flush();
//                }
//            }catch (Exception e){
//                PrintUtil.print(burpCallbacks, e.getMessage());
//            }finally {
//                try {
//                    if (null != in){
//                        in.close();
//                    }
//                    if (null != out){
//                        out.close();
//                    }
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        }).start();
    }


    public static byte[] uploadFileToRequset(IHttpRequestResponse httpRequestResponse, File file)
            throws IOException {
        IRequestInfo reqInfo = burpHelpers.analyzeRequest(httpRequestResponse);
        List<String> headers = reqInfo.getHeaders();
        String boundary = "XBD"+UUIDUtil.getUUID();

//        for (int i=0;i<headers.size();i++){
//            String header = headers.get(i);
//            if (header.startsWith("Content-Type")){
//                headers.remove(i);
//            }
//        }
//        headers.add("Content-Type: multipart/form-data; boundary=" + boundary);

        String formStr="";
        for (int i=0;i<headers.size();i++){
            String header = headers.get(i);
            if ("Content-Type".equals(header)){
                String[] subPropertys = header.split(";");
                for (String property : subPropertys) {
                    if (property.startsWith("boundary")){
                         formStr = property.split("=")[1];
                    }
                }
            }
        }



        byte[] request = httpRequestResponse.getRequest();
        int bodyOffset = reqInfo.getBodyOffset();
        byte[] bodyBytes = new byte[request.length-bodyOffset];
        System.arraycopy(request,bodyOffset,bodyBytes,0, bodyBytes.length);
        String bodyStr = new String(bodyBytes);

        int i = bodyStr.indexOf("Content-Type");
        int endi = bodyStr.indexOf(BOUNDARY_PREFIX+formStr, i);



//        MimeMultipart mimeMultipart = new MimeMultipart();
//
//
//        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
//        FileInputStream inputStream = new FileInputStream(file);
//        byte[] buffer = new byte[1024];
//        int len;
//        while ((len=inputStream.read(buffer))!=-1){
//            byteArray.write(buffer,0,len);
//        }
//
//        byte[] body = byteArray.toByteArray();
//        inputStream.close();
//        byteArray.close();

        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArray);
//        Map<String, Object> map = new HashMap<>();
//        for (Map.Entry<String, Object> entry : map.entrySet()) {
//            writeMetaData(boundary, dataOutputStream, entry);
//        }
//        writeFile(boundary,dataOutputStream, file);
//
//        String endStr = BOUNDARY_PREFIX+boundary+BOUNDARY_PREFIX+LINE_END;
//        byteArray.write(endStr.getBytes());
//
//        byte[] body = byteArray.toByteArray();

        String headBody = replaceFileName(bodyStr.substring(0,i), file.getName());
        String bottomBody = replaceFileName(bodyStr.substring(endi), file.getName());
        dataOutputStream.write(headBody.getBytes());
        writeFile(dataOutputStream,file);
        dataOutputStream.write(bottomBody.getBytes());
        byte[] body = byteArray.toByteArray();

        dataOutputStream.flush();
        dataOutputStream.close();
        return burpHelpers.buildHttpMessage(headers, body);
    }

//    public static void writeMetaData(String boundary, DataOutputStream dataOutputStream, Map.Entry<String, Object> entry ) throws IOException {
//        String boundaryStr = BOUNDARY_PREFIX+boundary+LINE_END;
//        dataOutputStream.write(boundaryStr.getBytes());
//        String contentDispositionStr = String.format("Content-Disposition: form-data; name=\"%s\"",entry.getKey())+LINE_END+LINE_END;
//        dataOutputStream.write(contentDispositionStr.getBytes());
//        String valueStr = entry.getValue().toString() + LINE_END;
//        dataOutputStream.write(valueStr.getBytes());
//    }
//    public static void writeFile(String boundary, DataOutputStream dataOutputStream, File file) throws IOException {
//        String boundaryStr = BOUNDARY_PREFIX+boundary+LINE_END;
//        dataOutputStream.write(boundaryStr.getBytes());
//        String fileName = file.getName();
//        String contentDispositionStr = String.format("Content-Disposition: form-data; name=\"multipartFile\"; filename=\"%s\"",fileName)+LINE_END;
//        dataOutputStream.write(contentDispositionStr.getBytes("utf-8"));
//
//        int i = fileName.lastIndexOf(".");
//        String contentType = String.format("Content-Type: %s",analyzeContentType(fileName.substring(i + 1)))+ LINE_END + LINE_END;
//        dataOutputStream.write(contentType.getBytes());
//
//        FileInputStream inputStream = new FileInputStream(file);
//        byte[] buffer = new byte[1024];
//        int len;
//        while ((len=inputStream.read(buffer))!=-1){
//            dataOutputStream.write(buffer,0,len);
//        }
//        inputStream.close();
//        dataOutputStream.write(LINE_END.getBytes());
//    }

    public static void writeFile( DataOutputStream dataOutputStream, File file) throws IOException {
        String fileName = file.getName();
        int i = fileName.lastIndexOf(".");
        String contentType = String.format("Content-Type: %s",analyzeContentType(fileName.substring(i + 1)))+ LINE_END + LINE_END;
        dataOutputStream.write(contentType.getBytes());

        FileInputStream inputStream = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int len;
        while ((len=inputStream.read(buffer))!=-1){
            dataOutputStream.write(buffer,0,len);
        }
        inputStream.close();
        dataOutputStream.write(LINE_END.getBytes());
    }

    public static String replaceFileName(String str, String fileName){
        int i = str.indexOf("filename=");
        String newStr=str;
        if (i!=-1){
            String oldName = str.substring(i,str.indexOf(LINE_END,i));
            String newName = String.format("filename=\"%s\"", fileName);
            newStr = str.replaceAll(oldName, newName);
        }
        return newStr;
    }

    public static String analyzeContentType(String endFix){
        return typeHandler.get(endFix);
    }
}
