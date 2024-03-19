package net.sanity.cosmicreachandroid.launch;

import android.icu.text.DecimalFormat;
import android.os.Build;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import finalforeach.cosmicreach.BlockGame;
import finalforeach.cosmicreach.RuntimeInfo;
import finalforeach.cosmicreach.io.SaveLocation;
import net.sanity.cosmicreachandroid.MainActivity;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrashScreen {
    public CrashScreen() {
    }

    private static String byteSizeToHumanReadable(long numBytes) {
        return RuntimeInfo.byteSizeToHumanReadable(numBytes);
    }

    public static void showCrash(long startTime, StringBuilder preStartErr, Exception ex) {

        long crashTime = System.currentTimeMillis();
        long ranFor = crashTime - startTime;
        long ranForHours = TimeUnit.MILLISECONDS.toHours(ranFor);
        long ranForMins = TimeUnit.MILLISECONDS.toMinutes(ranFor) - TimeUnit.MILLISECONDS.toHours(ranFor) * 60L;
        long ranForSec = TimeUnit.MILLISECONDS.toSeconds(ranFor) - TimeUnit.MILLISECONDS.toMinutes(ranFor) * 60L;
        long ranForMillis = ranFor - TimeUnit.MILLISECONDS.toSeconds(ranFor) * 1000L;
        String ranForTime = ranFor + " ms";
        if (ranForHours > 0L) {
            ranForTime = ranForHours + " hours, " + ranForMins + " minutes, " + ranForSec + " seconds";
        } else if (ranForMins > 0L) {
            ranForTime = ranForMins + " minutes, " + ranForSec + " seconds";
        } else if (ranForSec > 0L) {
            ranForTime = ranForSec + " seconds, " + ranForMillis + " ms";
        }

        String title = BlockGame.gameStarted ? "Crash while playing Cosmic Reach" : "Could not start Cosmic Reach";
        String infoText = "If writing a bug report, please copy the following logs (don't just screenshot!):";
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        OrderedMap<String, Object> infoItems = new OrderedMap<>();
        infoItems.put("Game started", BlockGame.gameStarted);
        String gameVersion = "unknown";

        try {
            System.out.println((new File("")).getAbsolutePath());
            if (Gdx.files == null) {
                Gdx.files = new Lwjgl3Files();
            }

            gameVersion = Gdx.files.internal("build_assets/version.txt").readString();
        } catch (Exception var38) {
            preStartErr.append(var38);
        }

        infoItems.put("Game version", gameVersion);
        infoItems.put("Ran for ", ranForTime);
        infoItems.put("Current time", ZonedDateTime.now().toString().replace("T", " at "));
        String var10002 = System.getProperty("os.name");
        infoItems.put("Operating system", var10002 + " " + System.getProperty("os.version"));
        infoItems.put("Arch", System.getProperty("os.arch"));
        infoItems.put("Java VM name", System.getProperty("java.vm.name"));
        infoItems.put("Java runtime version", System.getProperty("java.runtime.version"));
        infoItems.put("System user language", System.getProperty("user.language"));
        infoItems.put("CPU model", "unknown (by code, porting this was hard)"); // its VERY hard to get cpu model on android so i gave up
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        infoItems.put("Device model", model.toLowerCase().startsWith(manufacturer.toLowerCase()) ? model : manufacturer + " " + model);
        String osName = System.getProperty("os.name").toLowerCase();

        File saveFolder = SaveLocation.getSaveFolder();
        infoItems.put("Save location free space", byteSizeToHumanReadable(saveFolder.getFreeSpace()));
        infoItems.put("Save location total space", byteSizeToHumanReadable(saveFolder.getTotalSpace()));

        infoItems.put("Available processors", Runtime.getRuntime().availableProcessors());
        if (Gdx.app != null) {
            infoItems.put("Native heap use", byteSizeToHumanReadable(Gdx.app.getNativeHeap()));
            infoItems.put("Java heap use", byteSizeToHumanReadable(Gdx.app.getJavaHeap()));
        }

        infoItems.put("Max memory available", byteSizeToHumanReadable(Runtime.getRuntime().maxMemory()));
        infoItems.put("RAM available", "Unknown");

        String str;
            infoItems.put("RAM available", getAvailableRAM());

        if (Gdx.graphics != null) {
            infoItems.put("getGLVersion", Gdx.graphics.getGLVersion().getDebugVersionString());
        }

        infoItems.put("Prestart error logs", preStartErr);
        infoItems.put("Exception logs", sw);
        StringBuilder logText = new StringBuilder();

        for (ObjectMap.Entry<String, Object> entry : infoItems.entries()) {
            if (entry.value != null) {
                str = entry.value.toString();
                if (!str.isEmpty()) {
                    boolean addLineBreak = str.contains("\n");
                    logText.append("* ").append(entry.key).append(": ").append(addLineBreak ? "\n" : "").append(str).append("\n");
                }
            }
        }

        logText = new StringBuilder(logText.toString().replace("\t", "    "));

        try {
            (new File(SaveLocation.getSaveFolderLocation())).mkdirs();
            File errorLogFile = new File(SaveLocation.getSaveFolderLocation() + "/errorLogLatest.txt");
            FileOutputStream fos = new FileOutputStream(errorLogFile);

            try {
                fos.write(logText.toString().getBytes());
            } catch (Throwable var34) {
                try {
                    fos.close();
                } catch (Throwable var33) {
                    var34.addSuppressed(var33);
                }

                throw var34;
            }

            fos.close();
        } catch (Exception var35) {
            var35.printStackTrace();
        }

        /*JLabel label = new JLabel(infoText);
        label.setAlignmentX(0.5F);
        panel.add(label);
        JTextArea logTextArea = new JTextArea(logText);
        logTextArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setPreferredSize(new Dimension(1024, 576));
        logScrollPane.setVerticalScrollBarPolicy(22);
        panel.add(logScrollPane);
        JOptionPane.showMessageDialog((Component)null, panel, title, 0);*/

        MainActivity.showCrash(title + "\n" + infoText + "\n" + logText);

        ex.printStackTrace();
        //System.exit(1);
    }

    public static String getAvailableRAM() { // thanks https://stackoverflow.com/a/23508821

        RandomAccessFile reader = null;
        String load;
        DecimalFormat twoDecimalForm = new DecimalFormat("#.##");
        double totRam;
        String lastValue = "";
        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            load = reader.readLine();

            // Get the Number value from the string
            Pattern p = Pattern.compile("(\\d+)");
            Matcher m = p.matcher(load);
            String value = "";
            while (m.find()) {
                value = m.group(1);
                // System.out.println("Ram : " + value);
            }
            reader.close();

            totRam = Double.parseDouble(value);
            // totRam = totRam / 1024;

            double mb = totRam / 1024.0;
            double gb = totRam / 1048576.0;
            double tb = totRam / 1073741824.0;

            if (tb > 1) {
                lastValue = twoDecimalForm.format(tb).concat(" TB");
            } else if (gb > 1) {
                lastValue = twoDecimalForm.format(gb).concat(" GB");
            } else if (mb > 1) {
                lastValue = twoDecimalForm.format(mb).concat(" MB");
            } else {
                lastValue = twoDecimalForm.format(totRam).concat(" KB");
            }



        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return lastValue;
    }
}