package ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

/**
 * <p>
 *
 * </p>
 *
 * @author: dxl
 * @Time: 2022/8/24  22:34
 */
public class TestUI {
    public JButton button1;
    public JPanel panel1;

    public TestUI() {

        button1.setText("Asdasd");
        button1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setMultiSelectionEnabled(true);
                int returnVal = chooser.showOpenDialog(button1);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File[] arrayFile = chooser.getSelectedFiles();
                    for (File file : arrayFile) {
                        System.out.println(file);
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("TestUI");
        frame.setContentPane(new TestUI().panel1);
        frame.setVisible(true);
        frame.setSize(800, 800);
        init();
    }

    public static void init() {


    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        button1 = new JButton();
        button1.setText("Button");
        panel1.add(button1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

}