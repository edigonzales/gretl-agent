@param String clientId
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>GRETL Copilot Chat</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        :root {
            color-scheme: light dark;
            font-family: "Segoe UI", Roboto, sans-serif;
            background-color: #f5f5f5;
            color: #1f2933;
        }

        body {
            margin: 0;
            padding: 0;
            display: flex;
            justify-content: center;
            background: linear-gradient(180deg, #f5f5f5 0%, #dce7f7 100%);
        }

        .chat-shell {
            width: min(900px, 100%);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            padding: 2rem 1.5rem 3rem;
            box-sizing: border-box;
        }

        h1 {
            margin-top: 0;
            font-size: 2rem;
            font-weight: 600;
            text-align: center;
            color: #133c82;
        }

        .conversation {
            flex: 1;
            overflow-y: auto;
            background-color: rgba(255, 255, 255, 0.85);
            border-radius: 12px;
            padding: 1.5rem;
            box-shadow: 0 10px 30px rgba(19, 60, 130, 0.08);
            display: flex;
            flex-direction: column;
            gap: 1rem;
        }

        .conversation::-webkit-scrollbar {
            width: 10px;
        }

        .conversation::-webkit-scrollbar-thumb {
            background-color: rgba(19, 60, 130, 0.3);
            border-radius: 5px;
        }

        .message {
            border-radius: 10px;
            padding: 0.75rem 1rem;
            box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
            line-height: 1.5;
        }

        .message-body {
            white-space: pre-wrap;
        }

        .message-user {
            align-self: flex-end;
            background-color: #133c82;
            color: #ffffff;
        }

        .message-assistant {
            align-self: flex-start;
            background-color: #ffffff;
            border: 1px solid rgba(19, 60, 130, 0.2);
        }

        .message-system {
            align-self: center;
            background-color: #fdecea;
            color: #7a271a;
            border: 1px solid #f8b4a0;
        }

        .message-meta {
            font-size: 0.8rem;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.08em;
            margin-bottom: 0.3rem;
        }

        .composer {
            margin-top: 1.5rem;
            display: grid;
            gap: 0.75rem;
        }

        .composer label {
            font-weight: 600;
            color: #133c82;
        }

        .composer textarea {
            resize: vertical;
            min-height: 120px;
            border-radius: 10px;
            padding: 1rem;
            border: 1px solid rgba(19, 60, 130, 0.25);
            font: inherit;
        }

        .composer textarea:focus {
            outline: 3px solid rgba(19, 60, 130, 0.2);
        }

        .composer button {
            justify-self: flex-end;
            background-color: #133c82;
            color: #ffffff;
            border: none;
            padding: 0.75rem 1.75rem;
            font-size: 1rem;
            border-radius: 999px;
            cursor: pointer;
            transition: transform 0.15s ease, box-shadow 0.15s ease;
        }

        .composer button:hover,
        .composer button:focus {
            transform: translateY(-1px);
            box-shadow: 0 8px 16px rgba(19, 60, 130, 0.18);
        }

        .empty-state {
            text-align: center;
            color: rgba(19, 60, 130, 0.6);
            margin-top: auto;
        }

        @media (max-width: 600px) {
            .chat-shell {
                padding: 1.5rem 1rem 2rem;
            }

            h1 {
                font-size: 1.6rem;
            }
        }
    </style>
</head>
<body>
<main class="chat-shell">
    <h1>GRETL Copilot</h1>
    <div id="conversation"
         class="conversation"
         hx-ext="sse"
         sse-connect="/ui/chat/stream/${clientId}"
         sse-swap="beforeend"
         sse-target="#conversation">
        <div class="empty-state" id="empty-state">Start a conversation to see responses from the GRETL copilot.</div>
    </div>
    <form class="composer"
          hx-post="/ui/chat/messages"
          hx-target="#conversation"
          hx-swap="beforeend"
          hx-on::after-request="this.reset(); htmx.find('#message').focus();">
        <input type="hidden" name="clientId" id="clientId" value="${clientId}">
        <label for="message">Your request</label>
        <textarea id="message" name="message" placeholder="Ask for GRETL tasks, explanations, or new ideas." required></textarea>
        <button type="submit">Send</button>
    </form>
</main>
<script src="https://unpkg.com/htmx.org@2.0.8"></script>
<script src="https://cdn.jsdelivr.net/npm/htmx-ext-sse@2.2.4/sse.min.js"></script>
</body>
</html>
