/**
 * ==========================================================================
 * MohanMart Floating AI Marketplace Assistant Widget (O4 / Section 11 & 17)
 * Security: Renders all user & assistant text strictly via textContent (no innerHTML -> zero XSS).
 * ==========================================================================
 */
(function () {
    "use strict";

    function resolveContextPath() {
        var meta = document.querySelector('meta[name="context-path"]');
        if (meta && meta.getAttribute("content") !== null) {
            return meta.getAttribute("content");
        }
        if (window.MohanMart && typeof window.MohanMart.contextPath === "string") {
            return window.MohanMart.contextPath;
        }
        var path = window.location.pathname || "";
        return path.indexOf("/mohanmart") === 0 ? "/mohanmart" : "";
    }

    function getCsrfToken() {
        if (window.MohanMart && typeof window.MohanMart.getCsrfToken === "function") {
            return window.MohanMart.getCsrfToken();
        }
        var meta = document.querySelector('meta[name="_csrf"]');
        if (meta && meta.content) {
            return meta.content;
        }
        var input = document.querySelector('input[name="_csrf"]') || document.querySelector('input[name="csrfToken"]');
        return input && input.value ? input.value : "";
    }

    function createWidgetDom() {
        if (document.getElementById("mm-chat-widget")) {
            return;
        }

        var container = document.createElement("div");
        container.id = "mm-chat-widget";
        container.className = "mm-chat-widget";

        // Floating toggle button
        var toggleBtn = document.createElement("button");
        toggleBtn.id = "mm-chat-toggle";
        toggleBtn.type = "button";
        toggleBtn.className = "mm-chat-toggle";
        toggleBtn.setAttribute("aria-label", "Open MohanMart AI Assistant");
        toggleBtn.setAttribute("aria-expanded", "false");
        toggleBtn.textContent = "Ask MohanMart AI";

        // Chat panel
        var panel = document.createElement("section");
        panel.id = "mm-chat-panel";
        panel.className = "mm-chat-panel";
        panel.setAttribute("role", "dialog");
        panel.setAttribute("aria-label", "MohanMart Marketplace Assistant");
        panel.setAttribute("aria-hidden", "true");

        // Header
        var header = document.createElement("div");
        header.className = "mm-chat-header";

        var titleWrap = document.createElement("div");
        var title = document.createElement("div");
        title.className = "mm-chat-title";
        title.textContent = "MohanMart Assistant";

        var subtitle = document.createElement("div");
        subtitle.className = "mm-chat-subtitle";
        subtitle.textContent = "Product, Order & Marketplace Support";
        titleWrap.appendChild(title);
        titleWrap.appendChild(subtitle);

        var closeBtn = document.createElement("button");
        closeBtn.type = "button";
        closeBtn.className = "mm-chat-close";
        closeBtn.setAttribute("aria-label", "Close chat assistant");
        closeBtn.textContent = "×";

        header.appendChild(titleWrap);
        header.appendChild(closeBtn);

        // Quick FAQ Chips
        var chipsBar = document.createElement("div");
        chipsBar.className = "mm-chat-chips";
        var faqs = [
            "How to order?",
            "How does mock payment work?",
            "Order status meanings",
            "Returns & refunds",
            "How do reviews work?",
            "Categories available"
        ];
        faqs.forEach(function (faqText) {
            var chip = document.createElement("button");
            chip.type = "button";
            chip.className = "mm-chat-chip";
            chip.textContent = faqText;
            chip.addEventListener("click", function () {
                sendChatMessage(faqText);
            });
            chipsBar.appendChild(chip);
        });

        // Message list
        var messages = document.createElement("div");
        messages.id = "mm-chat-messages";
        messages.className = "mm-chat-messages";
        messages.setAttribute("aria-live", "polite");

        // Form
        var form = document.createElement("form");
        form.id = "mm-chat-form";
        form.className = "mm-chat-form";

        var input = document.createElement("input");
        input.id = "mm-chat-input";
        input.type = "text";
        input.className = "mm-chat-input";
        input.placeholder = "Ask about products, orders, shipping...";
        input.maxLength = 500;
        input.setAttribute("aria-label", "Type your question");

        var sendBtn = document.createElement("button");
        sendBtn.id = "mm-chat-send";
        sendBtn.type = "submit";
        sendBtn.className = "btn btn-primary btn-sm mm-chat-send";
        sendBtn.textContent = "Send";

        form.appendChild(input);
        form.appendChild(sendBtn);

        panel.appendChild(header);
        panel.appendChild(chipsBar);
        panel.appendChild(messages);
        panel.appendChild(form);

        container.appendChild(panel);
        container.appendChild(toggleBtn);
        document.body.appendChild(container);

        // Initial greeting
        appendMessage(
            "bot",
            "Hello! I am the MohanMart AI Assistant. Ask me about products, categories, ordering, mock escrow payment, shipping, returns, order statuses, or verified reviews."
        );

        // Event bindings
        toggleBtn.addEventListener("click", function () {
            var isOpen = panel.classList.toggle("open");
            toggleBtn.setAttribute("aria-expanded", String(isOpen));
            panel.setAttribute("aria-hidden", String(!isOpen));
            if (isOpen) {
                input.focus();
            }
        });

        closeBtn.addEventListener("click", function () {
            panel.classList.remove("open");
            toggleBtn.setAttribute("aria-expanded", "false");
            panel.setAttribute("aria-hidden", "true");
            toggleBtn.focus();
        });

        form.addEventListener("submit", function (e) {
            e.preventDefault();
            var text = input.value ? input.value.trim() : "";
            if (!text) {
                return;
            }
            input.value = "";
            sendChatMessage(text);
        });
    }

    function appendMessage(role, text) {
        var messages = document.getElementById("mm-chat-messages");
        if (!messages) {
            return null;
        }
        var bubble = document.createElement("div");
        bubble.className = "mm-chat-bubble mm-chat-" + role;
        // Strictly use textContent to prevent XSS
        bubble.textContent = text;
        messages.appendChild(bubble);
        messages.scrollTop = messages.scrollHeight;
        return bubble;
    }

    async function sendChatMessage(messageText) {
        var trimmed = (messageText || "").trim();
        if (!trimmed) {
            return;
        }
        if (trimmed.length > 500) {
            appendMessage("error", "Please keep your question under 500 characters.");
            return;
        }

        appendMessage("user", trimmed);
        var loadingBubble = appendMessage("bot", "Thinking...");
        var sendBtn = document.getElementById("mm-chat-send");
        var input = document.getElementById("mm-chat-input");
        if (sendBtn) sendBtn.disabled = true;
        if (input) input.disabled = true;

        try {
            var headers = {
                "Content-Type": "application/json",
                "Accept": "application/json"
            };
            var csrf = getCsrfToken();
            if (csrf) {
                headers["X-CSRF-Token"] = csrf;
            }

            var response = await fetch(resolveContextPath() + "/api/v1/chat", {
                method: "POST",
                headers: headers,
                body: JSON.stringify({ message: trimmed })
            });

            var payload = null;
            try {
                payload = await response.json();
            } catch (parseErr) {
                payload = null;
            }

            if (loadingBubble && loadingBubble.parentNode) {
                loadingBubble.parentNode.removeChild(loadingBubble);
            }

            if (response.ok && payload && payload.success && payload.data && payload.data.reply) {
                appendMessage("bot", String(payload.data.reply));
            } else if (response.status === 429) {
                var rateMsg = (payload && payload.message)
                    ? String(payload.message)
                    : "You have reached the limit of 10 messages per minute. Please wait a moment and try again.";
                appendMessage("error", rateMsg);
            } else if (response.status === 400) {
                var valMsg = (payload && payload.message)
                    ? String(payload.message)
                    : "Please enter a valid question (up to 500 characters).";
                appendMessage("error", valMsg);
            } else {
                appendMessage(
                    "bot",
                    "Our assistant is unavailable right now. Please browse Products or contact support@mohanmart.com."
                );
            }
        } catch (err) {
            if (loadingBubble && loadingBubble.parentNode) {
                loadingBubble.parentNode.removeChild(loadingBubble);
            }
            appendMessage(
                "bot",
                "Our assistant is unavailable right now. Please browse Products or contact support@mohanmart.com."
            );
        } finally {
            if (sendBtn) sendBtn.disabled = false;
            if (input) {
                input.disabled = false;
                input.focus();
            }
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", createWidgetDom);
    } else {
        createWidgetDom();
    }
})();
