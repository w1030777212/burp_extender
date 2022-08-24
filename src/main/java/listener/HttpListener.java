package listener;

import burp.*;
import common.util.PrintUtil;

/**
 * <p>
 *
 * </p>
 *
 * @author: dxl
 * @Time: 2022/8/24  21:24
 */
public class HttpListener implements IHttpListener {
    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
        IBurpExtenderCallbacks callbacks = BurpExtender.callbacks;
        if (toolFlag == callbacks.TOOL_REPEATER){
            if (messageIsRequest){
                IExtensionHelpers helpers = callbacks.getHelpers();
                IRequestInfo request = helpers.analyzeRequest(messageInfo.getHttpService(), messageInfo.getRequest());
            }
        }
    }
}
