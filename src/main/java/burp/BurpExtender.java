package burp;

import common.util.PrintUtil;
import listener.HttpListener;

import java.awt.*;

/**
 * <p>
 *
 * </p>
 *
 * @author: dxl
 * @Time: 2022/8/24  21:12
 */
public class BurpExtender implements IBurpExtender, ITab {
    public static IBurpExtenderCallbacks callbacks;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        PrintUtil.print(callbacks, "loading extender.....");
        BurpExtender.callbacks = callbacks;
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

}
