# AI Cover Letter Generator

Generates personalized, ATS-optimized cover letters by combining a candidate's resume with the most relevant sections of a job description, using a RAG pipeline and Claude.

## How it works

1. **Upload** resume (PDF) + job description (text or PDF), plus candidate and role details
2. **Parse & Chunk** — JD text is extracted and split into overlapping chunks
3. **Embed & Retrieve** — chunks are embedded (OpenAI `text-embedding-ada-002`) and ranked by cosine similarity against the resume, so only the most relevant JD sections are used
4. **Generate** — Claude writes the cover letter using the candidate's real achievements and the JD's exact language (for ATS keyword matching), in your chosen tone
5. **Export** — download the finished letter as a PDF

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2 (Web, WebFlux, Data JPA, Validation)
- **AI:** Claude API (Anthropic) for generation, OpenAI Embeddings API for semantic retrieval
- **Database:** MySQL
- **PDF:** Apache PDFBox (parsing), PDF export for the final letter
- **Frontend:** Thymeleaf, HTML/CSS
- **Build:** Maven

## Features

- Resume + JD upload (PDF or pasted text)
- Tailored, JD-aware cover letters (not generic templates)
- Tone selection: **Professional**, **Enthusiastic**, **Concise**
- Optional hiring manager name for a personalized greeting
- Downloadable PDF export
- History of all generated letters, viewable by ID

## Setup

### Prerequisites
- Java 17+
- Maven
- MySQL running locally
- Claude API key from [console.anthropic.com](https://console.anthropic.com)
- OpenAI API key from [platform.openai.com](https://platform.openai.com)

### Configuration

```bash
export CLAUDE_API_KEY=your_actual_key
export OPENAI_API_KEY=your_actual_key
export DB_PASSWORD=your_mysql_password
```

Reference them in `application.properties`:
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

App runs at `http://localhost:8081`

## Project Structure

```
src/main/java/com/shalini/coverletter/
├── controller/    # CoverLetterController — generate, history, view, download routes
├── service/       # PdfParserService, ChunkingService, EmbeddingService, CoverLetterAgentService, PdfExportService, CoverLetterService
├── model/         # CoverLetter, Chunk
└── repository/    # CoverLetterRepository (Spring Data JPA)
```

## Notes

This project is part of a two-tool **AI Career Toolkit**, alongside the [Resume JD Analyzer](../resume-jd-analyzer).
