package burp;

import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.MimeMultipart;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import common.util.PrintUtil;
import listener.HttpListener;
import ui.TestUI;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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


    private static final String MENU_NAME = "Upload File";
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
            JMenuItem menuButoon = new JMenuItem(MENU_NAME);
            menuButoon.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getActionCommand().equals(MENU_NAME)){
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

            menuItemList.add(menuButoon);
            return menuItemList;
        }
        return null;
    }

    public static byte[] uploadFileToRequset(IHttpRequestResponse httpRequestResponse, File file)
            throws IOException {
        IRequestInfo reqInfo = burpHelpers.analyzeRequest(httpRequestResponse);
        List<String> headers = reqInfo.getHeaders();
        String formStr = "";
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
        MimeMultipart mimeMultipart = new MimeMultipart();


        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        FileInputStream inputStream = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int len;
        while ((len=inputStream.read(buffer))!=-1){
            byteArray.write(buffer,0,len);
        }

        byte[] body = byteArray.toByteArray();
        inputStream.close();
        byteArray.close();
        return burpHelpers.buildHttpMessage(reqInfo.getHeaders(), body);
    }

    public static void writeMetaData(String boundary, ByteArrayOutputStream byteArray, Map.Entry<String, Object> entry ){

    }
}
