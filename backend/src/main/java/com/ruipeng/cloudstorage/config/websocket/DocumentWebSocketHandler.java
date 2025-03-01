package com.ruipeng.cloudstorage.config.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentWebSocketHandler extends TextWebSocketHandler {
    private static final Map<String, Map<String, WebSocketSession>> documentSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String documentId = getParameter(session, "documentId");
        String userId = getParameter(session, "userId");

        documentSessions.computeIfAbsent(documentId, k -> new ConcurrentHashMap<>())
                .put(userId, session);

        broadcastToDocument(documentId, createMessage("userJoined", userId, userId),null);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
        System.out.println("message.payload()"+message.getPayload());
        String documentId = (String) data.get("documentId");
        String userId = (String) data.get("userId");
        System.out.println("documentId: " + documentId + ", userId: " + userId);
        String type = (String) data.get("type");

        broadcastToDocument(documentId, message.getPayload(), userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String documentId = getParameter(session, "documentId");
        String userId = getParameter(session, "userId");

        if (documentId != null && userId != null) {
            documentSessions.getOrDefault(documentId, new ConcurrentHashMap<>()).remove(userId);
            broadcastToDocument(documentId, createMessage("userLeft", userId, userId));
        }
    }

    private String getParameter(WebSocketSession session, String param) {
        String query = session.getUri().getQuery();
        return query.contains(param) ?
                query.split(param + "=")[1].split("&")[0] : null;
    }

    private void broadcastToDocument(String documentId, String message, String excludeUserId) {
        documentSessions.getOrDefault(documentId, new ConcurrentHashMap<>())
                .forEach((userId, session) -> {
                    if (!userId.equals(excludeUserId) && session.isOpen()) {
                        try {
                            session.sendMessage(new TextMessage(message));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void broadcastToDocument(String documentId, String message) {
        broadcastToDocument(documentId, message, null);
    }

    private String createMessage(String type, String userId, Object data) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "userId", userId,
                    "data", data
            ));
        } catch (Exception e) {
            return "{}";
        }
    }
}