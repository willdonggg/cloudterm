package com.kodedu.cloudterm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.kodedu.cloudterm.helper.EncodingEnvironmentUtil;
import com.kodedu.cloudterm.helper.IOHelper;
import com.kodedu.cloudterm.helper.ThreadHelper;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.sun.jna.Platform;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

public class TerminalService {

//    @Value("${shell:#{null}}")
//    private String shellStarter;

    private boolean isReady;
    private String[] termCommand;
    private PtyProcess process;
    private Integer columns = 20;
    private Integer rows = 10;
    private BufferedReader inputReader;
    private BufferedReader errorReader;
    private OutputStream outputStream;
    private WebSocketSession session;

    private LinkedBlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();

    public TerminalService(WebSocketSession session) {
        this.session = session;
    }

    public void onTerminalInit() {

    }

    public void onTerminalReady(String command) {

        ThreadHelper.start(() -> {
            isReady = true;
            try {
                initializeProcess(command);
            } catch (Exception e) {
                // e.printStackTrace();
            }
        });

    }

    private void initializeProcess(String command) throws Exception {
        isReady = false;
        if (process != null) {
            print("");
            process.destroy();
        }

        String userHome = System.getProperty("user.home");
        Path dataDir = Paths.get(userHome).resolve(".terminalfx");
        IOHelper.copyLibPty(dataDir);

        Charset charset = Charset.forName("UTF-8");

        Map<String, String> envs = Maps.newHashMap(System.getenv());

        EncodingEnvironmentUtil.setLocaleEnvironmentIfMac(envs, charset);

        if (Platform.isWindows()) {
            this.termCommand = "cmd.exe".split("\\s+");
        } else {
            if (StringUtils.isEmpty(command)) {
                command = "python3";
            }
            this.termCommand = command.split("\\s+");
        }

//        if(Objects.nonNull(shellStarter)){
//            this.termCommand = shellStarter.split("\\s+");
//        }

        envs.put("TERM", "xterm");

        System.setProperty("PTY_LIB_FOLDER", dataDir.resolve("libpty").toString());

        this.process = PtyProcess.exec(termCommand, envs, userHome);

        process.setWinSize(new WinSize(columns, rows));
        this.inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        this.errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        this.outputStream = process.getOutputStream();

        ThreadHelper.start(() -> {
            printReader(inputReader);
        });

        ThreadHelper.start(() -> {
            printReader(errorReader);
        });

        process.waitFor();

    }

    public void print(String text) throws IOException {

        Map<String, String> map = new HashMap<>();
        map.put("type", "TERMINAL_PRINT");
        map.put("text", text);
        map.put("refresh", !isReady ? "true" : "false");

        String message = new ObjectMapper().writeValueAsString(map);

        if (session != null) {
            session.sendMessage(new TextMessage(message));
            isReady = true;
        }
    }

    private void printReader(BufferedReader bufferedReader) {
        try {
            int nRead;
            char[] data = new char[1 * 1024];

            while ((nRead = bufferedReader.read(data, 0, data.length)) != -1) {
                StringBuilder builder = new StringBuilder(nRead);
                builder.append(data, 0, nRead);
                print(builder.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onCommand(String command) throws InterruptedException {

        if (Objects.isNull(command)) {
            return;
        }

        commandQueue.put(command);
        ThreadHelper.start(() -> {
            try {
                if (outputStream != null) {
                    String msg = commandQueue.poll();
                    if (msg != null) {
                        outputStream.write(msg.getBytes("UTF-8"));
                        outputStream.flush();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    public void onTerminalResize(String columns, String rows) {
        if (Objects.nonNull(columns) && Objects.nonNull(rows)) {
            this.columns = Integer.valueOf(columns);
            this.rows = Integer.valueOf(rows);

            if (Objects.nonNull(process)) {
                process.setWinSize(new WinSize(this.columns, this.rows));
            }

        }
    }
}