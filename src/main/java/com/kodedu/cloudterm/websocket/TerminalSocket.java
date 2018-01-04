package com.kodedu.cloudterm.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodedu.cloudterm.service.TerminalService;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TerminalSocket extends TextWebSocketHandler {

    private static final Map<String, TerminalService> terminalServices = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        TerminalService service = new TerminalService(session);
        terminalServices.put((String) session.getAttributes().get("userTopicId"), service);
        super.afterConnectionEstablished(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        TerminalService terminalService = terminalServices.get(session.getAttributes().get("userTopicId"));
        Map<String, String> messageMap = getMessageMap(message);

        if (terminalService != null && messageMap.containsKey("type")) {
            String type = messageMap.get("type");

            switch (type) {
                case "TERMINAL_INIT":
                    terminalService.onTerminalInit();
                    break;
                case "TERMINAL_READY":
                    terminalService.onTerminalReady(null);
                    break;
                case "TERMINAL_COMMAND":
                    terminalService.onCommand(messageMap.get("command"));
                    break;
                case "TERMINAL_RESIZE":
                    terminalService.onTerminalResize(messageMap.get("columns"), messageMap.get("rows"));
                    break;
                default:
                    throw new RuntimeException("Unrecodnized action");
            }
        }
    }

    private Map<String, String> getMessageMap(TextMessage message) {
        try {
            Map<String, String> map = new ObjectMapper().readValue(message.getPayload(), new TypeReference<Map<String, String>>() {
            });

            return map;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }

    @Override
    public boolean supportsPartialMessages() {
        return super.supportsPartialMessages();
    }

    public static TerminalService getTerminal(String id) {
        return terminalServices.get(id);
    }
}