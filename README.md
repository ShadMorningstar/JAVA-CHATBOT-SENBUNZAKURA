# Senbunzakura AI — Professional Desktop AI Companion

Senbunzakura AI is a high-performance, highly responsive desktop AI companion application built using **JavaFX**, **MySQL**, and the **Groq API** (utilizing the advanced `llama-3.3-70b-versatile` model inference). Featuring a futuristic dark-cyberpunk aesthetic, this application goes beyond simple conversational bots by integrating local persistence layers, intelligent context state reloading, asynchronous multi-threaded networking bindings, dynamic media rendering pipelines, and an automated voice tracking interface.

## 🚀 Core Features

- **Advanced Multi-Session History Tracking:** Implements a fully managed sidebar navigation component allowing users to initialize, swap, cache, and drop independent conversation threads seamlessly backed by transaction-safe relational database queries.
- **Intelligent Permanent Memory Layer:** Automatically intercepts and parses user intents targeting long-term information retention (triggered by implicit inputs like *remember, memorize, yaad rakhna*). These updates are securely written to a dedicated persistence layer and dynamically stream-injected as system configuration prompts on sequential requests.
- **Asynchronous Execution Architecture:** Leverages Java's `CompletableFuture` asynchronous pipeline to offload intensive remote REST interactions into secondary background worker frames, completely isolating the main JavaFX Application Thread from UI freezes or display stuttering.
- **Dynamic Media Synthesis Pipeline:** Intercepts structural slash commands (`/draw [prompt]` or `/image [prompt]`) to route text parameters through an encoded image-generation API routing mechanism (Pollinations AI), embedding high-fidelity visual canvases natively inside custom rounded UI graphic cards.
- **Smart Voice-Tracking & Auto-Send Engine:** Features an intelligent background thread text-mutation listener designed to monitor voice-typing streams (such as native Windows Dictation `Win + H` triggers). Implements a 1.5-second absolute quiet threshold matrix to automatically fire the execution trigger once the user stops speaking, bypassing manual clicks.
- **High-Fidelity Cyberpunk Theme UI:** Tailored with a custom, high-fidelity dark neon layout template utilizing deep canvas colors (`#0d0e15`), structured containment blocks (`#1a1c29`), vivid electric blue (`#00f0ff`), and cyberpunk pink (`#ff2a5f`) visual accents with responsive clip-masks and built-in clipboard copying mechanics.

## 🛠️ System Architecture & Tech Stack

- **Frontend User Interface:** JavaFX 17+ (Constructed with composite structural components like `VBox`, `HBox`, `ScrollPane`, and custom clip nodes).
- **Backend Communication Engine:** Java 11+ Native HTTP Client architecture (`java.net.http.*`).
- **Language Model Inference:** Groq Cloud Systems Ecosystem (`llama-3.3-70b-versatile`).
- **Data Persistence Layer:** MySQL Server Enterprise/Community 8.0+ driven by `mysql-connector-j`.
- **Image Generation Framework:** Pollinations AI REST endpoint bindings.

## 🗄️ Database Schema Mapping

The persistence layer guarantees seamless environment bootstrapping on initialization. The utility manager automatically maps and provisions the following relational schemas:

1. **`important_memory`**: Handles unique key-value associative metadata pairs to persistently buffer context flags directly to the AI core logic.
2. **`chat_sessions`**: Tracks and sequences chat payload strings mapped against transactional session tokens, dynamic speaker metrics (`YOU` / `Senbunzakura`), content payloads, and creation timestamps.

```sql
CREATE TABLE IF NOT EXISTS important_memory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    info_key VARCHAR(250) UNIQUE,
    info_val TEXT
);

CREATE TABLE IF NOT EXISTS chat_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(50),
    sender VARCHAR(50),
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
