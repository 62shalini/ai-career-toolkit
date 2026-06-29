# Resume JD Analyzer

An AI-powered tool that analyzes how well a resume matches a job description using a real Retrieval-Augmented Generation (RAG) pipeline — and gives honest, actionable feedback powered by Claude.

## How it works

1. **Upload** your resume (PDF) and a job description (paste text or upload PDF)
2. **Parse** — Apache PDFBox extracts raw text from PDFs
3. **Chunk** — the JD is split into overlapping, paragraph-aware chunks
4. **Embed & Retrieve** — each chunk is embedded with OpenAI's `text-embedding-ada-002`; cosine similarity retrieves the JD sections most relevant to the resume
5. **Analyze** — the resume + retrieved JD context are sent to Claude (Anthropic API) with a structured prompt
6. **Result** — a match score (0–100), match level, matched/missing skills, specific resume rewrite suggestions, and an honest summary

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2 (Web, WebFlux, Data JPA, Validation)
- **AI:** Claude API (Anthropic) for analysis, OpenAI Embeddings API for semantic retrieval
- **Database:** MySQL
- **PDF Parsing:** Apache PDFBox
- **Frontend:** Thymeleaf, HTML/CSS
- **Build:** Maven

## Features

- Upload resume + JD (text or PDF)
- Semantic (RAG-based) matching — not just keyword scanning
- Match score with breakdown: matched skills, missing skills
- AI-generated resume rewrite suggestions tailored to the JD
- Analysis history, saved to MySQL

## Setup

### Prerequisites
- Java 17+
- Maven
- MySQL running locally
- A Claude API key from [console.anthropic.com](https://console.anthropic.com)
- An OpenAI API key from [platform.openai.com](https://platform.openai.com)

### Configuration

Set your credentials as environment variables instead of editing `application.properties` directly:

```bash
export CLAUDE_API_KEY=your_actual_key
export OPENAI_API_KEY=your_actual_key
export DB_PASSWORD=your_mysql_password
```

Then update `application.properties` to reference them:
```properties
claude.api.key=${CLAUDE_API_KEY}
openai.api.key=${OPENAI_API_KEY}
spring.datasource.password=${DB_PASSWORD}
```

### Run

```bash
mvn clean install
mvn spring-boot:run
```

App runs at `http://localhost:8080`

## Project Structure

```
src/main/java/com/shalini/resumeanalyzer/
├── controller/    # AnalyzerController — handles upload, analysis, history routes
├── service/       # PdfParserService, ChunkingService, EmbeddingService, AgentService, AnalyzerService
├── model/         # AnalysisResult, Chunk
└── repository/    # AnalysisResultRepository (Spring Data JPA)
```

## Notes

This project is part of a two-tool **AI Career Toolkit**, alongside the [Cover Letter Generator](../cover-letter-generator).
