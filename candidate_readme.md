# Backend Engineering Intern — Take-Home Assessment

Welcome, and thank you for your interest in joining our team. This assessment is designed to take **3–5 hours**. We are not looking for perfection — we are looking for how you think, how you work, and how you collaborate with AI tools.

---

## Overview

You will build a small Java 17 REST API for a **personal task manager**, using an AI agentic model as your primary development tool. When you submit, you will include both your code and a transcript of your conversation with the model.

The transcript is as important as the code. We want to see how you direct the model, evaluate its output, and recover when things go wrong.

---

## Requirements

### Technical stack
- **Java 17**
- **Spring Boot** (latest stable)
- **Maven or Gradle** (your choice)
- **H2 in-memory database** (no external database setup required)

### Your API must include

**1. Task CRUD endpoints**

A `Task` has at minimum:
- `id` (auto-generated)
- `title` (string, required)
- `description` (string, optional)
- `dueDate` (date)
- `priority` (LOW / MEDIUM / HIGH)
- `status` (TODO / IN_PROGRESS / DONE)

Implement the following endpoints:

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/tasks` | Create a new task |
| `GET` | `/tasks` | List all tasks |
| `GET` | `/tasks/{id}` | Get a single task |
| `PUT` | `/tasks/{id}` | Update a task |
| `DELETE` | `/tasks/{id}` | Delete a task |

**2. At least one AI-powered endpoint**

Integrate a call to an AI model (Claude, GPT-4, Gemini, or similar) within your API. You may choose your approach — here are some ideas to get you started, but you are welcome to come up with your own:

- `POST /tasks/suggest` — accepts a plain-language description (e.g., `"remind me to submit the quarterly report before Friday"`) and returns a structured task object
- `POST /tasks/{id}/summarize` — returns a plain-language summary or explanation of a task
- `POST /tasks/{id}/breakdown` — returns a suggested list of subtasks for a complex task

Your AI-powered endpoint should return well-structured JSON. It does not need to persist anything — a stateless call to the model is fine.

**3. A simple UI**

Include a minimal frontend that allows a reviewer to interact with your API without using a REST client. It does not need to be polished or feature-complete — a basic HTML page is fine. It should support:

- Viewing the list of tasks
- Creating a new task
- Triggering your AI-powered endpoint and displaying the result

Styling and aesthetics are not evaluated. The UI exists purely to make your project easier to explore.

**4. Basic tests**

Include a test suite that covers the core behavior of your API. At minimum:

- At least one unit test per service-layer method (happy path)
- At least one integration test that starts the Spring context and exercises each CRUD endpoint end-to-end
- At least one test for your AI-powered endpoint (mocking the external model call is fine and expected)

Tests must pass when running:

```bash
./mvnw test
```

or

```bash
./gradlew test
```

**4. A working README**

Your README (separate from this document) must allow a reviewer to clone your repo, run a single command, and have the API running locally. Include:
- Setup instructions
- How to run the project
- A description of your AI-powered endpoint and example request/response

### Build requirement

Your project must build and start with a single command, for example:

```bash
./mvnw spring-boot:run
```

or

```bash
./gradlew bootRun
```

We will run this command cold, with no prior setup beyond having Java 17 and internet access.

---

## What You Do Not Need to Build

- Authentication or authorization
- A production-grade database
- High test coverage beyond the basics described above
- Deployment configuration

---

## Using AI

You must use an AI agentic model as your **primary builder** for this project — not just as an assistant or autocomplete. This is the most important skill we are evaluating.

What we mean by agentic use: you should be directing the model to take on substantial, multi-step work — asking it to scaffold the project, implement entire features, debug its own output, and iterate based on your feedback. Think of yourself as the architect and reviewer, and the model as the engineer doing the building. Your job is to give it clear direction, evaluate what it produces, and guide it toward a working result.

Tools that support this style of working well include [Claude](https://claude.ai), [Cursor](https://cursor.sh), [ChatGPT](https://chat.openai.com), and [Gemini](https://gemini.google.com). Any tool that allows multi-turn, context-aware conversations with a model is acceptable.

What we are looking for in your transcript: evidence that you used the model to drive real construction work, not just to answer one-off questions. A strong transcript will show the model generating code, you reviewing and redirecting it, the model fixing problems, and so on — a genuine back-and-forth build session.

### Capturing your transcript

Before you start, open a conversation in your AI tool of choice and keep it running for the duration of the project. At submission time, export or copy the full conversation.

- **Claude:** Use [claude.ai](https://claude.ai). At the end of your session, copy the full conversation from the browser or use the export feature if available.
- **ChatGPT:** Use [chat.openai.com](https://chat.openai.com). You can share a conversation link or copy the text.
- **Cursor / Copilot / other tools:** Copy the chat panel contents, or take a sequential export if the tool supports it.

Submit your transcript as a plain text or Markdown file alongside your code. If your tool does not support export, a copy-paste of the full conversation is fine.

---

## Submission

Please submit through the Greenhouse link that was part of the email you received this README from.

**Deadline:** 3 business days from receiving this README.

---

## What We Are Looking For

The most important thing we evaluate is **how you use the AI model to build**. We want to see you directing it through real construction work — not just asking it questions or using it for autocomplete. Your transcript is the primary window into this, and we read it carefully.

Beyond that, we care about: whether your project builds and runs correctly, whether your Java is clean and well-structured, whether your tests are meaningful, and whether your README gives us everything we need to run it cold.

The code matters — but a candidate who shows strong, intentional agentic use with a rougher project will always be more interesting to us than one with polished code and a thin transcript.

---

## A Note on What "Good" Looks Like

A strong submission is not necessarily one with the most features or the cleanest code. We are looking for candidates who:

- Direct the AI with clear, thoughtful prompts
- Push back or ask for clarification when the model produces something unclear or wrong
- Understand the code they submit — even if the model helped write it
- Make deliberate choices and can explain them in their README

A transcript showing one debugging session where you identified a flaw in the model's suggestion, corrected it, and explained why — is worth more to us than a polished project with no visible thinking.

---

## Questions

If anything in this document is unclear, please reach out to angelo.veres@eulerity.com before starting. We would rather answer a question upfront than have you blocked or heading in the wrong direction.

Please make sure to not include your AI API keys in your submission. We will not use your key even if you forgot to remove it.

Good luck — we look forward to reading your submission.
