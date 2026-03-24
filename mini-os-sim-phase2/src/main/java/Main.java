import os.cli.Shell;
import os.gui.MainWindow;

import javax.swing.*;

/**
 * Application entry point.
 *
 * Default mode  : Swing GUI  (java -jar mini-os-simulator.jar)
 * CLI mode      : Interactive shell  (java -jar mini-os-simulator.jar --cli)
 */
public class Main {

    public static void main(String[] args) {
        boolean cliMode = false;
        for (String arg : args) {
            if (arg.equals("--cli") || arg.equals("-cli")) {
                cliMode = true;
                break;
            }
        }

        if (cliMode) {
            new Shell().run();
        } else {
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception ignored) {}
                new MainWindow();
            });
        }
    }
}
