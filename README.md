# Gemini Chat - Multi-User AI Application

Production-quality multi-user AI chat application built with Kotlin, Jetpack Compose, Firebase Firestore, Firebase Authentication (Google Sign-In), Firebase App Check, and the Firebase AI Logic SDK (`Firebase.ai`) for the Gemini 3 API.

## Architecture & Stack
- **UI & Framework**: 100% Jetpack Compose with Material 3 theming (responsive three-pane design: conversation sidebar, chat viewport, and collapsible sources/context drawer; dark & light modes).
- **AI Backend**: Firebase AI Logic (`Firebase.ai`) backed by Google Cloud App Check attestation. All requests route securely through the backend without exposing raw keys.
- **Data & Multi-Tenancy**: Cloud Firestore Enterprise edition with zero-trust security rules strictly scoped to `request.auth.uid`.
- **Authentication**: Jetpack Credential Manager with Google Sign-In (`GetSignInWithGoogleOption`). Multi-user isolation ensures accounts never see each other's data.

## Key Features

1. **Streaming Chat**: Real-time token streaming with visible typing indicator and working Stop button to abort mid-stream.
2. **Model Registry & Reasoning Controls**:
   - **Fast** (`gemini-3.5-flash-lite`): Low latency, quick responses & summaries.
   - **Balanced** (`gemini-3.8-flash`): Default versatile frontier-class model.
   - **Deep** (`gemini-4-argon`): Advanced reasoning, complex logic & code.
   - Per-conversation temperature slider, system instructions, and token/character counters.
3. **Markdown & Code Rendering**:
   - Syntax-highlighted code blocks with 1-tap Copy button.
   - Markdown tables, headers, and bullet formatting.
4. **Message Actions**:
   - Copy message text.
   - Edit-and-resend.
   - Regenerate response.
   - Branch conversation into a new thread.
   - Delete message.
5. **Multimodal Input**:
   - Zero-permission Android Photo Picker for image selection.
   - Camera preview capture.
   - Attachment preview thumbnails with remove buttons.
6. **Projects & Knowledge Context**:
   - Create named project containers with custom instructions, default model, and knowledge base notes.
   - Threads inside a project inherit instructions and knowledge automatically.
7. **Tools & Grounding**:
   - Toggle chips for Web Search Grounding, Code Execution, and Image Generation.
   - Grounded responses display interactive source citation chips and side panel cards.
8. **Quotas & Cost Control**:
   - Free tier daily message cap (50 messages/day) tracked in Firestore with friendly "resets in Xh" display.
   - "Use my own API key": Users can optionally enter their personal Gemini API key in Settings for unlimited usage.
9. **Workspace Connectors**:
   - Connections overview for Google Calendar, Drive, Docs, Gmail, and Sheets via Google Sign-In.
