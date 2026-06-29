# AI Career Toolkit

An AI-powered career toolkit with two integrated Spring Boot applications, built on a full Retrieval-Augmented Generation (RAG) pipeline powered by the **Claude API**. Designed to help job seekers tailor their resumes and generate personalized cover letters for any job description.

## Tools

### 1. [Resume-JD Analyzer](./resume-jd-analyzer)
Analyzes how well your resume matches a target job description. Performs semantic matching (not just keyword scanning), flags skill gaps, and suggests specific resume rewrites — with an honest match score and summary.

### 2. [Cover Letter Generator](./cover-letter-generator)
Generates a personalized, ATS-optimized cover letter for a specific company and role, written in the JD's own language, with tone selection and downloadable PDF export.

## Shared Architecture

Both tools are built around the same core pipeline:

```
PDF Upload → Text Extraction (PDFBox) → Chunking → Embedding (OpenAI) 
→ Semantic Retrieval (cosine similarity) → Claude API → Structured Output
```

This is genuine RAG, not a single static prompt: each job description is chunked and embedded, and only the most relevant sections (by cosine similarity to the resume) are passed to Claude. This keeps the AI's output focused and grounded in the actual JD content, rather than the entire document.

| | Resume-JD Analyzer | Cover Letter Generator |
|---|---|---|
| **Port** | 8080 | 8081 |
| **Input** | Resume + JD | Resume + JD + candidate/role details |
| **AI Output** | Match score, skill gaps, rewrite suggestions | Full cover letter text |
| **Export** | — | PDF download |
| **Storage** | MySQL (analysis history) | MySQL (letter history) |

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2
- **AI:** Claude API (Anthropic) for generation/analysis, OpenAI Embeddings API for semantic retrieval
- **Database:** MySQL (Spring Data JPA / Hibernate)
- **PDF Handling:** Apache PDFBox
- **Frontend:** Thymeleaf, HTML/CSS
- **Build:** Maven

## Getting Started

Each tool is a standalone Spring Boot app and can be run independently. See the individual READMEs for setup:

- [Resume-JD Analyzer setup](./resume-jd-analyzer/README.md)
- [Cover Letter Generator setup](./cover-letter-generator/README.md)

Both require a **Claude API key** ([console.anthropic.com](https://console.anthropic.com)) and an **OpenAI API key** ([platform.openai.com](https://platform.openai.com)) for embeddings, plus a local MySQL instance.

## Security Note

Real API keys and database passwords are never committed to this repository. Configuration files use placeholder values — set your actual credentials via environment variables when running locally (see setup instructions in each tool's README).

## Author

**Shalini R**
[LinkedIn](#) · [GitHub](#)
