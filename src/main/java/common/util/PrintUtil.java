package common.util;

import burp.IBurpExtenderCallbacks;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>
 *
 * </p>
 *
 * @author: dxl
 * @Time: 2022/8/24  21:42
 */
public class PrintUtil {
    public static void print(IBurpExtenderCallbacks callbacks, String content){
        OutputStream stdout = null;
        try {
            content += "\n";
            stdout = callbacks.getStdout();
            stdout.write(content.getBytes());
            stdout.flush();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (null != stdout) {
                    stdout.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
