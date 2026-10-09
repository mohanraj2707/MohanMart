package com.mohan.mohanmart.controller;

import com.mohan.mohanmart.dto.UserResponseDTO;
import com.mohan.mohanmart.exception.ValidationException;
import com.mohan.mohanmart.service.ChatService;
import com.mohan.mohanmart.service.impl.ChatServiceImpl;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Controller/Servlet handling customer support AI chatbot interactions.
 * REST endpoint: POST /api/v1/chat, /api/chat
 */
@WebServlet(name = "ChatServlet", urlPatterns = {"/api/chat", "/api/v1/chat"})
public class ChatServlet extends BaseServlet {

    private final ChatService chatService;

    public ChatServlet() {
        this(new ChatServiceImpl());
    }

    public ChatServlet(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String message = null;

            // Try parsing JSON body first
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = readJsonBody(req, Map.class);
                if (body != null && body.containsKey("message") && body.get("message") != null) {
                    message = String.valueOf(body.get("message"));
                }
            } catch (Exception ignored) {
            }

            // Fallback to form parameter
            if (message == null || message.trim().isEmpty()) {
                message = req.getParameter("message");
            }

            if (message == null || message.trim().isEmpty()) {
                throw new ValidationException("Chat message cannot be empty", "EMPTY_MESSAGE");
            }

            String trimmed = message.trim();
            if (trimmed.length() > ChatServiceImpl.MAX_MESSAGE_LENGTH) {
                throw new ValidationException(
                        "Chat message cannot exceed " + ChatServiceImpl.MAX_MESSAGE_LENGTH + " characters",
                        "MESSAGE_TOO_LONG");
            }

            UserResponseDTO currentUser = getSessionUser(req);
            Long userId = (currentUser != null) ? currentUser.getId() : null;
            HttpSession session = req.getSession(false);

            String reply;
            if (userId != null) {
                reply = chatService.processMessage(trimmed, userId);
            } else if (session != null && session.getId() != null) {
                reply = chatService.processMessage(trimmed, "session:" + session.getId());
            } else {
                reply = chatService.processMessage(trimmed, (Long) null);
            }

            writeJsonResponse(resp, HttpServletResponse.SC_OK, "Chat response generated successfully",
                    Collections.singletonMap("reply", reply));
        } catch (Exception e) {
            handleException(resp, e, true);
        }
    }
}
